/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.decosegfault.atlas.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/**
 * Camera controller for Atlas, meant to resemble Google Maps. Based on
 * <a href="https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/graphics/g3d/utils/CameraInputController.java">libGDX code</a>
 * <p>
 * Features to be added for Atlas: smooth zoom, zoom distance changes translation speed, min/max angles
 *
 * STATUS: ABANDONED because it sucks. FIXME REMOVE THIS FILE.
 *
 * @author libGDX authors
 * @author Matt Young
 */
public class OldAtlasCamController extends InputAdapter {
    /** The button for rotating the camera. */
    public int rotateButton = Buttons.LEFT;
    /** The angle to rotate when moved the full width or height of the screen. */
    public float rotateAngle = 360f;
    /** The button for translating the camera along the up/right plane */
    public int translateButton = Buttons.RIGHT;
    /** The units to translate the camera when moved the full width or height of the screen. */
    public float baseTranslateUnits = 10f;
    /** The button for translating the camera along the direction axis */
    public int forwardButton = Buttons.MIDDLE;
    /** The key which must be pressed to activate rotate, translate and forward or 0 to always activate. */
    public int activateKey = 0;
    /** Indicates if the activateKey is currently being pressed. */
    protected boolean activatePressed;
    /** Whether scrolling requires the activeKey to be pressed (false) or always allow scrolling (true). */
    public boolean alwaysScroll = true;
    /** The weight for each scrolled amount. */
    public float scrollFactor = -0.1f;
    /** Whether to update the camera after it has been changed. */
    public boolean autoUpdate = true;
    /** The target to rotate around. */
    public Vector3 target = new Vector3();
    /** Whether to update the target on translation */
    public boolean translateTarget = true;
    /** Whether to update the target on forward */
    public boolean forwardTarget = true;
    /** Whether to update the target on scroll */
    public boolean scrollTarget = false;
    public int forwardKey = Keys.W;
    protected boolean forwardPressed;
    public int backwardKey = Keys.S;
    protected boolean backwardPressed;
    public int strafeRightKey = Keys.D;
    protected boolean strafeRightPressed;
    public int strafeLeftKey = Keys.A;
    protected boolean strafeLeftPressed;
    protected boolean controlsInverted;
    /** The camera. */
    public Camera camera;
    /** The current (first) button being pressed. */
    protected int button = -1;

    private float startX, startY;
    private final Vector3 tmpV1 = new Vector3();
    private final Vector3 tmpV2 = new Vector3();
    public float zoomTarget = 1f; // new zoom value (end)

    public OldAtlasCamController(final Camera camera) {
        super();
        this.camera = camera;
    }

    public void update () {
        final float delta = Gdx.graphics.getDeltaTime();

        final float scaleFactor = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? 4f : 1f;
        baseTranslateUnits = camera.position.y * scaleFactor;

        if (strafeRightPressed || strafeLeftPressed || forwardPressed || backwardPressed) {
            // https://gamedev.stackexchange.com/q/26622
            Vector3 right = camera.direction.cpy().crs(camera.up).nor();

            if (strafeRightPressed) camera.position.add(right.scl(baseTranslateUnits * delta));
            if (strafeLeftPressed) camera.position.add(right.scl(-baseTranslateUnits * delta));

            if (forwardPressed) {
                camera.translate(tmpV1.set(camera.direction).scl(delta * baseTranslateUnits));
                if (forwardTarget) target.add(tmpV1);
            }
            if (backwardPressed) {
                camera.translate(tmpV1.set(camera.direction).scl(-delta * baseTranslateUnits));
                if (forwardTarget) target.add(tmpV1);
            }
            if (autoUpdate) camera.update();
        }
    }

    @Override
    public boolean scrolled (float amountX, float amountY) {
//        zoomTarget += amountY * scrollFactor * actualTranslateUnits;
//        actualTranslateUnits = (-zoomTarget / 15f) * baseTranslateUnits;
//        if (!isZooming) zoomProgress = 0f;
//        isZooming = true;
//        Logger.debug("Zoom from {} to {}", zoomOld, zoomTarget);
//        return true;

        // FIXME this code is fucking horrendous, @Henry plz help me

//        float zoomIncrement = amountY * scrollFactor * actualTranslateUnits;
//        zoomTarget += zoomIncrement;
//        actualTranslateUnits = (-zoomTarget / 20f) * baseTranslateUnits;
//        if (actualTranslateUnits == 0.0f) actualTranslateUnits = 10.0f;
//        return zoom(zoomIncrement);

//        return zoom(amountY * scrollFactor * actualTranslateUnits);
        return true;
    }

