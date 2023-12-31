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

import org.tinylog.kotlin.Logger
import java.io.IOException
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Graphics presets for Atlas
 *
 * @author Matt Young
 */
object GraphicsPresets {
    // presets will be:
    // - Genuine Potato (Worst possible settings, honestly just terrible)
    // - Standard (Balanced settings for most users)
    // - It Runs Crysis (Highest settings not expected to tank the FPS)
    // - NASA Supercomputer (Everything maxed out)

    private val genuinePotato = GraphicsPreset(
        name="Genuine Potato",
        description="Worst possible settings for atrocious computers",
        vehicleDrawDist=75.0f,
        vehicleLodDist=Float.MIN_VALUE, // always draw low LoDs lol
        tileDrawDist=75.0f,
        anisotropic=1.0f,
        msaa=0,
        shadows=false,
        workPerFrame=1,
    )

    private val standard = GraphicsPreset(
        name="Standard",
        description="Balanced settings for good framerate on most computers",
        vehicleDrawDist=3000.0f,
        vehicleLodDist=200.0f,
        tileDrawDist=8000.0f,
        anisotropic=4.0f,
        msaa=4,
        shadows=false,
        workPerFrame=10,
    )

    private val itRunsCrysis = GraphicsPreset(
        name="It Runs Crysis",
        description="High settings for powerful gaming PCs or workstations",
        vehicleDrawDist=5000.0f,
        vehicleLodDist=500.0f,
        tileDrawDist=15000.0f,
        anisotropic=16.0f,
        msaa=8,
        shadows=false,
        workPerFrame=20,
    )

    private val nasaSupercomputer = GraphicsPreset(
        name="NASA Supercomputer",
        description="Max possible settings for ridiculously overpowered PCs to flex",
        vehicleDrawDist=Float.MAX_VALUE, // always draw every vehicle
        vehicleLodDist=Float.MAX_VALUE, // always draw high LoDs
        tileDrawDist=Float.MAX_VALUE, // always draw every tile
        anisotropic=64.0f, // this is insane lmao
        msaa=64, // wtf
        shadows=true,
        workPerFrame=Int.MAX_VALUE,
    )

    private val presets = listOf(genuinePotato, standard, itRunsCrysis, nasaSupercomputer)

    fun forName(name: String): GraphicsPreset? {
        return presets.firstOrNull { it.name == name }
    }

    fun getPresets(): List<GraphicsPreset> {
        return presets
    }

    fun getDefaultPreset(): GraphicsPreset {
        return standard
    }

    fun getSavedGraphicsPreset(): GraphicsPreset {
        val path = Paths.get(System.getProperty("user.home"), "Documents", "DECOSegfault", "graphics.txt")
        Logger.info("Loading graphics preset from: $path")
        var name: String
        try {
            name = path.readText().trim()
            Logger.info("Using saved preset: $name")
        } catch (e: IOException) {
            Logger.info("Using default preset, saved does not exist")
            name = "Standard"
        }
        return forName(name) ?: run {
            Logger.error("Invalid graphics preset $name!!! Using standard!")
            return standard
        }
    }

    fun writePreset(name: String) {
        val path = Paths.get(System.getProperty("user.home"), "Documents", "DECOSegfault", "graphics.txt")
        if (!path.exists()) {
            Logger.debug("Creating graphics.txt file")
            path.createFile()
        }
        Logger.info("Writing preset $name to path: $path")
        path.writeText(name)
    }
}
