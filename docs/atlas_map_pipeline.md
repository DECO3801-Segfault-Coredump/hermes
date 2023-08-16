# Atlas OSM Map Pipeline
- Matt Young.

## Introduction
This document contains the full pipeline for extracting map data from OpenStreetMap and importing it into
Atlas.

## Extracting Brisbane
1. Go to https://app.protomaps.com/downloads/osm
2. Draw bounding polygon for Brisbane
3. Click download to get a .osm.pbf (OpenStreetMap binary Protobuf file)

The extract I used for Brisbane is: https://app.protomaps.com/downloads/osm/ed673440-97cd-4a6e-a7ec-082c12616543

## Generating PostgresSQL database

## Serving tiles
Use the instructions here: https://switch2osm.org/serving-tiles/using-a-docker-container/

1. `docker volume create osm-data`
2. `docker run -v /home/matt/workspace/deco3801/assets/mapdata/brisbane.osm.pbf:/data/region.osm.pbf -v osm-data:/data/database/ overv/openstreetmap-tile-server import`
   (you'll need to change paths for wherever you saved "brisbane.osm.pbf")
3. `docker run -p 8080:80 -e THREADS=16 -v osm-data:/data/database -d overv/openstreetmap-tile-server run`
4. Access the tile server locally at `http://localhost:8080/` and zoom in on Brisbane
