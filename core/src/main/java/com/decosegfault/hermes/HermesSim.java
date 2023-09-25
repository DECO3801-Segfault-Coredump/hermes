package com.decosegfault.hermes;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector3;
import com.decosegfault.hermes.data.VehicleData;
import com.decosegfault.hermes.types.SimType;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.*;
import org.tinylog.Logger;
import com.decosegfault.atlas.render.AtlasVehicle;

import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.gtfs.serialization.GtfsReader;
import com.decosegfault.hermes.data.RouteData;
import com.decosegfault.hermes.data.TripData;
import com.decosegfault.hermes.types.SimType;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;

import java.io.IOException;

/**
 * @author Lachlan Ellis
 * @author Matt Young
 */
public class HermesSim {

    public static Map<String, AtlasVehicle> vehicleMap = new HashMap<>();

    /** time of day will be in seconds, max 86400 (one day) before looping back to 0 */
    static int time;

    /**
     * ticks time by x seconds.
     * tick speed set by end user.
     * in history mode, interpolates movement based on start and end time and path length.
     * in live mode, tick does nothing.
     * in sim mode, moves vehicles at a set speed based on tick speed.
     */
    public static void tick() {
//        Logger.warn("tell me your mf length {}", vehicleMap.size());
        for (TripData trip : RouteHandler.tripsByShape.values()) {
            if(RouteHandler.simType == SimType.LIVE) {
                //trip.vehicle.tick(*position vector, z can be whatever*)
                //uhhh set the live data here lol
            } else if(RouteHandler.simType == SimType.HISTORY) {
//                for (TripData trip2LachlanEllisFixYourCode : RouteHandler.tripsByShape.values()) {
//	                trip2LachlanEllisFixYourCode.vehicle.tick(trip.routeMap.get(0));
//                }
//                Logger.warn("gimme your fuckin picec sejfbahjifbdiabdhs avjuo {} {}", trip.routeMap.get(0).x, trip.routeMap.get(0).y);
                trip.vehicle.tick(trip.routeMap.get(0));
            }
            //apply coordinate conversion function here
            vehicleMap.get(trip.routeID).updateTransformFromHermes(trip.vehicle.position);

        }

//        for (Map.Entry<String, AtlasVehicle> pair : vehicleMap.entrySet()) {
//            // TODO @Lachlan Ellis lookup vehicle position
//
//            var lakes = new Vector3(-27.499593094511493f, 153.01620933407332f, 0f);
//            pair.getValue().updateTransformFromHermes(lakes);
//        }
    }

    /**
     * load vehicle routes here.
     * in history mode, routes follow preset start and end times.
     * in live mode, routes aren't loaded at all; uses live data.
     * in sim mode, routes only use their first available start time.
     */
    public static void load(SimType simType) {
        RouteHandler.simType = simType;
        if(simType != SimType.LIVE) {
            read();
        }
        //placeholder
        RouteHandler.sortShapes();
        //RouteHandler.logRoutes();
//        RouteHandler.logTrips();
        //RouteHandler.logShapes();
        Logger.info("Linking Hermes-Atlas vehicles");
	    for (TripData trip : RouteHandler.tripsByShape.values()) {
	        if (trip.vehicle == null || trip.vehicle.vehicleType == null) {
                Logger.warn("Null trip vehicle! {} {}", trip.routeName, trip.routeID);
                continue;
            }
	        var vehicle = AtlasVehicle.Companion.createFromHermes(trip.vehicle.vehicleType);
	        vehicleMap.put(trip.routeID, vehicle);

        }
        Logger.info("GTFS Data Loaded");
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

        Map<AgencyAndId, ShapePoint> shapesById = store.getEntitiesByIdForEntityType(
            AgencyAndId.class, ShapePoint.class);

        for (Route element : routesById.values()) {
            RouteHandler.addRoute(element);
        }

        for (Trip element : tripsById.values()) {
            RouteHandler.addTrip(element);
        }

        for (ShapePoint element : shapesById.values()) {
            RouteHandler.addShape(element);
        }

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

