package com.mygdx.game.cards;

import com.badlogic.gdx.graphics.Color;
import com.mygdx.game.gfx.Pal;

/** A single playing card plus its Balatro-style modifiers. */
public class Card {

    public enum Suit {
        SPADES("Spades", Pal.SUIT_BLK),
        HEARTS("Hearts", Pal.SUIT_RED),
        DIAMONDS("Diamonds", Pal.SUIT_RED),
        CLUBS("Clubs", Pal.SUIT_BLK);

        public final String label;
        public final Color color;
        Suit(String l, Color c) { label = l; color = c; }
        public boolean red() { return this == HEARTS || this == DIAMONDS; }
    }

    public enum Rank {
        TWO("2", 2, 2), THREE("3", 3, 3), FOUR("4", 4, 4), FIVE("5", 5, 5),
        SIX("6", 6, 6), SEVEN("7", 7, 7), EIGHT("8", 8, 8), NINE("9", 9, 9),
        TEN("10", 10, 10), JACK("J", 11, 10), QUEEN("Q", 12, 10), KING("K", 13, 10),
        ACE("A", 14, 11);

        public final String label;
        public final int order;   // 2..14, ace high
        public final int chips;
        Rank(String l, int o, int c) { label = l; order = o; chips = c; }
        public boolean face() { return this == JACK || this == QUEEN || this == KING; }
    }

    /** Card enhancements. Mirrors Balatro's set, retuned for this game's numbers. */
    public enum Enhance {
        NONE("", "", Pal.CARD_FACE),
        BONUS("Bonus", "+40 Chips when scored", Pal.CHIPS),
        MULT("Mult", "+5 Mult when scored", Pal.MULT),
        WILD("Wild", "Counts as every suit", Pal.PINK),
        GLASS("Glass", "X2 Mult. 1 in 4 chance to shatter after scoring", Pal.TEAL),
        STEEL("Steel", "X1.5 Mult while this stays in your hand", Pal.STEEL),
        GOLD("Gold", "$3 at end of encounter if held in hand", Pal.GOLD),
        STONE("Stone", "+60 Chips. No rank and no suit"), // constructed below
        LUCKY("Lucky", "1 in 5: +20 Mult.  1 in 15: $20", Pal.GREEN);

        public final String label, desc;
        public final Color color;
        Enhance(String l, String d, Color c) { label = l; desc = d; color = c; }
        Enhance(String l, String d) { label = l; desc = d; color = Pal.STONE; }
    }

    public enum Edition {
        NONE("", ""), FOIL("Foil", "+60 Chips"), HOLO("Holo", "+12 Mult"), POLY("Polychrome", "X1.5 Mult");
        public final String label, desc;
        Edition(String l, String d) { label = l; desc = d; }
    }

    public enum Seal {
        NONE("", "", Pal.CARD_FACE),
        RED("Red Seal", "Retriggers this card once", Pal.RED),
        GOLD("Gold Seal", "$4 when scored", Pal.GOLD),
        BLUE("Blue Seal", "Levels the played hand when scored", Pal.CHIPS);
        public final String label, desc;
        public final Color color;
        Seal(String l, String d, Color c) { label = l; desc = d; color = c; }
    }

    public Suit suit;
    public Rank rank;
    public Enhance enh = Enhance.NONE;
    public Edition ed = Edition.NONE;
    public Seal seal = Seal.NONE;
    /** Set by boss modifiers for the duration of an encounter. Debuffed cards score nothing. */
    public boolean debuffed;
    /** Stable id so animation state can follow a card through shuffles. */
    public final int uid;

    private static int NEXT_UID = 1;

    public Card(Suit s, Rank r) { suit = s; rank = r; uid = NEXT_UID++; }

    public Card copy() {
        Card c = new Card(suit, rank);
        c.enh = enh; c.ed = ed; c.seal = seal;
        return c;
    }

    public boolean isStone() { return enh == Enhance.STONE; }

    public int chipValue() { return isStone() ? 60 : rank.chips; }

    public boolean suitIs(Suit s) {
        if (isStone()) return false;
        if (enh == Enhance.WILD) return true;
        return suit == s;
    }

    public String name() {
        if (isStone()) return "Stone Card";
        return rank.label + " of " + suit.label;
    }

    public Color faceColor() {
        if (isStone()) return Pal.STONE;
        if (enh == Enhance.NONE) return Pal.CARD_FACE;
        return Pal.mix(Pal.CARD_FACE, enh.color, 0.55f);
    }
}
