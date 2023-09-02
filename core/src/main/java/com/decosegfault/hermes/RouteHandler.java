package com.decosegfault.hermes;

import java.util.Map;

import com.decosegfault.hermes.data.RouteData;
import com.decosegfault.hermes.data.TripData;
import com.decosegfault.hermes.types.SimType;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;

import org.tinylog.Logger;

public class RouteHandler {
    static SimType simType;

    static Map<Integer, RouteData> routes;

    public static void AddRoute(Route route) {
        RouteData newRoute = new RouteData(route.getType());
        newRoute.routeID = route.getShortName();
        newRoute.routeName = route.getLongName();

        routes.put(route.getId().hashCode(), newRoute);
    }

    public static void AddTrip(Trip trip) {
        TripData newTrip = new TripData(routes.get(trip.getId().hashCode()).routeType);
        newTrip.routeID = trip.getTripShortName();
        newTrip.routeName = trip.getTripHeadsign();

        routes.get(trip.getId().hashCode()).tripList.add(newTrip);
    }

    /** testing only */
    public static void logRoutes() {
        for (RouteData element : routes.values()) {
            Logger.info(element.routeName + " : " + element.routeID + " : " + element.routeType);
        }
    }
}
