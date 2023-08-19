package com.decosegfault.hermes.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.decosegfault.hermes.HermesGame;
import org.tinylog.Logger;

import java.io.File;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        Logger.info("DECO3801 Hermes Traffic Simulator, by Team Segmentation fault (core dumped) - UQ 2023 Semester 2");
        Logger.info("Working dir: " + new File(".").getAbsolutePath());

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Logger.error("Uncaught exception in thread " + thread.getName() + ": " + throwable);
            Logger.error(throwable);
        });

        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new HermesGame(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("DECO3801 Hermes Traffic Simulator");
        configuration.useVsync(true);
        //// Limits FPS to the refresh rate of the currently active monitor.
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        //// useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.
        configuration.setWindowedMode(1600, 900);
        configuration.setWindowIcon("scallop128.png", "scallop64.png", "scallop32.png", "scallop16.png");
//        configuration.setWindowIcon("bk128.png", "bk64.png", "bk32.png", "bk16.png");
        return configuration;
    }
}
