package com.mygdx.game.battle;

import com.badlogic.gdx.graphics.Color;
import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandType;
import com.mygdx.game.gfx.Pal;
import com.mygdx.game.run.Run;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable state of a single scoring pass, plus the timeline of events the battle screen
 * replays one beat at a time (this is what makes the chips/mult readout feel like a slot machine).
 */
public class ScoreCtx {

    public static final int EV_BASE = 0, EV_CARD = 1, EV_HELD = 2, EV_RELIC = 3, EV_SIGIL = 5;

    public static class Ev {
        public int kind;
        public Card card;
        public int relicIdx = -1;
        public String text;
        public Color color;
        public double chips, mult;
        public boolean punchy;   // triggers extra shake / freeze
        public Ev(int kind, String text, Color color) { this.kind = kind; this.text = text; this.color = color; }
    }

    public Battle b;
    public Run run;
    public HandType hand;
    public int handLevel = 1;

    public final List<Card> played = new ArrayList<>();
    public final List<Card> scoring = new ArrayList<>();
    public final List<Card> held = new ArrayList<>();
    public final List<Ev> events = new ArrayList<>();

    public double chips, mult;
    public int money;
    public int relicIdx = -1;
    public int cardsScored;
    /** Extra triggers a relic wants to add to the card currently being scored. */
    public int retrig;
    /** Set true by relics that destroy the card currently being scored. */
    public final List<Card> destroyed = new ArrayList<>();

    private Ev push(int kind, Card c, String text, Color col) {
        Ev e = new Ev(kind, text, col);
        e.card = c;
        e.relicIdx = relicIdx;
        e.chips = chips;
        e.mult = mult;
        events.add(e);
        return e;
    }

    public void addChips(double v, Card src, int kind) {
        if (v == 0) return;
        chips += v;
        push(kind, src, "+" + fmt(v), Pal.CHIPS);
    }

    public void addMult(double v, Card src, int kind) {
        if (v == 0) return;
        mult += v;
        push(kind, src, "+" + fmt(v), Pal.MULT);
    }

    public void xMult(double v, Card src, int kind) {
        if (v == 1) return;
        mult *= v;
        Ev e = push(kind, src, "X" + fmt(v), Pal.MULT);
        e.punchy = true;
    }

    public void cash(int v, Card src, int kind) {
        if (v == 0) return;
        money += v;
        push(kind, src, "$" + v, Pal.GOLD);
    }

    public void note(String text, Color c, Card src, int kind) {
        push(kind, src, text, c);
    }

    public long total() {
        double t = chips * mult;
        if (t > 9.0e15) t = 9.0e15;
        return (long) t;
    }

    public static String fmt(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e12) return String.valueOf((long) v);
        return String.valueOf(Math.round(v * 100) / 100.0);
    }

    /** {@code d} with exactly {@code places} (1 or 2) decimals. */
    private static String fixed(double d, int places) {
        long scale = places == 1 ? 10 : 100;
        long n = Math.round(d * scale);
        StringBuilder frac = new StringBuilder(String.valueOf(n % scale));
        while (frac.length() < places) frac.insert(0, '0');
        return (n / scale) + "." + frac;
    }

    /** Compact readout for numbers that get silly late in a run. */
    public static String big(long v) {
        if (v < 100000) {
            String s = String.valueOf(v);
            StringBuilder sb = new StringBuilder();
            int c = 0;
            for (int i = s.length() - 1; i >= 0; i--) {
                sb.append(s.charAt(i));
                if (++c % 3 == 0 && i > 0) sb.append(',');
            }
            return sb.reverse().toString();
        }
        String[] suf = { "", "K", "M", "B", "T", "Qa", "Qi" };
        double d = v;
        int i = 0;
        while (d >= 1000 && i < suf.length - 1) { d /= 1000; i++; }
        return (d < 10 ? fixed(d, 2) : d < 100 ? fixed(d, 1) : String.valueOf((long) d)) + suf[i];
    }
}
