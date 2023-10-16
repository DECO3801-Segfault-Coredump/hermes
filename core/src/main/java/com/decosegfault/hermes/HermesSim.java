package com.decosegfault.hermes;

import java.util.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.decosegfault.hermes.data.VehicleData;
import com.decosegfault.hermes.frontend.FrontendData;
import com.decosegfault.hermes.frontend.FrontendEndpoint;
import com.decosegfault.hermes.frontend.FrontendServer;
import com.decosegfault.hermes.types.SimType;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.*;
import org.tinylog.Logger;
import com.decosegfault.atlas.render.AtlasVehicle;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import com.decosegfault.hermes.data.TripData;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Lachlan Ellis
 * @author Matt Young
 * @author Henry Batt
 * @author Cathy Nguyen
 */
public class HermesSim {

    private static FrontendServer server = new FrontendServer();
    public static ConcurrentHashMap<String, AtlasVehicle> vehicleMap = new ConcurrentHashMap<>();
    /** time of day will be in seconds, max 86400 (one day) before looping back to 0 */
    public static double time = 44100;
//    static float baseTime = 44100;
    static float speed = 10f;
    public static final List<TripData> vehiclesToCreate = new ArrayList<>();
    public static LiveDataFeed liveDataFeed = new LiveDataFeed();

    /**
     * ticks time by x seconds.
     * tick speed set by end user.
     * in history mode, interpolates movement based on start and end time and path length.
     * in live mode, tick does nothing.
     * in sim mode, moves vehicles at a set speed based on tick speed.
     */
    public static void tick(float delta) {
        if (System.getProperty("nohermes") != null) return;
        time += (delta * speed);
//        Logger.warn("Time: {} {}", floatTime, time);
//        Logger.warn("tell me your mf length {}", vehicleMap.size());
        int tripsActive = 0;

        vehiclesToCreate.clear();

        if (RouteHandler.simType == SimType.LIVE) {
            liveDataFeed.update();
            // CHECK IF THIS WORKS?
            for (Map.Entry<String, VehicleData> entry : liveDataFeed.vehicleDataMap.entrySet()) {
                var vehicle = AtlasVehicle.Companion.createFromHermes(entry.getValue().vehicleType);
                vehicleMap.put(entry.getKey(), vehicle);
            }
        } else {
            RouteHandler.tripsbyID.values().stream().forEach((trip) -> {
                trip.tick();
                if (trip.vehicle.hidden && vehicleMap.containsKey(trip.routeID)) {
                    vehicleMap.remove(trip.routeID);
                } else if (!trip.vehicle.hidden && !vehicleMap.containsKey(trip.routeID)) {
                    if (trip.vehicle.vehicleType == null) {
                        Logger.warn("Null trip vehicle! {} {}", trip.routeName, trip.routeID);
                        return;
                    }
                    vehiclesToCreate.add(trip);
                }
            });
        }
        // create vehicles - this has to be here because otherwise it breaks libGDX
        // the other way would be to have a "creating vehicle" lock
        for (TripData trip : vehiclesToCreate) {
            var vehicle = AtlasVehicle.Companion.createFromHermes(trip.vehicle.vehicleType);
            vehicleMap.put(trip.routeID, vehicle);
        }

        // parallelising this currently breaks everything
        vehicleMap.entrySet().stream().forEach((tripID) -> {
            TripData trip = RouteHandler.tripsbyID.get(tripID.getKey());
            tripID.getValue().updateTransform(
                new Vector3((float) trip.vehicle.position.getX(), (float) trip.vehicle.position.getY(), (float) trip.vehicle.position.getZ()));
            tripID.getValue().setHidden(trip.vehicle.hidden);
        });

//        Logger.warn("Trips Loaded: {}, {} active", vehicleMap.size(), tripsActive);
        // transmit data to the frontend
        FrontendData data = new FrontendData();
        data.setRouteLongName("fuck you");
        FrontendEndpoint.broadcast(data);
    }

