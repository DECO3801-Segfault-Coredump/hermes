package com.decosegfault.hermes;

import com.badlogic.gdx.math.Vector3;
import com.decosegfault.hermes.data.RouteData;
import com.decosegfault.hermes.data.VehicleData;
import com.decosegfault.hermes.types.VehicleType;
import com.google.transit.realtime.GtfsRealtime;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Retrieves and stores live vehicle data
 * @author Cathy Nguyen
 */
public class LiveDataFeed {
    private static final URI vehiclesURI;
    private static final URL vehiclesURL;
    public static ArrayList<VehicleData> vehiclesList = new ArrayList<>();

    static {
        try {
            vehiclesURI = new URI("https://gtfsrt.api.translink.com.au/api/realtime/SEQ/VehiclePositions");
            vehiclesURL = vehiclesURI.toURL();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Main method that uses OneBusAway's realtime API to parse live vehicle data from Translink
     * and retrieves and stores such data as VehicleData instances within vehiclesList
     */
    public static void main(String[] args) {
        GtfsRealtime.FeedMessage feed;
        try {
            feed = GtfsRealtime.FeedMessage.parseFrom(vehiclesURL.openStream());
        } catch (IOException e) {
            throw new RuntimeException();
        }
        storeVehicleData(feed);
    }

    /**
     * Returns list of stored VehicleData
     * @return ArrayList<VehicleData> vehiclesList
     */
    public static ArrayList<VehicleData> getVehiclesList() {
        return vehiclesList;
    }

    /**
     * Retrieves relevant vehicle data from live data feed and stores as VehicleData
     * instances in vehiclesList
     * @param feed Translink's GTFS-RT feed
     */
    public static void storeVehicleData(GtfsRealtime.FeedMessage feed) {
        for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
            RouteData routeData = RouteHandler.routes.get(entity.getVehicle().getTrip().getRouteId());
            VehicleType vehicleType = routeData.routeType;
            VehicleData vehicle = new VehicleData(vehicleType);
            vehicle.position = getVehiclePosition(entity);
            vehiclesList.add(vehicle);
        }
    }

    /**
     * Creates and returns a position vector of live vehicles
     * @param entity Unit of information for vehicle single vehicle from live data feed
     * @return Position vector of vehicle
     */
    public static Vector3 getVehiclePosition(GtfsRealtime.FeedEntity entity) {
        Vector3 vehiclePosition = new Vector3();
        float latitude = entity.getVehicle().getPosition().getLatitude();
        float longitude = entity.getVehicle().getPosition().getLongitude();
        vehiclePosition.x = latitude;
        vehiclePosition.y = longitude;
        return vehiclePosition;
    }
}
