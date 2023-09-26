package com.decosegfault.hermes;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.decosegfault.hermes.data.RouteData;
import com.decosegfault.hermes.data.TripData;
import com.decosegfault.hermes.types.SimType;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.ShapePoint;

import org.tinylog.Logger;

/**
 * @author Lachlan Ellis
 */
public class RouteHandler {
    public static SimType simType;

    static Map<String, RouteData> routes = new HashMap<String, RouteData>();

    static Map<String, TripData> tripsByRoute = new HashMap<String, TripData>();
    static Map<String, TripData> tripsByShape = new HashMap<String, TripData>();

    static Map<String, TripData> tripsbyID = new HashMap<>();

    //arbitrary speed lol
    public static float vehicleSpeed = 5;


    public static void addRoute(Route route) {
        RouteData newRoute = new RouteData(route.getType());
        newRoute.routeID = route.getShortName();
        newRoute.routeName = route.getLongName();

        routes.put(route.getId().getId(), newRoute);
    }

    //debug
    static boolean added = false;
    public static void addTrip(Trip trip) {
        //debug
//        if(!added) {
//            added = true;
            TripData newTrip = new TripData(routes.get(trip.getRoute().getId().getId()).routeType);
            newTrip.routeID = trip.getId().getId();
            newTrip.routeName = trip.getTripHeadsign();


            //check this later to make sure it's not a deep copy, if it is I will be sad
            tripsByShape.put(trip.getShapeId().getId(), newTrip);
            tripsByRoute.put(trip.getRoute().getId().getId(), newTrip);
            tripsbyID.put(trip.getId().getId(), newTrip);
//            Logger.warn("Added at: {}", trip.getId().getId());
//        }
    }

    public static void addShape(ShapePoint point) {
        if(tripsByShape.containsKey(point.getShapeId().getId())) {
            tripsByShape.get(point.getShapeId().getId()).routeMap.add(
                new Vector3((float) point.getLat(), (float) point.getLon(), point.getSequence()));
        }

    }

    public static void handleTime(StopTime time) {
        if(tripsbyID.containsKey(time.getTrip().getId().getId())) {
            TripData trip = tripsbyID.get(time.getTrip().getId().getId());
//            Logger.warn("Editing trip ID: {}", time.getTrip().getId().getId());
            if(time.getStopSequence() == 1) {
                trip.startTime = time.getDepartureTime();
//                Logger.warn("Start time: {}", time.getDepartureTime());
            }
            if(time.getArrivalTime() > trip.endTime) {
                trip.endTime = time.getArrivalTime();
//                Logger.warn("End time: {}", time.getArrivalTime());
            }
        }
    }

    public static void sortShapes() {
        for (TripData element : tripsByShape.values()) {
            element.routeMap.sort((a, b) -> Float.compare(a.z, b.z));
            for(int i = 0; i < element.routeMap.size(); i++) {
                element.routeMap.get(i).z = 0;
            }
        }
    }

    /** testing only */
    public static void logRoutes() {
        for (RouteData element : routes.values()) {
            Logger.info(element.routeName + " : " + element.routeID + " : " + element.routeType);
        }
    }

    public static void logTrips() {
        for (TripData trip : tripsByShape.values()) {
            Logger.info(trip.routeName + " : " + trip.routeID);
        }
    }

    public static void logShapes() {
        for (TripData trip : tripsByShape.values()) {
            for (Vector3 vector : trip.routeMap) {
                Logger.info(vector.x + "+" + vector.y + " : " + vector.z + " : " + trip.routeName);
            }
        }
    }

    public static void initTrips() {
        for (TripData trip : tripsByShape.values()) {
            trip.initTrip();
        }
    }
}
