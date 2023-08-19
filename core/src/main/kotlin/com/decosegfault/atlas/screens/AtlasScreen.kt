package com.decosegfault.atlas.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.decosegfault.atlas.map.LRUTileCache
import com.decosegfault.atlas.util.Assets
import com.decosegfault.atlas.util.Assets.ASSETS
import com.decosegfault.atlas.util.ImageAnimation
import ktx.app.clearScreen
import org.tinylog.kotlin.Logger

/**
 * Implements the main screen for rendering the simulation
 *
 * @author Matt Young, Henry Batt
 */
class AtlasScreen(private val game: Game) : Screen {
    private val profiler = GLProfiler(Gdx.graphics as Lwjgl3Graphics)
    private val tileCache = LRUTileCache()
    private lateinit var stage: Stage
    private lateinit var debugLabel: Label

    private fun createDebugUI() {
        val skin = ASSETS["ui/uiskin.json", Skin::class.java]
        debugLabel = Label("", skin, "window")
        debugLabel.pack()

        val container = Table()
        container.pad(10f)
        container.add(debugLabel)
        container.setFillParent(true)
        container.bottom().left()
        container.pack()

        stage.addActor(container)
    }

    override fun show() {
        Logger.info("Creating AtlasScreen")
        profiler.enable()
        stage = Stage(ScreenViewport())

        createDebugUI()
    }

    override fun render(delta: Float) {
        clearScreen(0.0f, 0.0f, 0.0f)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }

        val mem = (Gdx.app.javaHeap + Gdx.app.nativeHeap) / 1e6
        debugLabel.setText("FPS: ${Gdx.graphics.framesPerSecond}    Memory: $mem MiB    " +
         "Draw calls: ${profiler.drawCalls}\n${tileCache.getStats()}")

        stage.act()
        stage.draw()

        profiler.reset()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height)
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
        stage.dispose()
        profiler.disable()
        // we should no longer need assets since we quit the game
        Assets.ASSETS.dispose()
    }
}
