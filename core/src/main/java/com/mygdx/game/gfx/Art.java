package com.mygdx.game.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything visual that isn't gameplay: procedurally generated shape textures (no sprite art
 * on disk), the baked bitmap fonts, and drawing helpers.
 *
 * Every shape goes through SpriteBatch so a frame is one batch with no ShapeRenderer flushes.
 */
public class Art implements Disposable {

    /** Small UI text. */      public BitmapFont small;
    /** Body copy. */          public BitmapFont body;
    /** Emphasised body. */    public BitmapFont bodyB;
    /** Section headings. */   public BitmapFont head;
    /** Display headings. */   public BitmapFont big;
    /** Hero title. */         public BitmapFont huge;
    /** Condensed numerals. */ public BitmapFont num, numBig, numHuge;
    /** Inscriptional caps for hero titles. */ public BitmapFont title;
    /** Icon face. */          public BitmapFont icon, iconLg, iconXl;

    public TextureRegion white, circle, glow, ringTex, gradTex, sheenTex, paper, star;
    /** Real suit silhouettes, indexed to match Card.Suit: spade, heart, diamond, club. */
    public final TextureRegion[] suits = new TextureRegion[4];
    public NinePatch round, roundLine, roundSoft, roundTiny, roundTinyLine;

    private final GlyphLayout gl = new GlyphLayout();
    private final Color nc = new Color();
    private final List<Texture> owned = new ArrayList<>();
    private final List<BitmapFont> fonts = new ArrayList<>();

    public Art() {
        small = font(Fonts.SMALL);
        body = font(Fonts.BODY);
        bodyB = font(Fonts.BODY_B);
        head = font(Fonts.HEAD);
        big = font(Fonts.BIG);
        huge = font(Fonts.HUGE);
        num = font(Fonts.NUM);
        numBig = font(Fonts.NUM_BIG);
        numHuge = font(Fonts.NUM_HUGE);
        title = font(Fonts.TITLE);
        icon = font(Fonts.ICON);
        iconLg = font(Fonts.ICON_LG);
        iconXl = font(Fonts.ICON_XL);

        white   = reg(solid());
        circle  = reg(circleTex(128));
        glow    = reg(glowTex(160, 2.1f));
        ringTex = reg(ringTex(128, 9));
        gradTex = reg(gradientTex());
        sheenTex = reg(sheenTex(64));
        paper = reg(paperTex(128));
        paper.getTexture().setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        star = reg(starTex(96));
        for (int i = 0; i < 4; i++) suits[i] = reg(suitTex(160, i));

        round     = patch(roundedTex(48, 13, 0), 16);
        roundTiny = patch(roundedTex(28, 7, 0), 9);
        roundLine = patch(roundedTex(48, 13, 2.2f), 16);
        roundTinyLine = patch(roundedTex(28, 7, 1.8f), 9);
        roundSoft = patch(softRoundTex(72, 18, 9), 26);
    }

    private BitmapFont font(Fonts.Spec spec) {
        BitmapFont f = new BitmapFont(Gdx.files.internal(spec.path()));
        // baked pages load with nearest filtering; text is scaled a lot, so smooth it
        for (TextureRegion r : f.getRegions())
            r.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        f.setUseIntegerPositions(false);
        fonts.add(f);
        return f;
    }

    private TextureRegion reg(Texture t) { owned.add(t); return new TextureRegion(t); }

    private NinePatch patch(Texture t, int split) {
        owned.add(t);
        return new NinePatch(new TextureRegion(t), split, split, split, split);
    }

    // ---------------------------------------------------------------- textures

    private static Texture solid() {
        Pixmap p = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        p.setColor(1, 1, 1, 1);
        p.fill();
        Texture t = mk(p);
        p.dispose();
        return t;
    }

