package com.decosegfault.hermes.data;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.decosegfault.hermes.HermesSim;
import com.decosegfault.hermes.RouteHandler;
import com.decosegfault.hermes.types.SimType;
import com.decosegfault.hermes.types.VehicleType;
import org.tinylog.Logger;

/**
 * @author Lachlan Ellis
 */
public class TripData {
    public VehicleType routeType;
    public String routeID;
    public String routeName;
    public List<Vector3> routeMap =  new ArrayList<>();
    public VehicleData vehicle;

    public float pathLength = 0;
    List<StopData> stopList = new ArrayList<>();
    public int startTime = 0;
    /** optional, only used in history mode */
    public int endTime = -1;
    //debug
    int tickCount = 0;

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
            vehicle.hidden = false;
            Vector3 newPosition = new Vector3();
            int shapeIndex = 0;
            if (RouteHandler.simType == SimType.HISTORY) {
                double traversedPercent = ( (HermesSim.time - startTime)) / (float) (endTime - startTime);
                double traversedDist = traversedPercent * pathLength;
                double recordedDist = 0;
//                Logger.warn("Traversed: {}%, {}m.", traversedPercent*100, traversedDist);
//                Logger.warn("Timing: {}, {}, {}.", startTime, endTime, HermesSim.time);
//                Logger.warn("Perc Calc: {}, {}.", HermesSim.time - startTime, endTime - startTime);
                if (routeMap.size() > 1) {
                    for (int i = 1; i < routeMap.size(); i++) {
                        Vector3 tempLastVect = new Vector3(routeMap.get(i - 1).x, routeMap.get(i - 1).y, 0);
                        Vector3 tempCurVect = new Vector3(routeMap.get(i).x, routeMap.get(i).y, 0);
//                    Logger.warn("Checking at: {}", recordedDist);
//                    Logger.warn("Checking using Vectors on count: {} {}, {} {}. {}",
//                        tempLastVect.x, tempLastVect.y, tempCurVect.x, tempCurVect.y, tickCount);
                        if (recordedDist + Math.abs(routeMap.get(i).dst(routeMap.get(i - 1))) >= traversedDist) {
                            shapeIndex = i;
                            newPosition = tempLastVect.add((tempCurVect.sub(routeMap.get(i - 1)))
                                .scl((float) (Math.abs(traversedDist - recordedDist)
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
            } else if (RouteHandler.simType == SimType.SIMULATED) {

            } else {

            }
            vehicle.position.set(newPosition.x, newPosition.y,
                    new Vector2(routeMap.get(shapeIndex).x - routeMap.get(shapeIndex-1).x,
                        routeMap.get(shapeIndex).y - routeMap.get(shapeIndex-1).y).angleDeg());
//            vehicle.position.set(newPosition.x, newPosition.y, (float) ((HermesSim.time*1000)%360));
            vehicle.oldPosition = new Vector3(vehicle.position.x, vehicle.position.y, vehicle.position.z);
//            Logger.warn("Angle: {} .", new Vector2(routeMap.get(shapeIndex).x -routeMap.get(shapeIndex-1).x,
//                routeMap.get(shapeIndex).y - routeMap.get(shapeIndex-1).x).angleDeg());
        } else {
            vehicle.position.set(new Vector3(-27.499593094511493f, 153.01620933407332f, 0f));
            vehicle.hidden = true;
        }
    }
}
