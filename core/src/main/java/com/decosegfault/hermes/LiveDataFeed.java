package com.decosegfault.hermes;

import com.decosegfault.hermes.data.RouteData;
import com.decosegfault.hermes.types.VehicleType;
import com.google.transit.realtime.GtfsRealtime;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Cathy Nguyen
 */
public class LiveDataFeed {
    private static final URI vehiclesURI;
    private static final URL vehiclesURL;
    //public static Map<String, ArrayList<String>> vehicleData = new HashMap<>();

    static {
        try {
            vehiclesURI = new URI("https://gtfsrt.api.translink.com.au/api/realtime/SEQ/VehiclePositions");
            vehiclesURL = vehiclesURI.toURL();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void storeVehicleData() {
        GtfsRealtime.FeedMessage feed;
        try {
            feed = GtfsRealtime.FeedMessage.parseFrom(vehiclesURL.openStream());
        } catch (IOException e) {
            throw new RuntimeException();
        }
        for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
            String vehicleId = entity.getId();
            RouteData routeData = RouteHandler.routes.get(entity.getVehicle().getTrip().getRouteId());
            VehicleType vehicleType = routeData.routeType;
        }
    }
}
