package com.mygdx.game.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.GameScreen;
import com.mygdx.game.Main;
import com.mygdx.game.gfx.*;
import com.mygdx.game.i18n.I18n;

/**
 * Display and presentation options, as a list of steppers: click an arrow, click the value, or
 * use left/right on the highlighted row (up/down moves between rows).
 *
 * Only options the current platform can honour are listed - a browser tab cannot be resized by
 * the game, and Android has no window to size - so every row shown actually works.
 */
public class SettingsScreen implements GameScreen {

    /** Windowed sizes offered on desktop, all 16:9 so the viewport never letterboxes. */
    private static final int[][] SIZES = { { 1280, 720 }, { 1600, 900 }, { 1920, 1080 }, { 2560, 1440 } };
    private static final String[] FX = { "fx.off", "fx.clean", "fx.cinematic" };

    private abstract static class Row {
        final String key, label, tip;
        final Color color;

        Row(String key, String label, String tip, Color color) {
            this.key = key; this.label = label; this.tip = tip; this.color = color;
        }

        abstract String value();
        abstract void step(int delta);
    }

    private final Main g = Main.I;
    private final GameScreen back;
    private final Array<Row> rows = new Array<>();
    private int cursor;

    public SettingsScreen(GameScreen back) {
        this.back = back;
        boolean desktop = Gdx.app.getType() == Application.ApplicationType.Desktop;

        if (desktop) {
            rows.add(new Row("resolution", I18n.t("settings.resolution"), I18n.t("settings.resolutionTip"), Pal.CHIPS) {
                String value() { int i = sizeIndex(); return SIZES[i][0] + " x " + SIZES[i][1]; }
                void step(int d) { setSize(wrap(sizeIndex() + d, SIZES.length)); }
            });
            rows.add(new Row("fullscreen", I18n.t("settings.fullscreen"), I18n.t("settings.fullscreenTip"), Pal.PURPLE) {
                String value() { return onOff(Gdx.graphics.isFullscreen()); }
                void step(int d) { toggleFullscreen(); }
            });
        }
        rows.add(new Row("fx", I18n.t("settings.effects"), I18n.t("settings.effectsTip"), Pal.ORANGE) {
            String value() { return I18n.t(FX[g.post.enabled ? (g.post.bloomAmount > 0.2f ? 2 : 1) : 0]); }
            void step(int d) { g.hint(g.post.cycle()); }
        });
        rows.add(new Row("shake", I18n.t("settings.shake"), I18n.t("settings.shakeTip"), Pal.MULT) {
            String value() { return onOff(g.juice.shakeEnabled); }
            void step(int d) { g.juice.shakeEnabled = !g.juice.shakeEnabled; }
        });
        if (desktop) {
            rows.add(new Row("vsync", I18n.t("settings.framecap"), I18n.t("settings.framecapTip"), Pal.TEAL) {
                String value() { return g.vsync ? I18n.t("settings.vsync") : I18n.t("settings.uncapped"); }
                void step(int d) {
                    g.vsync = !g.vsync;
                    final boolean on = g.vsync;
                    g.afterFrame(new Runnable() { @Override public void run() { Gdx.graphics.setVSync(on); } });
                }
            });
        }
        rows.add(new Row("lang", I18n.t("settings.language"), I18n.t("settings.languageTip"), Pal.GREEN) {
            String value() { return I18n.lang().label; }
            void step(int d) {
                I18n.Lang[] all = I18n.Lang.values();
                I18n.set(all[wrap(I18n.lang().ordinal() + d, all.length)]);
                g.hint(I18n.lang().label);
            }
        });
    }

    // ---------------------------------------------------------------- platform actions
    // Changing the window fires GLFW's resize callback synchronously, which re-enters render(),
    // so every display change is deferred to after the frame.

    private static int sizeIndex() {
        int w = Gdx.graphics.getWidth(), best = 0;
        for (int i = 0; i < SIZES.length; i++) if (SIZES[i][0] <= w) best = i;
        return best;
    }

