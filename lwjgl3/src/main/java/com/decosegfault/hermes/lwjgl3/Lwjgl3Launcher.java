package com.decosegfault.hermes.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.decosegfault.atlas.AtlasGame;
import com.decosegfault.atlas.render.GraphicsPreset;
import com.decosegfault.atlas.render.GraphicsPresets;
import org.tinylog.Logger;

import java.io.File;

/**
 * Launches the desktop (LWJGL3) application.
 * @author czyzby & kotcrab (gdx-setup)
 * @author Tommy Ettinger (gdx-liftoff)
 * @author Matt Young (Atlas)
 */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        Logger.info("DECO3801 Hermes+Atlas, by Team Segmentation fault (core dumped) - UQ 2023 Semester 2");
        Logger.info("Working dir: " + new File(".").getAbsolutePath());

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Logger.error("Uncaught exception in thread " + thread.getName() + ": " + throwable);
            Logger.error(throwable);
        });

        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new AtlasGame(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Logger.debug("Performing early load of graphics preset");
        GraphicsPreset preset = GraphicsPresets.INSTANCE.getSavedGraphicsPreset();

        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Hermes + Atlas (DECO3801)");
        configuration.useVsync(false);
        // force the use of OpenGL 3.2 - if this causes the game to go nuclear, tag @Matt
        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL32, 3, 2);
        configuration.setWindowedMode(1600, 900);
        // "samples" controls MSAA samples
        configuration.setBackBufferConfig(8, 8, 8, 8, 16, 8, preset.getMsaa());
        Logger.debug("Used {} MSAA samples", preset.getMsaa());
        configuration.setWindowIcon("scallop128.png", "scallop64.png", "scallop32.png", "scallop16.png");
        return configuration;
    }
}
