package com.decosegfault.hermes;

import com.decosegfault.atlas.util.HPVector3;
import com.decosegfault.hermes.data.VehicleData;
import com.decosegfault.hermes.types.VehicleType;
import com.google.transit.realtime.GtfsRealtime;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
    public ArrayList<String> routeIDList = new ArrayList<>();
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

    public GtfsRealtime.FeedMessage getFeed() {
        return tripFeed;
    }

    public GtfsRealtime.FeedMessage getFeed2() {
        return vehiclePositionsFeed;
    }





    public void update() {
        try {
            vehiclePositionsFeed = GtfsRealtime.FeedMessage.parseFrom(vehiclesURL.openStream());
        } catch (IOException e) {
            System.out.println("rip");
        }
        routeIDList.clear();
        storeRouteID();
        vehiclePositions.clear();
        setVehiclePositions();
        // Calls tick() on existing VehicleData
        // If routeID exists in vehicleDataMap, call tick() to updates its position
        // Otherwise, add new vehicleDataMap entry
        for (String routeID : routeIDList) {
            VehicleData vehicleData;
            if (RouteHandler.routes.get(routeID) != null) {
                if (vehicleDataMap.containsKey(routeID)) {
                    VehicleData existingVehicleData = vehicleDataMap.get(routeID);
                    existingVehicleData.hidden = false;
                    existingVehicleData.tick(vehiclePositions.get(routeID));
                } else {
                    VehicleType vehicleType = RouteHandler.routes.get(routeID).routeType;
                    vehicleData = new VehicleData(vehicleType);
                    vehicleData.position = vehiclePositions.get(routeID);
                    vehicleDataMap.put(routeID, vehicleData);
                }
            }
        }
        // Now check for routeIDs in vehicleDataMap no longer present in routeIDList and hide vehicle
        for (Map.Entry<String, VehicleData> entry : vehicleDataMap.entrySet()) {
            if (!routeIDList.contains(entry.getKey())) {
                entry.getValue().hidden = true;
            }
        }
    }

    private void init() {
        storeRouteID();
        setVehiclePositions();
        storeVehicleData();
    }

    private void storeRouteID() {
        for (GtfsRealtime.FeedEntity entity : vehiclePositionsFeed.getEntityList()) {
            routeIDList.add(entity.getVehicle().getTrip().getRouteId());
        }
    }

    private void setVehiclePositions() {
        for (GtfsRealtime.FeedEntity entity : vehiclePositionsFeed.getEntityList()) {
            String routeID = entity.getVehicle().getTrip().getRouteId();
            double latitude = entity.getVehicle().getPosition().getLatitude();
            double longitude = entity.getVehicle().getPosition().getLongitude();
            HPVector3 position = new HPVector3(latitude, longitude, 0.0);
            vehiclePositions.put(routeID, position);
        }
    }

    private void storeVehicleData() {
        for (Map.Entry<String, HPVector3> entry : vehiclePositions.entrySet()) {
            String routeID = entry.getKey();
            if (RouteHandler.routes.get(routeID) != null) {
                VehicleType routeType = RouteHandler.routes.get(routeID).routeType;
                VehicleData vehicleData = new VehicleData(routeType);
                vehicleData.position = entry.getValue();
                vehicleDataMap.put(routeID, vehicleData);
            }
        }
    }
}
