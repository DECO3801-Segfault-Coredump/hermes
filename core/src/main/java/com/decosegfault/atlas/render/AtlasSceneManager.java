package com.decosegfault.atlas.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.PointLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.SpotLightsAttribute;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.RenderableSorter;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.decosegfault.atlas.map.BuildingChunk;
import com.decosegfault.atlas.map.BuildingManager;
import com.decosegfault.atlas.map.TileManager;
import com.decosegfault.atlas.map.Tile;
import net.mgsx.gltf.scene3d.attributes.PBRMatrixAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.lights.PointLightEx;
import net.mgsx.gltf.scene3d.lights.SpotLightEx;
import net.mgsx.gltf.scene3d.scene.*;
import net.mgsx.gltf.scene3d.shaders.PBRCommon;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.EnvironmentCache;
import net.mgsx.gltf.scene3d.utils.EnvironmentUtil;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the scene manager for Atlas, which is a fixed and re-implemented version of the default
 * SceneManager in gdx-gltf.
 * <p>
 * Matt's note: the original implementation of this class in the gdx-gltf repo is horrendous, I just fixed the
 * most glaring issues and made it compatible with Atlas.
 * <p>
 * Original source:
 * <a href="https://github.com/mgsx-dev/gdx-gltf/blob/master/gltf/src/net/mgsx/gltf/scene3d/scene/SceneManager.java">
 * net.mgsx.gltf.scene3d.scene.SceneManager
 * </a>
 *
 * @author mgsx
 * @author Matt Young (modifications for Atlas)
 * @author Henry Batt (modifications for Atlas)
 */
public class AtlasSceneManager implements Disposable {
    protected final EnvironmentCache computedEnvironment = new EnvironmentCache();
    private final RenderableSorter renderableSorter;
    private final PointLightsAttribute pointLights = new PointLightsAttribute();
    private final SpotLightsAttribute spotLights = new SpotLightsAttribute();
    /**
     * Shouldn't be null.
     */
    public Environment environment = new Environment();
    public Camera camera;
    private final Array<RenderableProvider> renderableProviders = new Array<>();
    private ModelBatch batch;
    private ModelBatch depthBatch;
    private SceneSkybox skyBox;
    private TransmissionSource transmissionSource;
    private MirrorSource mirrorSource;
    private CascadeShadowMap cascadeShadowMap;

    // the following are Atlas additions:

    /** Atlas graphics preset */
    private GraphicsPreset graphics;

    /** Vehicles that were not rendered */
    private int culledVehicles = 0;
    /** Vehicles that used low LoD */
    private int lowLodVehicles = 0;
    /** Vehicles that were fully rendered */
    private int renderedVehicles = 0;

    /** List of AtlasVehicles that actually got rendered */
    private final List<AtlasVehicle> renderedAtlasVehicles = new ArrayList<>();

    /** Ground plane tiling collection and batch drawer */
    private final Array<Decal> tileDecals = new Array<>();
    private DecalBatch decalBatch;

    /** Controller to render ground plane tiles */
    private TileManager tileManager;

    /** Controller used to render buildings */
    private BuildingManager buildingManager;

    public AtlasSceneManager(GraphicsPreset graphics) {
        this(24);
        this.graphics = graphics;
        if (graphics.getName().equals("Genuine Potato")) {
            Logger.info("Using non-PBR shader for Genuine Potato graphics");
            setDepthShaderProvider(new DepthShaderProvider());
            setShaderProvider(new DefaultShaderProvider());
        } else {
            Logger.info("Using PBR shader for " + graphics.getName() + " graphics");
        }
    }

    private AtlasSceneManager(int maxBones) {
        this(PBRShaderProvider.createDefault(maxBones), PBRShaderProvider.createDefaultDepth(maxBones));
    }

    private AtlasSceneManager(ShaderProvider shaderProvider, DepthShaderProvider depthShaderProvider) {
        this(shaderProvider, depthShaderProvider, new SceneRenderableSorter());
    }

