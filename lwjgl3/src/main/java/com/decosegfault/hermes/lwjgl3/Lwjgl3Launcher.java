package com.decosegfault.hermes.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.decosegfault.atlas.AtlasGame;
import com.decosegfault.atlas.render.GraphicsPreset;
import com.decosegfault.atlas.render.GraphicsPresets;
import com.decosegfault.atlas.util.AtlasUtils;
import org.tinylog.Logger;

import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * Launches the desktop (LWJGL3) application.
 * @author czyzby & kotcrab (gdx-setup)
 * @author Tommy Ettinger (gdx-liftoff)
 * @author Matt Young (Atlas)
 */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        // The very first thing we want to do is display the config prompts, even before restarting the JVM
        // on Mac. This is because -xstartOnFirstThread breaks Swing.
        // Reference: https://stackoverflow.com/q/43359687
        if (System.getProperty("jvmIsRestarted") == null && System.getProperty("debug") == null) {
            displayConfigPrompts();
        } else {
            Logger.info("Skipping displaying config prompts due to debug or jvmIsRestarted");
            Logger.debug("jvmIsRestarted=" + System.getProperty("jvmIsRestarted")
                + ", debug=" + System.getProperty("debug"));
        }

        // Now, we can actually spawn a new JVM for the Mac users.
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.

        Logger.info("DECO3801 Hermes+Atlas, by Team Segmentation fault (core dumped) - UQ 2023 Semester 2");
        Logger.info("Working dir: " + new File(".").getAbsolutePath());

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Logger.error("Uncaught exception in thread " + thread.getName() + ": " + throwable);
            Logger.error(throwable);
            System.exit(1);
        });

        createApplication();
    }

    /** Display prompts asking for user config settings */
    private static void displayConfigPrompts() {
        Logger.info("Displaying prompts");
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);

        // first ask for graphics setting
        Object[] presets = GraphicsPresets.INSTANCE.getPresets().stream().map(GraphicsPreset::getName).toArray();
        Object graphicsResult = JOptionPane.showInputDialog(frame,
        "Select Atlas graphics preset:",
        "DECO3801 Atlas Config", JOptionPane.QUESTION_MESSAGE, null, presets, presets[1]);
        // TODO default graphics preset should be one saved to graphics.txt

        // next ask for simulation mode
        Object[] simModes = new String[]{"History", "Live", "Simulated"};
        Object simResult = JOptionPane.showInputDialog(frame,
        "Select Hermes simulation mode:",
        "DECO3801 Hermes Config", JOptionPane.QUESTION_MESSAGE, null, simModes, simModes[1]);

        frame.dispose();

        // write sim result and graphics result to file
        if (graphicsResult != null) GraphicsPresets.INSTANCE.writePreset((String) graphicsResult);
        if (simResult != null) AtlasUtils.INSTANCE.writeHermesPreset(simResult.toString());
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new AtlasGame(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Logger.debug("Performing early load of graphics preset");
        GraphicsPreset preset = GraphicsPresets.INSTANCE.getSavedGraphicsPreset();

        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Hermes + Atlas (DECO3801)");

        // Enable vsync if we are on Mac in release mode
        // VSync looks better on Mac (the game appears as if its lagging at high framerates otherwise)
        if (System.getProperty("debug") != null) {
            Logger.info("Debug mode, disabling vsync");
            configuration.useVsync(false);
        } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            Logger.info("Release mode, Mac, enabling vsync");
            configuration.useVsync(true);
        }

        // force the use of OpenGL 3.2 - if this causes the game to go nuclear, tag @Matt
//        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL32, 3, 2);
        configuration.setWindowedMode(1600, 900);
        // "samples" controls MSAA samples
        configuration.setBackBufferConfig(8, 8, 8, 8, 16, 8, preset.getMsaa());
        Logger.debug("Used {} MSAA samples", preset.getMsaa());
        configuration.setWindowIcon("scallop128.png", "scallop64.png", "scallop32.png", "scallop16.png");
        return configuration;
    }
}
