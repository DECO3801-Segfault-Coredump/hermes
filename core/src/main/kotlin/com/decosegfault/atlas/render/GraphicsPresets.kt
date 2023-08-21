package com.decosegfault.atlas.render

/**
 * Graphics presets for the renderer
 *
 * @author Matt Young
 */
class GraphicsPresets {
    // presets will be:
    // - Genuine Potato (Worst possible settings, honestly just terrible)
    // - Standard (Balanced settings for most users)
    // - It Runs Crysis (Highest settings not expected to tank the FPS)
    // - NASA Supercomputer (Everything maxed out)

    private val genuinePotato = GraphicsPreset(
        name="Genuine Potato",
        description="Worst possible settings for atrocious computers",
        vehicleDrawDist=10.0f,
        vehicleLodDist=Float.MAX_VALUE, // always draw low LoDs
        tileDrawDist=25.0f,
    )

    private val standard = GraphicsPreset(
        name="Standard",
        description="Balanced settings for good framerate on most computers",
        vehicleDrawDist=100.0f,
        vehicleLodDist=50.0f,
        tileDrawDist=100.0f,
    )

    private val itRunsCrysis = GraphicsPreset(
        name="It Runs Crysis",
        description="High settings for powerful gaming PCs or workstations",
        vehicleDrawDist=200.0f,
        vehicleLodDist=100.0f,
        tileDrawDist=200.0f,
    )

    private val nasaSupercomputer = GraphicsPreset(
        name="NASA Supercomputer",
        description="Max possible settings for ridiculously overpowered PCs to flex",
        vehicleDrawDist=Float.MAX_VALUE, // always draw every vehicle
        vehicleLodDist=Float.MIN_VALUE, // always draw high LoDs
        tileDrawDist=Float.MAX_VALUE, // always draw every tile
    )

    private val presets = listOf(genuinePotato, standard, itRunsCrysis, nasaSupercomputer)

    fun forName(name: String): GraphicsPreset {
        return presets.first { it.name == name }
    }
}
