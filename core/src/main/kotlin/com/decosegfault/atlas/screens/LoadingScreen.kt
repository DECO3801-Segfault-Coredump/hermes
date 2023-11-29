/*
 * Copyright (c) 2023 DECO3801 Team Segmentation fault (core dumped).
 *
 * See the "@author" comment for who retains the copyright on this file.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.decosegfault.atlas.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.decosegfault.atlas.map.GCTileCache
import com.decosegfault.atlas.map.TileServerManager
import com.decosegfault.atlas.util.Assets
import com.decosegfault.atlas.util.AtlasUtils
import com.decosegfault.atlas.util.ImageAnimation
import com.decosegfault.hermes.HermesSim
import ktx.app.clearScreen
import org.tinylog.kotlin.Logger
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * Loading screen for the application. This class is partially based on my (Matt)'s previous work:
 * https://github.com/UQRacing/gazilla/blob/master/core/src/main/kotlin/com/uqracing/gazilla/client/screens/LoadingScreen.kt
 *
 * @author Matt Young
 */
class LoadingScreen(private val game: Game) : ScreenAdapter() {
    private enum class LoadingStage(val text: String) {
        STARTING_TILESERVER("Starting tile server..."),
        CHECKING_CONNECTIVITY("Checking tile server connectivity..."),
        LOADING_3D_ASSETS("Loading 3D assets..."),
        STARTING_HERMES("Starting Hermes..."),
        DONE("Done.")
    }

    private lateinit var skin: Skin
    private lateinit var stage: Stage
    private lateinit var label: Label
    private var currentStage = LoadingStage.STARTING_TILESERVER
    private lateinit var loadingGif: TextureAtlas
    private lateinit var loader: ImageAnimation
    private lateinit var noTileServer: Texture
    private var time = 0f

    override fun show() {
        GCTileCache.init()
        skin = Skin(Gdx.files.internal("ui/uiskin.json"))
        stage = Stage(ScreenViewport())
        loadingGif = TextureAtlas(Gdx.files.internal("sprite/whatdadogdoin.atlas"))
        noTileServer = Texture(Gdx.files.internal("sprite/notileserver.png"))

        label = Label(currentStage.text, skin, "window")
        label.pack()

        loader = ImageAnimation()
        loader.setAnimation(Animation(0.01f, loadingGif.regions))
        loader.pack()

        val container = Table()
        container.add(loader).width(200.0f).height(160.0f)
        container.row().pad(10.0f)
        container.add(label)
        container.setFillParent(true)
        container.center()
        container.pack()

        stage.addActor(container)

        // run checks outside of render loop so we don't block render
        thread(isDaemon=true, name="LoadWorker") {
            // Start tile server
            TileServerManager.maybeStartTileServer()
            currentStage = LoadingStage.CHECKING_CONNECTIVITY

            // Wait for tile server
            Logger.info("Checking tile server connectivity")
            while (!TileServerManager.pollTileServer()) {
                Thread.sleep(1000)
            }
            Thread.sleep(500)

            // 3D assets will now load in main thread
            Logger.info("Loading assets")
            Assets.load(Assets.ASSETS)
            currentStage = LoadingStage.LOADING_3D_ASSETS
            Thread.sleep(1000)
        }
    }

    /** Launches a thread which starts Hermes */
    private fun startHermes() {
        thread(isDaemon = true, name = "StartHermes") {
            // temporary
            Logger.info("Starting Hermes")
            val simType = AtlasUtils.readHermesPreset()
            HermesSim.load(simType)
            currentStage = LoadingStage.DONE
        }
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        label.setText(currentStage.text)
        stage.act(delta)
        stage.draw()

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }

        // if the tile server probably failed to start
        if ((currentStage == LoadingStage.STARTING_TILESERVER
            || currentStage == LoadingStage.CHECKING_CONNECTIVITY)
            && time >= 10f) {
            val region = TextureRegion(noTileServer)
            val animation = Animation(100f, region)
            loader.setAnimation(animation)
        }
        time += delta

        if (currentStage == LoadingStage.LOADING_3D_ASSETS) {
            if (Assets.ASSETS.update()) {
                Logger.info("Done loading assets")
                startHermes()
                currentStage = LoadingStage.STARTING_HERMES
            } else {
                // not yet done
                val completion = (Assets.ASSETS.progress * 100.0).roundToInt()
                label.setText("Loading 3D assets... ($completion%)")
            }
        } else if (currentStage == LoadingStage.DONE) {
            if (System.getProperty("uvtexturing") != null) {
                Logger.debug("Entering UVTexturingScreen")
                game.screen = UVTexturingScreen()
            } else {
                game.screen = SimulationScreen(game)
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun hide() {
        dispose()
    }

    override fun dispose() {
        skin.dispose()
        stage.dispose()
        loadingGif.dispose()
        noTileServer.dispose()
    }
}
