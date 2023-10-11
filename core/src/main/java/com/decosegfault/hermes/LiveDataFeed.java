package com.decosegfault.hermes;

import com.google.transit.realtime.GtfsRealtime;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Retrieves and stores live vehicle data
 * @author Cathy Nguyen
 * @author Lachlan Ellis
 */
public class LiveDataFeed {
    private static URL vehiclesURL;
    private static URL tripUpdatesURL;
    private static void init() {
        try {
            URI vehiclesURI = new URI("https://gtfsrt.api.translink.com.au/api/realtime/SEQ/VehiclePositions");
            URI tripUpdatesURI = new URI("https://gtfsrt.api.translink.com.au/api/realtime/SEQ/TripUpdates");
            vehiclesURL = vehiclesURI.toURL();
            tripUpdatesURL = tripUpdatesURI.toURL();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        init();
//        System.out.println(getVehiclePositionsFeed().getEntity(0));
//        System.out.println(getTripUpdatesFeed().getEntity(0));

        System.out.println(RouteHandler.tripsbyID);
    }

    public static GtfsRealtime.FeedMessage getVehiclePositionsFeed() {
        GtfsRealtime.FeedMessage feed;
        try {
            feed = GtfsRealtime.FeedMessage.parseFrom(vehiclesURL.openStream());
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return feed;
    }

    public static GtfsRealtime.FeedMessage getTripUpdatesFeed() {
        GtfsRealtime.FeedMessage feed;
        try {
            feed = GtfsRealtime.FeedMessage.parseFrom(tripUpdatesURL.openStream());
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return feed;
    }



//    public static void storeVehicleData(GtfsRealtime.FeedMessage feed) {
//        HashMap<String, AtlasVehicle> vehicleMapCopy = new HashMap<>(HermesSim.vehicleMap);
//        HermesSim.vehicleMap.clear();
//        for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
//            TripData tripData = RouteHandler.tripsbyID.get(entity.getVehicle().getTrip().getTripId())
//
//            VehicleData vehicle = new VehicleData(tripData.routeType);
//            vehicle.position = getVehiclePosition(entity);
//            if(vehicleMapCopy.containsKey(tripData.routeID)) {
//                HermesSim.vehicleMap.put(tripData.routeID, vehicleMapCopy.get(tripData.routeID));
//            } else {
//                HermesSim.vehiclesToCreate.add(tripData);
//            }
//
//        }
//    }
//
//    public static HPVector3 getVehiclePosition(GtfsRealtime.FeedEntity entity) {
//        double latitude = entity.getVehicle().getPosition().getLatitude();
//        double longitude = entity.getVehicle().getPosition().getLongitude();
//        return new HPVector3(latitude, longitude, 0.0);
//    }
}
