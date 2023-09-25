package com.decosegfault.hermes.data;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Vector3;
import com.decosegfault.hermes.types.VehicleType;

/**
 * @author Lachlan Ellis
 */
public class TripData {
    public VehicleType routeType;
    public String routeID;
    public String routeName;
    public List<Vector3> routeMap =  new ArrayList<>();
    public VehicleData vehicle;
    List<StopData> stopList = new ArrayList<>();
    int startTime;
    /** optional, only used in history mode */
    int endTime = -1;
    public TripData(int type) {
        switch (type) {
            case 2 -> routeType = VehicleType.TRAIN;
            case 4 -> routeType = VehicleType.FERRY;
            default -> routeType = VehicleType.BUS;
        }
        vehicle = new VehicleData(routeType);
    }

    public TripData(VehicleType type) {
        routeType = type;
        vehicle = new VehicleData(routeType);
    }

    public void tick() {

    }
}
