package com.decosegfault.atlas.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import net.mgsx.gltf.loaders.glb.GLBAssetLoader
import net.mgsx.gltf.loaders.gltf.GLTFAssetLoader
import net.mgsx.gltf.scene3d.scene.SceneAsset

/**
 * Asset manager for Atlas
 *
 * This was borrowed from my (Matt) previous work:
 * [https://github.com/UQRacing/gazilla/blob/master/core/src/main/kotlin/com/uqracing/gazilla/client/utils/Assets.kt]
 */
object Assets {
    private val allowedExtensions = listOf("glb", "atlas", "png", "jpg", "fnt")

    /**
     * Recursively loads all assets from the directory [path] as a [clazz] type into [assets]
     */
    private fun <T> loadFromDir(assets: AssetManager, path: FileHandle, clazz: Class<T>) {
        val files = path.list()

        for (file in files) {
            if (file.isDirectory) {
                loadFromDir(assets, file, clazz)
            } else if (file.extension() in allowedExtensions) {
                assets.load(file.path(), clazz)
            }
        }
    }

    /**
     * Loads all assets required into the specified AssetManager
     */
    fun load(assets: AssetManager) {
        assets.logger.level = com.badlogic.gdx.utils.Logger.DEBUG
        assets.setLoader(SceneAsset::class.java, ".gltf", GLTFAssetLoader())
        assets.setLoader(SceneAsset::class.java, ".glb", GLBAssetLoader())

        // load misc assets
        assets.load("net/mgsx/gltf/shaders/brdfLUT.png", Texture::class.java)
        assets.load("debug/debug.fnt", BitmapFont::class.java)
        assets.load("ui/uiskin.json", Skin::class.java)

        // load vehicle 3D models
        loadFromDir(assets, Gdx.files.internal("assets/atlas"), SceneAsset::class.java)
    }

    val ASSETS = AssetManager()
}
