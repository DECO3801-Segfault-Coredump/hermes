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
