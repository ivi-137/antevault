package com.mygdx.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.mygdx.game.cards.Card;

/** Playing cards, drawn entirely from generated primitives. No card art on disk. */
public final class CardArt {

    public static final float W = 98, H = 140;

    private CardArt() {}

    /**
     * How far a square-cornered overlay must be inset to stay inside the rounded face.
     *
     * The face is a nine-patch whose corner arc has a radius of about 13 units, so a point
     * inset by i on both axes is inside the arc when (13 - i) * sqrt(2) <= 13, i.e. i >= 3.8.
     * Six gives that a comfortable margin at every card scale.
     */
    private static final float CORNER_SAFE = 6f;

    private static final Color tmp = new Color();
    private static final Color tmp2 = new Color();
    private static final Matrix4 mat = new Matrix4();
    private static final Matrix4 saved = new Matrix4();

    /** Draws a real suit silhouette, centred, sized by its height. */
    public static void pip(Batch b, Art art, Card.Suit s, float cx, float cy, float size, Color col, float a) {
        b.setColor(col.r, col.g, col.b, col.a * a);
        b.draw(art.suits[s.ordinal()], cx - size / 2f, cy - size / 2f, size, size);
        b.setColor(Color.WHITE);
    }

    public static void pipRot(Batch b, Art art, Card.Suit s, float cx, float cy, float size,
                              Color col, float a, float rot) {
        b.setColor(col.r, col.g, col.b, col.a * a);
        b.draw(art.suits[s.ordinal()], cx - size / 2f, cy - size / 2f, size / 2f, size / 2f,
            size, size, 1, 1, rot);
        b.setColor(Color.WHITE);
    }

    // ---------------------------------------------------------------- card

    public static void draw(Batch b, Art art, Card c, float x, float y, float w, float h,
                            float rot, boolean selected, boolean hideSuit, float flash,
                            Color rim, float rimAmt, float alpha, float time) {
        if (Math.abs(rot) > 0.05f) {
            saved.set(b.getTransformMatrix());
            mat.set(saved);
            mat.translate(x + w / 2f, y + h / 2f, 0).rotate(0, 0, 1, rot).translate(-(x + w / 2f), -(y + h / 2f), 0);
            b.setTransformMatrix(mat);
            body(b, art, c, x, y, w, h, selected, hideSuit, flash, rim, rimAmt, alpha, time);
            b.setTransformMatrix(saved);
        } else {
            body(b, art, c, x, y, w, h, selected, hideSuit, flash, rim, rimAmt, alpha, time);
        }
    }

