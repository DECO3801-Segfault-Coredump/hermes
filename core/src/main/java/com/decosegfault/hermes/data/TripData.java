package com.decosegfault.hermes.data;

import java.util.ArrayList;
import com.badlogic.gdx.math.Vector2;
import com.decosegfault.hermes.types.VehicleType;

/**
 * @author Lachlan Ellis
 */
public class TripData {
    VehicleType routeType;
    public String routeID;
    public String routeName;
    ArrayList<Vector2> routeMap =  new ArrayList<Vector2>();
    ArrayList<VehicleData> vehicleList = new ArrayList<VehicleData>();
    ArrayList<StopData> stopList = new ArrayList<StopData>();
    public TripData(int type) {
        switch (type) {
            case 2 -> routeType = VehicleType.TRAIN;
            case 3 -> routeType = VehicleType.BUS;
            case 4 -> routeType = VehicleType.FERRY;
        }
    }

    public TripData(VehicleType type) {
        routeType = type;
    }

    public void tick() {

    }
}
