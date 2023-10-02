package com.decosegfault.atlas.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import com.decosegfault.atlas.screens.SimulationScreen
import com.decosegfault.atlas.util.Assets.ASSETS
import com.decosegfault.atlas.util.AtlasUtils
import com.decosegfault.atlas.util.Triangle
import io.github.sebasbaumh.postgis.MultiPolygon
import io.github.sebasbaumh.postgis.PGgeometry
import io.github.sebasbaumh.postgis.Polygon
import ktx.collections.isNotEmpty
import net.mgsx.gltf.exporters.GLTFExporter
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import org.postgresql.PGConnection
import org.postgresql.geometric.PGpolygon
import org.tinylog.kotlin.Logger
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID
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

    private val exporter = GLTFExporter()

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

        // PostGIS requires lon/lat order, but we have lat/lon, hence we supply y,x
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

                // extract all polygons from PostGIS
                val pgPolys = mutableListOf<Polygon>()
                if (geometry.geometry is Polygon) {
                    pgPolys.add(geometry.geometry as Polygon)
                } else if (geometry.geometry is MultiPolygon) {
                    pgPolys.addAll((geometry.geometry as MultiPolygon).geometries)
                }

                // convert each PostGIS polygon into a libGDX Polygon
                val gdxPolygons = mutableListOf<com.badlogic.gdx.math.Polygon>()
                for (pgPolygon in pgPolys) {
                    val vertices = mutableListOf<Float>()
                    for (point in pgPolygon.coordinates) {
                        // note y/x ordering as PostGIS uses long/lat whereas we use lat/long
                        vertices.add(point.y.toFloat())
                        vertices.add(point.x.toFloat())
                    }
                    // now that we've unpacked the verts, we can make a polygon
                    val gdxPolygon = com.badlogic.gdx.math.Polygon(vertices.toFloatArray())
                    gdxPolygons.add(gdxPolygon)
                }

                val building = Building(id, gdxPolygons, floors)
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
    private fun extrudeToModel(modelBuilder: ModelBuilder, tris: List<List<Triangle>>, height: Float) {
        // iterate over each block of triangles and extrude this block on its own (which came from a
        // single polygon upstream)
        for ((i, triGroup) in tris.withIndex()) {
            // by this point, modelBuilder should have already called begin(), so we are OK to just add parts
            val mpb = modelBuilder.part(
                "building${tris.hashCode()}${height}$i", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.Normal.toLong()
                    or VertexAttributes.Usage.TextureCoordinates.toLong(),
                BUILDING_MATERIAL
            )

            // extrude each triangle into a prism
            for (tri in triGroup) {
                tri.extrudeUpToPrismMesh(mpb, height)
            }
        }
    }

    /**
     * Generates a building chunk. Buildings are packaged together into a ModelCache.
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun generateBuildingChunk(buildings: Set<Building>): ModelCache {
        val begin = System.nanoTime()
        val cache = ModelCache()
        cache.begin()

        // each chunk actually only consists of one model with multiple parts; each building is a part
        // we do it this way to reduce the number of times we have to block the main thread
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()

        for (building in buildings) {
            // calculate the triangulation of the floor plan
            // note DON'T translate the building, we centre it about the origin and translate the VERTICES
            val triangles = building.triangulate()

            // limit the height of buildings in case of errors, and if a height is missing give a default
            // height of 1 floor
            val height = MathUtils.clamp(building.floors.toFloat(), 1f, MAX_STOREYS) * STOREY_HEIGHT

            // extrude the triangulation into a 3D model and insert into cache
            extrudeToModel(modelBuilder, triangles, height)
        }

        // we have to call back to the main thread to upload the model and model instance to the GPU
        val modelFuture = CompletableFuture<ModelInstance>()
        val modelRunnable = Runnable {
            val model = modelBuilder.end() // FIXME: memory leak here (need to free model)

//            if (model.nodes.isNotEmpty())
//                exporter.export(model, Gdx.files.absolute("/tmp/atlas_model_${model.hashCode().toHexString()}.gltf"))

            modelFuture.complete(ModelInstance(model))
        }
        SimulationScreen.addWork(modelRunnable)
        // wait for the future to get back to us, and add it to the cache
        val instance = modelFuture.get()
        cache.add(instance)

        // we need to send this work (cache.end()) back to the main thread
        val cacheFuture = CompletableFuture<Boolean>()
        val cacheRunnable = Runnable {
            cache.end()
            cacheFuture.complete(true)
        }
        SimulationScreen.addWork(cacheRunnable)
        // wait for the cache to generate
        cacheFuture.get()

        Logger.debug("Processing buildings ${buildings.hashCode()} took ${(System.nanoTime() - begin) / 1e6} ms")
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
        private const val STOREY_HEIGHT = 4.3f * 3f

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

        private val BUILDING_MATERIAL = Material().apply {
//            set(PBRColorAttribute.createBaseColorFactor(Color.GRAY))
            set(PBRTextureAttribute.createBaseColorTexture(ASSETS["sprite/uvchecker1.png", Texture::class.java]))
        }
    }
}
