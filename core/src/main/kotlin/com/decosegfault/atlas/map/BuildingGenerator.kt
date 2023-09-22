package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.decosegfault.atlas.screens.SimulationScreen
import com.decosegfault.atlas.util.AtlasUtils
import com.decosegfault.atlas.util.Triangle
import io.github.sebasbaumh.postgis.MultiPolygon
import io.github.sebasbaumh.postgis.PGgeometry
import io.github.sebasbaumh.postgis.Polygon
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import org.postgresql.PGConnection
import org.postgresql.geometric.PGpolygon
import org.tinylog.kotlin.Logger
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.CompletableFuture

/**
 * Class that handles the asynchronous generation of building models from OpenStreetMap PostGIS data.
 * For more info see docs/atlas_buildings_design.md.
 *
 * @author Matt Young
 */
class BuildingGenerator : Disposable {
    private val conn = DriverManager.getConnection(DB_URI, DB_USERPASS, DB_USERPASS).apply {
        with (this as PGConnection) {
            addDataType("geometry", PGgeometry::class.java)
            addDataType("polygon", PGpolygon::class.java)
        }
    }

    /** Prepared statement consisting of [BUILDING_QUERY] */
    private var statement = conn.prepareStatement(BUILDING_QUERY)

    init {
        Logger.info("Connected to PostGIS successfully! ${conn.metaData.databaseProductName} ${conn.metaData.databaseProductVersion}")
    }

    /**
     * Determines the OSM buildings that line in the bounding box from [min] to [max]. This will block as
     * it queries the DB. Coordinates of min/max are expected to be in Atlas coordinates and are internally
     * converted to WGS84 long/lat.
     * @return list of buildings
     */
    fun getBuildingsInBounds(min: Vector2, max: Vector2): MutableSet<Building> {
        val buildings = mutableSetOf<Building>()

        // convert to WGS84 lat/long
        val minWGS84 = AtlasUtils.atlasToLatLong(min)
        val maxWGS84 = AtlasUtils.atlasToLatLong(max)
//        Logger.debug("Min Atlas: $min, Min WGS84: $minWGS84 (lat/long)")
//        Logger.debug("Max Atlas: $max, Max WGS84: $maxWGS84 (lat/long)")

        // PostGIS requires lon/lat order, but we have lat/lon
        statement.setFloat(1, minWGS84.y)
        statement.setFloat(2, minWGS84.x)
        statement.setFloat(3, maxWGS84.y)
        statement.setFloat(4, maxWGS84.x)

        try {
            val result = statement.executeQuery()
            while (result.next()) {
                val id = result.getLong(1)
                val geometry = result.getObject(2) as PGgeometry
                val floors = result.getInt(3)
//                Logger.debug("ID: $id, geom: $geometry, floors: $floors")

                // convert PostGIS polygon to libGDX Polygon
                if (geometry.geometry !is Polygon) {
                    Logger.error("PostGIS gave us unsupported geometry: ${geometry.geometry} ${geometry.geometry?.javaClass}")
                    return hashSetOf()
                }
                val pgPolygon = geometry.geometry as Polygon
                val vertices = mutableListOf<Float>()
                for (point in pgPolygon.coordinates) {
                    // note y/x ordering as PostGIS uses long/lat whereas we use lat/long
                    vertices.add(point.y.toFloat())
                    vertices.add(point.x.toFloat())
                }

                // convert libGDX polygon to building (also convert from WGS84 lat/long to long/lat)
                val gdxPolygon = com.badlogic.gdx.math.Polygon(vertices.toFloatArray())
                val building = Building(id, gdxPolygon, floors)
                // convert in place to Atlas coords from WGS84
                building.toAtlas()
                buildings.add(building)
            }
        } catch (e: SQLException) {
            Logger.warn("Unable to query building in bounds, min: $min, max: $max, $e")
            Logger.warn(e)
        }

        return buildings
    }

    /**
     * Extrudes the given triangulated base of a building into a 3D model
     * @param height height in metres
     */
    private fun extrudeToModel(building: Building, tris: List<Triangle>, height: Float): Model {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()
        val mpb = modelBuilder.part(
            "building", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.Normal.toLong(),
            BUILDING_MATERIAL
        )

        // extrude each triangle into a prism
        for (tri in tris) {
            tri.extrudeUpToPrismMesh(mpb, height)
//            println(tri.dumpAsWgs84Wkt())
        }

        // we need to send this work (modelBuilder.end()) back to the main thread
        val future = CompletableFuture<Model>()
        val runnable = Runnable {
            // testing only (draw as bounding rectangle instead of polygon)
//            val rect = building.polygon.boundingRectangle
//            BoxShapeBuilder.build(mpb, rect.x, 0f, rect.y, rect.width, height, rect.height)
            future.complete(modelBuilder.end())
        }
        SimulationScreen.addWork(runnable)
        // wait for the future to get back to us
        return future.get()
    }