    private static void body(Batch b, Art art, Card c, float x, float y, float w, float h,
                             boolean selected, boolean hideSuit, float flash,
                             Color rim, float rimAmt, float alpha, float time) {
        float cx = x + w / 2f, cy = y + h / 2f;
        float u = w / W;                       // one design unit

        art.shadow(b, x + 1.5f, y - 7 * u, w - 3, h, 0.5f * alpha, 10 * u);
        if (rimAmt > 0.01f) art.bloom(b, x, y, w, h, rim, rimAmt * 0.8f, 15 * u);

        // --- dark edge, ivory face inset inside it
        Color edge = c.debuffed ? Pal.STONE : Pal.CARD_EDGE;
        art.panel(b, x, y, w, h, edge, alpha);

        float in = 4.5f * u;
        float fx = x + in, fy = y + in, fw = w - in * 2, fh = h - in * 2;

        Color face = c.faceColor();
        if (c.debuffed) face = Pal.mix(face, Pal.STONE, 0.55f);
        if (flash > 0.01f) face = Pal.mix(face, Color.WHITE, Math.min(1, flash));
        art.panel(b, fx, fy, fw, fh, face, alpha);

        // Paper grain and the shading gradients are square quads. They get inset far enough
        // that their corners stay inside the rounded silhouette - drawn flush they would fill
        // the rounded corners back in and the card would read as a hard rectangle.
        float gx = fx + CORNER_SAFE, gy = fy + CORNER_SAFE;
        float gw = fw - CORNER_SAFE * 2, gh = fh - CORNER_SAFE * 2;
        b.setColor(1, 1, 1, 0.055f * alpha);
        b.draw(art.paper.getTexture(), gx, gy, gw, gh, 0f, 0f, gw / 90f, gh / 90f);
        b.setColor(Color.WHITE);
        Pal.mix(tmp2, face, Pal.CARD_EDGE, 0.22f);
        art.sheen(b, gx, gy, gw, gh * 0.30f, tmp2, 0.30f * alpha);
        art.sheen(b, gx, gy + gh * 0.55f, gw, gh * 0.45f, Color.WHITE, 0.16f * alpha);

        Color ink = c.isStone() ? Pal.PANEL_LO : (hideSuit ? Pal.SUIT_BLK : c.suit.color);
        if (c.debuffed) ink = Pal.mix(ink, Pal.PANEL_LO, 0.55f);

        // hairline frame inside the face
        Pal.mix(tmp, ink, face, 0.70f);
        art.outlineSmall(b, fx + 3 * u, fy + 3 * u, fw - 6 * u, fh - 6 * u, tmp, 0.55f * alpha);

        if (c.isStone()) drawStone(b, art, x, y, w, h, u, alpha);
        else if (hideSuit) drawHidden(b, art, c, cx, cy, u, ink, face, alpha);
        else drawFace(b, art, c, x, y, w, h, u, ink, face, alpha);

        // --- edition overlays
        switch (c.ed) {
            case FOIL:
                art.panel(b, fx, fy, fw, fh, Pal.CHIPS, 0.22f * alpha);
                sweep(b, art, gx, gy, gw, gh, Pal.CHIPS, 0.55f * alpha, time * 0.55f + c.uid * 0.11f);
                break;
            case HOLO:
                art.panel(b, fx, fy, fw, fh, Pal.MULT, 0.20f * alpha);
                sweep(b, art, gx, gy, gw, gh, Pal.PINK, 0.45f * alpha, time * 0.42f + c.uid * 0.19f);
                break;
            case POLY: {
                float t = (time * 0.5f + c.uid * 0.137f) % 1f;
                tmp.set(0.55f + 0.45f * MathUtils.sin(t * MathUtils.PI2),
                        0.55f + 0.45f * MathUtils.sin(t * MathUtils.PI2 + 2.09f),
                        0.55f + 0.45f * MathUtils.sin(t * MathUtils.PI2 + 4.19f), 1f);
                art.panel(b, fx, fy, fw, fh, tmp, 0.30f * alpha);
                sweep(b, art, gx, gy, gw, gh, Color.WHITE, 0.5f * alpha, time * 0.7f + c.uid * 0.31f);
                break;
            }
            default: break;
        }

        // --- enhancement banner
        if (c.enh != Card.Enhance.NONE && !c.isStone()) {
            float bh = 15 * u;
            art.panelSmall(b, fx + 2 * u, fy + 2 * u, fw - 4 * u, bh, c.enh.color, 0.95f * alpha);
            art.textFitC(b, art.small, c.enh.label.toUpperCase(), cx, fy + 2 * u + bh - 4.5f * u,
                fw - 10 * u, Pal.PANEL_LO, alpha, u);
        }

        // --- wax seal
        if (c.seal != Card.Seal.NONE) {
            float sx = x + w - 13 * u, sy = y + h - 13 * u;
            art.halo(b, sx, sy, 13 * u, c.seal.color, 0.5f * alpha);
            art.disc(b, sx, sy, 6.5f * u, c.seal.color, alpha);
            art.disc(b, sx - 1.5f * u, sy + 1.5f * u, 2.4f * u, Color.WHITE, 0.4f * alpha);
        }

        if (selected) {
            art.outline(b, x - 1.5f, y - 1.5f, w + 3, h + 3, Pal.GOLD, 0.95f * alpha);
            art.bloom(b, x, y, w, h, Pal.GOLD, 0.26f * alpha, 13 * u);
        }

        if (c.debuffed) {
            art.line(b, fx + 8 * u, fy + 8 * u, fx + fw - 8 * u, fy + fh - 8 * u, 3.5f, Pal.RED, 0.85f * alpha);
            art.line(b, fx + fw - 8 * u, fy + 8 * u, fx + 8 * u, fy + fh - 8 * u, 3.5f, Pal.RED, 0.85f * alpha);
        }
    }

