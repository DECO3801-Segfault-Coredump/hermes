package com.decosegfault.atlas.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.decosegfault.atlas.map.TileServerManager
import com.decosegfault.atlas.util.Assets
import com.decosegfault.atlas.util.ImageAnimation
import ktx.app.clearScreen
import org.tinylog.kotlin.Logger
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * Loading screen for the application. This class is based on my (Matt)'s previous work:
 * https://github.com/UQRacing/gazilla/blob/master/core/src/main/kotlin/com/uqracing/gazilla/client/screens/LoadingScreen.kt
 *
 * @author Matt Young
 */
class LoadingScreen(private val game: Game) : ScreenAdapter() {
    private enum class LoadingStage(val text: String) {
        STARTING_TILESERVER("Starting tile server..."),
        CHECKING_CONNECTIVITY("Checking tile server connectivity..."),
        LOADING_3D_ASSETS("Loading 3D assets..."),

        DONE("Done.")
    }

    private lateinit var skin: Skin
    private lateinit var stage: Stage
    private lateinit var label: Label
    private var currentStage = LoadingStage.STARTING_TILESERVER
    private lateinit var bkLoader: TextureAtlas
    private lateinit var loader: ImageAnimation

    override fun show() {
        skin = Skin(Gdx.files.internal("ui/uiskin.json"))
        stage = Stage(ScreenViewport())
        bkLoader = TextureAtlas(Gdx.files.internal("sprite/whatdadogdoin.atlas"))

        label = Label(currentStage.text, skin, "window")
        label.pack()

        loader = ImageAnimation()
        loader.setAnimation(Animation(0.01f, bkLoader.regions))
        loader.setOrigin(64.0f, 64.0f)
        loader.pack()

        val container = Table()
        container.add(loader).width(160.0f).height(120.0f)
        container.row().pad(10.0f)
        container.add(label)
        container.setFillParent(true)
        container.center()
        container.pack()

        stage.addActor(container)

        // run checks outside of render loop so we don't block render
        thread(isDaemon = true) {
            // Start tile server
            TileServerManager.maybeStartTileServer()
            currentStage = LoadingStage.CHECKING_CONNECTIVITY

            // Wait for tile server
            Logger.debug("Checking tile server connectivity")
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

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        label.setText(currentStage.text)
        stage.act(delta)
        stage.draw()

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }

        if (currentStage == LoadingStage.LOADING_3D_ASSETS) {
            if (Assets.ASSETS.update()) {
                Logger.info("Done loading assets")
                currentStage = LoadingStage.DONE
            } else {
                // not yet done
                val completion = (Assets.ASSETS.progress * 100.0).roundToInt()
                label.setText("Loading 3D assets... ($completion%)")
            }
        } else if (currentStage == LoadingStage.DONE) {
            game.screen = SimulationScreen(game)
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

    override fun hide() {
        dispose()
    }

    override fun dispose() {
        skin.dispose()
        stage.dispose()
        bkLoader.dispose()
    }
}