    public boolean zoom (float amount) {
        if (!alwaysScroll && activateKey != 0 && !activatePressed) return false;
        camera.translate(tmpV1.set(camera.direction).scl(amount));
        if (scrollTarget) target.add(tmpV1);
        if (autoUpdate) camera.update();
        return true;
    }

    private int touched;
    private boolean multiTouch;

    @Override
    public boolean touchDown (int screenX, int screenY, int pointer, int button) {
        touched |= (1 << pointer);
        multiTouch = !MathUtils.isPowerOfTwo(touched);
        if (multiTouch)
            this.button = -1;
        else if (this.button < 0 && (activateKey == 0 || activatePressed)) {
            startX = screenX;
            startY = screenY;
            this.button = button;
        }
        return super.touchDown(screenX, screenY, pointer, button) || (activateKey == 0 || activatePressed);
    }

    @Override
    public boolean touchUp (int screenX, int screenY, int pointer, int button) {
        touched &= ~(1 << pointer);
        multiTouch = !MathUtils.isPowerOfTwo(touched);
        if (button == this.button) this.button = -1;
        return super.touchUp(screenX, screenY, pointer, button) || activatePressed;
    }

    /** Sets the AtlasCameraControllers' control inversion.
     * @param invertControls Whether or not to invert the controls */
    public void setInvertedControls (boolean invertControls) {
        if (this.controlsInverted != invertControls) {
            // Flip the rotation angle
            this.rotateAngle = -this.rotateAngle;
        }
        this.controlsInverted = invertControls;
    }

    protected boolean process (float deltaX, float deltaY, int button) {
        if (button == rotateButton) {
            tmpV1.set(camera.direction).crs(camera.up).y = 0f;
            camera.rotateAround(target, tmpV1.nor(), deltaY * rotateAngle);
            camera.rotateAround(target, Vector3.Y, deltaX * -rotateAngle);
        } else if (button == translateButton) {
            camera.translate(tmpV1.set(camera.direction).crs(camera.up).nor().scl(-deltaX * baseTranslateUnits));
            camera.translate(tmpV2.set(camera.up).scl(-deltaY * baseTranslateUnits));
            if (translateTarget) target.add(tmpV1).add(tmpV2);
        } else if (button == forwardButton) {
            camera.translate(tmpV1.set(camera.direction).scl(deltaY * baseTranslateUnits));
            if (forwardTarget) target.add(tmpV1);
        }
        if (autoUpdate) camera.update();
        return true;
    }

    @Override
    public boolean touchDragged (int screenX, int screenY, int pointer) {
        boolean result = super.touchDragged(screenX, screenY, pointer);
        if (result || this.button < 0) return result;
        final float deltaX = (screenX - startX) / Gdx.graphics.getWidth();
        final float deltaY = (startY - screenY) / Gdx.graphics.getHeight();
        startX = screenX;
        startY = screenY;
        return process(deltaX, deltaY, button);
    }

    @Override
    public boolean keyDown (int keycode) {
        if (keycode == activateKey) activatePressed = true;
        if (keycode == forwardKey)
            forwardPressed = true;
        else if (keycode == backwardKey)
            backwardPressed = true;
        else if (keycode == strafeRightKey)
            strafeRightPressed = true;
        else if (keycode == strafeLeftKey) strafeLeftPressed = true;
        return false;
    }

    @Override
    public boolean keyUp (int keycode) {
        if (keycode == activateKey) {
            activatePressed = false;
            button = -1;
        }
        if (keycode == forwardKey)
            forwardPressed = false;
        else if (keycode == backwardKey)
            backwardPressed = false;
        else if (keycode == strafeRightKey)
            strafeRightPressed = false;
        else if (keycode == strafeLeftKey) strafeLeftPressed = false;
        return false;
    }
}
