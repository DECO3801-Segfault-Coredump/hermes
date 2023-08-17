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
**TODO this will be necessary for buildings (unless we can pre generate it)**

## Serving tiles
Use the instructions here: https://switch2osm.org/serving-tiles/using-a-docker-container/

1. Create data volume: `docker volume create osm-data`
2. Create tile cache volume: `docker volume create osm-tiles`
3. Import data: `docker run -v /home/matt/workspace/deco3801/assets/mapdata/brisbane.osm.pbf:/data/region.osm.pbf -v osm-data:/data/database/ overv/openstreetmap-tile-server import`
   (you'll need to change paths for wherever you saved "brisbane.osm.pbf")
4. Serve tiles: `docker run -p 8080:80 -p 5432:5432 -e THREADS=16 -v osm-data:/data/database -v osm-tiles:/data/tiles -d overv/openstreetmap-tile-server run`
5. Access the tile server locally at `http://localhost:8080/` and zoom in on Brisbane

## Pre-rendering tiles
The map can be pretty slow on the first run. We can pre-render the tiles to save time. **Note, this is necessary for good UX**

1. Run `docker ps` to find the container name
2. Login to the container: `docker exec -it <name> /bin/bash`
3. Download the script: `cd /tmp && wget https://raw.githubusercontent.com/alx77/render_list_geo.pl/master/render_list_geo.pl && chmod +x render_list_geo.pl`
4. Generate tiles: `time ./render_list_geo.pl -n 32 -z 11 -Z 20 -y "-27.780999227328973" -Y "-26.899691691210972" -x "152.741860574895" -X "153.37445809991218"`

**Warning:** This will take a significant amount of time (1h on a very powerful desktop), consume all your CPU and
also use about 10 GB of disk space. I suggest running it overnight. It's worth it though, as it makes the map
much more responsive and reduces OSM CPU usage when running Atlas.

You should also change `-n 32` to however many CPU cores you have, I have 32 so I used that.

**notes**

min coord: -27.780999227328973, 152.741860574895
max coord: -26.899691691210972, 153.37445809991218\

note for some reason lat/long has to be swapped for this stupid ass script

`./render_list_geo.pl -n 32 -z 11 -Z 20 -y "-27.780999227328973" -Y "-26.899691691210972" -x "152.741860574895" -X "153.37445809991218"`

Source: https://github.com/Overv/openstreetmap-tile-server/issues/15
