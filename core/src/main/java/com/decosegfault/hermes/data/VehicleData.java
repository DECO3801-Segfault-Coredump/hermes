package com.decosegfault.hermes.data;

import com.badlogic.gdx.math.Vector3;
import com.decosegfault.hermes.LiveDataFeed;
import com.decosegfault.hermes.types.VehicleType;
import com.google.transit.realtime.GtfsRealtime;
import java.util.Objects;

/**
 * @author Lachlan Ellis
 */
public class VehicleData {
    public VehicleType vehicleType;
    public Vector3 position; //describes the 2d position and the orientation
    public VehicleData(VehicleType type) {
        vehicleType = type;
    }
    public void tick() {

    }
}
