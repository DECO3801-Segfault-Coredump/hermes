package com.decosegfault.atlas.screens

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.FitViewport
import com.decosegfault.atlas.map.BuildingManager
import com.decosegfault.atlas.map.GCBuildingCache
import com.decosegfault.atlas.map.GCTileCache
import com.decosegfault.atlas.render.*
import com.decosegfault.atlas.util.Assets
import com.decosegfault.atlas.util.Assets.ASSETS
import com.decosegfault.atlas.util.FirstPersonCamController
import com.decosegfault.hermes.VehicleType
import com.google.common.util.concurrent.ThreadFactoryBuilder
import ktx.app.clearScreen
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneSkybox
import net.mgsx.gltf.scene3d.utils.IBLBuilder
import org.tinylog.kotlin.Logger
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.Lock
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Implements the main screen for rendering the simulation
 *
 * @author Matt Young
 */
class SimulationScreen(private val game: Game) : ScreenAdapter() {
    private val graphics = GraphicsPresets.getSavedGraphicsPreset()

    private val profiler = GLProfiler(Gdx.graphics as Lwjgl3Graphics)

    /** UI stage */
    private val stage = Stage(FitViewport(1920f, 1080f))

    /** Debug text */
    private lateinit var debugLabel: Label
    private val mux = InputMultiplexer()

    private val cam = PerspectiveCamera().apply {
        fieldOfView = 75f
        near = 0.1f
        far = max(graphics.tileDrawDist, graphics.vehicleDrawDist) * 10
//        rotate(Vector3.X, -90f)
        translate(0f, 300f, 0f)
        update()
    }

    /** a camera used to test culling */
    private val debugCam = PerspectiveCamera().apply {
        fieldOfView = 75f
        near = 0.1f
        far = 900f
        position.set(-71.186066f,56.803688f,97.34365f)
        direction.set(0.4725332f,-0.3691399f,-0.80027723f)
        update()
    }

    private val camController = FirstPersonCamController(cam)

    private var isUsingDebugCam = false

    /** Viewport for main 3D camera */
    private val cameraViewport = ExtendViewport(1920f, 1080f, cam)
    private lateinit var sceneManager: AtlasSceneManager

    /** The sun light */
    private val sun = if (graphics.shadows) DirectionalShadowLight() else DirectionalLightEx()

    /** Debug shape renderer */
    private val shapeRender = ShapeRenderer()

    /** Vehicles for benchmark, in future this will be from Hermes */
    private val vehicles = mutableListOf<AtlasVehicle>()

    /** Counter for when to move vehicles */
    private var debugCounter = 0.0f

    /** True if vehicles should move in benchmark */
    private var shouldVehiclesMove = true

    /** True if debug drawing enabled */
    private var isDebugDraw = System.getProperty("debug") != null

