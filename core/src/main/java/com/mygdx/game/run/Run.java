package com.mygdx.game.run;

import com.mygdx.game.battle.Blind;
import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandType;

import java.util.*;

/** Everything that persists across a single run. */
public class Run {

    public final List<Card> deck = new ArrayList<>();
    public final List<Relic> relics = new ArrayList<>();
    public final List<Charm> charms = new ArrayList<>();
    public final EnumMap<HandType, Integer> levels = new EnumMap<>(HandType.class);
    public final EnumSet<Sigil> sigils = EnumSet.noneOf(Sigil.class);
    public final EnumSet<HandType> discovered = EnumSet.noneOf(HandType.class);
    public final int[] sigilRank = new int[Sigil.values().length];

    public final Set<String> cleared = new HashSet<>();
    /** Per-node persistent state; currently keeps bazaar stock between visits. */
    public final Map<String, Object> nodeState = new HashMap<>();

    public String node = "n0";

    public int gold = 14;
    public int baseHandSize = 8, baseHands = 4, baseDiscards = 3;
    public int baseSlots = 5;
    public int baseCharmSlots = 2;

    /** Set when the run was started from the tutorial entry. */
    public boolean tutorial;
    public com.mygdx.game.battle.Tutorial tut;

    public int ante = 1;
    public int blindsCleared, handsPlayed;
    public long bestScore;
    public boolean won;

    public final Random rnd = new Random();

    public Run() {
        for (Card.Suit s : Card.Suit.values())
            for (Card.Rank r : Card.Rank.values())
                deck.add(new Card(s, r));
        for (HandType t : HandType.values()) levels.put(t, 1);
        for (HandType t : HandType.values()) if (!t.secret()) discovered.add(t);
    }

    public int level(HandType t) { Integer v = levels.get(t); return v == null ? 1 : v; }

    public void levelUp(HandType t, int n) { levels.put(t, level(t) + n); discovered.add(t); }

    // ---- aggregate item modifiers --------------------------------------------

    public int handSize() {
        int v = baseHandSize;
        for (Relic r : relics) v += r.extraHandSize;
        return Math.max(3, v);
    }

    public int hands(Blind e) {
        int v = e.hands + (baseHands - 4);
        for (Relic r : relics) v += r.extraHands;
        return Math.max(1, v);
    }

    public int discards(Blind e) {
        int v = e.discards + (baseDiscards - 3);
        for (Relic r : relics) v += r.extraDiscards;
        return Math.max(0, v);
    }

    public int slots() {
        int v = baseSlots;
        for (Relic r : relics) v += r.extraSlots;
        return v;
    }

    public int flushSize() {
        int v = 5;
        for (Relic r : relics) v = Math.min(v, r.flushSize);
        return v;
    }

    public int straightSize() {
        int v = 5;
        for (Relic r : relics) v = Math.min(v, r.straightSize);
        return v;
    }

    public boolean gaps() {
        for (Relic r : relics) if (r.gappedStraights) return true;
        return false;
    }

    public boolean has(Sigil s) { return sigils.contains(s); }

    public int rank(Sigil s) { return Math.max(1, sigilRank[s.ordinal()]); }

    public void grant(Sigil s) {
        if (sigils.add(s)) sigilRank[s.ordinal()] = 1;
        else sigilRank[s.ordinal()]++;
    }

    public boolean hasRelic(String id) {
        for (Relic r : relics) if (r.id.equals(id)) return true;
        return false;
    }

    public void addRelic(Relic r) {
        if (relics.size() >= slots()) return;
        relics.add(r);
    }

    public boolean full() { return relics.size() >= slots(); }

    public int charmSlots() {
        int v = baseCharmSlots;
        for (Relic r : relics) v += r.extraCharmSlots;
        return v;
    }

    public boolean charmsFull() { return charms.size() >= charmSlots(); }

    /** Returns false when the pouch is full. */
    public boolean addCharm(Charm c) {
        if (c == null || charmsFull()) return false;
        charms.add(c);
        return true;
    }

    /** Balatro-style interest, paid out when a seal is banked. */
    public int interest() { return Math.min(5, gold / 5); }

    public int deckCountSuit(Card.Suit s) {
        int n = 0;
        for (Card c : deck) if (c.suitIs(s)) n++;
        return n;
    }
}
