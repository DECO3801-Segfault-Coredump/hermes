package com.decosegfault.hermes.data;

import com.decosegfault.atlas.util.HPVector3;
import com.decosegfault.hermes.types.VehicleType;

/**
 * @author Lachlan Ellis
 */
public class VehicleData {
    public VehicleType vehicleType;
    public HPVector3 position = new HPVector3(0, 0, 0); //describes the 2d position and the orientation
    public HPVector3 oldPosition = new HPVector3(0, 0, 0);
    public boolean hidden = false;

    /**
     * @param type
     */
    public VehicleData(VehicleType type) {
        vehicleType = type;
    }

    /**
     * @param newPosition
     */
    public void tick(HPVector3 newPosition) {
        oldPosition = position;
        position = newPosition;
    }
}