    /** Executor to schedule Hermes tick asynchronously in its own thread */
    private val hermesExecutor = Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder().setNameFormat("Hermes").build()
    )

    private val atlasTileManager = AtlasTileManager()

    private val buildingManager = BuildingManager()

    private fun createTextUI() {
        val skin = ASSETS["ui/uiskin.json", Skin::class.java]

        // hack to use linear scaling instead of nearest neighbour for text
        // makes the text slightly less ugly, but ideally we should use FreeType
        // https://stackoverflow.com/a/33633682/5007892
        for (region in skin.getFont("window").regions) {
            region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }

        // generate black texture for background
        // https://stackoverflow.com/a/38881685/5007892
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGB888)
        pixmap.setColor(Color.BLACK)
        pixmap.fill()
        val black = Texture(pixmap)
        pixmap.dispose()

        debugLabel = Label("", skin, "window")
        val style = Label.LabelStyle(debugLabel.style)
        style.background = Image(black).drawable
        debugLabel.style = style
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
        val osmContainer = Table()
        osmContainer.pad(10f)
        val osmLabel = Label("(c) OpenStreetMap contributors", skin, "window")
        osmLabel.style = style
        osmContainer.add(osmLabel)
        osmContainer.setFillParent(true)
        osmContainer.bottom().right()
        osmContainer.pack()

        stage.addActor(container)
        stage.addActor(osmContainer)
    }

    // based on:
    // - https://github.com/mgsx-dev/gdx-gltf/blob/master/demo/core/src/net/mgsx/gltf/examples/GLTFQuickStartExample.java
    // - https://github.com/UQRacing/gazilla/blob/master/core/src/main/kotlin/com/uqracing/gazilla/client/screens/SimulationScreen.kt
    // -     (this is my [Matt's] own code from a previous project)
    private fun initialise3D() {
        sceneManager = AtlasSceneManager(graphics)
        sceneManager.setCamera(cam)

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

        // setup ground plane tile manager
        sceneManager.setAtlasTileManager(atlasTileManager)
        sceneManager.setBuildingManager(buildingManager)

        // setup decal batch for rendering
        sceneManager.decalBatch = DecalBatch(CameraGroupStrategy(cam))

        // apply graphics settings
        Assets.applyGraphicsPreset(graphics)

        // print OpenGL debug info for later diagnostics if/when the game crashes on someone's computer
        val v = Gdx.graphics.glVersion
        Logger.info("GL version: ${v.majorVersion}.${v.minorVersion}.${v.releaseVersion} vendor: ${v.vendorString} renderer: ${v.rendererString}")
    }

    private fun constructBenchmarkScene() {
        Logger.debug("Construct test scene")
        vehicles.clear()
        for (i in 0..200) {
            val modelName = listOf("bus", "train", "ferry").random()
            val modelLow = ASSETS["atlas/${modelName}_low.glb", SceneAsset::class.java]
            val modelHigh = ASSETS["atlas/${modelName}_high.glb", SceneAsset::class.java]
            val vehicle = AtlasVehicle(modelHigh, modelLow, if (modelName == "train") VehicleType.TRAIN else VehicleType.BUS)
            vehicle.updateTransform(Vector3.Zero)
            vehicles.add(vehicle)
        }
    }

    private fun initialiseHermes() {
        // Hermes.init()

        // tell Hermes to tick every 100 ms, in its own thread asynchronously, so we don't block the renderer
        hermesExecutor.scheduleAtFixedRate({
            // HermesSim.tick()
        }, 0L, 100L, TimeUnit.MILLISECONDS)
    }

    override fun show() {
        Logger.info("Creating AtlasScreen")
        profiler.enable()

        Logger.info("Using graphics preset: ${graphics.name}")

        createTextUI()
        initialise3D()
        constructBenchmarkScene()
        initialiseHermes()

        mux.addProcessor(camController)
        mux.addProcessor(stage)
        Gdx.input.inputProcessor = mux
    }

    override fun render(delta: Float) {
        clearScreen(0.0f, 0.0f, 0.0f)

        // first, handle work queue emergency situations to prevent your RAM from filling up
        if (TEX_WORK_QUEUE.size >= WORK_QUEUE_ABSOLUTE_MAX) {
            Logger.error("PANIC: Work queue emergency!! Size: ${TEX_WORK_QUEUE.size}")
            Logger.error("Stats: ${GCTileCache.getStats()}")
            TEX_WORK_QUEUE.clear()
            GCTileCache.dispose()
            throw OutOfMemoryError("PANIC: Work queue emergency!")
        }

        // try and take up to WORK_PER_FRAME items from the work queue and run them
        var workIdx = 0
        synchronized(TEX_WORK_QUEUE) {
            var item = TEX_WORK_QUEUE.poll()
            while (item != null && workIdx < WORK_PER_FRAME) {
                item.run()
                item = TEX_WORK_QUEUE.poll()
                workIdx++
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // quit the game
            Gdx.app.exit()
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            // toggle debug
            Logger.debug("Toggling debug")
            isDebugDraw = !isDebugDraw
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            // forces tile cache GC
            GCTileCache.garbageCollect(true)
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            // toggle fullscreen
            if (Gdx.graphics.isFullscreen) {
                Logger.debug("Enter windowed mode from fullscreen")
                Gdx.graphics.setWindowedMode(1600, 900)
            } else {
                Logger.debug("Enter fullscreen from window mode")
                Gdx.graphics.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode())
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            // vehicle movement in benchmark
            Logger.debug("Toggling vehicle movement")
            shouldVehiclesMove = !shouldVehiclesMove
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            // debug camera pose and frustum culling
            //Logger.debug("Camera pose:\npos: ${cam.position}\ndirection: ${cam.direction}")
            Logger.debug("Toggling usingDebugCam")
            isUsingDebugCam = !isUsingDebugCam
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            Logger.debug("Reset camera")
            cam.position.set(0f, 200f, 0f)
        }

        // update benchmark
        debugCounter += delta
        if (debugCounter >= 0.5f && shouldVehiclesMove) {
            debugCounter = 0f
            for (vehicle in vehicles) {
                // x, y, theta degrees
                val x = Random.nextDouble(-50.0, 50.0).toFloat()
                val y = Random.nextDouble(-50.0, 50.0).toFloat()
                val pos = Vector3(x, y, 0f)
                vehicle.updateTransform(pos)
            }
        }

        // render 3D
        camController.update(delta)
        if (isUsingDebugCam) {
            // move camera to debug spot
            cam.position.set(debugCam.position)
            cam.direction.set(debugCam.direction)
            cam.up.set(debugCam.up)
            // use old frustum so we get culling
            cam.update(false)
        } else {
            cam.update()
        }
        sceneManager.update(delta, vehicles)
        sceneManager.render()
        GCTileCache.nextFrame()
        GCBuildingCache.nextFrame()

        // render debug UI
        if (isDebugDraw) {
            val mem = ((Gdx.app.javaHeap + Gdx.app.nativeHeap) / 1e6).roundToInt()
            val deltaMs = (delta * 1000.0f).roundToInt()
            debugLabel.setText(
            """FPS: ${Gdx.graphics.framesPerSecond} (${deltaMs} ms)    Draw calls: ${profiler.drawCalls}    Memory: $mem MB
            |${GCTileCache.getStats()}
            |${GCBuildingCache.getStats()}
            |Vehicles    culled: ${sceneManager.cullRate}%    low LoD: ${sceneManager.lowLodRate}%    full: ${sceneManager.fullRenderRate}%    total: ${sceneManager.totalVehicles}
            |Tiles on screen: ${atlasTileManager.numRetrievedTiles}
            |Texture work queue    done: $workIdx    left: ${TEX_WORK_QUEUE.size}
            |Graphics preset: ${graphics.name}
            |pitch: ${camController.quat.pitch}, roll: ${camController.quat.roll}, yaw: ${camController.quat.yaw}
            """.trimMargin())
        } else {
            debugLabel.setText("FPS: ${Gdx.graphics.framesPerSecond}    Draw calls: ${profiler.drawCalls}")
        }

        // draw bounding boxes for vehicles
        if (isDebugDraw) {
            shapeRender.projectionMatrix = cam.combined
            shapeRender.begin(ShapeRenderer.ShapeType.Line)
            for (vehicle in vehicles) {
                vehicle.debug(shapeRender)
            }

            for (tile in atlasTileManager.getTilesCulled(cam, graphics)) {
                tile.debug(shapeRender)
            }

            shapeRender.end()

//            shapeRender.begin(ShapeRenderer.ShapeType.Filled)
//            shapeRender.point(camController.target.x, camController.target.y, camController.target.z)
//            shapeRender.end()
        }

//        shapeRender.projectionMatrix = stage.camera.combined
//        shapeRender.begin(ShapeRenderer.ShapeType.Filled)
//        shapeRender.color = Color.BLACK
//        shapeRender.circle(Gdx.graphics.width / 2f, Gdx.graphics.height / 2f, 4f)
//        shapeRender.end()

        stage.act()
        stage.draw()

        profiler.reset()
        sceneManager.resetStats()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        cameraViewport.update(width, height)
    }

    override fun hide() {
        // I believe this is valid, but if the game explodes, this is probably why
        dispose()
    }

    override fun dispose() {
        stage.dispose()
        GCTileCache.dispose()
        profiler.disable()
        shapeRender.dispose()
        Logger.debug("Shutting down Hermes executor")
        hermesExecutor.shutdownNow()
        // we should no longer need assets since we quit the game
        ASSETS.dispose()
    }

    companion object {
        /**
         * List of work items to process per frame. Unlike Gdx.app.postRunnable, only [WORK_PER_FRAME] are
         * processed per frame.
         *
         * **IMPORTANT:** MUST BE SYNCHRONISED USING A `synchronized` BLOCK! This is not concurrent
         * by default due to the stupid way in which I pull items off the queue.
         */
        val TEX_WORK_QUEUE = LinkedList<Runnable>()

        /** Number of items from [TEX_WORK_QUEUE] to process per frame */
        private const val WORK_PER_FRAME = 50

        /** Absolute max number of items in the work queue to prevent RAM from filling up */
        private const val WORK_QUEUE_ABSOLUTE_MAX = 8192
    }
}
