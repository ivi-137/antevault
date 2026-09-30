package com.mygdx.game.run;

import com.mygdx.game.battle.ScoreCtx;
import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandType;
import com.mygdx.game.gfx.Icon;
import com.mygdx.game.gfx.Pal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The item catalogue. Each entry is a factory, so stacking items get their own counter and every
 * lambda closes over the instance it belongs to.
 */
public final class Relics {

    public interface Factory { Relic make(); }

    private static final List<Factory> FACTORIES = new ArrayList<>();
    private static final List<Relic> PROTOS = new ArrayList<>();

    private Relics() {}

    private static void def(Factory f) {
        FACTORIES.add(f);
        PROTOS.add(f.make());
    }

    public static Relic make(int i) { return FACTORIES.get(i).make(); }

    /** Rarity-weighted draw that skips anything the run already carries. */
    public static Relic random(Run run, Random rnd, int maxRarity) {
        List<Integer> bag = new ArrayList<>();
        for (int i = 0; i < PROTOS.size(); i++) {
            Relic p = PROTOS.get(i);
            if (p.rarity > maxRarity) continue;
            if (run != null && run.hasRelic(p.id)) continue;
            int w = p.rarity == 0 ? 10 : p.rarity == 1 ? 5 : p.rarity == 2 ? 2 : 1;
            for (int k = 0; k < w; k++) bag.add(i);
        }
        if (bag.isEmpty()) return make(rnd.nextInt(PROTOS.size()));
        return make(bag.get(rnd.nextInt(bag.size())));
    }

    private static boolean pairish(HandType h) {
        return h == HandType.PAIR || h == HandType.TWO_PAIR || h == HandType.THREE_KIND
            || h == HandType.FULL_HOUSE || h == HandType.FOUR_KIND || h == HandType.FIVE_KIND
            || h == HandType.FLUSH_HOUSE || h == HandType.FLUSH_FIVE;
    }

    // ------------------------------------------------------------------ table

