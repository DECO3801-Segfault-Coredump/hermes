package com.decosegfault.atlas.screens

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.decosegfault.atlas.map.LRUTileCache
import com.decosegfault.atlas.render.AtlasCameraController
import com.decosegfault.atlas.render.AtlasSceneManager
import com.decosegfault.atlas.render.AtlasVehicle
import com.decosegfault.atlas.render.GraphicsPresets
import com.decosegfault.atlas.util.Assets.ASSETS
import ktx.app.clearScreen
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneSkybox
import net.mgsx.gltf.scene3d.utils.IBLBuilder
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
    private val mux = InputMultiplexer()
    private val cam = PerspectiveCamera().apply {
        fieldOfView = 75f
        near = 0.1f
        far = 500f
    }
    private val camController = AtlasCameraController(cam).apply {
        translateButton = Input.Buttons.RIGHT
        translateUnits = 40f
        scrollFactor = -0.1f
    }
    private val cameraViewport = ExtendViewport(1600f, 900f, cam)
    private lateinit var sceneManager: AtlasSceneManager
    private lateinit var sun: DirectionalLightEx
    private val graphics = GraphicsPresets.forName("It Runs Crysis") // TODO load from Gdx.preferences
    private lateinit var shapeRender: ShapeRenderer

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

        shapeRender = ShapeRenderer()
    }

    // based on:
    // - https://github.com/mgsx-dev/gdx-gltf/blob/master/demo/core/src/net/mgsx/gltf/examples/GLTFQuickStartExample.java
    // - https://github.com/UQRacing/gazilla/blob/master/core/src/main/kotlin/com/uqracing/gazilla/client/screens/SimulationScreen.kt
    // -     (this is my [Matt's] own code from a previous project)
    private fun initialise3D() {
        sceneManager = AtlasSceneManager(graphics)
        sceneManager.setCamera(cam)

        sun = DirectionalLightEx()
        sun.direction.set(1f, -3f, 1f).nor()
        sun.color.set(Color.WHITE)
        sceneManager.environment.add(sun)

        // setup quick IBL (image based lighting)
        val iblBuilder = IBLBuilder.createOutdoor(sun)
        val environmentCubemap = iblBuilder.buildEnvMap(1024)
        val diffuseCubemap = iblBuilder.buildIrradianceMap(256)
        val specularCubemap = iblBuilder.buildRadianceMap(10)
        iblBuilder.dispose()

        val brdfLut = ASSETS["net/mgsx/gltf/shaders/brdfLUT.png", Texture::class.java]

        sceneManager.setAmbientLight(1f)
        sceneManager.environment.set(PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLut))
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap))
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap))

        // setup skybox
        sceneManager.skyBox = SceneSkybox(environmentCubemap)

        // force anisotropic filtering for all assets
        val models = com.badlogic.gdx.utils.Array<SceneAsset>()
        ASSETS.getAll(SceneAsset::class.java, models)
        for (model in models) {
            for (texture in model.textures) {
                texture.setAnisotropicFilter(16.0f) // TODO make this customisable in graphics
            }
        }
    }

    override fun show() {
        Logger.info("Creating AtlasScreen")
        profiler.enable()
        stage = Stage(ScreenViewport())

        createTextUI()
        initialise3D()

        mux.addProcessor(camController)
        mux.addProcessor(stage)
        Gdx.input.inputProcessor = mux
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

        // render 3D
        camController.update()
        cam.update()
        // TESTING CODE
        val trainLow = Scene(ASSETS["atlas/train_low.glb", SceneAsset::class.java].scene)
        val trainHigh = Scene(ASSETS["atlas/train_high.glb", SceneAsset::class.java].scene)
        val vehicle = AtlasVehicle(trainHigh.modelInstance, trainLow.modelInstance)
        vehicle.updateTransform(Vector3.Zero)
        val vehicles = listOf(vehicle)
        sceneManager.update(delta, vehicles)
        sceneManager.render()

        // render debug UI
        val mem = ((Gdx.app.javaHeap + Gdx.app.nativeHeap) / 1e6).roundToInt()
        val cullRate = ((sceneManager.culledVehicles / sceneManager.totalVehicles) * 100f).roundToInt()
        debugLabel.setText("""FPS: ${Gdx.graphics.framesPerSecond}    Memory: $mem MB    Draw calls: ${profiler.drawCalls}
            |${tileCache.getStats()}
            |Vehicles    cull rate: $cullRate%    total rendered: ${sceneManager.renderedVehicles}
            |Graphics preset: ${graphics.name}
        """.trimMargin())
        stage.act()
        stage.draw()

        if (debugLabel.isVisible) {
            shapeRender.projectionMatrix = cam.combined
            shapeRender.begin(ShapeRenderer.ShapeType.Line)
            vehicle.debug(shapeRender, cam)
            shapeRender.end()

            shapeRender.begin(ShapeRenderer.ShapeType.Filled)
            shapeRender.point(camController.target.x, camController.target.y, camController.target.z)
            shapeRender.end()
        }

        profiler.reset()
        sceneManager.resetStats()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height)
        cameraViewport.update(width, height)
    }

    override fun hide() {
        // I believe this is valid, but if the game explodes, this is probably why
        dispose()
    }

    override fun dispose() {
        stage.dispose()
        tileCache.dispose()
        profiler.disable()
        shapeRender.dispose()
        // we should no longer need assets since we quit the game
        ASSETS.dispose()
    }
}
