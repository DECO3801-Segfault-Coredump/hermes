package com.decosegfault.hermes;

import com.badlogic.gdx.Game;
import com.decosegfault.atlas.screens.LoadingScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class HermesGame extends Game {
    @Override
    public void create() {
        setScreen(new LoadingScreen(this));
    }
}