    /** Diagonal specular band travelling across the face; clipped to the card by its own width. */
    private static void sweep(Batch b, Art art, float x, float y, float w, float h,
                              Color c, float a, float phase) {
        float t = (phase % 1f);
        if (t > 0.55f) return;                 // long pause between sweeps
        float p = t / 0.55f;
        float cxp = x - w * 0.5f + p * (w * 2f);
        float bandW = w * 0.30f;
        float fade = MathUtils.sin(p * MathUtils.PI);
        for (int i = 0; i < 3; i++) {
            float off = (i - 1) * bandW * 0.28f;
            float bx = cxp + off;
            float lo = Math.max(x, bx - bandW * 0.14f);
            float hi = Math.min(x + w, bx + bandW * 0.14f);
            if (hi <= lo) continue;
            art.rect(b, lo, y, hi - lo, h, c, a * fade * (i == 1 ? 0.20f : 0.10f));
        }
    }

    private static void drawStone(Batch b, Art art, float x, float y, float w, float h, float u, float alpha) {
        float cx = x + w / 2f, cy = y + h / 2f;
        for (int i = 0; i < 6; i++) {
            float px = cx + MathUtils.sin(i * 2.3f) * w * 0.22f;
            float py = cy + MathUtils.cos(i * 1.7f) * h * 0.18f;
            art.disc(b, px, py, w * 0.15f, Pal.mix(Pal.STONE, Pal.PANEL_LO, 0.20f + i * 0.07f), alpha);
        }
        art.textFitC(b, art.bodyB, "STONE", cx, cy + 2 * u, w * 0.72f, Pal.PANEL_LO, alpha, u);
        art.textFitC(b, art.small, "+60 Chips", cx, cy - 16 * u, w * 0.72f, Pal.PANEL_LO, alpha * 0.9f, u);
    }

    private static void drawHidden(Batch b, Art art, Card c, float cx, float cy, float u,
                                   Color ink, Color face, float alpha) {
        art.textFitC(b, art.big, c.rank.label, cx, cy + 14 * u, 46 * u, ink, alpha, u);
        art.textFitC(b, art.big, "?", cx, cy - 22 * u, 40 * u, Pal.mix(ink, face, 0.55f), alpha * 0.75f, u * 0.8f);
    }

    /** Classic pip positions in normalised card space, x and y both in -1..1. */
    private static float[] pipLayout(int order) {
        switch (order) {
            case 2:  return new float[]{ 0, 1,  0, -1 };
            case 3:  return new float[]{ 0, 1,  0, 0,  0, -1 };
            case 4:  return new float[]{ -1, 1,  1, 1,  -1, -1,  1, -1 };
            case 5:  return new float[]{ -1, 1,  1, 1,  0, 0,  -1, -1,  1, -1 };
            case 6:  return new float[]{ -1, 1,  1, 1,  -1, 0,  1, 0,  -1, -1,  1, -1 };
            case 7:  return new float[]{ -1, 1,  1, 1,  0, 0.5f,  -1, 0,  1, 0,  -1, -1,  1, -1 };
            case 8:  return new float[]{ -1, 1,  1, 1,  0, 0.5f,  -1, 0,  1, 0,  0, -0.5f,  -1, -1,  1, -1 };
            case 9:  return new float[]{ -1, 1,  1, 1,  -1, 0.34f,  1, 0.34f,  0, 0,
                                         -1, -0.34f,  1, -0.34f,  -1, -1,  1, -1 };
            default: return new float[]{ -1, 1,  1, 1,  0, 0.66f,  -1, 0.34f,  1, 0.34f,
                                         -1, -0.34f,  1, -0.34f,  0, -0.66f,  -1, -1,  1, -1 };
        }
    }

