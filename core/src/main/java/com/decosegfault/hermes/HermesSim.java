package com.decosegfault.hermes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.decosegfault.atlas.render.AtlasVehicle;
import com.decosegfault.atlas.util.AtlasUtils;
import com.decosegfault.atlas.util.HPVector3;
import com.decosegfault.hermes.data.RouteData;
import com.decosegfault.hermes.data.TripData;
import com.decosegfault.hermes.data.VehicleData;
import com.decosegfault.hermes.frontend.FrontendData;
import com.decosegfault.hermes.frontend.FrontendEndpoint;
import com.decosegfault.hermes.frontend.FrontendServer;
import com.decosegfault.hermes.frontend.RouteExpectedReal;
import com.decosegfault.hermes.types.SimType;
import com.decosegfault.hermes.types.VehicleType;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.*;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.tinylog.Logger;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Lachlan Ellis
 * @author Matt Young
 * @author Henry Batt
 * @author Cathy Nguyen
 */
public class HermesSim {
    public static Map<String, HPVector3> brisbaneOlympics = new HashMap<>() {
        {
            put("Suncorp Stadium", new HPVector3(-27.4648, 153.0095, 500.0));
            put("Brisbane Entertainment Centre", new HPVector3(-27.3422, 153.0704, 700.0));
            put("The Gabba", new HPVector3(-27.4858, 153.0381, 450.0));
            put("Emporium Hotel Southbank", new HPVector3(-27.481382543911, 153.02309927206, 300.0));
            put("Central Station", new HPVector3(-27.4662, 153.0262, 200.0));
            put("Roma Street Busway Station", new HPVector3(-27.275829, 153.010703, 100.0));
        }
    };
    private static FrontendServer server;
    public static ConcurrentHashMap<String, AtlasVehicle> vehicleMap = new ConcurrentHashMap<>();
    /** time of day will be in seconds, max 86400 (one day) before looping back to 0 */
    public static double time = 0;
    public static double MAX_TIME = 86400;
    static float speed = 10f;
    public static final List<TripData> vehiclesToCreate = new ArrayList<>();
    public static LiveDataFeed liveDataFeed = new LiveDataFeed();

    public static FrontendData frontendData;

    public static Set<String> affectedRoutes = new HashSet<>();

    public static List<RouteExpectedReal> expectedReals = new ArrayList<>();

    public static int frontendCounter = 0;

    /**
     * ticks time by x seconds.
     * tick speed set by end user.
     * in history mode, interpolates movement based on start and end time and path length.
     * in live mode, tick does nothing.
     * in sim mode, moves vehicles at a set speed based on tick speed.
     */
    public static void tick(float delta) {
        if (System.getProperty("nohermes") != null) return;
        time = (time + (delta * speed)) % MAX_TIME;

        frontendData = new FrontendData();

        vehiclesToCreate.clear();
        expectedReals.clear();

        if (RouteHandler.simType == SimType.LIVE) {
            liveDataFeed.update();
            ConcurrentHashMap<String, AtlasVehicle> slayMap = new ConcurrentHashMap<>();

            for (Map.Entry<String, VehicleData> entry : liveDataFeed.vehicleDataMap.entrySet()) {
                String tripID = entry.getKey();
                if (!vehicleMap.containsKey(tripID)) {
                    VehicleType type = entry.getValue().vehicleType;
                    String routeID = liveDataFeed.tripIDMap.get(tripID);
                    String name = RouteHandler.routes.get(routeID).routeName;
                    String id = RouteHandler.routes.get(routeID).routeID;
                    StringBuilder vehicleName = new StringBuilder();
                    if (type == VehicleType.TRAIN) {
                        vehicleName.append(id).append(" line");
                    } else if (type == VehicleType.FERRY) {
                        vehicleName.append(id).append(" voyage");
                    } else {
                        vehicleName.append("Route ").append(id);
                    }
                    vehicleName.append(": ").append(name).append("\t").append(type);
                    var vehicle = AtlasVehicle.Companion.createFromHermes(type, vehicleName.toString());
                    slayMap.put(tripID, vehicle);
                } else {
                    slayMap.put(tripID, vehicleMap.get(tripID));
                }
                HPVector3 position = entry.getValue().position;
                Vector3 pos = AtlasUtils.INSTANCE.latLongToAtlas(new Vector3((float) position.getX(), (float) position.getY(), (float) position.getZ()));
                slayMap.get(tripID).updateTransform(pos);
            }
            vehicleMap = slayMap;
        } else {
            RouteHandler.tripsbyID.values().stream().forEach((trip) -> {
                trip.tick();
                if (trip.vehicle.hidden && vehicleMap.containsKey(trip.routeID)) {
                    vehicleMap.remove(trip.routeID);
                } else if (!trip.vehicle.hidden && !vehicleMap.containsKey(trip.routeID)) {
                    if (trip.vehicle.vehicleType == null) {
                        Logger.warn("Null trip vehicle! {} {}", trip.routeName, trip.routeID);
                        return;
                    }
                    vehiclesToCreate.add(trip);
                }
            });

            // create vehicles - this has to be here because otherwise it breaks libGDX
            // the other way would be to have a "creating vehicle" lock
            for (TripData trip : vehiclesToCreate) {
                StringBuilder vehicleName = new StringBuilder();

                if (trip.vehicle.vehicleType == VehicleType.TRAIN) {
                    vehicleName.append(trip.routeVehicleName).append(" line");
                } else if (trip.vehicle.vehicleType == VehicleType.FERRY) {
                    vehicleName.append(trip.routeVehicleName).append(" voyage");
                } else {
                    vehicleName.append("Route ").append(trip.routeVehicleName);
                }
                vehicleName.append(": ").append(trip.routeName).append("\t").append(trip.vehicle.vehicleType);

                var vehicle = AtlasVehicle.Companion.createFromHermes(trip.vehicle.vehicleType, vehicleName.toString());
                vehicleMap.put(trip.routeID, vehicle);
            }

            // parallelising this currently breaks everything
            vehicleMap.entrySet().stream().forEach((tripID) -> {
                TripData trip = RouteHandler.tripsbyID.get(tripID.getKey());
                tripID.getValue().updateTransform(
                    new Vector3((float) trip.vehicle.position.getX(), (float) trip.vehicle.position.getY(), (float) trip.vehicle.position.getZ()));
                tripID.getValue().setHidden(trip.vehicle.hidden);
            });
        }

        // transmit data to the frontend
        frontendData.setInterestPoints(HermesSim.brisbaneOlympics);
        frontendData.setBusesInInterest(affectedRoutes.stream().toList());
        frontendData.setRouteExpectedReals(expectedReals);
        frontendData.setRouteFrequency(calculateRouteFrequency());
        frontendData.setVehicleTypes(calculateVehicleTypes());

        if (frontendCounter++ % 10 == 0) {
            FrontendEndpoint.broadcast(frontendData);
        }
    }

