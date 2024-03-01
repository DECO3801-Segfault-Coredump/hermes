/*
 * Copyright (c) 2023 DECO3801 Team Segmentation fault (core dumped).
 *
 * See the "@author" comment for who retains the copyright on this file.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.decosegfault.hermes.frontend;

import com.decosegfault.atlas.util.HPVector3;

import java.util.List;
import java.util.Map;

public class FrontendData {
    // red circles -> name: lat, long, radius
    private Map<String, HPVector3> interestPoints;

    // list of all the buses in the circles
//    private List<String> busesInInterest;
    private Map<List<String>, String> busesInInterest;

    // number of each route per tick (Ex. 66s)
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

    public Map<List<String>, String> getBusesInInterest() {
        return busesInInterest;
    }

    public void setBusesInInterest(Map<List<String>, String> busesInInterest) {
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
