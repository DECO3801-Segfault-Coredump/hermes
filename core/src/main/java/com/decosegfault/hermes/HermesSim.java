/**
 * @author Lachlan Ellis
 */

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

public class HermesSim {
    public static void tick() {
        read();
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

