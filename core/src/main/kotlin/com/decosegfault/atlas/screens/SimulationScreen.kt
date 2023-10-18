package com.decosegfault.atlas.screens

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
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
import com.decosegfault.atlas.map.TileManager
import com.decosegfault.atlas.render.*
import com.decosegfault.atlas.util.Assets
import com.decosegfault.atlas.util.Assets.ASSETS
import com.decosegfault.atlas.util.FirstPersonCamController
import com.decosegfault.hermes.HermesSim
import com.decosegfault.hermes.RouteHandler
import com.decosegfault.hermes.types.SimType
import com.google.common.util.concurrent.ThreadFactoryBuilder
import ktx.app.clearScreen
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight
import net.mgsx.gltf.scene3d.scene.SceneSkybox
import net.mgsx.gltf.scene3d.utils.IBLBuilder
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.tinylog.kotlin.Logger
import java.time.LocalDateTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.*
import java.util.zip.Deflater
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime


/**
 * Implements the main screen for rendering the simulation
 *
 * @author Matt Young
 * @author Henry Batt
 */
class SimulationScreen(private val game: Game) : ScreenAdapter() {
    private val graphics = GraphicsPresets.getSavedGraphicsPreset()

    private val profiler = GLProfiler(Gdx.graphics as Lwjgl3Graphics)

    /** UI stage */
    private val stage = Stage(FitViewport(1920f, 1080f))

    /** Debug text */
    private lateinit var debugLabel: Label
    private val mux = InputMultiplexer()

    /** Status text */
    private lateinit var statusLabel: Label

    private var selectedVehicle: AtlasVehicle? = null

    private var followingBus = false

    private var showUIAdjustments = true

    private val cam = PerspectiveCamera().apply {
        fieldOfView = 75f
        near = 0.5f
        far = max(graphics.tileDrawDist, graphics.vehicleDrawDist) * 20
//        rotate(Vector3.X, -90f)
        translate(0f, 300f, 0f)
        update()
    }

    private val camController = FirstPersonCamController(cam)

    /** Viewport for main 3D camera */
    private val cameraViewport = ExtendViewport(1920f, 1080f, cam)
    private lateinit var sceneManager: AtlasSceneManager

    /** The sun light */
    private val sun = if (graphics.shadows) DirectionalShadowLight() else DirectionalLightEx()

    /** Debug shape renderer */
    private val shapeRender = ShapeRenderer()

    /** True if debug drawing enabled */
    private var isDebugDraw = System.getProperty("debug") != null

