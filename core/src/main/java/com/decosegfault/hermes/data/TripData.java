/*
 * Copyright (c) 2023 DECO3801 Team Segmentation fault (core dumped).
 *
 * See the "@author" comment for who retains the copyright on this file.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.decosegfault.hermes.data;

import com.badlogic.gdx.math.Vector3;
import com.decosegfault.atlas.util.AtlasUtils;
import com.decosegfault.atlas.util.HPVector2;
import com.decosegfault.atlas.util.HPVector3;
import com.decosegfault.hermes.HermesSim;
import com.decosegfault.hermes.RouteHandler;
import com.decosegfault.hermes.frontend.RouteExpectedReal;
import com.decosegfault.hermes.types.SimType;
import com.decosegfault.hermes.types.VehicleType;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the data for each individual trips.
 *
 * @author Lachlan Ellis
 * @author Henry Batt
 */
public class TripData {
    public VehicleType routeType;
    public String routeIDReal;
    public String routeID;
    public String routeName;
    public String routeVehicleName;
    public List<HPVector3> routeMap =  new ArrayList<>();
    public VehicleData vehicle;

    public double pathLength = 0;
    List<StopData> stopList = new ArrayList<>();
    public int startTime = 0;
    /** optional, only used in history mode */
    public int endTime = -1;
    //debug
    int tickCount = 0;

    // these two only used in Simulated mode
    double previousDist;
    public double previousTime = 0;

    String inBound;

    int actualEndTime;

    boolean didRouteEnd = false;

    /**
     * @param type
     */
    public TripData(int type) {
        switch (type) {
            case 2 -> routeType = VehicleType.TRAIN;
            case 4 -> routeType = VehicleType.FERRY;
            default -> routeType = VehicleType.BUS;
        }
        vehicle = new VehicleData(routeType);
    }

    /**
     * @param type
     * @param in
     */
    public TripData(VehicleType type, String in) {
        routeType = type;
        vehicle = new VehicleData(routeType);
        inBound = in;
    }

    /**
     *
     */
    public void initTrip() {
        if(routeMap.size() > 1) {
            for(int i = 1; i < routeMap.size(); i++) {
                pathLength += routeMap.get(i).dst(routeMap.get(i-1));
            }
        } else {
            //throw error, badly formatted data
        }
    }

    /**
     *
     */
    public void tick() {
        if (startTime <= HermesSim.time && endTime >= HermesSim.time) {
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

                double recordedDist = 0;
                if (routeMap.size() > 1) {
                    for (int i = 1; i < routeMap.size(); i++) {
                        HPVector2 tempLastVect = new HPVector2(routeMap.get(i - 1).getX(), routeMap.get(i - 1).getY());
                        HPVector2 tempCurVect = new HPVector2(routeMap.get(i).getX(), routeMap.get(i).getY());
                        if (recordedDist + Math.abs(routeMap.get(i).dst(routeMap.get(i - 1))) >= traversedDist) {
                            shapeIndex = i;
                            newPosition = tempLastVect.add((tempCurVect.sub(tempLastVect))
                                .scl( (Math.abs(traversedDist - recordedDist)
                                                                    / Math.abs(routeMap.get(i).dst(routeMap.get(i - 1))))));
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
                if (HermesSim.time >= startTime && !didRouteEnd) {
                    actualEndTime = (int) HermesSim.time;
                    didRouteEnd = true;
                }
            } else {
                double angle = (-1 * new HPVector2(routeMap.get(shapeIndex).getX() - routeMap.get(shapeIndex - 1).getX(),
                    routeMap.get(shapeIndex).getY() - routeMap.get(shapeIndex - 1).getY()).angleDeg())%360;
                vehicle.position.set(newPosition.getX(), newPosition.getY(),
                    angle);
                vehicle.oldPosition = new HPVector3(vehicle.position.getX(), vehicle.position.getY(), vehicle.position.getZ());
                Vector3 testVector = AtlasUtils.INSTANCE.latLongZoomToSlippyCoord(vehicle.position.getX(), vehicle.position.getY());
            }

        } else {
            vehicle.position.set(-27.499593094511493, 153.01620933407332, 0);
            vehicle.hidden = true;
            if (HermesSim.time >= startTime && !didRouteEnd) {
                actualEndTime = (int) HermesSim.time;

                RouteExpectedReal expectedReal = new RouteExpectedReal();
                expectedReal.setRouteName(this.routeName);
                expectedReal.setActualTime(this.actualEndTime);
                expectedReal.setExpectedTime(this.endTime);
                HermesSim.expectedReals.add(expectedReal);

                didRouteEnd = true;
            }
        }
    }
}
