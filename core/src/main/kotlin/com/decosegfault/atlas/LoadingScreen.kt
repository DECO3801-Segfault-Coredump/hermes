package com.decosegfault.atlas

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.clearScreen

private enum class LoadingStage(val text: String) {
    STARTING_TILESERVER("Starting tile server..."),
    CHECKING_CONNECTIVITY("Checking tile server connectivity..."),
    LOADING_3D_ASSETS("Loading 3D assets..."),

    DONE("Done.")
}

// This class is based on my (Matt)'s previous work:
// https://github.com/UQRacing/gazilla/blob/master/core/src/main/kotlin/com/uqracing/gazilla/client/screens/LoadingScreen.kt
class LoadingScreen(private val game: Game) : ScreenAdapter() {
    private lateinit var skin: Skin
    private lateinit var stage: Stage
    private lateinit var label: Label
    private var currentStage = LoadingStage.STARTING_TILESERVER
    private lateinit var whatTheScallop: Texture
    private lateinit var loader: Image

    override fun show() {
        skin = Skin(Gdx.files.internal("ui/uiskin.json"))
        stage = Stage(ScreenViewport())
        whatTheScallop = Texture(Gdx.files.internal("sprite/whatthescallop.jpg"))

        label = Label(currentStage.text, skin, "window")
        label.pack()

        loader = Image(whatTheScallop)
        loader.setSize(128.0f, 128.0f)
        loader.setOrigin(64.0f, 64.0f)
        loader.pack()

        val container = Table()
        container.add(loader).maxWidth(128.0f).minWidth(128.0f).width(128.0f).height(128.0f)
        container.row()
        container.add(label)
        container.setFillParent(true)
        container.center()
        container.pack()

        stage.addActor(container)
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        label.setText(currentStage.text)
        stage.act(delta)
        stage.draw()

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }

        loader.rotation += 100.0f * delta

        // loading state machine
        when (currentStage) {
            LoadingStage.STARTING_TILESERVER -> {
                TileServerManager.maybeStartTileServer()
                currentStage = LoadingStage.CHECKING_CONNECTIVITY
            }

            LoadingStage.CHECKING_CONNECTIVITY -> {
                // TODO
                currentStage = LoadingStage.LOADING_3D_ASSETS
            }

            LoadingStage.LOADING_3D_ASSETS -> {
                // TODO
                currentStage = LoadingStage.DONE
            }

            LoadingStage.DONE -> {
                // TODO change screens
                game.screen = AtlasScreen(game)
            }
        }

//        if (ASSETS.update()) {
//            // finished loading assets
//            Logger.debug("Finished loading assets")
//            label.setText("Initialising renderer...")
//            // make sure the text gets updated
//            stage.act(delta)
//            stage.draw()
//            game.screen = SimulationScreen()
//        } else {
//            // not yet done
//            val completion = (ASSETS.progress * 100.0).roundToInt()
//            label.setText("Loading assets ($completion%)")
//        }
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        skin.dispose()
        stage.dispose()
        whatTheScallop.dispose()
    }
}

