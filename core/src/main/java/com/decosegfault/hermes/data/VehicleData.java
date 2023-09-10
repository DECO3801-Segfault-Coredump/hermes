package com.decosegfault.hermes.data;

import com.badlogic.gdx.math.Vector3;
import com.decosegfault.hermes.types.VehicleType;

import java.util.ArrayList;

/**
 * @author Lachlan Ellis
 */
public class VehicleData {
    VehicleType vehicleType;
    Vector3 position; //describes the 2d position and the orientation
    public VehicleData(VehicleType type) {
        vehicleType = type;
    }
    public void tick() {

    }
}