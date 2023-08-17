# Atlas Building Renderer Design
- Matt Young.

## Introduction
This document describes the pipeline that turns OpenStreetMap data into 3D buildings for Atlas.

## Notes
We will use the same Docker container for rendering, by connecting to the PostgresSQL database.

`psql -h localhost -p 5432 -U renderer gis` (password is also `renderer`)

Then we can use this Java library: https://github.com/sebasbaumh/postgis-java-ng

We may also be able to use this: https://osm2pgsql.org/examples/3dbuildings/

Otherwise, we should port OSMBuilding: https://github.com/Beakerboy/OSMBuilding/
