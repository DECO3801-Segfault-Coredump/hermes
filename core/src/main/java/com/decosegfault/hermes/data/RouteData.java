package com.decosegfault.hermes.data;

import java.util.ArrayList;

import com.decosegfault.hermes.types.VehicleType;

/**
 * @author Lachlan Ellis
 */
public class RouteData {
    public VehicleType routeType;
    public String routeID;
    public String routeName;
    ArrayList<StopData> stopList = new ArrayList<StopData>();
    public ArrayList<TripData> tripList = new ArrayList<TripData>();
    public RouteData(int type) {
        switch (type) {
            case 2 -> routeType = VehicleType.TRAIN;
            case 3 -> routeType = VehicleType.BUS;
            case 4 -> routeType = VehicleType.FERRY;
        }
    }
    public void tick() {

    }
}
