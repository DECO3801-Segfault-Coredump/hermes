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
    public Vector3 position; //describes the 2d position and the orientation
    public Vector3 oldPosition = new Vector3(0, 0, 0);
    public VehicleData(VehicleType type) {
        vehicleType = type;
    }
    public void tick(Vector3 newPosition) {
        position.set(newPosition.x, newPosition.y, (new Vector2(newPosition.x, newPosition.y)).angleDeg(new Vector2(oldPosition.x, oldPosition.y)));
        oldPosition = newPosition;
    }
}
