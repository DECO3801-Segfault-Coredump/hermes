package com.decosegfault.atlas.render

/**
 * Graphics preset for the render. **All distances are in metres.**
 *
 * @param vehicleDrawDist Above this distance, vehicles are not rendered
 * @param vehicleLodDist Above this distance, low poly LoDs are used; below, high poly LoDs are used
 * @param tileDrawDist Above this distance, map tiles are not rendered
 * @param anisotropic Number of texture anisotropic filtering samples
 * @param msaa MSAA samples (currently not implemented)
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
)
