package com.decosegfault.hermes.data;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;

/**
 * @author Lachlan Ellis
 */
public class VehicleData {
    int vehicleType;
    int startTime;
    /** optional, only used in history mode */
    int endTime = -1;
    Vector3 position; //describes the 2d position and the orientation
    public void tick() {

    }
}
