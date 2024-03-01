/*
 * Copyright (c) 2023 DECO3801 Team Segmentation fault (core dumped).
 *
 * See the "@author" comment for who retains the copyright on this file.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.decosegfault.atlas.render

/**
 * Graphics preset for the render. **All distances are in metres.**
 *
 * @param vehicleDrawDist Above this distance, vehicles are not rendered
 * @param vehicleLodDist Above this distance, low poly LoDs are used; below, high poly LoDs are used
 * @param tileDrawDist Above this distance, map tiles are not rendered
 * @param anisotropic Number of texture anisotropic filtering samples
 * @param msaa MSAA samples
 * @param shadows true/false should shadows be rendered
 * @param workPerFrame number of items from SimulationScreen.WORK_QUEUE to process per frame
 *
 * @author Matt Young
 */
data class GraphicsPreset(
    val name: String,
    val description: String,

    val vehicleDrawDist: Float,
    val vehicleLodDist: Float,
    val tileDrawDist: Float,

    val anisotropic: Float,

    var msaa: Int,

    var shadows: Boolean,

    var workPerFrame: Int,
)
