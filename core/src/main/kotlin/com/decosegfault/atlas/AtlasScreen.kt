package com.decosegfault.atlas

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import ktx.app.clearScreen
import org.tinylog.kotlin.Logger

/** Implements the main screen for rendering the simulation */
class AtlasScreen : Screen {
    override fun show() {
        Logger.info("Creating AtlasScreen")
    }

    override fun render(delta: Float) {
        clearScreen(0.0f, 0.0f, 0.0f)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }
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
    }
}