    static {
        // ---- common -------------------------------------------------------
        def(() -> new Relic("sliver", "Sliver of Ruin", "+4 Mult.", 4)
            .look(Icon.s(Icon.BOLT), Pal.MULT)
            .indep(c -> c.addMult(4, null, ScoreCtx.EV_RELIC)));

        def(() -> new Relic("idol", "Chipped Idol", "+60 Chips.", 4)
            .look(Icon.s(Icon.GEM), Pal.CHIPS)
            .indep(c -> c.addChips(60, null, ScoreCtx.EV_RELIC)));

        def(() -> new Relic("ruby", "Ruby Heart", "Each scored Heart gives +4 Mult.", 5)
            .look(Icon.s(Icon.HEART), Pal.SUIT_RED)
            .scored((c, card) -> { if (card.suitIs(Card.Suit.HEARTS)) c.addMult(4, card, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("kris", "Kris of Spades", "Each scored Spade gives +45 Chips.", 5)
            .look(Icon.s(Icon.SEAL), Pal.STEEL)
            .scored((c, card) -> { if (card.suitIs(Card.Suit.SPADES)) c.addChips(45, card, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("thorn", "Thorn Fetish", "Each scored Club gives +3 Mult.", 5)
            .look(Icon.s(Icon.FEATHER), Pal.GREEN)
            .scored((c, card) -> { if (card.suitIs(Card.Suit.CLUBS)) c.addMult(3, card, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("prism", "Prism Lens", "Each scored Diamond gives +40 Chips.", 5)
            .look(Icon.s(Icon.DIAMOND), Pal.PINK)
            .scored((c, card) -> { if (card.suitIs(Card.Suit.DIAMONDS)) c.addChips(40, card, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("thread", "Quick Thread", "+1 discard every seal.", 5)
            .look(Icon.s(Icon.HOURGLASS), Pal.TEAL).mods(0, 1, 0));

        def(() -> new Relic("satchel", "Deep Satchel", "+1 card in your hand.", 6)
            .look(Icon.s(Icon.SUITCASE), Pal.PURPLE).mods(0, 0, 1));

        def(() -> new Relic("greed", "Greedy Sigil", "Each scored Diamond pays $1.", 5)
            .look(Icon.s(Icon.COINS), Pal.GOLD)
            .scored((c, card) -> { if (card.suitIs(Card.Suit.DIAMONDS)) c.cash(1, card, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("gilded", "Gilded Frame", "Every scored card pays $1, up to $5 a hand.", 6)
            .look(Icon.s(Icon.MONEY), Pal.GOLD)
            .scored((c, card) -> { if (c.money < 5) c.cash(1, card, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("twinlock", "Twinlock", "Hands that contain a Pair get +9 Mult.", 6)
            .look(Icon.s(Icon.LOCK), Pal.MULT)
            .indep(c -> { if (pairish(c.hand)) c.addMult(9, null, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("flow", "Flowstone", "+70 Chips when you play exactly five cards.", 5)
            .look(Icon.s(Icon.SHAPES), Pal.CHIPS)
            .indep(c -> { if (c.played.size() == 5) c.addChips(70, null, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("lone", "Lone Wolf", "+22 Mult when you play exactly one card.", 6)
            .look(Icon.s(Icon.MOON), Pal.ORANGE)
            .indep(c -> { if (c.played.size() == 1) c.addMult(22, null, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("vessel", "Empty Vessel", "X2 Mult when you play exactly two cards.", 7)
            .look(Icon.s(Icon.RING), Pal.PURPLE)
            .indep(c -> { if (c.played.size() == 2) c.xMult(2, null, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("coat", "Hoarder's Coat", "+16 Chips for every card still in your hand.", 5)
            .look(Icon.s(Icon.BOOK), Pal.CHIPS)
            .indep(c -> c.addChips(16 * c.held.size(), null, ScoreCtx.EV_RELIC)));

        def(() -> new Relic("odd", "Odd Ledger", "Odd-ranked scored cards give +32 Chips.", 5)
            .look(Icon.s(Icon.DICE), Pal.CHIPS)
            .scored((c, card) -> { if (!card.isStone() && card.rank.order % 2 == 1) c.addChips(32, card, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("even", "Even Keel", "Even-ranked scored cards give +3 Mult.", 5)
            .look(Icon.s(Icon.CHESS), Pal.MULT)
            .scored((c, card) -> { if (!card.isStone() && card.rank.order % 2 == 0) c.addMult(3, card, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("mask", "Mask of Ash", "Gains +3 Mult per card discarded this seal. Now +# Mult.", 6)
            .look(Icon.s(Icon.EYE), Pal.STONE).ashes(3));

        // ---- uncommon ------------------------------------------------------
        def(() -> new Relic("regicide", "Regicide", "X1.5 Mult if the hand contains a face card.", 8)
            .look(Icon.s(Icon.WARDEN), Pal.MULT).rare(1)
            .indep(c -> {
                for (Card card : c.scoring)
                    if (!card.isStone() && card.rank.face()) { c.xMult(1.5, null, ScoreCtx.EV_RELIC); return; }
            }));

        def(() -> new Relic("cap", "Sorcerer's Cap", "X1.35 Mult.", 9)
            .look(Icon.s(Icon.PRISM), Pal.PURPLE).rare(1)
            .indep(c -> c.xMult(1.35, null, ScoreCtx.EV_RELIC)));

        def(() -> new Relic("compass", "Suit Compass", "+14 Mult if every scored card shares a suit.", 8)
            .look(Icon.s(Icon.COMPASS), Pal.TEAL).rare(1)
            .indep(c -> {
                if (c.scoring.size() < 2) return;
                for (Card.Suit s : Card.Suit.values()) {
                    boolean all = true;
                    for (Card card : c.scoring) if (!card.suitIs(s)) { all = false; break; }
                    if (all) { c.addMult(14, null, ScoreCtx.EV_RELIC); return; }
                }
            }));

        def(() -> new Relic("ballast", "Deck Ballast", "+1 Mult for every 4 cards left in the draw pile.", 7)
            .look(Icon.s(Icon.ANCHOR), Pal.STEEL).rare(1)
            .indep(c -> c.addMult(c.b.drawPile.size() / 4, null, ScoreCtx.EV_RELIC)));

        def(() -> new Relic("echoblade", "Echo Blade", "The first scored card triggers a second time.", 9)
            .look(Icon.s(Icon.ECHO), Pal.TEAL).rare(1)
            .retrigger((c, card) -> { if (!c.scoring.isEmpty() && c.scoring.get(0) == card) c.retrig += 1; }));

        def(() -> new Relic("shortcut", "Shortcut Rune", "Straights may be built with one-rank gaps.", 9)
            .look(Icon.s(Icon.UP), Pal.GREEN).rare(1).gapped());

        def(() -> new Relic("tome", "Tome of Order", "+1 hand every seal.", 10)
            .look(Icon.s(Icon.SCROLL), Pal.GOLD).rare(1).mods(1, 0, 0));

        def(() -> new Relic("ledger", "Ledger of Debt", "Gains +6 Mult per hand played this seal. Now +# Mult.", 7)
            .look(Icon.s(Icon.GAVEL), Pal.MULT).rare(1).ledger(6));

        def(() -> new Relic("march", "Marching Standard", "Gains X0.15 Mult per hand played this seal. Now X# Mult.", 9)
            .look(Icon.s(Icon.TOWER), Pal.GOLD).rare(1).march(0.15));

        def(() -> new Relic("ace", "Ace Protocol", "Each scored Ace gives X0.25 extra Mult.", 8)
            .look(Icon.s(Icon.STAR), Pal.PINK).rare(1)
            .scored((c, card) -> { if (!card.isStone() && card.rank == Card.Rank.ACE) c.xMult(1.25, card, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("longgame", "The Long Game", "X2 Mult on the final hand of a seal.", 9)
            .look(Icon.s(Icon.SUN), Pal.ORANGE).rare(1)
            .indep(c -> { if (c.b.handsLeft <= 1) c.xMult(2, null, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("cartomancer", "Cartomancer", "+2 cards in hand, but one fewer discard.", 8)
            .look(Icon.s(Icon.HAND), Pal.CHIPS).rare(1).mods(0, -1, 2));

        // ---- rare -----------------------------------------------------------
        def(() -> new Relic("fourfingers", "Four Fingers", "Flushes and Straights need only four cards.", 12)
            .look(Icon.s(Icon.BURST), Pal.PURPLE).rare(2).fingers());

        def(() -> new Relic("untouched", "Untouched", "X0.3 Mult for each discard you have not spent.", 11)
            .look(Icon.s(Icon.CHECK), Pal.TEAL).rare(2)
            .indep(c -> {
                double m = 1 + 0.3 * Math.max(0, c.b.discardsLeft);
                if (m > 1) c.xMult(m, null, ScoreCtx.EV_RELIC);
            }));

        def(() -> new Relic("brink", "On The Brink", "X2.5 Mult while you have exactly one hand left.", 11)
            .look(Icon.s(Icon.FIRE), Pal.RED).rare(2)
            .indep(c -> { if (c.b.handsLeft == 1) c.xMult(2.5, null, ScoreCtx.EV_RELIC); }));

        def(() -> new Relic("overload", "Overload Core", "X3 Mult, but you get one fewer hand.", 13)
            .look(Icon.s(Icon.DUNGEON), Pal.MULT).rare(2).mods(-1, 0, 0)
            .indep(c -> c.xMult(3, null, ScoreCtx.EV_RELIC)));

        def(() -> new Relic("hoard", "Miser's Hoard", "+1 Mult for every $4 you are carrying.", 12)
            .look(Icon.s(Icon.PURSE), Pal.GOLD).rare(2)
            .indep(c -> c.addMult(c.run.gold / 4, null, ScoreCtx.EV_RELIC)));

        // ---- legendary --------------------------------------------------------
        def(() -> new Relic("crown", "Ante Crown", "X5 Mult. Every hand you play costs $3.", 18)
            .look(Icon.s(Icon.TROPHY), Pal.GOLD).rare(3)
            .indep(c -> { c.xMult(5, null, ScoreCtx.EV_RELIC); c.run.gold = Math.max(0, c.run.gold - 3); }));

        def(() -> new Relic("pouch", "Deep Pouch", "+2 charm slots.", 7)
            .look(Icon.s(Icon.SUITCASE), Pal.TEAL).rare(1).pouch(2));

        def(() -> new Relic("elder", "Elder Soul", "+1 item slot, +1 hand, +1 card in hand.", 20)
            .look(Icon.s(Icon.SEALWAX), Pal.TEAL).rare(3).elder());
    }
}
