package com.decosegfault.atlas.render

import org.tinylog.kotlin.Logger
import java.io.IOException
import java.nio.file.Paths
import kotlin.io.path.readText

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
    )

    private val standard = GraphicsPreset(
        name="Standard",
        description="Balanced settings for good framerate on most computers",
        vehicleDrawDist=130.0f,
        vehicleLodDist=70.0f,
        tileDrawDist=130.0f,
        anisotropic=16.0f,
        msaa=4,
    )

    private val itRunsCrysis = GraphicsPreset(
        name="It Runs Crysis",
        description="High settings for powerful gaming PCs or workstations",
        vehicleDrawDist=500.0f,
        vehicleLodDist=100.0f,
        tileDrawDist=200.0f,
        anisotropic=16.0f,
        msaa=8,
    )

    private val nasaSupercomputer = GraphicsPreset(
        name="NASA Supercomputer",
        description="Max possible settings for ridiculously overpowered PCs to flex",
        vehicleDrawDist=Float.MAX_VALUE, // always draw every vehicle
        vehicleLodDist=Float.MAX_VALUE, // always draw high LoDs
        tileDrawDist=Float.MAX_VALUE, // always draw every tile
        anisotropic=64.0f, // this is insane lmao
        msaa=32, // wtf
    )

    private val presets = listOf(genuinePotato, standard, itRunsCrysis, nasaSupercomputer)

    fun forName(name: String): GraphicsPreset? {
        return presets.firstOrNull { it.name == name }
    }

    fun getPresets(): List<GraphicsPreset> {
        return presets
    }

    fun getSavedGraphicsPreset(): GraphicsPreset {
        val path = Paths.get(System.getProperty("user.home"), "Documents", "DECOSegfault", "graphics.txt")
        Logger.debug("Loading graphics preset from: $path")
        var name: String
        try {
            name = path.readText().trim()
            Logger.debug("Using saved preset: $name")
        } catch (e: IOException) {
            Logger.debug("Using default preset, saved does not exist")
            name = "Standard"
        }
        return forName(name) ?: run {
            Logger.error("Invalid graphics preset $name!!! Using standard!")
            return standard
        }
    }
}
