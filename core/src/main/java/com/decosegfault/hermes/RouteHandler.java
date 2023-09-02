package com.decosegfault.hermes;

import java.util.HashMap;
import java.util.Map;

import com.decosegfault.hermes.data.RouteData;
import com.decosegfault.hermes.data.TripData;
import com.decosegfault.hermes.types.SimType;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;

import org.tinylog.Logger;

/**
 * @author Lachlan Ellis
 */
public class RouteHandler {
    static SimType simType;

    static Map<String, RouteData> routes = new HashMap<String, RouteData>();

    public static void addRoute(Route route) {
        RouteData newRoute = new RouteData(route.getType());
        newRoute.routeID = route.getShortName();
        newRoute.routeName = route.getLongName();

        routes.put(route.getId().getId(), newRoute);
    }

    public static void addTrip(Trip trip) {
        TripData newTrip = new TripData(routes.get(trip.getRoute().getId().getId()).routeType);
        newTrip.routeID = trip.getId().getId();
        newTrip.routeName = trip.getTripHeadsign();

        routes.get(trip.getRoute().getId().getId()).tripList.add(newTrip);
    }

    /** testing only */
    public static void logRoutes() {
        for (RouteData element : routes.values()) {
            Logger.info(element.routeName + " : " + element.routeID + " : " + element.routeType);
        }
    }

    public static void logTrips() {
        for (RouteData element : routes.values()) {
            for (TripData trip : element.tripList) {
                Logger.info(trip.routeName + " : " + trip.routeID);
            }
        }
    }
}
