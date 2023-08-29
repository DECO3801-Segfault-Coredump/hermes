package com.decosegfault.hermes.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.decosegfault.atlas.AtlasGame;
import com.decosegfault.atlas.render.GraphicsPreset;
import com.decosegfault.atlas.render.GraphicsPresets;
import org.tinylog.Logger;

import java.io.File;

/** Launches the desktop (LWJGL3) application. */
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
        //// Limits FPS to the refresh rate of the currently active monitor.
//        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        //// useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.
        configuration.setWindowedMode(1600, 900);
        // "samples" controls MSAA samples
        configuration.setBackBufferConfig(8, 8, 8, 8, 16, 8, preset.getMsaa());
        Logger.debug("Used {} MSAA samples", preset.getMsaa());
        configuration.setWindowIcon("scallop128.png", "scallop64.png", "scallop32.png", "scallop16.png");
        return configuration;
    }
}
