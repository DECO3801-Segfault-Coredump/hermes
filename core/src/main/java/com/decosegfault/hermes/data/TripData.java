package com.decosegfault.hermes.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.decosegfault.atlas.util.AtlasUtils;
import com.decosegfault.atlas.util.HPVector2;
import com.decosegfault.hermes.HermesSim;
import com.decosegfault.hermes.RouteHandler;
import com.decosegfault.hermes.types.SimType;
import com.decosegfault.hermes.types.VehicleType;
import com.decosegfault.atlas.util.HPVector3;
import org.tinylog.Logger;

/**
 * @author Lachlan Ellis
 */
public class TripData {
    public VehicleType routeType;
    public String routeID;
    public String routeName;
    public List<HPVector3> routeMap =  new ArrayList<>();
    public VehicleData vehicle;

    public double pathLength = 0;
    List<StopData> stopList = new ArrayList<>();
    public int startTime = 0;
    /** optional, only used in history mode */
    public int endTime = -1;
    //debug
    int tickCount = 0;

    //these two only used in Simulated mode
    double previousDist;
    public double previousTime = 0;

    String inBound;

    public TripData(int type) {
        switch (type) {
            case 2 -> routeType = VehicleType.TRAIN;
            case 4 -> routeType = VehicleType.FERRY;
            default -> routeType = VehicleType.BUS;
        }
        vehicle = new VehicleData(routeType);
    }

    public TripData(VehicleType type, String in) {
        routeType = type;
        vehicle = new VehicleData(routeType);
        inBound = in;
    }

    //
    public void initTrip() {
        if(routeMap.size() > 1) {
            for(int i = 1; i < routeMap.size(); i++) {
                pathLength += routeMap.get(i).dst(routeMap.get(i-1));
//                Logger.warn("Pathlength updated to: {} by {}", pathLength, routeMap.get(i).dst(routeMap.get(i-1)));
//                Logger.warn("Update based on: {}x {}y, {}x {}y",
//                    routeMap.get(i-1).x, routeMap.get(i-1).y, routeMap.get(i).x, routeMap.get(i).y);
            }
        } else {
            //throw error, badly formatted data
        }
    }

    public void tick() {
        if (startTime <= HermesSim.time && endTime >= HermesSim.time) {
//            Logger.warn("Currently Running: {}", routeName);
//            Logger.warn("Running: {} {}", routeName, routeID);
            vehicle.hidden = false;
            HPVector2 newPosition = new HPVector2(0, 0);
            int shapeIndex = 0;
            if (RouteHandler.simType != SimType.LIVE) {
                double traversedDist;
                if(RouteHandler.simType == SimType.SIMULATED) {
                    traversedDist = previousDist + (HermesSim.time - previousTime) * RouteHandler.vehicleSpeed;
                    previousDist = traversedDist;
                    previousTime = HermesSim.time;
                } else {
                    double traversedPercent = (HermesSim.time - startTime) /  (endTime - startTime);
                    traversedDist = traversedPercent * pathLength;
                }

//                if(Objects.equals(inBound, "1")) {
//                    Logger.warn("inverted");
//                    traversedPercent = 1 - traversedPercent;
//                }
//
                double recordedDist = 0;
//                Logger.warn("Traversed: {}%, {}m.", traversedPercent*100, traversedDist);
//                Logger.warn("Timing: {}, {}, {}.", startTime, endTime, HermesSim.time);
//                Logger.warn("Perc Calc: {}, {}.", HermesSim.time - startTime, endTime - startTime);
                if (routeMap.size() > 1) {
                    for (int i = 1; i < routeMap.size(); i++) {
//                        Vector3 tempLastVect = new Vector3(routeMap.get(i - 1).x, routeMap.get(i - 1).y, 0);
                        HPVector2 tempLastVect = new HPVector2(routeMap.get(i - 1).getX(), routeMap.get(i - 1).getY());
                        HPVector2 tempCurVect = new HPVector2(routeMap.get(i).getX(), routeMap.get(i).getY());
//                    Logger.warn("Checking at: {}", recordedDist);
//                    Logger.warn("Checking using Vectors on count: {} {}, {} {}. {}",
//                        tempLastVect.x, tempLastVect.y, tempCurVect.x, tempCurVect.y, tickCount);
                        if (recordedDist + Math.abs(routeMap.get(i).dst(routeMap.get(i - 1))) >= traversedDist) {
                            shapeIndex = i;
                            newPosition = tempLastVect.add((tempCurVect.sub(tempLastVect))
                                .scl( (Math.abs(traversedDist - recordedDist)
                                                                    / Math.abs(routeMap.get(i).dst(routeMap.get(i - 1))))));
//                        Logger.warn("Found! at: {}", recordedDist);
//                        Logger.warn("Scaled to: {}%", Math.abs(traversedDist-recordedDist)
//                            /Math.abs(routeMap.get(i).dst(routeMap.get(i-1))) * 100);
//                        Logger.warn("Scaling by: {} / {}", Math.abs(traversedDist-recordedDist),
//                            Math.abs(routeMap.get(i).dst(routeMap.get(i-1))));
                            break;
                        }
                        recordedDist += Math.abs(routeMap.get(i).dst(routeMap.get(i - 1)));
                    }
                } else {
                    //throw error, badly formatted data
                }
            } else {
                //any live updates
            }
            if(shapeIndex == 0) {
                vehicle.hidden = true;
                endTime = (int) HermesSim.time;
            } else {
                double angle = (-1 * new HPVector2(routeMap.get(shapeIndex).getX() - routeMap.get(shapeIndex - 1).getX(),
                    routeMap.get(shapeIndex).getY() - routeMap.get(shapeIndex - 1).getY()).angleDeg())%360;
//            if(Objects.equals(inBound, "1")) {
//                angle = (angle-180)%360;
//            }
                vehicle.position.set(newPosition.getX(), newPosition.getY(),
                    angle);
//            vehicle.position.set(newPosition.x, newPosition.y, (float) ((HermesSim.time*1000)%360));
                vehicle.oldPosition = new HPVector3(vehicle.position.getX(), vehicle.position.getY(), vehicle.position.getZ());
//            Logger.warn("Angle: {} .", new HPVector2(routeMap.get(shapeIndex).getX() - routeMap.get(shapeIndex - 1).getX(),
//                routeMap.get(shapeIndex).getY() - routeMap.get(shapeIndex - 1).getY()).angleDeg());
                Vector3 testVector = AtlasUtils.INSTANCE.latLongZoomToSlippyCoord(vehicle.position.getX(), vehicle.position.getY());
//            Logger.warn("Vehicle position: {}x, {}y, {}s, {}", vehicle.position.getX(), vehicle.position.getY(),
//                HermesSim.time, HermesSim.tickCount);
//            Logger.warn("Equivalent: {}x, {}y", testVector.x, testVector.y);
//            Logger.warn("Equivalent2: {}x, {}y", (float)vehicle.position.getX(), (float)vehicle.position.getY());
            }

        } else {
            vehicle.position.set(-27.499593094511493, 153.01620933407332, 0);
            vehicle.hidden = true;
        }
    }
}
