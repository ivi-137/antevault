package com.mygdx.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

/**
 * The cinematic director. Gameplay code calls these in plain language ("punch", "shock",
 * "hitstop") and this class decays them into the uniforms and camera values the renderer wants.
 *
 * Every value decays back to rest on its own, so callers only ever trigger effects and
 * never have to clear them.
 */
public class Cine {

    // --- shader-side, all decaying toward rest
    public float shockX = 0.5f, shockY = 0.5f;
    public float shockRadius, shockAmt;
    public float punch;        // radial blur burst
    public float chroma;       // chromatic aberration spike
    public float defocus;      // full screen blur

    // --- 5 special effects, all shader side
    public float haze;         // 2: heat shimmer around impacts
    public float burst;        // 3: radial god-ray burst from a point
    public float burstX = 0.5f, burstY = 0.5f;
    public float grade;        // 4: colour grade push
    public final Color gradeCol = new Color(Pal.GOLD);
    public float edge;         // 5: igniting frame edges

    // --- camera / timing
    public float zoom;         // additive zoom punch (negative = pushes in)
    public float hitstop;      // seconds of frozen game time
    public float letterbox;    // 0..1 bar height
    private boolean bars;

    // --- 5 cinematic features
    /** 1: sustained slow motion, separate from the short hit-stop. */
    public float slowmo;
    /** 2: camera push, in 1280x720 units, eased toward a subject. */
    public float pushX, pushY, pushTargetX, pushTargetY, pushAmt;
    /** 3: anticipation speed lines converging on a point. */
    public float lines, linesX = 0.5f, linesY = 0.5f;
    /** 4: title card wipe, 0..1 as it sweeps across. */
    public float card;
    public String cardTitle, cardSub;
    /** 5: focus rack - how hard the backdrop is thrown out of focus. */
    public float rack;

    // --- full screen colour flash
    public final Color flashCol = new Color(Pal.GOLD);
    public float flash;

    /** Expanding ring from a point in screen space (pixels, y up, 1280x720 space). */
    public void shock(float x, float y, float strength) {
        shockX = x / 1280f;
        shockY = y / 720f;
        shockRadius = 0f;
        shockAmt = Math.max(shockAmt, strength);
    }

    public void punch(float a) { punch = Math.max(punch, a); }
    public void chroma(float a) { chroma = Math.max(chroma, a); }
    public void zoom(float a) { zoom = Math.max(zoom, a); }
    public void hitstop(float t) { hitstop = Math.max(hitstop, t); }
    public void defocus(float a) { defocus = Math.max(defocus, a); }

    public void flash(Color c, float a) { flashCol.set(c); flash = Math.max(flash, a); }

    // ---- 5 cinematic features ------------------------------------------------

    /** 1: hold the world in slow motion for a beat. */
    public void slowmo(float seconds) { slowmo = Math.max(slowmo, seconds); }

    /** 2: drift the camera toward a subject, in screen units. */
    public void pushTo(float x, float y, float amount) {
        pushTargetX = (x - 640f) * 0.06f;
        pushTargetY = (y - 360f) * 0.06f;
        pushAmt = Math.max(pushAmt, amount);
    }

    public void releasePush() { pushAmt = 0; pushTargetX = 0; pushTargetY = 0; }

    /** 3: anticipation streaks converging on a point. */
    public void speedLines(float x, float y, float amount) {
        linesX = x / 1280f; linesY = y / 720f;
        lines = Math.max(lines, amount);
    }

    /** 4: sweep a title card across the screen. */
    public void titleCard(String title, String sub) {
        cardTitle = title; cardSub = sub; card = 1f;
    }

    /** 5: throw the backdrop out of focus so the foreground reads. */
    public void rack(float amount) { rack = Math.max(rack, amount); }

    // ---- 5 special effects ---------------------------------------------------

    public void haze(float a) { haze = Math.max(haze, a); }

    public void burst(float x, float y, float a) {
        burstX = x / 1280f; burstY = y / 720f;
        burst = Math.max(burst, a);
    }

    public void grade(Color c, float a) { gradeCol.set(c); grade = Math.max(grade, a); }

    public void edge(float a) { edge = Math.max(edge, a); }

    public void letterbox(boolean on) { bars = on; }

    /** A full "big moment" package: shove, blur, colour split and a beat of frozen time. */
    public void impact(float x, float y, float scale, Color col) {
        shock(x, y, 0.010f * scale);
        punch(0.030f * scale);
        chroma(0.05f * scale);
        zoom(0.035f * scale);
        hitstop(0.055f * scale);
        flash(col, 0.16f * scale);
        haze(0.35f * scale);
        edge(0.30f * scale);
        grade(col, 0.16f * scale);
    }

    /** The full cinematic treatment, for a seal breaking. */
    public void finale(float x, float y, Color col) {
        impact(x, y, 1.6f, col);
        slowmo(1.1f);
        burst(x, y, 1.0f);
        speedLines(x, y, 0.9f);
        rack(1.0f);
        pushTo(x, y, 1f);
        grade(col, 0.34f);
    }

    public void update(float dt) {
        if (slowmo > 0) slowmo = Math.max(0, slowmo - dt);
        haze = decay(haze, dt, 2.2f);
        burst = decay(burst, dt, 1.7f);
        grade = decay(grade, dt, 1.6f);
        edge = decay(edge, dt, 2.4f);
        lines = decay(lines, dt, 2.6f);
        rack = decay(rack, dt, 1.5f);
        if (card > 0) card = Math.max(0, card - dt * 0.45f);
        pushAmt = decay(pushAmt, dt, 1.2f);
        pushX = MathUtils.lerp(pushX, pushTargetX * pushAmt, Math.min(1, dt * 3.2f));
        pushY = MathUtils.lerp(pushY, pushTargetY * pushAmt, Math.min(1, dt * 3.2f));
        if (shockAmt > 0) {
            shockRadius += dt * 1.15f;
            shockAmt = Math.max(0, shockAmt - dt * 0.024f);
            if (shockRadius > 1.5f) { shockAmt = 0; shockRadius = 0; }
        }
        punch = decay(punch, dt, 4.5f);
        chroma = decay(chroma, dt, 3.2f);
        zoom = decay(zoom, dt, 6.0f);
        flash = decay(flash, dt, 3.4f);
        defocus = decay(defocus, dt, 5.0f);
        letterbox = MathUtils.lerp(letterbox, bars ? 1f : 0f, Math.min(1, dt * 6f));
        if (hitstop > 0) hitstop = Math.max(0, hitstop - dt);
    }

    private static float decay(float v, float dt, float rate) {
        if (v <= 0) return 0;
        return Math.max(0, v - v * dt * rate - dt * 0.02f);
    }

    /** Combined time scale: hit-stop bites hard, slow motion is gentler. */
    public float timeScale() {
        if (hitstop > 0) return 0.14f;
        if (slowmo > 0) return 0.35f;
        return 1f;
    }

    public void reset() {
        shockAmt = 0; shockRadius = 0; punch = 0; chroma = 0; zoom = 0;
        flash = 0; defocus = 0; hitstop = 0; bars = false; letterbox = 0;
        haze = 0; burst = 0; grade = 0; edge = 0; lines = 0; rack = 0; slowmo = 0;
        card = 0; cardTitle = null; pushAmt = 0; pushX = pushY = 0;
        pushTargetX = pushTargetY = 0;
    }
}
