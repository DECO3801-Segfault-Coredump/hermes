/*
 * Copyright (c) 2023 DECO3801 Team Segmentation fault (core dumped).
 *
 * See the "@author" comment for who retains the copyright on this file.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.decosegfault.atlas.util

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.decosegfault.atlas.render.GraphicsPreset
import net.mgsx.gltf.loaders.glb.GLBAssetLoader
import net.mgsx.gltf.loaders.gltf.GLTFAssetLoader
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.utils.MaterialConverter
import org.tinylog.kotlin.Logger

/**
 * Asset manager for Atlas
 *
 * This was borrowed from my (Matt) previous work:
 * [https://github.com/UQRacing/gazilla/blob/master/core/src/main/kotlin/com/uqracing/gazilla/client/utils/Assets.kt]
 * @author Matt Young
 * @author Henry Batt
 */
object Assets {
    private val allowedExtensions = listOf("glb", "atlas", "png", "jpg", "fnt")

    /**
     * Loads all assets required into the specified AssetManager
     */
    fun load(assets: AssetManager) {
//        assets.logger.level = com.badlogic.gdx.utils.Logger.DEBUG
        assets.setLoader(SceneAsset::class.java, ".gltf", GLTFAssetLoader())
        assets.setLoader(SceneAsset::class.java, ".glb", GLBAssetLoader())

        // load misc assets
        assets.load("net/mgsx/gltf/shaders/brdfLUT.png", Texture::class.java)
        assets.load("debug/debug.fnt", BitmapFont::class.java)
        assets.load("ui/uiskin.json", Skin::class.java)
        assets.load("sprite/blocks1.jpg", Texture::class.java)
        assets.load("sprite/brown_age_by_darkwood67.jpg", Texture::class.java)
        assets.load("sprite/brown_ice_by_darkwood67.jpg", Texture::class.java)
        assets.load("sprite/crosshair.png", Texture::class.java)
        assets.load("sprite/uvchecker1.png", Texture::class.java)

        // load vehicle 3D models
        val vehicles = listOf("bus", "train", "ferry")
        for (vehicle in vehicles) {
            assets.load("atlas/${vehicle}_low.glb", SceneAsset::class.java)
            assets.load("atlas/${vehicle}_high.glb", SceneAsset::class.java)
        }
    }

    /** Applies the graphics setting [graphics] to all assets previously loaded */
    fun applyGraphicsPreset(graphics: GraphicsPreset) {
        // force anisotropic filtering for all assets based on graphics preset level
        val models = com.badlogic.gdx.utils.Array<SceneAsset>()
        ASSETS.getAll(SceneAsset::class.java, models)
        Logger.info("Updating ${models.size} models to have ${graphics.anisotropic}x anisotropic filtering")
        for (model in models) {
            for (texture in model.textures) {
                texture.setAnisotropicFilter(graphics.anisotropic)
            }
        }

        // if we are in Genuine Potato mode, convert PBR assets to non-PBR
        if (graphics.name == "Genuine Potato") {
            for (model in models) {
                MaterialConverter.makeCompatible(model.scene.model.materials)
            }
            Logger.info("Converted PBR materials to non-PBR for Genuine Potato graphics mode")
        }
    }

    val ASSETS = AssetManager()
}
