package com.decosegfault.atlas.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.FitViewport
import com.decosegfault.atlas.map.BuildingGenerator
import com.decosegfault.atlas.map.GCBuildingCache
import com.decosegfault.atlas.map.GCTileCache
import com.decosegfault.atlas.render.AtlasSceneManager
import com.decosegfault.atlas.render.GraphicsPresets
import com.decosegfault.atlas.util.Assets
import com.decosegfault.atlas.util.FirstPersonCamController
import com.decosegfault.atlas.util.Triangle
import com.decosegfault.hermes.HermesSim
import ktx.app.clearScreen
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight
import net.mgsx.gltf.scene3d.scene.SceneSkybox
import net.mgsx.gltf.scene3d.utils.IBLBuilder
import org.tinylog.kotlin.Logger
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Screen used to bruteforce the UV coordinates for use in Triangle
 * @author Matt Young
 */
class UVTexturingScreen : ScreenAdapter() {
    private val graphics = GraphicsPresets.getSavedGraphicsPreset()

    /** UI stage */
    private val stage = Stage(FitViewport(1920f, 1080f))

    private val cam = PerspectiveCamera().apply {
        fieldOfView = 75f
        near = 0.5f
        far = max(graphics.tileDrawDist, graphics.vehicleDrawDist) * 20
//        rotate(Vector3.X, -90f)
        translate(0f, 100f, 0f)
        update()
    }

    private val camController = FirstPersonCamController(cam)

    /** Viewport for main 3D camera */
    private val cameraViewport = ExtendViewport(1920f, 1080f, cam)

    private val sun = if (graphics.shadows) DirectionalShadowLight() else DirectionalLightEx()

    private val shapeRender = ShapeRenderer()

    private val skin = Assets.ASSETS["ui/uiskin.json", Skin::class.java]

    private val batch = SpriteBatch()

    private lateinit var sceneManager: AtlasSceneManager

    private val mux = InputMultiplexer()

    private val material = Material().apply {
        set(PBRTextureAttribute.createBaseColorTexture(Assets.ASSETS["sprite/uvchecker1.png", Texture::class.java]))
    }

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

        val brdfLut = Assets.ASSETS["net/mgsx/gltf/shaders/brdfLUT.png", Texture::class.java]

        sceneManager.setAmbientLight(1f)
        sceneManager.environment.set(PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLut))
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap))
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap))

        // setup skybox
        sceneManager.skyBox = SceneSkybox(environmentCubemap)

        // apply graphics settings
        Assets.applyGraphicsPreset(graphics)

        // print OpenGL debug info for later diagnostics if/when the game crashes on someone's computer
        val v = Gdx.graphics.glVersion
        Logger.info("GL version: ${v.majorVersion}.${v.minorVersion}.${v.releaseVersion} vendor: ${v.vendorString} renderer: ${v.rendererString}")
    }

    override fun show() {
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
        }

        // make a new model each frame
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()
        val mpb = modelBuilder.part(
            "building", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.Normal.toLong()
                or VertexAttributes.Usage.TextureCoordinates.toLong(),
            material
        )
        val triangle = Triangle(Vector2(-30f, 0f), Vector2(30f, 0f), Vector2(0f, 60f))
        triangle.extrudeUpToPrismMesh(mpb, 20f)

        val model = modelBuilder.end()
        val inst = ModelInstance(model)

        // render 3D
        camController.update(delta)
        cam.update()
        sceneManager.updateDirect(delta, listOf(inst))
        sceneManager.render()

        stage.act()
        stage.draw()

        sceneManager.resetStats()

        model.dispose()

//        val mem = ((Gdx.app.javaHeap + Gdx.app.nativeHeap) / 1e6).roundToInt()
//        Logger.debug("(${Gdx.graphics.framesPerSecond} FPS, $mem MB)")
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        cameraViewport.update(width, height)
    }

    override fun hide() {
        dispose()
    }

    override fun dispose() {
        stage.dispose()
        shapeRender.dispose()
        batch.dispose()
        Assets.ASSETS.dispose()
    }
}
