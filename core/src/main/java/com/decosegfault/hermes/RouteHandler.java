package com.decosegfault.hermes;

import com.badlogic.gdx.math.Vector3;
import com.decosegfault.atlas.util.AtlasUtils;
import com.decosegfault.atlas.util.HPVector2;
import com.decosegfault.atlas.util.HPVector3;
import com.decosegfault.hermes.data.RouteData;
import com.decosegfault.hermes.data.TripData;
import com.decosegfault.hermes.types.SimType;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.tinylog.Logger;
import java.util.*;

/**
 * This static class andles all the operations on transport data.
 * All data added in HermesSim.read is processed in this class.
 *
 * @author Lachlan Ellis
 * @author Henry Batt
 */
public class RouteHandler {
    public static SimType simType;

    static Map<String, RouteData> routes = new HashMap<String, RouteData>();

    static Map<String, List<TripData>> tripsByShape = new HashMap<>();

    static Map<String, TripData> tripsbyID = new HashMap<>();

    // Arbitrary speed lol
    public static float vehicleSpeed = 5;


    /**
     * This function processes a route instance into the hashmap
     *
     * @param route The RouteData instance being processed.
     */
    public static void addRoute(Route route) {
        RouteData newRoute = new RouteData(route.getType());
        newRoute.routeID = route.getShortName();
        newRoute.routeName = route.getLongName();

        routes.put(route.getId().getId(), newRoute);
    }

    /**
     * This function processes a route instance into the hashmap.
     * Also sets up the shapes map with each shape ID.
     *
     * @param trip The TripData instance being processed.
     */
    public static void addTrip(Trip trip) {
        TripData newTrip = new TripData(routes.get(trip.getRoute().getId().getId()).routeType, trip.getDirectionId());
        newTrip.routeID = trip.getId().getId();
        newTrip.routeName = trip.getTripHeadsign();
        newTrip.routeVehicleName = trip.getRoute().getShortName();

        if (trip.getShapeId() != null) {
            if (!tripsByShape.containsKey(trip.getShapeId().getId())) {
                tripsByShape.put(trip.getShapeId().getId(), new ArrayList<>());
            }
            tripsByShape.get(trip.getShapeId().getId()).add(newTrip);
        }
        tripsbyID.put(trip.getId().getId(), newTrip);
    }

    /**
     * This function processes a shape instance into all relevant trips.
     *
     * @param point The ShapePoint instance being processed.
     */
    public static void addShape(ShapePoint point) {
        for (TripData trip : tripsByShape.get(point.getShapeId().getId())) {
            Vector3 tempVector2 = new Vector3((float) point.getLat(), (float) point.getLon(), 0);
            Vector3 tempVector = AtlasUtils.INSTANCE.latLongToAtlas(tempVector2);
            HPVector2 pointPos = new HPVector2(tempVector.x, tempVector.y);
            trip.routeMap.add(new HPVector3(tempVector.x, tempVector.y, point.getSequence()));

            // COMPARE this TRIP against each BRISBANE OLYMPICS EVENT
            for (Map.Entry<String, HPVector3> entry : HermesSim.brisbaneOlympics.entrySet()) {
                HPVector2 stadiumPos = new HPVector2(entry.getValue().getX(), entry.getValue().getY());
                // remember z is the radius
                if (pointPos.dst(stadiumPos) <= entry.getValue().getZ()) {
                    // yeah nah we got an affected route didn't we
                    HermesSim.affectedRoutes.put(entry.getKey(), routes.get(trip.routeID).routeName);
                }
            }
        }
    }

    /**
     * This function adds the start and end time for all processed trips.
     *
     * @param time The StopTime instance being processed.
     */
    public static void handleTime(StopTime time) {
        if (tripsbyID.containsKey(time.getTrip().getId().getId())) {
            TripData trip = tripsbyID.get(time.getTrip().getId().getId());
            if (time.getStopSequence() == 1) {
                trip.startTime = time.getDepartureTime();
                trip.previousTime = time.getDepartureTime();
            }
            if (time.getArrivalTime() > trip.endTime) {
                trip.endTime = time.getArrivalTime();
            }
        }
    }

    /**
     * Sorts the shapes into their order in each trip's path.
     */
    public static void sortShapes() {
        for (TripData element : tripsbyID.values()) {
            element.routeMap.sort(Comparator.comparingDouble(HPVector3::getZ));
            for (int i = 0; i < element.routeMap.size(); i++) {
                element.routeMap.get(i).setZ(0);
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
        for (TripData trip : tripsbyID.values()) {
            Logger.info(trip.routeName + " : " + trip.routeID);
        }
    }

    public static void logShapes() {
        for (TripData trip : tripsbyID.values()) {
            for (HPVector3 vector : trip.routeMap) {
                Logger.info(vector.getX() + "+" + vector.getY() + " : " + vector.getZ() + " : " + trip.routeName);
            }
        }
    }

    public static void initTrips() {
        for (TripData trip : tripsbyID.values()) {
            trip.initTrip();
        }
    }
}