    /**
     * load vehicle routes here.
     * in history mode, routes follow preset start and end times.
     * in live mode, routes aren't loaded at all; uses live data.
     * in sim mode, routes only use their first available start time.
     */
    public static void load(SimType simType) {
        if (System.getProperty("nohermes") != null) {
            Logger.warn("Skipping Hermes load, -Dnohermes=true");
            return;
        }

        RouteHandler.simType = simType;
        if (RouteHandler.simType == SimType.LIVE) {
            read();
            return;
        } else {
            read();
        }

        //placeholder
        RouteHandler.sortShapes();
        RouteHandler.initTrips();
        //RouteHandler.logRoutes();
//        RouteHandler.logTrips();
        //RouteHandler.logShapes();
        Logger.warn("Trips Loaded: {}", RouteHandler.tripsbyID.size());
        Logger.info("Linking Hermes-Atlas vehicles");
//	    for (TripData trip : RouteHandler.tripsbyID.values()) {
//	        if (trip.vehicle == null || trip.vehicle.vehicleType == null) {
//                Logger.warn("Null trip vehicle! {} {}", trip.routeName, trip.routeID);
//                continue;
//            }
//            var vehicle = AtlasVehicle.Companion.createFromHermes(trip.vehicle.vehicleType);
//            vehicleMap.put(trip.routeID, vehicle);
//
//        }
        Logger.info("GTFS Data Loaded");
        Logger.info("Starting frontend server");
        server.start();
    }

    public static void read()  {
        GtfsReader reader = new GtfsReader();
        try {
            // gtfs.zip is internal, extract it to /tmp so that the file reader can read it
            FileHandle tmpPath = Gdx.files.absolute(System.getProperty("java.io.tmpdir") + "/DECOSegfault_hermes_gtfs.zip");
            Logger.info("Copying Hermes gtfs.zip to " + tmpPath.path() + "..." + System.getProperty("java.io.tmpdir"));

            FileHandle gtfsZip = Gdx.files.internal("assets/hermes/gtfs.zip");
            Logger.info("Copying Hermes gtfs.zip 2 to " + gtfsZip.file().getAbsolutePath());
            gtfsZip.copyTo(tmpPath);

            reader.setInputLocation(tmpPath.file());
        } catch (IOException noFile) {
            throw new IllegalArgumentException(noFile);
        }

        /**
         * You can register an entity handler that listens for new objects as they
         * are read
         */
//        reader.addEntityHandler(new GtfsEntityHandler());

        /**
         * Or you can use the internal entity store, which has references to all the
         * loaded entities
         */
        GtfsDaoImpl store = new GtfsDaoImpl();
        reader.setEntityStore(store);

        try {
            reader.run();
        } catch (IOException noFile) {
            throw new IllegalArgumentException("uh oh");
        }

        // Access entities through the store
        Map<AgencyAndId, Route> routesById = store.getEntitiesByIdForEntityType(
            AgencyAndId.class, Route.class);

        Map<AgencyAndId, Trip> tripsById = store.getEntitiesByIdForEntityType(
                AgencyAndId.class, Trip.class);

        Map<AgencyAndId, ShapePoint> shapesById = null;
        Map<AgencyAndId, StopTime> stopTimesById = null;

        if (RouteHandler.simType != SimType.LIVE) {
            shapesById = store.getEntitiesByIdForEntityType(
                AgencyAndId.class, ShapePoint.class);

            stopTimesById = store.getEntitiesByIdForEntityType(
                AgencyAndId.class, StopTime.class);
        }

        for (Route element : routesById.values()) {
            RouteHandler.addRoute(element);
        }

        for (Trip element : tripsById.values()) {
            RouteHandler.addTrip(element);
        }

        if (RouteHandler.simType != SimType.LIVE) {
            for (StopTime element : stopTimesById.values()) {
                RouteHandler.handleTime(element);
            }
            for (ShapePoint element : shapesById.values()) {
                RouteHandler.addShape(element);
            }
        }

        try {
            reader.close();
        } catch (IOException noFile) {
            throw new IllegalArgumentException("uh oh");
        }
    }

    public static void increaseSpeed() {
        speed *= 2;
    }

    public static void decreaseSpeed() {
        speed /= 2;
    }

    public static void shutdown() {
        server.stop();
    }

//    private static class GtfsEntityHandler implements EntityHandler {
//
//        public void handleEntity(Object bean) {
//            if (bean instanceof Route) {
//                RouteHandler.addRoute((Route) bean);
//            }
//            if (bean instanceof Trip) {
//                RouteHandler.addTrip((Trip) bean);
//            }
//        }
//    }
}

