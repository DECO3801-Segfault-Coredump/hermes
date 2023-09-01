package com.decosegfault.hermes;

import com.badlogic.gdx.Gdx;
import org.tinylog.Logger;

import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.serialization.GtfsReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Lachlan Ellis
 */
public class HermesSim {

    /** time of day will be in seconds, max 86400 (one day) before looping back to 0 */
    int time;

    /**
     * ticks time by x seconds.
     * tick speed set by end user.
     * in sim mode, interpolates movement based on start and end time and path length.
     * in live mode, tick does nothing.
     * in sim mode, moves vehicles at a set speed based on tick speed.
     */
    public void tick() {
        read();
    }

    /**
     * load vehicle routes here.
     * in history mode, routes follow preset start and end times.
     * in live mode, routes aren't loaded at all; uses live data.
     * in sim mode, routes only use their first available start time.
     */
    public void load() {

    }

    public static void read()  {
        GtfsReader reader = new GtfsReader();
        try {
            reader.setInputLocation(Gdx.files.internal("hermes/gtfs.zip").file());
        } catch (IOException noFile) {
            throw new IllegalArgumentException(noFile);
        }


        /**
         * You can register an entity handler that listens for new objects as they
         * are read
         */
        reader.addEntityHandler(new GtfsEntityHandler());

        /**
         * Or you can use the internal entity store, which has references to all the
         * loaded entities
         */
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

        for (Route route : routesById.values()) {
//            System.out.println("route: " + route.getShortName());
        }
    }

    private static class GtfsEntityHandler implements EntityHandler {

        public void handleEntity(Object bean) {
            if (bean instanceof Stop) {
                Stop stop = (Stop) bean;
                Logger.info("stop: " + stop.getName());
            }
        }
    }
}

