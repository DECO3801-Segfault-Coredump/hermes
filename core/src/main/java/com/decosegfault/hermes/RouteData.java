package com.decosegfault.hermes;

import java.util.ArrayList;
import com.badlogic.gdx.math.Vector2;

/**
 * @author Lachlan Ellis
 */
public class RouteData {
    VehicleType routeType;
    int routeNumber;
    String routeName;
    ArrayList<Vector2> routeMap;
    ArrayList<VehicleData> vehicleList;
    ArrayList<StopData> stopList;
    public void tick() {

    }
}