    /** Executor to schedule Hermes tick asynchronously in its own thread */
    private val hermesExecutor = Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder().setNameFormat("Hermes").build()
    )

    private val tileManager = TileManager()

    private val buildingManager = BuildingManager()

    private val skin = ASSETS["ui/uiskin.json", Skin::class.java]

    private val batch = SpriteBatch()

    private var hermesDelta = 0f

    private val deltaWindow = DescriptiveStatistics(1024)

    private lateinit var crosshairContainer: Table

    private fun createTextUI() {
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
        val debugContainer = Table()
        debugContainer.pad(10f)
        debugContainer.add(debugLabel)
        debugContainer.setFillParent(true)
        debugContainer.top().right()
        debugContainer.pack()

        // status details UI
        statusLabel = Label("", skin, "window")
        statusLabel.style = style
        statusLabel.pack()

        val statusContainer = Table()
        statusContainer.pad(10f)
        statusContainer.add(statusLabel)
        statusContainer.setFillParent(true)
        statusContainer.top().left()
        statusContainer.pack()

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

        crosshairContainer = Table()
        crosshairContainer.setFillParent(true)
        crosshairContainer.add(Image(ASSETS["sprite/crosshair.png", Texture::class.java])).width(32f).height(32f)
        crosshairContainer.center()
        crosshairContainer.pack()

        stage.addActor(debugContainer)
        stage.addActor(statusContainer)
        stage.addActor(crosshairContainer)
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
//        sceneManager.environment.set(ColorAttribute(ColorAttribute.Fog, Color.GRAY))
//        sceneManager.environment.set(FogAttribute(FogAttribute.FogEquation).set(1f, 2000f, 8f))

        // setup skybox
        sceneManager.skyBox = SceneSkybox(environmentCubemap)

        // setup ground plane tile manager
        sceneManager.setTileManager(tileManager)
        sceneManager.setBuildingManager(buildingManager)

        // setup decal batch for rendering
        sceneManager.decalBatch = DecalBatch(CameraGroupStrategy(cam))

        // apply graphics settings
        Assets.applyGraphicsPreset(graphics)

        // print OpenGL debug info for later diagnostics if/when the game crashes on someone's computer
        val v = Gdx.graphics.glVersion
        Logger.info("GL version: ${v.majorVersion}.${v.minorVersion}.${v.releaseVersion} vendor: ${v.vendorString} renderer: ${v.rendererString}")
    }

    private fun initialiseHermes() {
        // tell Hermes to tick every 100 ms, in its own thread asynchronously, so we don't block the renderer
        Logger.info("Hermes tick rate: $HERMES_TICK_RATE ms (delta: $HERMES_DELTA s)")
        hermesExecutor.scheduleAtFixedRate({
            try {
                hermesDelta = measureNanoTime { HermesSim.tick(HERMES_DELTA) } / 1e6f
            } catch (e: Exception) {
                Logger.error("Hermes exception: $e")
                Logger.error(e)
            }
        }, 0L, HERMES_TICK_RATE.toLong(), TimeUnit.MILLISECONDS)
    }

    override fun show() {
        Logger.info("Creating AtlasScreen")
        profiler.enable()

        Logger.info("Using graphics preset: ${graphics.name}")

        createTextUI()
        initialise3D()
//        constructBenchmarkScene()
        initialiseHermes()

        mux.addProcessor(camController)
        mux.addProcessor(stage)
        Gdx.input.inputProcessor = mux
    }

    override fun render(delta: Float) {
        clearScreen(0.0f, 0.0f, 0.0f)

        // first, handle work queue emergency situations to prevent your RAM from filling up
        if (WORK_QUEUE.size >= WORK_QUEUE_ABSOLUTE_MAX) {
            Logger.error("PANIC: Work queue emergency!! Size: ${WORK_QUEUE.size}")
            Logger.error("Stats: ${GCTileCache.getStats()}")
            WORK_QUEUE.clear()
            GCTileCache.dispose()
            throw OutOfMemoryError("PANIC: Work queue emergency!")
        }

        // try and take up to WORK_PER_FRAME items from the work queue and run them
        var workIdx = 0
        val iterator = WORK_QUEUE.iterator()
        while (iterator.hasNext() && workIdx < graphics.workPerFrame) {
            iterator.next().run()
            iterator.remove()
            workIdx++
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // quit the game
            Logger.debug("Quitting")
            Gdx.app.exit()
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            // toggle debug
            Logger.debug("Toggling debug")
            isDebugDraw = !isDebugDraw
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            // forces tile cache GC
            Logger.debug("Force tile cache GC next frame")
            GCTileCache.gcNextFrame()
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            // toggle fullscreen
            if (Gdx.graphics.isFullscreen) {
                Logger.debug("Enter windowed mode from fullscreen")
                Gdx.graphics.setWindowedMode(1600, 900)
            } else {
                Logger.debug("Enter fullscreen from window mode")
                Gdx.graphics.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode())
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            Logger.debug("Camera pose:\npos: ${cam.position}\ndirection: ${cam.direction}")
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            Logger.debug("Reset camera")
            cam.position.set(0f, 200f, 0f)
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            val vehicle = HermesSim.vehicleMap.values.filter { !it.hidden }.randomOrNull()
            if (vehicle != null) {
                Logger.debug("Going to randomly selected vehicle: $vehicle, ${vehicle.hashCode()}")
                val position = vehicle.transform.getTranslation(Vector3())
                cam.position.set(position.x, 200f, position.z)
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)) {
            Logger.debug("Increment speed")
            HermesSim.increaseSpeed()
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.COMMA)) {
            Logger.debug("Decrement speed")
            HermesSim.decreaseSpeed()
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            Logger.debug("Toggle follow bus")
            followingBus = !followingBus
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            Logger.debug("Toggle show UI adjustments")
            crosshairContainer.isVisible = !crosshairContainer.isVisible
            showUIAdjustments = !showUIAdjustments
        }

        // follow selected vehicle if enabled
        if (selectedVehicle != null && followingBus) {
            cam.position.set(selectedVehicle!!.bbox.getCenter(Vector3()))
            cam.position.y += 100f
        }

        // render 3D
        camController.update(delta)
        cam.update()
        sceneManager.update(delta, HermesSim.vehicleMap.values)
        sceneManager.render()
        GCTileCache.nextFrame()
        GCBuildingCache.nextFrame()

        // render debug UI
        deltaWindow.addValue(delta.toDouble())
        val deltaMin = (deltaWindow.min * 1000.0f).roundToInt()
        val deltaMax = (deltaWindow.max * 1000.0f).roundToInt()
        val deltaAvg = (deltaWindow.mean * 1000.0f).roundToInt()
        val deltaDev = (deltaWindow.standardDeviation * 1000.0f).roundToInt()

        if (isDebugDraw) {
            val mem = ((Gdx.app.javaHeap + Gdx.app.nativeHeap) / 1e6).roundToInt()
            debugLabel.setText(
            """FPS: ${Gdx.graphics.framesPerSecond}    Draw calls: ${profiler.drawCalls}    Memory: $mem MB
            |Delta    min: $deltaMin ms     max: $deltaMax ms     avg: $deltaAvg ms     stddev: $deltaDev ms
            |${GCTileCache.getStats()}
            |${GCBuildingCache.getStats()}
            |Vehicles    culled: ${sceneManager.cullRate}    low LoD: ${sceneManager.lowLodRate}    full: ${sceneManager.fullRenderRate}    total: ${sceneManager.totalVehicles}    on screen: ${sceneManager.lowLodRate + sceneManager.fullRenderRate}
            |Tiles on screen: ${tileManager.numRetrievedTiles}
            |Work queue    done: $workIdx    left: ${WORK_QUEUE.size}
            |Graphics preset: ${graphics.name}
            |Hermes compute time: ${hermesDelta.roundToInt()} ms
            |Hermes sim time: ${HermesSim.time.roundToInt()}
            |pitch: ${camController.quat.pitch}, roll: ${camController.quat.roll}, yaw: ${camController.quat.yaw}
            |x: ${cam.position.x}, y: ${cam.position.y}, z: ${cam.position.z}
            """.trimMargin())
        } else {
            debugLabel.setText("FPS: ${Gdx.graphics.framesPerSecond}    Draw calls: ${profiler.drawCalls}")
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Logger.debug("Select Vehicle")
            selectVehicle()
        }
        drawStatusText()

        // draw bounding boxes for vehicles
        if (isDebugDraw) {
            shapeRender.projectionMatrix = cam.combined
            shapeRender.begin(ShapeRenderer.ShapeType.Line)
            for (vehicle in HermesSim.vehicleMap.values) {
                vehicle.debug(shapeRender)
            }

            val tiles = tileManager.getTilesCulledHeightScaled(cam, graphics)
            for (tile in tiles) {
                tile.debug(shapeRender)
            }

            val buildingChunks = buildingManager.getBuildingChunksCulled(cam, graphics)
            for (buildingChunk in buildingChunks) {
                buildingChunk.debugBBox(shapeRender)
            }

            shapeRender.end()

        } else if (showUIAdjustments) {
            shapeRender.projectionMatrix = cam.combined
            shapeRender.begin(ShapeRenderer.ShapeType.Line)
            // this is stupid
            for (vehicle in HermesSim.vehicleMap.values) {
                if (vehicle == selectedVehicle) continue
                vehicle.draw(shapeRender, false)
            }
            selectedVehicle?.draw(shapeRender, true)
            shapeRender.end()
        }

        stage.act()
        stage.draw()

        profiler.reset()
        sceneManager.resetStats()

        if (Gdx.input.isKeyJustPressed(Input.Keys.SEMICOLON)) {
            Logger.debug("Taking screenshot")
            val pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.width, Gdx.graphics.height)
            PixmapIO.writePNG(Gdx.files.absolute("/tmp/atlas_screenshot.png"), pixmap, Deflater.BEST_COMPRESSION, true)
            pixmap.dispose()
        }
    }

    /**
     * Selects the vehicle at the current screen centre and saves if exists.
     */
    private fun selectVehicle() {
        val possibleVehicle = sceneManager.intersectRayVehicle(cam.getPickRay(Gdx.graphics.width/2f, Gdx.graphics.height/2f));
        if (possibleVehicle != null) {
            selectedVehicle = if (possibleVehicle == selectedVehicle) {
                // unselect by clicking twice
                null
            } else {
                // select by clicking once
                possibleVehicle
            }
        }
        Logger.debug("selectVehicle() selected $possibleVehicle")
    }

    /**
     * Calculates the time of current buses being displayed.
     * Live time for live data, and or historical time.
     */
    private fun calculateTime() : String {
        if (RouteHandler.simType == SimType.LIVE) {
            return java.time.LocalTime.now().format(TIME_FORMATTER)
        }

        val time = BASE_DATE.plusSeconds(HermesSim.time.toLong())
        return time.format(TIME_FORMATTER)
    }

    /**
     * Display the status information on the label.
     */
    private fun drawStatusText() {
        var vehicleName = "Not selected"
        if (selectedVehicle != null) {
            vehicleName = selectedVehicle!!.name
        }
        statusLabel.setText(
            """
            |Time:  ${calculateTime()}
            |Selected:    $vehicleName
            """.trimMargin())
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        cameraViewport.update(width, height, true)
                stage.camera.update()
    }

    override fun hide() {
        // I believe this is valid, but if the game explodes, this is probably why
        dispose()
    }

    override fun dispose() {
        HermesSim.shutdown()
        stage.dispose()
        GCTileCache.dispose()
        GCBuildingCache.dispose()
        profiler.disable()
        shapeRender.dispose()
        batch.dispose()
        Logger.debug("Shutting down Hermes executor")
        hermesExecutor.shutdownNow()
        // we should no longer need assets since we quit the game
        ASSETS.dispose()
        Logger.info("Goodbye! Enjoy your day")
    }

    companion object {
        /**
         * List of work items to process per frame. Unlike Gdx.app.postRunnable, only [WORK_PER_FRAME] are
         * processed per frame.
         */
        private val WORK_QUEUE = ConcurrentLinkedQueue<Runnable>()

        /** Absolute max number of items in the work queue to prevent RAM from filling up */
        private const val WORK_QUEUE_ABSOLUTE_MAX = 8192

        /** Hermes ticks every this many milliseconds */
        private const val HERMES_TICK_RATE = 50f

        /** Hermes tick delta */
        private const val HERMES_DELTA = HERMES_TICK_RATE / 1000f

        private val BASE_DATE = LocalDateTime.of(2023, Month.JANUARY, 1, 0, 0)

        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:m:s a")

        fun addWork(runnable: Runnable) {
            WORK_QUEUE.add(runnable)
        }
    }
}
