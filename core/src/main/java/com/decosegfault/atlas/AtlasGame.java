package com.decosegfault.atlas;

import com.badlogic.gdx.Game;
import com.decosegfault.atlas.screens.LoadingScreen;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 * @author Matt Young
 */
public class AtlasGame extends Game {
    @Override
    public void create() {
        setScreen(new LoadingScreen(this));
    }
}
