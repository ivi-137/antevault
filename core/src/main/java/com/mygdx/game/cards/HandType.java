package com.mygdx.game.cards;

import com.badlogic.gdx.graphics.Color;
import com.mygdx.game.gfx.Pal;

/** Poker hands, ordered weakest to strongest, with their base and per-level scoring. */
public enum HandType {
    HIGH_CARD    ("High Card",       5,   1,  10, 1, Pal.DIM),
    PAIR         ("Pair",            10,  2,  15, 1, Pal.DIM),
    TWO_PAIR     ("Two Pair",        20,  2,  20, 1, Pal.TEAL),
    THREE_KIND   ("Three of a Kind", 30,  3,  20, 2, Pal.TEAL),
    STRAIGHT     ("Straight",        30,  4,  30, 3, Pal.GREEN),
    FLUSH        ("Flush",           35,  4,  15, 2, Pal.GREEN),
    FULL_HOUSE   ("Full House",      40,  4,  25, 2, Pal.PURPLE),
    FOUR_KIND    ("Four of a Kind",  60,  7,  30, 3, Pal.PURPLE),
    STRAIGHT_FLUSH("Straight Flush", 100, 8,  40, 4, Pal.ORANGE),
    FIVE_KIND    ("Five of a Kind",  120, 12, 35, 3, Pal.ORANGE),
    FLUSH_HOUSE  ("Flush House",     140, 14, 40, 4, Pal.GOLD),
    FLUSH_FIVE   ("Flush Five",      160, 16, 50, 3, Pal.GOLD);

    public final String label;
    public final int baseChips, baseMult, chipsPerLvl, multPerLvl;
    public final Color color;

    HandType(String l, int bc, int bm, int cpl, int mpl, Color c) {
        label = l; baseChips = bc; baseMult = bm; chipsPerLvl = cpl; multPerLvl = mpl; color = c;
    }

    public int chipsAt(int level) { return baseChips + chipsPerLvl * (level - 1); }
    public int multAt(int level)  { return baseMult + multPerLvl * (level - 1); }

    /** Hands the player is likely to see; the three "secret" ones are hidden until discovered. */
    public boolean secret() {
        return this == FIVE_KIND || this == FLUSH_HOUSE || this == FLUSH_FIVE;
    }
}
