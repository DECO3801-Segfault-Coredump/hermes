package com.decosegfault.hermes.data;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.decosegfault.hermes.types.VehicleType;

import java.util.ArrayList;
import java.util.Objects;

/**
 * @author Lachlan Ellis
 */
public class VehicleData {
    public VehicleType vehicleType;
    public Vector3 position = new Vector3(0, 0, 0);; //describes the 2d position and the orientation
    public Vector3 oldPosition = new Vector3(0, 0, 0);
    public boolean hidden = false;
    public VehicleData(VehicleType type) {
        vehicleType = type;
    }
    public void tick(Vector3 newPosition) {

    }
}
