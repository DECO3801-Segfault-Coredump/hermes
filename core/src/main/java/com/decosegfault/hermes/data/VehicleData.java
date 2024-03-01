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

import com.decosegfault.atlas.util.HPVector3;
import com.decosegfault.hermes.types.VehicleType;

/**
 * @author Lachlan Ellis
 */
public class VehicleData {
    public VehicleType vehicleType;
    public HPVector3 position = new HPVector3(0, 0, 0); //describes the 2d position and the orientation
    public HPVector3 oldPosition = new HPVector3(0, 0, 0);
    public boolean hidden = false;

    /**
     * @param type
     */
    public VehicleData(VehicleType type) {
        vehicleType = type;
    }

    /**
     * @param newPosition
     */
    public void tick(HPVector3 newPosition) {
        oldPosition = position;
        position = newPosition;
    }
}
