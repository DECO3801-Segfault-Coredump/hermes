package com.decosegfault.atlas

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.profiling.GLProfiler
import ktx.app.clearScreen
import org.tinylog.kotlin.Logger

/** Implements the main screen for rendering the simulation */
class AtlasScreen(private val game: Game) : Screen {
    private lateinit var font: BitmapFont
    private lateinit var batch: SpriteBatch
    private val profiler = GLProfiler(Gdx.graphics as Lwjgl3Graphics)

    override fun show() {
        Logger.info("Creating AtlasScreen")
        font = BitmapFont(Gdx.files.internal("debug/debug.fnt"))
        batch = SpriteBatch()
        profiler.enable()
    }

    override fun render(delta: Float) {
        clearScreen(0.0f, 0.0f, 0.0f)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }

        batch.begin()
        font.draw(
            batch, "FPS: ${Gdx.graphics.framesPerSecond}   " +
                "MEM: ${(Gdx.app.javaHeap + Gdx.app.nativeHeap) / 1e6} MB    " +
                "DRAW: ${profiler.drawCalls}",
            5.0f, 32.0f
        )
        batch.end()

        profiler.reset()
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
        font.dispose()
        batch.dispose()
        profiler.disable()
    }
}
