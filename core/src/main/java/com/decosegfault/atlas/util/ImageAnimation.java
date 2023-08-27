package com.decosegfault.atlas.util;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * libgdx scene2d image with animation capability
 *
 * @author mgsx
 * @see <a href="https://gist.github.com/mgsx-dev/9042ed0b755960fd221b849481a3cca6">GitHub Gist</a>
 */
public class ImageAnimation extends Image {
    private Animation<TextureRegion> animation;
    private float time;
    protected float speed = 1f;

    protected TextureRegionDrawable drawable = new TextureRegionDrawable();

    public ImageAnimation() {
        super();
        setDrawable(drawable);
    }

    public void setAnimation(Animation<TextureRegion> animation) {
        this.animation = animation;
    }

    public void setPose(TextureRegion textureRegion) {
        drawable.setRegion(textureRegion);
        setDrawable(drawable);
        invalidateHierarchy();
        setSize(getPrefWidth(), getPrefHeight());
        invalidate();
        this.animation = null;
    }

    @Override
    public void act(float delta) {
        time += delta * speed;
        if (animation != null && animation.getAnimationDuration() > 0) {
            TextureRegion frame = animation.getKeyFrame(time, true);
            drawable.setRegion(frame);
            setDrawable(drawable);
            invalidateHierarchy();
            invalidate();
        }

        super.act(delta);
    }
}
