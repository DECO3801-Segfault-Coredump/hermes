package com.decosegfault.hermes;

import com.badlogic.gdx.Game;
import com.decosegfault.atlas.AtlasScreen;
import com.decosegfault.atlas.LoadingScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class HermesGame extends Game {
    @Override
    public void create() {
        setScreen(new LoadingScreen(this));
    }
}
