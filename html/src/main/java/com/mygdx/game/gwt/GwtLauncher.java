package com.mygdx.game.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderCallback;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderState;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.mygdx.game.Main;

/** Launches the browser build. The page shell (index.html) owns the loading screen. */
public class GwtLauncher extends GwtApplication {

    @Override
    public GwtApplicationConfiguration getConfig() {
        // fill the window; the game keeps its 16:9 frame inside it
        GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(true);
        cfg.padVertical = 0;
        cfg.padHorizontal = 0;
        return cfg;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new Main();
    }

    /** Drives the loading bar in index.html instead of the default libGDX-branded one. */
    @Override
    public PreloaderCallback getPreloaderCallback() {
        final Element loader = Document.get().getElementById("loader");
        final Element bar = Document.get().getElementById("loader-bar");
        final Element message = Document.get().getElementById("loader-msg");
        return new PreloaderCallback() {
            @Override
            public void update(PreloaderState state) {
                if (bar != null) bar.getStyle().setWidth(100 * state.getProgress(), Style.Unit.PCT);
                if (state.hasEnded() && loader != null) loader.addClassName("done");
            }

            @Override
            public void error(String file) {
                if (message != null) message.setInnerText("Could not load " + file + ". Try reloading the page.");
            }
        };
    }
}
