package com.decosegfault.hermes;

import com.badlogic.gdx.Game;
import com.decosegfault.atlas.AtlasScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class HermesGame extends Game {
    @Override
    public void create() {
        setScreen(new AtlasScreen());
    }
}
