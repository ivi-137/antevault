package com.mygdx.game.run;

import com.badlogic.gdx.graphics.Color;
import com.mygdx.game.gfx.Icon;
import com.mygdx.game.gfx.Pal;

/**
 * The four permanent unlocks. Each is at once a power you spend during a seal and a key that
 * opens one class of sealed door on the world map - the metroidvania spine of a run.
 *
 * Names and descriptions live in {@code I18n}; this enum is only data and numbers.
 */
public enum Sigil {
    KEY  ("Q", Pal.TEAL,   Icon.KEY),
    TIDE ("W", Pal.PURPLE, Icon.TIDE),
    ECHO ("E", Pal.GOLD,   Icon.ECHO),
    PRISM("R", Pal.PINK,   Icon.PRISM);

    /** Keyboard shortcut. */
    public final String key;
    public final Color color;
    public final char icon;

    Sigil(String key, Color color, char icon) {
        this.key = key; this.color = color; this.icon = icon;
    }

    /** Charges refresh at the start of every seal. */
    public int charges(int rank) { return rank; }

    public int keyDraw(int rank) { return 2 + rank; }

    public double prismMult(int rank) { return 2.0 + 0.5 * (rank - 1); }

    public String prismText(int rank) {
        double v = prismMult(rank);
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