    /**
     * Generates a building chunk. Buildings are packaged together into a ModelCache.
     */
    fun generateBuildingChunk(buildings: Set<Building>): ModelCache {
        val begin = System.nanoTime()
        Logger.warn("Starting generating buildings ${buildings.hashCode()}")
        val cache = ModelCache()
        cache.begin()

        for (building in buildings) {
            // calculate the triangulation of the floor plan
            val triangles = building.triangulate()

            // limit the height of buildings in case of errors, and if a height is missing give a default
            // height of 1 floor
            val height = MathUtils.clamp(building.floors.toFloat(), 1f, MAX_STOREYS) * STOREY_HEIGHT

            // extrude the triangulation into a 3D model and insert into cache
            val model = extrudeToModel(building, triangles, height) // FIXME: memory leak here (need to free model)

            // once again, we have to call back to the main thread
            val future = CompletableFuture<ModelInstance>()
            val runnable = Runnable {
                future.complete(ModelInstance(model))
            }
            SimulationScreen.addWork(runnable)
            val instance = future.get()

            // we DON'T translate the building, we centre it about the origin and translate the VERTICES
            // this is kind of stupid, but it ensures accurate positioning

            // testing ONLY
//            val centroid = triangles[0].centroid()
//            instance.transform.translate(centroid.x, 0f, centroid.y)
//            instance.calculateTransforms()

//            Logger.debug("Adding building ${building.osmId} at centroid ${building.polygon.getCentroid(Vector2())}")
            cache.add(instance)
        }

        // we need to send this work (cache.end()) back to the main thread
        val future = CompletableFuture<Boolean>()
        val runnable = Runnable {
            cache.end()
            future.complete(true)
        }
        SimulationScreen.addWork(runnable)
        future.get()
        Logger.warn("Processing buildings ${buildings.hashCode()} took ${(System.nanoTime() - begin) / 1e6} ms")
        return cache
    }

    override fun dispose() {
        statement.close()
        conn.close()
    }

    companion object {
        /** PostGIS JDBC URI */
        private const val DB_URI = "jdbc:postgresql://localhost:5432/gis"

        /** PostGIS DB username & password */
        private const val DB_USERPASS = "renderer"

        /** WGS84 projection ID according to EPSG: https://epsg.io/3857 */
        private const val WGS84_ID = 4326

        /** Web Mercator projection ID according to EPSG: https://epsg.io/3857 */
        private const val WEB_MERCATOR_ID = 3857

        /** Approximate height in metres of one storey. Source: https://en.wikipedia.org/wiki/Storey#Overview */
        private const val STOREY_HEIGHT = 4.3f

        /**
         * The tallest building in Brisbane is currently the Brisbane Skytower which is 90 storeys tall.
         * We clamp building storeys to this many in case of OSM data misinputs so they're not mega tall.
         */
        private val MAX_STOREYS = 90f

        /**
         * Query to find polygons and tags for OSM buildings in a bounding box.
         * Coordinates **MUST be in WGS84 long/lat (IN THAT ORDER).**
         * Selects the polygon of the building ("way") and the number of levels if available. For more info see
         * the notes section of atlas_buildings_design.md.
         *
         * Original source: https://gis.stackexchange.com/a/460730 (updated by Matt)
         */
        private val BUILDING_QUERY = """SELECT osm_id, ST_Transform(way, $WGS84_ID), tags->'building:levels'
        |FROM planet_osm_polygon
        |WHERE (way && ST_Transform(ST_SetSRID(ST_MakeBox2D(ST_Point(?, ?), ST_Point(?, ?)), $WGS84_ID), $WEB_MERCATOR_ID))
        |AND building IS NOT NULL;
        """.trimMargin()

        private val BUILDING_COLOUR = Color.LIGHT_GRAY

        private val BUILDING_MATERIAL = Material().apply {
            set(PBRColorAttribute.createBaseColorFactor(BUILDING_COLOUR))
        }
    }
}
