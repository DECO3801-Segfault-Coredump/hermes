# Atlas Tile Renderer Design
- Matt Young.

## Introduction
This is the main part of the Atlas renderer. Its job is to render OpenStreetMap tiles as a 3D plane in LibGDX.

## Starting the tile server
The tile server is a Docker image, as detailed in docs/atlas_map_pipeline.md. Once it's been installed, Atlas
will attempt to automatically start the tile server. It will then verify the tile server is connected successfully.

If the tile server crashes, Atlas should throw an error dialogue and probably quit the game.

## Constructing the grid
TODO

## Fetching and caching tiles
Once the grid is constructed, Atlas will fetch tiles from the tile server using HTTP. To avoid blocking the
main thread, the HTTP fetch is done async  using `Gdx.net`.

The tile PNG will be downloaded from the server either using `Pixmap.downloadFromUrl` which uses `Gdx.net` 
and so is async. Once the tile has been fetched, the Pixmap will be converted into a Texture (which 
implies it is also uploaded to the GPU - more on that in a sec). This will then be stored in a class `Tile`
with `Vector3 position` and `Texture tex`.

Once the Tile has been created, it will be stored in a `TileCache`, which is basically a LRU cache of Tiles.
When we download a texture, we want to cache it - but if we keep downloading tiles forever, this implies _unlimited_
VRAM usage (or at least, however large as Brisbane is). To fix this, we will store a configurable number of
Tiles in the ring buffer. When the LRU cache is full, we evict the oldest Tile and call `dispose()` on it to
free the texture VRAM.

When we're trying to render a Tile, we locate it in the LRU cache. If it's in there, great, we use that texture.
Otherwise, we will re-download it from the tile server.

The cache will either use Guava's CacheBuilder or Caffeine: https://github.com/ben-manes/caffeine (probably
Caffeine due to its support of async loading, which makes sense for HTTP)

TODO: tile server should save generated PNGs to a permanent docker volume

TODO we should ideally pre-generate various zoom levels as well

## Scene optimisation
Tiles whose centroid is a configurable distance away from the camera will not be added to the render list.

Tiles where at least one of the four corners is not contained inside the camera's view frustum are also not
added to the render list (frustum culling).

Once these checks have been performed, the 
