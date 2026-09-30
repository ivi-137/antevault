package com.mygdx.game.gfx;

import com.badlogic.gdx.graphics.Color;

/**
 * Palette. Underworld reds and golds over near-black, with cool accents kept for the
 * chips/mult readouts so the scoring maths stays instantly legible.
 */
public final class Pal {
    private Pal() {}

    public static Color hex(String h) { return Color.valueOf(h); }

    public static final Color BG0      = hex("0a0610");
    public static final Color BG1      = hex("1b0d1d");
    public static final Color BG2      = hex("3a1424");
    public static final Color EMBER    = hex("7a2417");
    public static final Color SKY      = hex("241a3d");

    public static final Color PANEL    = hex("241528");
    public static final Color PANEL_HI = hex("3b2342");
    public static final Color PANEL_LO = hex("140b18");
    public static final Color LINE     = hex("6b4457");

    public static final Color INK      = hex("f7eeda");
    public static final Color DIM      = hex("b39aa4");
    public static final Color DIMMER   = hex("7b6470");

    public static final Color GOLD     = hex("ffc95c");
    public static final Color BRASS    = hex("c2903a");
    public static final Color CHIPS    = hex("4bb0ff");
    public static final Color MULT     = hex("ff3f52");
    public static final Color GREEN    = hex("5fd68a");
    public static final Color PURPLE   = hex("a259ff");
    public static final Color TEAL     = hex("2fd8c8");
    public static final Color ORANGE   = hex("ff8534");
    public static final Color PINK     = hex("ff5fa2");
    public static final Color RED      = hex("e83a3a");
    public static final Color STEEL    = hex("9fb0cc");
    public static final Color STONE    = hex("8a7d84");

    /** Card face + suit colours. */
    public static final Color CARD_FACE = hex("f7efdd");
    public static final Color CARD_EDGE = hex("190e1a");
    public static final Color SUIT_RED  = hex("c62436");
    public static final Color SUIT_BLK  = hex("241b2b");

    public static Color mix(Color a, Color b, float t) {
        return new Color(a.r + (b.r - a.r) * t, a.g + (b.g - a.g) * t,
            a.b + (b.b - a.b) * t, a.a + (b.a - a.a) * t);
    }

    /** Non-allocating mix into {@code out}. */
    public static Color mix(Color out, Color a, Color b, float t) {
        out.set(a.r + (b.r - a.r) * t, a.g + (b.g - a.g) * t,
            a.b + (b.b - a.b) * t, a.a + (b.a - a.a) * t);
        return out;
    }

    /** Accent colour for each world region, used to tint its backdrop. */
    public static Color region(int r) {
        switch (r) {
            case 2:  return TEAL;
            case 3:  return GOLD;
            case 4:  return PURPLE;
            case 5:  return MULT;
            default: return ORANGE;
        }
    }
}