    private void setSize(final int i) {
        g.afterFrame(new Runnable() {
            @Override public void run() {
                Gdx.graphics.setWindowedMode(SIZES[i][0], SIZES[i][1]);
                g.hint(SIZES[i][0] + " x " + SIZES[i][1]);
            }
        });
    }

    private void toggleFullscreen() {
        g.afterFrame(new Runnable() {
            @Override public void run() {
                if (Gdx.graphics.isFullscreen()) {
                    int i = sizeIndex();
                    Gdx.graphics.setWindowedMode(SIZES[i][0], SIZES[i][1]);
                } else {
                    Graphics.DisplayMode dm = Gdx.graphics.getDisplayMode();
                    Gdx.graphics.setFullscreenMode(dm);
                }
            }
        });
    }

    private static String onOff(boolean on) { return on ? I18n.t("settings.on") : I18n.t("settings.off"); }

    private static int wrap(int v, int n) { return ((v % n) + n) % n; }

    // ---------------------------------------------------------------- drawing

    @Override
    public void draw(SpriteBatch b, float dt) {
        Art a = g.art;
        UI ui = g.ui;
        float cx = Main.VW / 2f;

        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) cursor = wrap(cursor + 1, rows.size);
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) cursor = wrap(cursor - 1, rows.size);
        int keyDelta = Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) ? 1
            : Gdx.input.isKeyJustPressed(Input.Keys.LEFT) ? -1 : 0;

        a.textBoxC(b, a.title, I18n.t("settings.title"), cx - 300, 600, 600, 72, Pal.GOLD, 1f, 0.6f, 10);

        float w = 580, x = cx - w / 2, rowH = 54, gap = 10;
        float y = 528;
        for (int i = 0; i < rows.size; i++) {
            int delta = drawRow(b, a, ui, rows.get(i), i, x, y - rowH, w, rowH, dt);
            if (i == cursor && keyDelta != 0) delta = keyDelta;
            if (delta != 0) rows.get(i).step(delta);
            y -= rowH + gap;
        }

        a.textBoxC(b, a.small, "arrows / click to change  ·  up and down to move  ·  Esc to go back",
            0, 156, Main.VW, 20, Pal.DIMMER, 1f, 1f, 20);

        if (ui.button(b, a, "setback", I18n.t("menu.back"), cx - 110, 90, 220, 50, Pal.PANEL_HI, true, dt).clicked
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            g.set(back == null ? new TitleScreen() : back);
        }
    }

    /** Draws one row as {@code label   < value >} and returns -1, 0 or +1 for a click. */
    private int drawRow(SpriteBatch b, Art a, UI ui, Row r, int index, float x, float top, float w, float h, float dt) {
        boolean active = index == cursor;
        if (active) a.bloom(b, x, top, w, h, r.color, 0.16f, 10);
        a.panel(b, x, top, w, h, Pal.PANEL_LO, 0.92f);
        a.outline(b, x, top, w, h, active ? r.color : Pal.LINE, active ? 0.75f : 0.35f);

        float arrowW = 40, valueW = 172;
        float stepX = x + w - 14 - (arrowW * 2 + valueW);
        a.textBoxC(b, a.bodyB, r.label, x + 20, top, stepX - x - 30, h, Pal.INK, 1f, 1f, 0);

        int delta = 0;
        if (ui.chip(b, a, r.key + "L", "<", stepX, top + 10, arrowW, h - 20, Pal.PANEL_HI, true, dt)) delta = -1;
        if (ui.chip(b, a, r.key + "V", r.value(), stepX + arrowW + 2, top + 8, valueW - 4, h - 16,
            Pal.mix(r.color, Pal.PANEL, 0.4f), true, dt)) delta = 1;
        if (ui.chip(b, a, r.key + "R", ">", stepX + arrowW + valueW, top + 10, arrowW, h - 20,
            Pal.PANEL_HI, true, dt)) delta = 1;

        if (ui.hovering(x, top, w, h)) {
            ui.tip(r.label, r.tip, r.color);
            if (ui.justDown) cursor = index;
        }
        return delta;
    }
}
