package com.mygdx.game.battle;

import com.badlogic.gdx.graphics.Color;
import com.mygdx.game.gfx.Pal;

/** Rule benders carried by champion and warden seals, in the spirit of Balatro's boss blinds. */
public enum BossMod {
    NONE       ("",             "",                                                        Pal.DIM),
    INHIBITOR  ("Inhibitor",    "Every poker hand scores as if it were level 1.",           Pal.RED),
    FOG_OF_WAR ("Fog of War",   "You cannot read the suits of cards in your hand.",         Pal.STEEL),
    SUMP       ("The Sump",     "Your hand size is reduced by 2.",                          Pal.PURPLE),
    NULL_ZONE  ("Null Zone",    "One suit is debuffed and scores nothing.",                 Pal.TEAL),
    EXECUTIONER("Executioner",  "You get one fewer hand.",                                  Pal.RED),
    DRAIN      ("The Drain",    "You get one fewer discard.",                               Pal.CHIPS),
    BRAMBLE    ("Bramble Ward", "The first card scored in each hand gives no Chips.",        Pal.GREEN),
    TITHE      ("The Tithe",    "Every hand you play costs $2.",                            Pal.GOLD),
    SHACKLE    ("Shackle",      "Hands of fewer than three cards score nothing.",           Pal.STONE),
    MIRROR     ("The Mirror",   "The same hand type cannot be played twice in a row.",      Pal.PINK),
    JAMMER     ("The Jammer",   "Every Sigil has one fewer charge.",                        Pal.ORANGE);

    public final String label, desc;
    public final Color color;

    BossMod(String l, String d, Color c) { label = l; desc = d; color = c; }

    public static final BossMod[] POOL = {
        INHIBITOR, FOG_OF_WAR, SUMP, NULL_ZONE, EXECUTIONER, DRAIN,
        BRAMBLE, TITHE, SHACKLE, MIRROR, JAMMER
    };
}
