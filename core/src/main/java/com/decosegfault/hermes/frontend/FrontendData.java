package com.decosegfault.hermes.frontend;

import com.decosegfault.atlas.util.HPVector2;
import com.decosegfault.atlas.util.HPVector3;
import com.decosegfault.hermes.types.VehicleType;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.Map;

public class FrontendData {
    // red circles -> name: lat, long, radius
    private Map<String, HPVector3> interestPoints;

    // list of all the buses in the circles
    private List<String> busesInInterest;

    // number of 66s per tick
    private Map<String, Integer> routeFrequency;

    // number of vehicles in the entire simulation per type (pie chart)
    private Map<String, Integer> vehicleTypes;

    // expected vs real route arrival time
    private List<RouteExpectedReal> routeExpectedReals;

    public Map<String, HPVector3> getInterestPoints() {
        return interestPoints;
    }

    public void setInterestPoints(Map<String, HPVector3> interestPoints) {
        this.interestPoints = interestPoints;
    }

    public List<String> getBusesInInterest() {
        return busesInInterest;
    }

    public void setBusesInInterest(List<String> busesInInterest) {
        this.busesInInterest = busesInInterest;
    }

    public Map<String, Integer> getRouteFrequency() {
        return routeFrequency;
    }

    public void setRouteFrequency(Map<String, Integer> routeFrequency) {
        this.routeFrequency = routeFrequency;
    }

    public Map<String, Integer> getVehicleTypes() {
        return vehicleTypes;
    }

    public void setVehicleTypes(Map<String, Integer> vehicleTypes) {
        this.vehicleTypes = vehicleTypes;
    }

    public List<RouteExpectedReal> getRouteExpectedReals() {
        return routeExpectedReals;
    }

    public void setRouteExpectedReals(List<RouteExpectedReal> routeExpectedReals) {
        this.routeExpectedReals = routeExpectedReals;
    }
}
