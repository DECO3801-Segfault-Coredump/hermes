package com.decosegfault.hermes.data;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.decosegfault.hermes.types.VehicleType;

/**
 * @author Lachlan Ellis
 */
public class RouteData {
    public VehicleType routeType;
    public String routeID;
    public String routeName;
    List<StopData> stopList = new ArrayList<>();

    public RouteData(int type) {
        switch (type) {
            case 2 -> routeType = VehicleType.TRAIN;
            case 4 -> routeType = VehicleType.FERRY;
            default -> routeType = VehicleType.BUS;
        }
    }
    public void tick() {

    }
}
