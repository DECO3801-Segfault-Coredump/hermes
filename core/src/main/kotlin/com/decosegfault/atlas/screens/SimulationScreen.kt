package com.decosegfault.atlas.screens

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.decosegfault.atlas.map.LRUTileCache
import com.decosegfault.atlas.util.Assets
import com.decosegfault.atlas.util.Assets.ASSETS
import ktx.app.clearScreen
import org.tinylog.kotlin.Logger
import kotlin.math.roundToInt

/**
 * Implements the main screen for rendering the simulation
 *
 * @author Matt Young
 */
class SimulationScreen(private val game: Game) : ScreenAdapter() {
    private val profiler = GLProfiler(Gdx.graphics as Lwjgl3Graphics)
    private val tileCache = LRUTileCache()
    private lateinit var stage: Stage
    private lateinit var debugLabel: Label

    private fun createTextUI() {
        val skin = ASSETS["ui/uiskin.json", Skin::class.java]
        debugLabel = Label("", skin, "window")
        debugLabel.pack()

        // debug UI
        val container = Table()
        container.pad(10f)
        container.add(debugLabel)
        container.setFillParent(true)
        container.bottom().left()
        container.pack()

        // add a label saying "(c) OpenStreetMap contributors" in the bottom right corner to comply with
        // the OpenStreetMap licence
        // ref: https://www.openstreetmap.org/copyright
        val container2 = Table()
        container2.pad(10f)
        container2.add(Label("(c) OpenStreetMap contributors", skin))
        container2.setFillParent(true)
        container2.bottom().right()
        container2.pack()

        stage.addActor(container)
        stage.addActor(container2)
    }

    override fun show() {
        Logger.info("Creating AtlasScreen")
        profiler.enable()
        stage = Stage(ScreenViewport())

        createTextUI()
    }

    override fun render(delta: Float) {
        clearScreen(0.0f, 0.0f, 0.0f)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // quit the game
            Gdx.app.exit()
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            // toggle debug
            Logger.debug("Toggling debug")
            debugLabel.isVisible = !debugLabel.isVisible
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            // clears tile LRU cache
            tileCache.purge()
        }

        val mem = ((Gdx.app.javaHeap + Gdx.app.nativeHeap) / 1e6).roundToInt()
        debugLabel.setText("FPS: ${Gdx.graphics.framesPerSecond}    Memory: $mem MB    " +
            "Draw calls: ${profiler.drawCalls}\n${tileCache.getStats()}")
        stage.act()
        stage.draw()

        profiler.reset()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height)
    }

    override fun hide() {
        // I believe this is valid, but if the game explodes, this is probably why
        dispose()
    }

    override fun dispose() {
        stage.dispose()
        tileCache.dispose()
        profiler.disable()
        // we should no longer need assets since we quit the game
        ASSETS.dispose()
    }
}
