package com.decosegfault.hermes.data;

import com.decosegfault.hermes.types.VehicleType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lachlan Ellis
 */

public class RouteData {
    public VehicleType routeType;
    public String routeID;
    public String routeName;
    List<StopData> stopList = new ArrayList<>();

    /**
     * @param type
     */
    public RouteData(int type) {
        switch (type) {
            case 2 -> routeType = VehicleType.TRAIN;
            case 4 -> routeType = VehicleType.FERRY;
            default -> routeType = VehicleType.BUS;
        }
    }
}
