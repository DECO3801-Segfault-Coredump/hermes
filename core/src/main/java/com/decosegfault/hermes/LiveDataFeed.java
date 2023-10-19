package com.decosegfault.hermes;

import com.decosegfault.atlas.util.HPVector3;
import com.decosegfault.hermes.data.RouteData;
import com.decosegfault.hermes.data.TripData;
import com.decosegfault.hermes.data.VehicleData;
import com.decosegfault.hermes.frontend.FrontendData;
import com.decosegfault.hermes.types.VehicleType;
import com.google.transit.realtime.GtfsRealtime;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Retrieves and stores live vehicle data
 * @author Cathy Nguyen
 * @author Lachlan Ellis
 */
public class LiveDataFeed {

    private final GtfsRealtime.FeedMessage tripFeed;
    private final URL vehiclesURL;
    private GtfsRealtime.FeedMessage vehiclePositionsFeed;
    public HashMap<String, String> tripIDMap = new HashMap<>();
    public Map<String, HPVector3> vehiclePositions = new HashMap<>();
    public Map<String, VehicleData> vehicleDataMap = new HashMap<>();

    public LiveDataFeed() {
        try {
            URI vehiclesURI = new URI("https://gtfsrt.api.translink.com.au/api/realtime/SEQ/VehiclePositions");
            vehiclesURL = vehiclesURI.toURL();
            vehiclePositionsFeed = GtfsRealtime.FeedMessage.parseFrom(vehiclesURL.openStream());

            URI tripURI = new URI("https://gtfsrt.api.translink.com.au/api/realtime/SEQ/TripUpdates");
            URL tripURL = tripURI.toURL();
            tripFeed = GtfsRealtime.FeedMessage.parseFrom(tripURL.openStream());
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
        init();
    }

    public GtfsRealtime.FeedMessage getTripFeed() {
        return tripFeed;
    }

    public GtfsRealtime.FeedMessage getVehiclePositionsFeed() {
        return vehiclePositionsFeed;
    }





    public void update() {
        try {
            vehiclePositionsFeed = GtfsRealtime.FeedMessage.parseFrom(vehiclesURL.openStream());
        } catch (IOException e) {
            System.out.println("rip");
        }
        tripIDMap.clear();
        storeTripID();
        vehiclePositions.clear();
        setVehiclePositions();
        // Calls tick() on existing VehicleData
        // If routeID exists in vehicleDataMap, call tick() to updates its position
        // Otherwise, add new vehicleDataMap entry
        for (Map.Entry<String, HPVector3> entry : vehiclePositions.entrySet()) {
            VehicleData vehicleData;
            String routeID = tripIDMap.get(entry.getKey());
            if (RouteHandler.routes.get(routeID) != null) {
                String tripID = entry.getKey();
                if (vehicleDataMap.containsKey(entry.getKey())) {
                    VehicleData existingVehicleData = vehicleDataMap.get(tripID);
                    existingVehicleData.tick(vehiclePositions.get(tripID));
                } else {
                    VehicleType vehicleType = RouteHandler.routes.get(routeID).routeType;
                    vehicleData = new VehicleData(vehicleType);
                    vehicleData.position = vehiclePositions.get(tripID);
                    vehicleDataMap.put(tripID, vehicleData);
                }
            }
        }
    }

    private void init() {
        storeTripID();
        setVehiclePositions();
        storeVehicleData();
    }

    private void storeTripID() {
        for (GtfsRealtime.FeedEntity entity : vehiclePositionsFeed.getEntityList()) {
            tripIDMap.put(entity.getVehicle().getTrip().getTripId(), entity.getVehicle().getTrip().getRouteId());
        }
    }

    private void setVehiclePositions() {
        for (GtfsRealtime.FeedEntity entity : vehiclePositionsFeed.getEntityList()) {
            String tripID = entity.getVehicle().getTrip().getTripId();
            double latitude = entity.getVehicle().getPosition().getLatitude();
            double longitude = entity.getVehicle().getPosition().getLongitude();
            HPVector3 position = new HPVector3(latitude, longitude, 0.0);
            vehiclePositions.put(tripID, position);
        }
    }

    private void storeVehicleData() {
        for (Map.Entry<String, HPVector3> entry : vehiclePositions.entrySet()) {
            String tripID = entry.getKey();
            String routeID = tripIDMap.get(tripID);
            if (RouteHandler.routes.get(routeID) != null) {
                VehicleType routeType = RouteHandler.routes.get(routeID).routeType;
                VehicleData vehicleData = new VehicleData(routeType);
                vehicleData.position = entry.getValue();
                vehicleDataMap.put(tripID, vehicleData);
            }
        }
    }
}