    private AtlasSceneManager(ShaderProvider shaderProvider, DepthShaderProvider depthShaderProvider, RenderableSorter renderableSorter) {
        this.renderableSorter = renderableSorter;

        batch = new ModelBatch(shaderProvider, renderableSorter);

        depthBatch = new ModelBatch(depthShaderProvider);

        float lum = 1f;
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, lum, lum, lum, 1));
    }

    public void setEnvironmentRotation(float azymuthAngleDegree) {
        PBRMatrixAttribute attribute = environment.get(PBRMatrixAttribute.class, PBRMatrixAttribute.EnvRotation);
        if (attribute != null) {
            attribute.set(azymuthAngleDegree);
        } else {
            environment.set(PBRMatrixAttribute.createEnvRotation(azymuthAngleDegree));
        }
    }

    public void removeEnvironmentRotation() {
        environment.remove(PBRMatrixAttribute.EnvRotation);
    }

    public ModelBatch getBatch() {
        return batch;
    }

    public void setBatch(ModelBatch batch) {
        this.batch = batch;
    }

    public ModelBatch getDepthBatch() {
        return depthBatch;
    }

    public void setDepthBatch(ModelBatch depthBatch) {
        this.depthBatch = depthBatch;
    }

    public DecalBatch getDecalBatch() {
        return decalBatch;
    }

    public void setDecalBatch(DecalBatch decalBatch) {
        this.decalBatch = decalBatch;
    }

    public void setShaderProvider(ShaderProvider shaderProvider) {
        batch.dispose();
        batch = new ModelBatch(shaderProvider, renderableSorter);
    }

    public void setDepthShaderProvider(DepthShaderProvider depthShaderProvider) {
        depthBatch.dispose();
        depthBatch = new ModelBatch(depthShaderProvider);
    }

    /**
     * Enable/disable opaque objects pre-rendering for transmission (refraction effect).
     *
     * @param transmissionSource set null to disable pre-rendering.
     */
    public void setTransmissionSource(TransmissionSource transmissionSource) {
        if (this.transmissionSource != transmissionSource) {
            if (this.transmissionSource != null) this.transmissionSource.dispose();
            this.transmissionSource = transmissionSource;
        }
    }

    /**
     * Enable/disable pre-rendering for mirror effect.
     *
     * @param mirrorSource set null to disable mirror.
     */
    public void setMirrorSource(MirrorSource mirrorSource) {
        if (this.mirrorSource != mirrorSource) {
            if (this.mirrorSource != null) this.mirrorSource.dispose();
            this.mirrorSource = mirrorSource;
        }
    }

    /**
     * Enable/disable pre-rendering for cascade shadow map.
     *
     * @param cascadeShadowMap set null to disable.
     */
    public void setCascadeShadowMap(CascadeShadowMap cascadeShadowMap) {
        if (this.cascadeShadowMap != cascadeShadowMap) {
            if (this.cascadeShadowMap != null) this.cascadeShadowMap.dispose();
            this.cascadeShadowMap = cascadeShadowMap;
        }
    }

    /**
     * Updates skybox, vehicle render list, and ground plane tiling list.
     * Will check with {@link AtlasVehicle#getRenderModel(Camera, GraphicsPreset)}
     * to perform frustum culling and distance thresholding.
     */
    public void update(float delta, List<AtlasVehicle> vehicles) {
        renderableProviders.clear();
        renderedAtlasVehicles.clear();

        for (AtlasVehicle vehicle : vehicles) {
            var model = vehicle.getRenderModel(camera, graphics);
            if (model == null) {
                // we were asked not to render this vehicle
                culledVehicles++;
                continue;
            }
            // we were asked to render this vehicle
            renderableProviders.add(model);
            renderedAtlasVehicles.add(vehicle);

            // check if we used low LoD
            if (vehicle.getDidUseLowLod()) {
                lowLodVehicles++;
            } else {
                renderedVehicles++;
            }
        }

        // Load ground plane tiles for rendering
        tileDecals.clear();
        for (Tile tile : tileManager.getTilesCulled( camera, graphics)){
            var decal = tile.getDecal();
            if (decal != null) tileDecals.add(decal);
        }

        // Submit building chunks for rendering
        for (BuildingChunk chunk : buildingManager.getBuildingChunksCulled(camera, graphics)) {
            var modelCache = chunk.getBuildingCache();
            if (modelCache != null) renderableProviders.add(modelCache);
        }

        if (camera != null) {
            updateEnvironment();
            if (skyBox != null) skyBox.update(camera, delta);
        }
    }

    /**
     * Computes the first AtlasVehicle this Ray hits, or null if no vehicles. Useful for selecting vehicles using
     * a screen pick ray. Only queries the actual list of rendered vehicles on the last call to update(), so
     * you must call this **only** after calling update().
     */
    public AtlasVehicle intersectRayVehicle(Ray ray) {
        for (AtlasVehicle vehicle : renderedAtlasVehicles) {
            if (vehicle.intersectRay(ray)) {
                return vehicle;
            }
        }
        return null;
    }

    public void resetStats() {
        culledVehicles = 0;
        renderedVehicles = 0;
        lowLodVehicles = 0;
    }

    public int getTotalVehicles() {
        return culledVehicles + renderedVehicles + lowLodVehicles;
    }

    /**
     * Returns the rate for the given metric
     * @param metric which metric to query
     * @return the percentage of total vehicles this metric occupies
     */
    private int getRate(int metric) {
        if (getTotalVehicles() <= 0) {
            return 0;
        }
        return Math.round(((float) metric / getTotalVehicles()) * 100f);
    }

    public int getCullRate() {
        return getRate(culledVehicles);
    }

    public int getLowLodRate() {
        return getRate(lowLodVehicles);
    }

    public int getFullRenderRate() {
        return getRate(renderedVehicles);
    }

    /**
     * Automatically set skybox rotation matching this environement rotation. Subclasses could override this
     * method in order to change this behavior.
     */
    protected void updateSkyboxRotation() {
        if (skyBox != null) {
            PBRMatrixAttribute rotationAttribute = environment.get(PBRMatrixAttribute.class, PBRMatrixAttribute.EnvRotation);
            if (rotationAttribute != null) {
                skyBox.setRotation(rotationAttribute.matrix);
            }
        }
    }

    protected void updateEnvironment() {
        updateSkyboxRotation();

        computedEnvironment.setCache(environment);
        pointLights.lights.clear();
        spotLights.lights.clear();
        if (environment != null) {
            for (Attribute a : environment) {
                if (a instanceof PointLightsAttribute) {
                    pointLights.lights.addAll(((PointLightsAttribute) a).lights);
                    computedEnvironment.replaceCache(pointLights);
                } else if (a instanceof SpotLightsAttribute) {
                    spotLights.lights.addAll(((SpotLightsAttribute) a).lights);
                    computedEnvironment.replaceCache(spotLights);
                } else {
                    computedEnvironment.set(a);
                }
            }
        }
        cullLights();
    }

    protected void cullLights() {
        PointLightsAttribute pla = environment.get(PointLightsAttribute.class, PointLightsAttribute.Type);
        if (pla != null) {
            for (PointLight light : pla.lights) {
                if (light instanceof PointLightEx l) {
                    if (l.range != null && !camera.frustum.sphereInFrustum(l.position, l.range)) {
                        pointLights.lights.removeValue(l, true);
                    }
                }
            }
        }
        SpotLightsAttribute sla = environment.get(SpotLightsAttribute.class, SpotLightsAttribute.Type);
        if (sla != null) {
            for (SpotLight light : sla.lights) {
                if (light instanceof SpotLightEx l) {
                    if (l.range != null && !camera.frustum.sphereInFrustum(l.position, l.range)) {
                        spotLights.lights.removeValue(l, true);
                    }
                }
            }
        }
    }

    /**
     * render all scenes. because shadows use frame buffers, if you need to render scenes to a frame buffer,
     * you should instead first call {@link #renderShadows()}, bind your frame buffer and then call
     * {@link #renderColors()}
     */
    public void render() {
        if (camera == null) return;

        PBRCommon.enableSeamlessCubemaps();

        renderShadows();

        //renderMirror(); // Matt: shouldn't be necessary in Atlas

        renderTransmission();

        renderColors();

        renderDecal();
    }

    public void renderMirror() {
        if (mirrorSource != null) {
            mirrorSource.begin(camera, computedEnvironment, skyBox);
            renderColors();
            mirrorSource.end();
        }
    }

    public void renderTransmission() {
        if (transmissionSource != null) {
            transmissionSource.begin(camera);
            transmissionSource.render(renderableProviders, environment);
            if (skyBox != null) transmissionSource.render(skyBox);
            transmissionSource.end();
            computedEnvironment.set(transmissionSource.attribute);
        }
    }

    /**
     * Render shadows only to interal frame buffers. (useful when you're using your own frame buffer to render
     * scenes)
     */
    public void renderShadows() {
        DirectionalShadowLight shadowLight = getFirstDirectionalShadowLight();
        if (shadowLight != null) {
            shadowLight.begin();
            renderDepth(shadowLight.getCamera());
            shadowLight.end();

            environment.shadowMap = shadowLight;
        } else {
            environment.shadowMap = null;
        }
        computedEnvironment.shadowMap = environment.shadowMap;

        if (cascadeShadowMap != null) {
            for (DirectionalShadowLight light : cascadeShadowMap.lights) {
                light.begin();
                renderDepth(light.getCamera());
                light.end();
            }
            computedEnvironment.set(cascadeShadowMap.attribute);
        }
    }

    /**
     * Render only depth (packed 32 bits), usefull for post processing effects. You typically render it to a
     * FBO with depth enabled.
     */
    public void renderDepth() {
        renderDepth(camera);
    }

    /**
     * Render only depth (packed 32 bits) with custom camera. Useful to render shadow maps.
     */
    public void renderDepth(Camera camera) {
        depthBatch.begin(camera);
        depthBatch.render(renderableProviders);
        depthBatch.end();
    }

    /**
     * Render all tile decals.
     */
    public void renderDecal() {
        for (Decal decal: tileDecals) {
            decalBatch.add(decal);
        }

        decalBatch.flush();
    }

    /**
     * Render colors only. You should call {@link #renderShadows()} before. (useful when you're using your own
     * frame buffer to render scenes)
     */
    public void renderColors() {
        batch.begin(camera);
        batch.render(renderableProviders, computedEnvironment);
        if (skyBox != null) batch.render(skyBox);
        batch.end();
    }

    public DirectionalLight getFirstDirectionalLight() {
        DirectionalLightsAttribute dla = environment.get(DirectionalLightsAttribute.class, DirectionalLightsAttribute.Type);
        if (dla != null) {
            for (DirectionalLight dl : dla.lights) {
                if (dl != null) {
                    return dl;
                }
            }
        }
        return null;
    }

    public DirectionalShadowLight getFirstDirectionalShadowLight() {
        DirectionalLightsAttribute dla = environment.get(DirectionalLightsAttribute.class, DirectionalLightsAttribute.Type);
        if (dla != null) {
            for (DirectionalLight dl : dla.lights) {
                if (dl instanceof DirectionalShadowLight) {
                    return (DirectionalShadowLight) dl;
                }
            }
        }
        return null;
    }

    public SceneSkybox getSkyBox() {
        return skyBox;
    }

    public void setSkyBox(SceneSkybox skyBox) {
        this.skyBox = skyBox;
    }

    /**
     * Sets the tile manager to used for ground plane.
     *
     * @param tileManager  Tile manager instance to use.
     */
    public void setTileManager(TileManager tileManager) {
        this.tileManager = tileManager;
    }

    /**
     * Sets the building manager
     * @param buildingManager New building manager instance
     */
    public void setBuildingManager(BuildingManager buildingManager) {
        this.buildingManager = buildingManager;
    }

    public void setAmbientLight(float lum) {
        environment.get(ColorAttribute.class, ColorAttribute.AmbientLight).color.set(lum, lum, lum, 1);
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public int getActiveLightsCount() {
        return EnvironmentUtil.getLightCount(computedEnvironment);
    }

    public int getTotalLightsCount() {
        return EnvironmentUtil.getLightCount(environment);
    }


    @Override
    public void dispose() {
        batch.dispose();
        depthBatch.dispose();
        if (transmissionSource != null) transmissionSource.dispose();
        if (mirrorSource != null) mirrorSource.dispose();
        if (cascadeShadowMap != null) cascadeShadowMap.dispose();
    }
}