    private static Texture mk(Pixmap p) {
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private static float sdRound(float px, float py, float hw, float hh, float r) {
        float qx = Math.abs(px) - (hw - r);
        float qy = Math.abs(py) - (hh - r);
        float mx = Math.max(qx, 0), my = Math.max(qy, 0);
        return (float) Math.sqrt(mx * mx + my * my) + Math.min(Math.max(qx, qy), 0) - r;
    }

    private static Texture roundedTex(int size, int radius, float thickness) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float h = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float d = sdRound(x + 0.5f - h, y + 0.5f - h, h, h, radius);
                float a = thickness <= 0
                    ? clamp01(0.5f - d)
                    : clamp01(Math.min(-d, d + thickness) + 0.5f);
                p.drawPixel(x, y, Color.rgba8888(1, 1, 1, a));
            }
        }
        Texture t = mk(p);
        p.dispose();
        return t;
    }

    private static Texture softRoundTex(int size, int radius, float feather) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float h = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float d = sdRound(x + 0.5f - h, y + 0.5f - h, h - feather, h - feather, radius);
                float a = clamp01(1f - d / feather);
                a = a * a * (3 - 2 * a);
                p.drawPixel(x, y, Color.rgba8888(1, 1, 1, a));
            }
        }
        Texture t = mk(p);
        p.dispose();
        return t;
    }

    private static Texture circleTex(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float h = size / 2f;
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                float dx = x + 0.5f - h, dy = y + 0.5f - h;
                float d = (float) Math.sqrt(dx * dx + dy * dy) - (h - 1);
                p.drawPixel(x, y, Color.rgba8888(1, 1, 1, clamp01(0.5f - d)));
            }
        Texture t = mk(p);
        p.dispose();
        return t;
    }

    private static Texture ringTex(int size, float thickness) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float h = size / 2f;
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                float dx = x + 0.5f - h, dy = y + 0.5f - h;
                float d = (float) Math.sqrt(dx * dx + dy * dy) - (h - 1);
                p.drawPixel(x, y, Color.rgba8888(1, 1, 1, clamp01(Math.min(-d, d + thickness) + 0.5f)));
            }
        Texture t = mk(p);
        p.dispose();
        return t;
    }

    private static Texture glowTex(int size, float power) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float h = size / 2f;
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f - h) / h, dy = (y + 0.5f - h) / h;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = (float) Math.pow(clamp01(1f - d), power);
                p.drawPixel(x, y, Color.rgba8888(1, 1, 1, a));
            }
        Texture t = mk(p);
        p.dispose();
        return t;
    }

    /** White at the top fading to transparent — the gloss band across a card face. */
    private static Texture sheenTex(int size) {
        Pixmap p = new Pixmap(1, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < size; y++) {
            float t = y / (float) (size - 1);        // 0 = top row of the pixmap
            float a = (float) Math.pow(1f - t, 2.4f);
            p.drawPixel(0, y, Color.rgba8888(1, 1, 1, a));
        }
        Texture t = mk(p);
        p.dispose();
        return t;
    }

    private static Texture gradientTex() {
        int h = 256;
        Pixmap p = new Pixmap(1, h, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        Color c = new Color();
        for (int y = 0; y < h; y++) {
            float k = 1f - y / (float) (h - 1);
            if (k < 0.55f) Pal.mix(c, Pal.BG0, Pal.BG1, k / 0.55f);
            else Pal.mix(c, Pal.BG1, Pal.BG2, (k - 0.55f) / 0.45f);
            p.drawPixel(0, y, Color.rgba8888(c.r, c.g, c.b, 1f));
        }
        Texture t = mk(p);
        p.dispose();
        return t;
    }

    /** Faint paper fibre, tiled across card faces so they are not flat slabs. */
    private static Texture paperTex(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        java.util.Random r = new java.util.Random(9137);
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                float n = 0.5f + (r.nextFloat() - 0.5f) * 0.9f;
                float fibre = 0.5f + 0.5f * (float) Math.sin(x * 0.7f + Math.sin(y * 0.31f) * 2.3f);
                float v = clamp01(n * 0.55f + fibre * 0.45f);
                p.drawPixel(x, y, Color.rgba8888(v, v, v, 1f));
            }
        Texture t = mk(p);
        p.dispose();
        return t;
    }

    /** Four-point sparkle used by the cinematic layer. */
    private static Texture starTex(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float h = size / 2f;
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f - h) / h, dy = (y + 0.5f - h) / h;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float core = (float) Math.pow(clamp01(1f - d), 6f);
                float ax = (float) Math.pow(clamp01(1f - Math.abs(dx) * 6.5f), 2f) * clamp01(1f - Math.abs(dy));
                float ay = (float) Math.pow(clamp01(1f - Math.abs(dy) * 6.5f), 2f) * clamp01(1f - Math.abs(dx));
                float a = clamp01(core + (ax + ay) * 0.8f);
                p.drawPixel(x, y, Color.rgba8888(1, 1, 1, a));
            }
        Texture t = mk(p);
        p.dispose();
        return t;
    }

    // ---- suit silhouettes ---------------------------------------------------

    private static float sdCircle(float x, float y, float cx, float cy, float r) {
        float dx = x - cx, dy = y - cy;
        return (float) Math.sqrt(dx * dx + dy * dy) - r;
    }

    /** Signed distance to the half plane left of the line through a -> b. */
    private static float sdHalf(float x, float y, float ax, float ay, float bx, float by) {
        float ex = bx - ax, ey = by - ay;
        float len = (float) Math.sqrt(ex * ex + ey * ey);
        if (len < 1e-5f) return 1f;
        return ((x - ax) * ey - (y - ay) * ex) / len;
    }

    private static float sdTriangle(float x, float y, float ax, float ay, float bx, float by, float cx, float cy) {
        float d1 = sdHalf(x, y, ax, ay, bx, by);
        float d2 = sdHalf(x, y, bx, by, cx, cy);
        float d3 = sdHalf(x, y, cx, cy, ax, ay);
        // Flip for clockwise winding so "inside" is negative either way.
        float area2 = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay);
        float s = area2 < 0 ? -1f : 1f;
        return Math.max(s * d1, Math.max(s * d2, s * d3));
    }

    /** Tapered stem shared by spades and clubs. */
    private static float sdStem(float x, float y) {
        float top = 0.06f;
        float t = clamp01((top - y) / 1.02f);
        float w = 0.055f + 0.40f * t * t * t;
        return Math.max(Math.abs(x) - w, Math.max(y - top, -0.96f - y));
    }

    private static float suitField(int suit, float x, float y) {
        switch (suit) {
            case 0: { // spade: inverted heart plus stem
                float lobes = Math.min(sdCircle(x, y, -0.42f, -0.16f, 0.47f),
                                       sdCircle(x, y, 0.42f, -0.16f, 0.47f));
                float tip = sdTriangle(x, y, -0.88f, -0.16f, 0f, 1.00f, 0.88f, -0.16f);
                return Math.min(Math.min(lobes, tip), sdStem(x, y));
            }
            case 1: { // heart
                float lobes = Math.min(sdCircle(x, y, -0.42f, 0.34f, 0.47f),
                                       sdCircle(x, y, 0.42f, 0.34f, 0.47f));
                float tip = sdTriangle(x, y, -0.88f, 0.34f, 0.88f, 0.34f, 0f, -0.98f);
                return Math.min(lobes, tip);
            }
            case 2:   // diamond
                return (Math.abs(x) / 0.64f + Math.abs(y)) - 1f;
            default: { // club
                float lobes = Math.min(sdCircle(x, y, 0f, 0.50f, 0.42f),
                             Math.min(sdCircle(x, y, -0.50f, -0.14f, 0.42f),
                                      sdCircle(x, y, 0.50f, -0.14f, 0.42f)));
                return Math.min(lobes, sdStem(x, y));
            }
        }
    }

    /** Renders a suit into an alpha texture with 3x3 supersampling. */
    private static Texture suitTex(int size, int suit) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        int ss = 3;
        float scale = 1.08f;   // shape space is roughly [-1,1]; leave a hair of margin
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                float acc = 0;
                for (int sy = 0; sy < ss; sy++) {
                    for (int sx = 0; sx < ss; sx++) {
                        float u = (px + (sx + 0.5f) / ss) / size * 2f - 1f;
                        float v = 1f - (py + (sy + 0.5f) / ss) / size * 2f;
                        if (suitField(suit, u * scale, v * scale) <= 0) acc++;
                    }
                }
                p.drawPixel(px, py, Color.rgba8888(1, 1, 1, acc / (ss * ss)));
            }
        }
        Texture t = mk(p);
        p.dispose();
        return t;
    }

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    // ---------------------------------------------------------------- shapes

    public void rect(Batch b, float x, float y, float w, float h, Color c) { rect(b, x, y, w, h, c, 1f); }

    public void rect(Batch b, float x, float y, float w, float h, Color c, float a) {
        b.setColor(c.r, c.g, c.b, c.a * a);
        b.draw(white, x, y, w, h);
        b.setColor(Color.WHITE);
    }

    /** Vertical gradient fill using the shared sheen ramp, top brightest. */
    public void sheen(Batch b, float x, float y, float w, float h, Color c, float a) {
        b.setColor(c.r, c.g, c.b, c.a * a);
        b.draw(sheenTex, x, y, w, h);
        b.setColor(Color.WHITE);
    }

    public void panel(Batch b, float x, float y, float w, float h, Color c) { panel(b, x, y, w, h, c, 1f); }

    public void panel(Batch b, float x, float y, float w, float h, Color c, float a) {
        np(b, round, x, y, w, h, c, a);
    }

    public void panelSmall(Batch b, float x, float y, float w, float h, Color c, float a) {
        np(b, roundTiny, x, y, w, h, c, a);
    }

    public void outline(Batch b, float x, float y, float w, float h, Color c, float a) {
        np(b, roundLine, x, y, w, h, c, a);
    }

    public void outlineSmall(Batch b, float x, float y, float w, float h, Color c, float a) {
        np(b, roundTinyLine, x, y, w, h, c, a);
    }

    public void bloom(Batch b, float x, float y, float w, float h, Color c, float a, float spread) {
        np(b, roundSoft, x - spread, y - spread, w + spread * 2, h + spread * 2, c, a);
    }

    public void shadow(Batch b, float x, float y, float w, float h, float a, float spread) {
        nc.set(0, 0, 0, a);
        roundSoft.setColor(nc);
        roundSoft.draw(b, x - spread, y - spread, w + spread * 2, h + spread * 2);
        roundSoft.setColor(Color.WHITE);
    }

    private void np(Batch b, NinePatch p, float x, float y, float w, float h, Color c, float a) {
        nc.set(c.r, c.g, c.b, c.a * a);
        p.setColor(nc);
        p.draw(b, x, y, w, h);
        p.setColor(Color.WHITE);
    }

    public void disc(Batch b, float cx, float cy, float r, Color c) { disc(b, cx, cy, r, c, 1f); }

    public void disc(Batch b, float cx, float cy, float r, Color c, float a) {
        b.setColor(c.r, c.g, c.b, c.a * a);
        b.draw(circle, cx - r, cy - r, r * 2, r * 2);
        b.setColor(Color.WHITE);
    }

    public void ring(Batch b, float cx, float cy, float r, Color c, float a) {
        b.setColor(c.r, c.g, c.b, c.a * a);
        b.draw(ringTex, cx - r, cy - r, r * 2, r * 2);
        b.setColor(Color.WHITE);
    }

    public void halo(Batch b, float cx, float cy, float r, Color c, float a) {
        b.setColor(c.r, c.g, c.b, c.a * a);
        b.draw(glow, cx - r, cy - r, r * 2, r * 2);
        b.setColor(Color.WHITE);
    }

    public void line(Batch b, float x1, float y1, float x2, float y2, float thick, Color c, float a) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;
        float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
        b.setColor(c.r, c.g, c.b, c.a * a);
        b.draw(white, x1, y1 - thick / 2f, 0, thick / 2f, len, thick, 1, 1, ang);
        b.setColor(Color.WHITE);
    }

    public void bar(Batch b, float x, float y, float w, float h, float t, Color track, Color fill) {
        panelSmall(b, x, y, w, h, track, 1f);
        float fw = Math.max(0, Math.min(1, t)) * (w - 5);
        if (fw > 2) {
            panelSmall(b, x + 2.5f, y + 2.5f, fw, h - 5, fill, 1f);
            sheen(b, x + 2.5f, y + h / 2f, fw, h / 2f - 2.5f, Color.WHITE, 0.16f);
        }
    }

    // ---------------------------------------------------------------- text

    public float textW(BitmapFont f, CharSequence s) { gl.setText(f, s); return gl.width; }
    public void text(Batch b, BitmapFont f, CharSequence s, float x, float y, Color c) { text(b, f, s, x, y, c, 1f); }

    public void text(Batch b, BitmapFont f, CharSequence s, float x, float y, Color c, float a) {
        f.setColor(c.r, c.g, c.b, c.a * a);
        f.draw(b, s, x, y);
        f.setColor(Color.WHITE);
    }

    public void textC(Batch b, BitmapFont f, CharSequence s, float cx, float y, Color c) { textC(b, f, s, cx, y, c, 1f); }

    public void textC(Batch b, BitmapFont f, CharSequence s, float cx, float y, Color c, float a) {
        gl.setText(f, s);
        text(b, f, s, cx - gl.width / 2f, y, c, a);
    }

    public void textR(Batch b, BitmapFont f, CharSequence s, float rx, float y, Color c) { textR(b, f, s, rx, y, c, 1f); }

    public void textR(Batch b, BitmapFont f, CharSequence s, float rx, float y, Color c, float a) {
        gl.setText(f, s);
        text(b, f, s, rx - gl.width, y, c, a);
    }

    public float textWrap(Batch b, BitmapFont f, CharSequence s, float x, float y, float w, Color c) {
        return textWrap(b, f, s, x, y, w, c, 1f);
    }

    public float textWrap(Batch b, BitmapFont f, CharSequence s, float x, float y, float w, Color c, float a) {
        nc.set(c.r, c.g, c.b, c.a * a);
        f.setColor(nc);
        gl.setText(f, s, nc, w, Align.left, true);
        f.draw(b, s, x, y, w, Align.left, true);
        f.setColor(Color.WHITE);
        return gl.height;
    }

    public float wrapH(BitmapFont f, CharSequence s, float w) {
        gl.setText(f, s, Color.WHITE, w, Align.left, true);
        return gl.height;
    }

    /** Centered text drawn at an arbitrary scale without permanently changing the font. */
    public void textScaled(Batch b, BitmapFont f, CharSequence s, float cx, float y, Color c, float a, float scale) {
        float sx = f.getScaleX(), sy = f.getScaleY();
        f.getData().setScale(sx * scale, sy * scale);
        textC(b, f, s, cx, y, c, a);
        f.getData().setScale(sx, sy);
    }

    /**
     * Centered text that shrinks until it fits {@code maxW}, so a long number can never
     * spill out of its box or collide with a neighbour.
     */
    public float textFitC(Batch b, BitmapFont f, CharSequence s, float cx, float y, float maxW,
                          Color c, float a, float maxScale) {
        gl.setText(f, s);
        float natural = gl.width / Math.max(0.0001f, f.getScaleX());
        float scale = maxScale;
        if (natural * scale > maxW) scale = maxW / natural;
        textScaled(b, f, s, cx, y, c, a, scale);
        return scale;
    }

    public float textFitC(Batch b, BitmapFont f, CharSequence s, float cx, float y, float maxW, Color c) {
        return textFitC(b, f, s, cx, y, maxW, c, 1f, 1f);
    }

    /**
     * Fits one line inside a rectangle and centres it both ways.
     *
     * The text is shrunk until it fits the rect's width and height, so a value placed in a box
     * can never spill over the box edge or collide with whatever sits above or below it.
     * Returns the scale actually used.
     */
    public float textBoxC(Batch b, BitmapFont f, CharSequence s, float x, float y, float w, float h,
                          Color c, float a, float maxScale, float padX) {
        if (s == null || s.length() == 0) return maxScale;
        float base = f.getScaleX(), baseY = f.getScaleY();
        gl.setText(f, s);
        float natW = gl.width / Math.max(1e-4f, base);
        float natH = gl.height / Math.max(1e-4f, baseY);
        float availW = Math.max(4f, w - padX * 2f);
        float availH = Math.max(4f, h - 4f);
        float scale = maxScale;
        if (natW * scale > availW) scale = availW / natW;
        if (natH * scale > availH) scale = Math.min(scale, availH / natH);

        f.getData().setScale(base * scale, baseY * scale);
        gl.setText(f, s);
        // draw(y) puts the top of the line box at y, so centring means half the text height above centre
        float top = y + (h + gl.height) / 2f;
        text(b, f, s, x + (w - gl.width) / 2f, top, c, a);
        f.getData().setScale(base, baseY);
        return scale;
    }

    public float textBoxC(Batch b, BitmapFont f, CharSequence s, float x, float y, float w, float h, Color c) {
        return textBoxC(b, f, s, x, y, w, h, c, 1f, 1f, 6f);
    }

    // ---------------------------------------------------------------- icons

    /** Draws an icon glyph centred on (cx, cy) at roughly {@code size} pixels tall. */
    public void icon(Batch b, char c, float cx, float cy, float size, Color col, float a) {
        BitmapFont f = size <= 22 ? icon : (size <= 34 ? iconLg : iconXl);
        float natural = f == icon ? 20f : (f == iconLg ? 30f : 46f);
        float scale = size / natural;
        String s = String.valueOf(c);
        float sx = f.getScaleX(), sy = f.getScaleY();
        f.getData().setScale(sx * scale, sy * scale);
        gl.setText(f, s);
        f.setColor(col.r, col.g, col.b, col.a * a);
        f.draw(b, s, cx - gl.width / 2f, cy + gl.height / 2f);
        f.setColor(Color.WHITE);
        f.getData().setScale(sx, sy);
    }

    public void icon(Batch b, char c, float cx, float cy, float size, Color col) {
        icon(b, c, cx, cy, size, col, 1f);
    }

    /** Draws a one-character icon glyph, or falls back to text for plain labels. */
    public void glyph(Batch b, String g, float cx, float cy, float size, Color col, float a) {
        if (g == null || g.isEmpty()) return;
        if (g.length() == 1 && g.charAt(0) >= '\uE000') { icon(b, g.charAt(0), cx, cy, size, col, a); return; }
        textFitC(b, big, g, cx, cy - size * 0.34f, size * 1.7f, col, a, size / 32f);
    }

    @Override
    public void dispose() {
        for (Texture t : owned) t.dispose();
        for (BitmapFont f : fonts) f.dispose();
    }
}
