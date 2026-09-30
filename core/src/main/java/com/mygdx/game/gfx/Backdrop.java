package com.mygdx.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * The painted backdrop: a warm ember floor bleeding up into a cold vault ceiling, silhouetted
 * arches and columns, drifting fog banks and hanging chains. Everything is generated from the
 * shared primitives, tinted per region, and parallaxed against the camera.
 */
public class Backdrop {

    private final Color tmp = new Color();
    private final Color tmp2 = new Color();

    /** Column silhouette seeds, stable for the life of the run. */
    private final float[][] columns = new float[9][4];
    private final float[][] fog = new float[6][5];
    private final float[][] chains = new float[7][3];

    public Backdrop() {
        for (int i = 0; i < columns.length; i++) {
            columns[i][0] = MathUtils.random(-120f, 1400f);
            columns[i][1] = MathUtils.random(0.25f, 1f);        // depth
            columns[i][2] = MathUtils.random(70f, 190f);        // width
            columns[i][3] = MathUtils.random(260f, 620f);       // height
        }
        for (int i = 0; i < fog.length; i++) {
            fog[i][0] = MathUtils.random(-200f, 1480f);
            fog[i][1] = MathUtils.random(-40f, 520f);
            fog[i][2] = MathUtils.random(230f, 560f);
            fog[i][3] = MathUtils.random(6f, 22f);
            fog[i][4] = MathUtils.random(0f, 6.28f);
        }
        for (int i = 0; i < chains.length; i++) {
            chains[i][0] = MathUtils.random(40f, 1240f);
            chains[i][1] = MathUtils.random(90f, 330f);          // length
            chains[i][2] = MathUtils.random(0f, 6.28f);
        }
    }

    /**
     * @param region  1..5, tints the whole scene
     * @param parX    horizontal parallax offset in world units
     * @param intense 0..1, how much ember light to throw
     */
    public void draw(SpriteBatch b, Art art, int region, float time, float dt,
                     float w, float h, float parX, float intense) {
        Color accent = Pal.region(region);

        // --- sky: cold vault above, warm forge below
        b.setColor(Color.WHITE);
        b.draw(art.gradTex, -90, -90, w + 180, h + 180);
        Pal.mix(tmp, Pal.SKY, accent, 0.10f);
        art.sheen(b, -90, h * 0.42f, w + 180, h * 0.68f, tmp, 0.55f);

        // --- ember floor glow
        Pal.mix(tmp2, Pal.EMBER, accent, 0.25f);
        float pulse = 0.72f + 0.28f * MathUtils.sin(time * 0.7f);
        art.halo(b, w * 0.5f, -60, w * 0.85f, tmp2, (0.30f + 0.12f * pulse) * intense);
        art.halo(b, w * 0.22f, -40, w * 0.40f, Pal.ORANGE, 0.13f * pulse * intense);
        art.halo(b, w * 0.80f, -40, w * 0.36f, tmp2, 0.11f * pulse * intense);

        // --- far arch: a big blind rosette behind everything
        float ax = w * 0.5f - parX * 0.05f;
        art.ring(b, ax, h * 0.62f, h * 0.40f, Pal.mix(Pal.LINE, accent, 0.4f), 0.10f);
        art.ring(b, ax, h * 0.62f, h * 0.31f, Pal.mix(Pal.LINE, accent, 0.6f), 0.08f);
        for (int i = 0; i < 12; i++) {
            float a = i / 12f * MathUtils.PI2 + time * 0.03f;
            float r0 = h * 0.31f, r1 = h * 0.40f;
            art.line(b, ax + MathUtils.cos(a) * r0, h * 0.62f + MathUtils.sin(a) * r0,
                ax + MathUtils.cos(a) * r1, h * 0.62f + MathUtils.sin(a) * r1, 3f,
                Pal.mix(Pal.LINE, accent, 0.5f), 0.09f);
        }

        // --- columns, near ones darker and parallaxed harder
        for (float[] c : columns) {
            float depth = c[1];
            float x = c[0] - parX * (0.10f + depth * 0.35f);
            x = wrap(x, -260, w + 260);
            float cw = c[2] * (0.5f + depth * 0.8f);
            float ch = c[3] * (0.6f + depth * 0.9f);
            Pal.mix(tmp, Pal.BG1, Pal.PANEL_LO, depth);
            art.rect(b, x, -40, cw, ch, tmp, 0.55f + depth * 0.35f);
            // capital + base
            art.panelSmall(b, x - cw * 0.10f, ch - 46 - 40, cw * 1.2f, 24, tmp, 0.7f + depth * 0.25f);
            art.panelSmall(b, x - cw * 0.08f, -40, cw * 1.16f, 20, tmp, 0.7f + depth * 0.25f);
            // rim light from the forge below
            art.rect(b, x, -40, 3.5f, ch, Pal.mix(Pal.EMBER, accent, 0.4f), 0.20f * depth * intense);
            art.rect(b, x + cw - 3.5f, -40, 3.5f, ch, Pal.mix(Pal.EMBER, accent, 0.4f), 0.10f * depth * intense);
        }

        // --- hanging chains
        for (float[] c : chains) {
            float x = wrap(c[0] - parX * 0.22f, -60, w + 60);
            float len = c[1];
            float sway = MathUtils.sin(time * 0.6f + c[2]) * 5f;
            for (int i = 0; i < 9; i++) {
                float t = i / 9f;
                float yy = h + 20 - t * len;
                art.disc(b, x + sway * t, yy, 3.2f, Pal.PANEL_LO, 0.5f);
            }
            art.disc(b, x + sway, h + 20 - len, 6f, Pal.mix(Pal.BRASS, Pal.PANEL_LO, 0.35f), 0.55f);
        }

        // --- fog banks
        for (float[] f : fog) {
            f[0] += f[3] * dt;
            if (f[0] > w + 400) f[0] = -400;
            float y = f[1] + MathUtils.sin(time * 0.25f + f[4]) * 16f;
            Pal.mix(tmp, Pal.PANEL_HI, accent, 0.22f);
            art.halo(b, f[0] - parX * 0.06f, y, f[2], tmp, 0.055f);
        }
    }

    private static float wrap(float v, float lo, float hi) {
        float span = hi - lo;
        while (v < lo) v += span;
        while (v > hi) v -= span;
        return v;
    }
}