    public static Map<String, Integer> calculateRouteFrequency() {
        Map<String, Integer> routeFrequency = new HashMap<>();

        if (RouteHandler.simType == SimType.LIVE) {
            for (Map.Entry<String, String> entry : liveDataFeed.tripIDMap.entrySet()) {
                RouteData maybeRoute = RouteHandler.routes.get(entry.getValue());
                if (maybeRoute == null) {
                    continue;
                }
                String routeName = maybeRoute.routeName;
                if (routeFrequency.containsKey(routeName)) {
                    Integer count = routeFrequency.get(routeName);
                    routeFrequency.put(routeName, count + 1);
                } else {
                    routeFrequency.put(routeName, 1);
                }
            }
        } else {
            for (Map.Entry<String, TripData> entry : RouteHandler.tripsbyID.entrySet()) {
                String routeName = entry.getValue().routeName;
                if (routeFrequency.containsKey(routeName)) {
                    Integer count = routeFrequency.get(routeName);
                    routeFrequency.put(routeName, count + 1);
                } else {
                    routeFrequency.put(routeName, 1);
                }
            }
        }
        return routeFrequency;
    }

    public static Map<String, Integer> calculateVehicleTypes() {
        Map<String, Integer> vehicleTypes = new HashMap<>();
        for (Map.Entry<String, RouteData> entry : RouteHandler.routes.entrySet()) {
            String type = entry.getValue().routeType.toString();
            if (vehicleTypes.containsKey(type)) {
                Integer count = vehicleTypes.get(type);
                vehicleTypes.put(type, count + 1);
            } else {
                vehicleTypes.put(type, 1);
            }
        }
        return vehicleTypes;
    }

    /**
     * load vehicle routes here.
     * in history mode, routes follow preset start and end times.
     * in live mode, routes aren't loaded at all; uses live data.
     * in sim mode, routes only use their first available start time.
     */
    public static void load(SimType simType) {
        if (System.getProperty("nohermes") != null) {
            Logger.warn("Skipping Hermes load, -Dnohermes=true");
            return;
        }

        Logger.info("Starting frontend server");
        server = new FrontendServer();
        server.start();

        RouteHandler.simType = simType;
        if (RouteHandler.simType == SimType.LIVE) {
            read();
            return;
        } else {
            read();
        }

        RouteHandler.sortShapes();
        RouteHandler.initTrips();
        Logger.warn("Trips Loaded: {}", RouteHandler.tripsbyID.size());
        Logger.info("Linking Hermes-Atlas vehicles");
        Logger.info("GTFS Data Loaded");
    }

    public static void read() {
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

        // You can register an entity handler that listens for new objects as they are read.
        // Or you can use the internal entity store, which has references to all the loaded entities.

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

        Map<AgencyAndId, ShapePoint> shapesById = null;
        Map<AgencyAndId, StopTime> stopTimesById = null;

        if (RouteHandler.simType != SimType.LIVE) {
            shapesById = store.getEntitiesByIdForEntityType(
                AgencyAndId.class, ShapePoint.class);

            stopTimesById = store.getEntitiesByIdForEntityType(
                AgencyAndId.class, StopTime.class);
        }

        for (Route element : routesById.values()) {
            RouteHandler.addRoute(element);
        }

        if (RouteHandler.simType != SimType.LIVE) {
            for (Trip element : tripsById.values()) {
                RouteHandler.addTrip(element);
            }

            for (StopTime element : stopTimesById.values()) {
                RouteHandler.handleTime(element);
            }
            for (ShapePoint element : shapesById.values()) {
                RouteHandler.addShape(element);
            }
        }

        try {
            reader.close();
        } catch (IOException noFile) {
            throw new IllegalArgumentException("uh oh");
        }
    }

    public static void increaseSpeed() {
        speed *= 2;
    }

    public static void decreaseSpeed() {
        speed /= 2;
    }

    public static void shutdown() {
        server.stop();
    }
}

