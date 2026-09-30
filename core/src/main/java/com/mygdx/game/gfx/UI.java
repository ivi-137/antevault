package com.mygdx.game.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.HashMap;

/**
 * Immediate-mode UI. Widgets are identified by a caller-supplied key so hover/press
 * animation state can persist between frames without a retained widget tree.
 */
public class UI {

    public final Vector2 mouse = new Vector2();
    public boolean down, justDown, justUp;
    public boolean rightJustDown;
    /**
     * Left-button press and release seen since the last frame, set by the input processor.
     * Polling alone would lose a click whose press and release both land inside one frame
     * (a slow machine, a throttled browser tab), so events are what the widgets trust.
     */
    public boolean pressEvent, releaseEvent;
    public float scroll;
    /** Filled by the input processor between frames. */
    public float scrollAccum;

    private final Vector3 tmp = new Vector3();
    private final HashMap<String, float[]> state = new HashMap<>();
    private boolean prevDown;

    private String tipText;
    private String tipTitle;
    private Color tipColor = Pal.INK;
    private float tipX, tipY;
    private float tipW = 250;

    public void begin(Viewport vp, float dt) {
        tmp.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        vp.unproject(tmp);
        mouse.set(tmp.x, tmp.y);
        boolean d = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        justDown = pressEvent || (d && !prevDown);
        justUp = releaseEvent || (!d && prevDown);
        down = d;
        prevDown = d;
        pressEvent = releaseEvent = false;
        rightJustDown = Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT);
        scroll = scrollAccum;
        scrollAccum = 0;
        tipText = null; tipTitle = null;
    }

    private float[] st(String key) {
        float[] s = state.get(key);
        if (s == null) { s = new float[3]; state.put(key, s); }
        return s;
    }

    public static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean hovering(float x, float y, float w, float h) {
        return inside(mouse.x, mouse.y, x, y, w, h);
    }

    /**
     * True when a press that began on this widget is released on it. State slot 2 remembers that
     * the press started here, so dragging onto a widget from elsewhere never counts as a click.
     */
    private boolean clicked(float[] s, boolean hov) {
        if (justDown) s[2] = hov ? 1 : 0;
        boolean hit = justUp && hov && s[2] > 0.5f;
        if (justUp) s[2] = 0;
        return hit;
    }

    /** Animated hover value in 0..1 for an arbitrary rect. */
    public float hover(String key, float x, float y, float w, float h, float dt) {
        float[] s = st(key);
        boolean h2 = hovering(x, y, w, h);
        s[0] = MathUtils.lerp(s[0], h2 ? 1 : 0, Math.min(1, dt * 16f));
        return s[0];
    }

    public static class Btn {
        public boolean clicked, hovered;
        public float hoverT, pressT;
    }

    private final Btn btnResult = new Btn();

    /** Returns a reusable result object; read it before calling {@code button} again. */
    public Btn button(Batch b, Art art, String key, String label, float x, float y, float w, float h,
                      Color base, boolean enabled, float dt) {
        float[] s = st(key);
        boolean hov = enabled && hovering(x, y, w, h);
        s[0] = MathUtils.lerp(s[0], hov ? 1 : 0, Math.min(1, dt * 18f));
        boolean pressed = hov && down;
        s[1] = MathUtils.lerp(s[1], pressed ? 1 : 0, Math.min(1, dt * 26f));
        boolean clicked = clicked(s, hov);

        float lift = s[0] * 3f - s[1] * 5f;
        float a = enabled ? 1f : 0.4f;

        Color c = new Color(base);
        c.mul(1f + s[0] * 0.25f);
        // shadow plate
        art.panel(b, x, y - 5 + lift * 0.2f, w, h, Pal.PANEL_LO, 0.85f * a);
        if (enabled && s[0] > 0.01f) art.bloom(b, x, y + lift, w, h, base, s[0] * 0.35f, 10);
        art.panel(b, x, y + lift, w, h, c, a);
        art.outline(b, x, y + lift, w, h, Color.WHITE, (0.10f + s[0] * 0.22f) * a);

        art.textBoxC(b, art.head, label, x, y + lift, w, h, enabled ? Pal.INK : Pal.DIMMER, a, 1f, 14);

        btnResult.clicked = clicked;
        btnResult.hovered = hov;
        btnResult.hoverT = s[0];
        btnResult.pressT = s[1];
        return btnResult;
    }

    /** Small chip-style button (used for sort toggles, reroll, etc.). */
    public boolean chip(Batch b, Art art, String key, String label, float x, float y, float w, float h,
                        Color base, boolean enabled, float dt) {
        float[] s = st(key);
        boolean hov = enabled && hovering(x, y, w, h);
        s[0] = MathUtils.lerp(s[0], hov ? 1 : 0, Math.min(1, dt * 18f));
        boolean clicked = clicked(s, hov);
        float a = enabled ? 1f : 0.35f;
        art.panelSmall(b, x, y, w, h, Pal.mix(base, Color.WHITE, s[0] * 0.25f), a);
        art.textBoxC(b, art.small, label, x, y, w, h, enabled ? Pal.INK : Pal.DIMMER, a, 1f, 6);
        return clicked;
    }

    // ------------------------------------------------------------- tooltips

    public void tip(String title, String text, Color color) {
        tipTitle = title; tipText = text; tipColor = color;
        tipX = mouse.x; tipY = mouse.y;
    }

    public void tipWidth(float w) { tipW = w; }

    public void drawTip(Batch b, Art art, float screenW, float screenH) {
        if (tipText == null && tipTitle == null) return;
        float pad = 10;
        float w = tipW;
        float th = tipTitle != null ? art.head.getLineHeight() + 4 : 0;
        float bh = tipText != null ? art.wrapH(art.body, tipText, w - pad * 2) : 0;
        float h = th + bh + pad * 2;
        float x = tipX + 16, y = tipY - h - 8;
        if (x + w > screenW - 6) x = tipX - w - 16;
        if (x < 6) x = 6;
        if (y < 6) y = tipY + 20;
        if (y + h > screenH - 6) y = screenH - h - 6;

        art.bloom(b, x, y, w, h, Color.BLACK, 0.5f, 12);
        art.panel(b, x, y, w, h, Pal.PANEL_LO, 0.97f);
        art.outline(b, x, y, w, h, tipColor, 0.6f);
        float cy = y + h - pad;
        if (tipTitle != null) {
            art.text(b, art.head, tipTitle, x + pad, cy, tipColor);
            cy -= art.head.getLineHeight() + 2;
        }
        if (tipText != null) art.textWrap(b, art.body, tipText, x + pad, cy, w - pad * 2, Pal.INK);
        tipText = null; tipTitle = null;
        tipW = 250;
    }
}
