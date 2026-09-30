package com.mygdx.game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mygdx.game.Main;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {

    private static final File CRASH_LOG = new File("antevault-crash.log");

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // macOS support; also helps on Windows.

        // Launched without a console (javaw, double-click) a crash would otherwise leave no trace.
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread thread, Throwable t) { writeCrashLog("uncaught", t); }
        });

        Main game = new Main();
        game.errors = new Main.ErrorSink() {
            @Override public void report(String where, Throwable t) { writeCrashLog(where, t); }
        };
        new Lwjgl3Application(game, configuration());
    }

    private static Lwjgl3ApplicationConfiguration configuration() {
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setTitle("ANTEVAULT");
        c.useVsync(true);
        c.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        c.setWindowedMode(1280, 720);
        c.setWindowSizeLimits(960, 540, -1, -1);
        c.setWindowIcon("icon128.png", "icon64.png", "icon32.png", "icon16.png");
        return c;
    }

    private static void writeCrashLog(String where, Throwable t) {
        t.printStackTrace();
        try (PrintWriter w = new PrintWriter(new FileWriter(CRASH_LOG, true))) {
            w.println("=== " + new Date() + "  " + where + "  (java " + System.getProperty("java.version") + ") ===");
            t.printStackTrace(w);
            w.println();
        } catch (Exception ignored) {
            // nothing further to try if the log itself cannot be written
        }
    }
}
