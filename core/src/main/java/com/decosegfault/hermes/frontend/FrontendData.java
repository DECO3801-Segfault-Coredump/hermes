package com.decosegfault.hermes.frontend;

import com.decosegfault.hermes.types.VehicleType;
import org.jetbrains.annotations.Contract;

public class FrontendData {
    public String routeID;
    public String routeShortName;
    public String routeLongName;
    public VehicleType vehicleType;
    public String expectedTime;
    public String actualTime;

    public void setRouteID(String routeID) {
        this.routeID = routeID;
    }

    public void setRouteShortName(String routeShortName) {
        this.routeShortName = routeShortName;
    }

    public void setRouteLongName(String routeLongName) {
        this.routeLongName = routeLongName;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public void setExpectedTime(String expectedTime) {
        this.expectedTime = expectedTime;
    }

    public void setActualTime(String actualTime) {
        this.actualTime = actualTime;
    }

    public String getRouteID() {
        return routeID;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public String getRouteLongName() {
        return routeLongName;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public String getExpectedTime() {
        return expectedTime;
    }

    public String getActualTime() {
        return actualTime;
    }

    /**
    public void setRouteID(String routeId) {
        routeID = routeId;
    }

    public void setRouteShortName(String shortName) {
        routeShortName = shortName;
    }

    public void setRouteLongName(String longName) {
        routeLongName = longName;
    }

    public void setVehicleType(VehicleType type) {
        vehicleType = type;
    }

    public void setExpectedTime(String time) {
        expectedTime = time;
    }

    public void setActualTime(String time) {
        actualTime = time;
    }
     */
}