    private static void drawFace(Batch b, Art art, Card c, float x, float y, float w, float h,
                                 float u, Color ink, Color face, float alpha) {
        float cx = x + w / 2f, cy = y + h / 2f;

        // --- corner indices.
        // These own a reserved column at each end of the card; the pip field below is sized so
        // it can never reach into them, whatever the rank or the card scale.
        float idxCx = 15 * u;          // centre of the index column, from the card edge
        float idxW = 17 * u;           // the widest a rank glyph may be
        float idxPip = 11 * u;

        art.textFitC(b, art.numBig, c.rank.label, x + idxCx, y + h - 9 * u, idxW, ink, alpha, u * 0.58f);
        pip(b, art, c.suit, x + idxCx, y + h - 32 * u, idxPip, ink, alpha);
        pipRot(b, art, c.suit, x + w - idxCx, y + 32 * u, idxPip, ink, alpha * 0.92f, 180);
        art.textFitC(b, art.numBig, c.rank.label, x + w - idxCx, y + 26 * u, idxW, ink, alpha * 0.92f, u * 0.58f);

        if (c.rank.face()) {
            float mw = w * 0.46f, mh = h * 0.42f;
            Pal.mix(tmp, ink, face, 0.78f);
            art.panelSmall(b, cx - mw / 2, cy - mh / 2, mw, mh, tmp, alpha);
            art.outlineSmall(b, cx - mw / 2, cy - mh / 2, mw, mh, ink, 0.5f * alpha);
            art.halo(b, cx, cy + 6 * u, mw * 0.55f, ink, 0.10f * alpha);
            art.textFitC(b, art.big, c.rank.label, cx, cy + 14 * u, mw * 0.7f, ink, alpha, u);
            pip(b, art, c.suit, cx, cy - 16 * u, 16 * u, ink, alpha * 0.85f);
        } else if (c.rank == Card.Rank.ACE) {
            art.halo(b, cx, cy, w * 0.38f, ink, 0.16f * alpha);
            pip(b, art, c.suit, cx, cy, w * 0.46f, ink, alpha);
        } else {
            float[] pts = pipLayout(c.rank.order);
            boolean crowded = pts.length > 14;
            float size = crowded ? w * 0.150f : w * 0.180f;

            // The pip field is inset far enough that a pip's bounding box stops short of the
            // index columns horizontally and of the index rows vertically.
            float colX = Math.max(0f, w / 2f - (idxCx + idxW / 2f) - size / 2f - 3 * u);
            float rowY = h / 2f - 40 * u - size / 2f;

            for (int i = 0; i < pts.length; i += 2) {
                float px = cx + pts[i] * colX;
                float py = cy + pts[i + 1] * rowY;
                // the lower half of a real card is printed upside down
                if (pts[i + 1] < -0.05f) pipRot(b, art, c.suit, px, py, size, ink, alpha, 180);
                else pip(b, art, c.suit, px, py, size, ink, alpha);
            }
        }
    }

    /** Face-down card for the draw pile. */
    public static void back(Batch b, Art art, float x, float y, float w, float h, float alpha, float time) {
        float u = w / W;
        art.shadow(b, x + 1.5f, y - 6 * u, w - 3, h, 0.42f * alpha, 9 * u);
        art.panel(b, x, y, w, h, Pal.CARD_EDGE, alpha);
        float in = 4.5f * u;
        art.panel(b, x + in, y + in, w - in * 2, h - in * 2, Pal.mix(Pal.PURPLE, Pal.PANEL_LO, 0.58f), alpha);
        art.outlineSmall(b, x + 7 * u, y + 7 * u, w - 14 * u, h - 14 * u, Pal.TEAL, 0.35f * alpha);
        for (int i = 0; i < 4; i++) {
            float r = (w * 0.40f) * (1f - i * 0.21f);
            art.ring(b, x + w / 2, y + h / 2, r, Pal.PURPLE,
                (0.26f + 0.12f * MathUtils.sin(time * 1.6f + i)) * alpha);
        }
        art.disc(b, x + w / 2, y + h / 2, w * 0.09f, Pal.TEAL, 0.85f * alpha);
    }
}
