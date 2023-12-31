# Atlas Building Renderer Design
- Matt Young.

## Introduction
This document describes the pipeline that turns OpenStreetMap data into 3D buildings for Atlas.

## Implementation
### Overview
Like the Atlas tile renderer, the building generator needs to be highly asynchronous. Because buildings
don't currently have textures (or if they do, share basically the same textures), we should be able to get
away with _never_ blocking the render thread.

The implementation faces two significant obstacles:
1. We would like to generate model caches for entire chunks, not just each building
2. If a building overlaps multiple chunks, it's ambiguous which chunk gets to process it. We would get duplicate buildings.

To fix issue 2, I plan to introduce a "first in, best dressed" scheme for building to chunk allocation. As
explained later, each chunk gets its own thread, and whichever thread "bags" a building first (using a
concurrent data structure to prevent race conditions), will get to process that building in that chunk for
as long as the chunk is kept in the chunk cache.

First, we will divide the map into fixed-size chunks using something like Henry's code. For example, we'll
divide the map into chunks at something like zoom level 16. So they'll be medium-sized.

In `AtlasSceneManager`, we call `BuildingManager#getBuildingsCulled` which will determine what chunks are
visible. We then call `GCBuildingChunkCache#retrieve`. If the building chunk is in the cache, we get a
`ModelCache` back which we can draw.

If the building chunk is not in the cache, the `GCBuildingChunkCache` will call `BuildingGenerator#generateBuildingChunk`.
Like `GCTileCache`, `GCBuildingChunkCache` is multi-threaded and each building chunk will be submitted to a
new thread on the executor. There will be `$nproc` threads.

### Constructing building geometry
The BuildingGenerator's main job is to query the PostGIS database, extrude the building geometry and assign
textures if possible. Let's break it down.

1. `generateBuildingChunk` calls `getBuildingsIn(rect: Rectangle)`
2. We convert Atlas coords to lat/long
3. We submit a query to the PostGIS database and ask for all buildings in this rectangle
    - At this point, we are in an executor belonging to `GCBuildingChunkCache`, so we are allowed to block
4. For each building:
    - We convert the building geometry back to Atlas coords
    - We compute the Delaunay triangulation of the building polygon
    - We extrude the vertices of each triangle based on the building height if present; otherwise a default height
    - We convert vertices into a model
    - (Optional) We assign textures to the model
5. We return the building models individually back to `generateBuildingChunk`
6. Building models are packaged back into a `ModelCache`
7. The `ModelCache` is sent back to the `GCBuildingChunkCache`, which is in turn sent to `BuildingManager`,
which is in turn drawn to the screen

## Notes
**PostGIS**

We will use the same Docker container for rendering, by connecting to the PostgresSQL database.

`psql -h localhost -p 5432 -U renderer gis` (password is also `renderer`)

Then we can use this Java library: https://github.com/sebasbaumh/postgis-java-ng

We may also be able to use this: https://osm2pgsql.org/examples/3dbuildings/

Otherwise, we should port OSMBuilding: https://github.com/Beakerboy/OSMBuilding/

Querying buildings using PostGIS: https://gis.stackexchange.com/a/460730

PostGIS queries: https://www.bostongis.com/PrinterFriendly.aspx?content_name=loading_osm_postgis

Projection type: If you open the PostGIS database in DBeaver, and go to Databases -> gis -> Schemas -> Public -> Views, 
click on `geometry_columns`, then click on Data, you'll see `srid` is 3857 which is Web Mercator. So the
database is entirely in Web Mercator coords, which is great for us. Sources:
- https://epsg.io/3857
- https://gis.stackexchange.com/a/22156

**Extrusion**

TODO

**Coordinates**

Centre of Brisbane in Atlas is: `-27.410786,153.01758`

Convert to EPSG 3857 Web Mercator (from WGS84 to Web Mercator):
https://epsg.io/transform#s_srs=4326&t_srs=3857&x=153.0175800&y=-27.4107860

Yields: `x=17033839.088019006, y=-3174888.4441493554`

Subtract these from the PostGIS database to centre the buildings

---

random building web mercator: `17042011.25221322 -3178371.2622345123`

actual in game: `27370.577 11667.356`

