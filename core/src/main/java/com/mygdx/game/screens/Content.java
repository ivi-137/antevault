package com.mygdx.game.screens;

import com.mygdx.game.Main;
import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandType;
import com.mygdx.game.gfx.Pal;
import com.mygdx.game.i18n.I18n;
import com.mygdx.game.run.Charm;
import com.mygdx.game.run.Charms;
import com.mygdx.game.run.Relic;
import com.mygdx.game.run.Relics;
import com.mygdx.game.run.Run;
import com.mygdx.game.run.Sigil;
import com.mygdx.game.world.WorldMap;

import java.util.Random;

/** Builders for every non-combat node: spoils, shrines, caches and sanctums. */
public final class Content {

    private Content() {}

    private static Run run() { return Main.I.run; }
    private static Random rnd() { return Main.I.run.rnd; }

    // ---------------------------------------------------------------- helpers

    public static Card randomDeckCard() {
        Run r = run();
        if (r.deck.isEmpty()) return null;
        return r.deck.get(rnd().nextInt(r.deck.size()));
    }

    public static Card enhanceRandom(Card.Enhance e, int count) {
        Card last = null;
        for (int i = 0; i < count; i++) {
            Card c = randomDeckCard();
            if (c == null) break;
            c.enh = e;
            last = c;
        }
        return last;
    }

    public static Card sealRandom(Card.Seal s, int count) {
        Card last = null;
        for (int i = 0; i < count; i++) {
            Card c = randomDeckCard();
            if (c == null) break;
            c.seal = s;
            last = c;
        }
        return last;
    }

    public static Card rollCard(boolean fancy) {
        Card c = new Card(Card.Suit.values()[rnd().nextInt(4)], Card.Rank.values()[rnd().nextInt(13)]);
        if (fancy) {
            int roll = rnd().nextInt(100);
            if (roll < 34) c.enh = Card.Enhance.values()[1 + rnd().nextInt(Card.Enhance.values().length - 1)];
            else if (roll < 60) c.ed = Card.Edition.values()[1 + rnd().nextInt(3)];
            else if (roll < 74) c.seal = Card.Seal.values()[1 + rnd().nextInt(3)];
        }
        return c;
    }

    public static HandType rollHand() {
        HandType[] pool = {
            HandType.HIGH_CARD, HandType.PAIR, HandType.TWO_PAIR, HandType.THREE_KIND,
            HandType.STRAIGHT, HandType.FLUSH, HandType.FULL_HOUSE, HandType.FOUR_KIND,
            HandType.STRAIGHT_FLUSH
        };
        return pool[rnd().nextInt(pool.length)];
    }

    private static void removeRandom(int n) {
        Run r = run();
        for (int i = 0; i < n && r.deck.size() > 20; i++) r.deck.remove(rnd().nextInt(r.deck.size()));
    }

    private static String cardBlurb(Card c) {
        StringBuilder sb = new StringBuilder(c.name());
        if (c.enh != Card.Enhance.NONE) sb.append("\n").append(c.enh.desc);
        if (c.ed != Card.Edition.NONE) sb.append("\n").append(c.ed.desc);
        if (c.seal != Card.Seal.NONE) sb.append("\n").append(c.seal.desc);
        return sb.toString();
    }

    private static ChoiceScreen.Option itemOption(Relic r, boolean full) {
        ChoiceScreen.Option o = new ChoiceScreen.Option(r.name,
            full ? "ITEM SLOTS FULL" : r.rarityName() + " item", r.desc, r.color, r.glyph);
        o.showRelic(r);
        o.lock(!full);
        o.act(() -> run().addRelic(r));
        return o;
    }

    // ---------------------------------------------------------------- spoils

    public static ChoiceScreen reward(WorldMap.Node node) {
        Run run = run();
        int maxRarity = node.tier >= 3 ? 3 : node.tier >= 2 ? 2 : (node.ante >= 2 ? 2 : 1);
        Relic a = Relics.random(run, rnd(), maxRarity);
        Relic bItem = Relics.random(run, rnd(), maxRarity);
        int guard = 0;
        while (bItem.id.equals(a.id) && guard++ < 20) bItem = Relics.random(run, rnd(), maxRarity);

        ChoiceScreen cs = new ChoiceScreen(node, "SPOILS",
            "The seal is broken. Take one thing from what is left.", Pal.GOLD);
        boolean full = run.full();
        cs.add(itemOption(a, full));
        cs.add(itemOption(bItem, full));

        final int gold = 9 + node.tier * 5 + node.ante * 3;
        cs.add(new ChoiceScreen.Option("War Chest", "$" + gold,
            "Bank the coin instead. Bazaars are where builds are really made.", Pal.GOLD, "$")
            .act(() -> run().gold += gold));

        final Charm charm = Charms.random(rnd(), node.tier >= 2 ? 2 : 1);
        cs.add(new ChoiceScreen.Option(charm.name,
            run.charmsFull() ? "POUCH FULL" : charm.rarityName() + " charm",
            charm.desc, charm.color, String.valueOf(charm.icon))
            .lock(!run.charmsFull())
            .act(() -> run().addCharm(charm)));

        final HandType ht = rollHand();
        cs.add(new ChoiceScreen.Option("Codex: " + I18n.hand(ht), "+1 hand level",
            "Raises " + ht.label + " to level " + (run.level(ht) + 1) + ": "
                + ht.chipsAt(run.level(ht) + 1) + " Chips X " + ht.multAt(run.level(ht) + 1) + " Mult.",
            ht.color, "^")
            .act(() -> run().levelUp(ht, 1)));

        cs.allowSkip = false;
        return cs;
    }

    // ---------------------------------------------------------------- caches

    public static ChoiceScreen cache(WorldMap.Node node) {
        Run run = run();
        Relic r = Relics.random(run, rnd(), node.ante >= 3 ? 3 : 2);
        final Card c1 = rollCard(true);
        final Card c2 = rollCard(true);
        ChoiceScreen cs = new ChoiceScreen(node, "SEALED CACHE",
            "Somebody hid this a long time ago and never came back for it.", Pal.ORANGE);
        cs.add(itemOption(r, run.full()));
        cs.add(new ChoiceScreen.Option("Marked Card", "add to deck", cardBlurb(c1), Pal.CHIPS, "+")
            .showCard(c1).act(() -> run().deck.add(c1)));
        cs.add(new ChoiceScreen.Option("Marked Card", "add to deck", cardBlurb(c2), Pal.CHIPS, "+")
            .showCard(c2).act(() -> run().deck.add(c2)));
        final Charm charm = Charms.random(rnd(), node.ante >= 3 ? 2 : 1);
        cs.add(new ChoiceScreen.Option(charm.name,
            run.charmsFull() ? "POUCH FULL" : charm.rarityName() + " charm",
            charm.desc, charm.color, String.valueOf(charm.icon))
            .lock(!run.charmsFull())
            .act(() -> run().addCharm(charm)));

        final int gold = 16 + node.ante * 4;
        cs.add(new ChoiceScreen.Option("Coin Pile", "$" + gold,
            "Dusty, heavy, and immediately useful.", Pal.GOLD, "$")
            .act(() -> run().gold += gold));
        cs.allowSkip = false;
        return cs;
    }

    // ---------------------------------------------------------------- sanctum

    public static ChoiceScreen rest(WorldMap.Node node) {
        Run run = run();
        ChoiceScreen cs = new ChoiceScreen(node, "SANCTUM",
            "Nothing here wants anything from you. Take one thing and go.", Pal.GREEN);

        cs.add(new ChoiceScreen.Option("Attune", "+1 card in hand",
            "Permanently draw one more card. Currently " + run.handSize() + ".", Pal.CHIPS, "A")
            .act(() -> run().baseHandSize++));

        cs.add(new ChoiceScreen.Option("Steady", "+1 discard",
            "Permanently gain one extra discard in every seal.", Pal.MULT, "D")
            .act(() -> run().baseDiscards++));

        Sigil weakest = null;
        for (Sigil s : Sigil.values())
            if (run.has(s) && (weakest == null || run.sigilRank[s.ordinal()] < run.sigilRank[weakest.ordinal()]))
                weakest = s;
        final Sigil target = weakest;
        if (target != null) {
            cs.add(new ChoiceScreen.Option("Sharpen " + I18n.sigilShort(target),
                "rank " + run.rank(target) + " -> " + (run.rank(target) + 1),
                I18n.sigilDesc(target, run.rank(target) + 1) + "\nAnd one more charge per seal.",
                target.color, String.valueOf(target.icon))
                .act(() -> run().grant(target)));
        } else {
            cs.add(new ChoiceScreen.Option("Fortify", "+1 hand",
                "No Sigils to sharpen yet. Take an extra hand in every seal instead.", Pal.GOLD, "H")
                .act(() -> run().baseHands++));
        }

        cs.add(new ChoiceScreen.Option("Refine", "2 cards enhanced",
            "Stamps two random cards in your deck with a Bonus mark: +40 Chips each when scored.",
            Pal.CHIPS, "R")
            .act(() -> enhanceRandom(Card.Enhance.BONUS, 2)));

        cs.allowSkip = false;
        return cs;
    }

    // ---------------------------------------------------------------- shrines

    public static ChoiceScreen shrine(WorldMap.Node node) {
        Run run = run();
        int pick = rnd().nextInt(8);
        ChoiceScreen cs;
        switch (pick) {
            case 0: {
                cs = new ChoiceScreen(node, "THE GAMBLER'S CAIRN",
                    "Chips left by someone who meant to stop while ahead.", Pal.PURPLE);
                Relic r = Relics.random(run, rnd(), 3);
                cs.add(new ChoiceScreen.Option("Ante Up", "pay $8",
                    "Take a random item, sight unseen. It could be anything.", Pal.PURPLE, "?")
                    .lock(run.gold >= 8 && !run.full())
                    .act(() -> { run().gold -= 8; run().addRelic(r); }));
                cs.add(new ChoiceScreen.Option("Cash Out", "$18, -1 card in hand",
                    "Sweep the cairn clean. You will hold one fewer card for the rest of the run.",
                    Pal.GOLD, "$")
                    .lock(run.handSize() > 5)
                    .act(() -> { run().gold += 18; run().baseHandSize--; }));
                break;
            }
            case 1: {
                cs = new ChoiceScreen(node, "THE LEDGER ALTAR",
                    "It only writes in one column.", Pal.RED);
                final HandType h1 = rollHand(), h2 = rollHand();
                cs.add(new ChoiceScreen.Option("Pay in Gold", "-$12",
                    "Level " + h1.label + " twice, to level " + (run.level(h1) + 2) + ".", Pal.GOLD, "$")
                    .lock(run.gold >= 12)
                    .act(() -> { run().gold -= 12; run().levelUp(h1, 2); }));
                cs.add(new ChoiceScreen.Option("Pay in Cards", "-3 deck cards",
                    "Level " + h2.label + " twice. A thinner deck is not always worse.", Pal.RED, "!")
                    .lock(run.deck.size() > 24)
                    .act(() -> { removeRandom(3); run().levelUp(h2, 2); }));
                break;
            }
            case 2: {
                cs = new ChoiceScreen(node, "THE FORGE",
                    "Still warm. Feed it three cards and it gives them back changed.", Pal.ORANGE);
                cs.add(new ChoiceScreen.Option("Glass", "3 cards",
                    "Three random deck cards become Glass: X2 Mult, but they can shatter.", Pal.TEAL, "G")
                    .act(() -> enhanceRandom(Card.Enhance.GLASS, 3)));
                cs.add(new ChoiceScreen.Option("Steel", "3 cards",
                    "Three random deck cards become Steel: X1.5 Mult while held in hand.", Pal.STEEL, "S")
                    .act(() -> enhanceRandom(Card.Enhance.STEEL, 3)));
                cs.add(new ChoiceScreen.Option("Gold", "3 cards",
                    "Three random deck cards become Gold: $3 each if held when a seal breaks.", Pal.GOLD, "$")
                    .act(() -> enhanceRandom(Card.Enhance.GOLD, 3)));
                break;
            }
            case 3: {
                cs = new ChoiceScreen(node, "THE CRACKED ORRERY",
                    "Half of it still turns. That half is willing to help.", Pal.TEAL);
                cs.add(new ChoiceScreen.Option("Widen", "+2 cards in hand",
                    "Permanently draw two more cards every seal.", Pal.CHIPS, "W")
                    .act(() -> run().baseHandSize += 2));
                cs.add(new ChoiceScreen.Option("Deepen", "+1 hand",
                    "Permanently play one more hand in every seal. Rarely the wrong pick.",
                    Pal.GOLD, "H")
                    .act(() -> run().baseHands++));
                break;
            }
            case 4: {
                cs = new ChoiceScreen(node, "THE ARCHIVE",
                    "Rows and rows of hands other people already played.", Pal.CHIPS);
                final HandType a = rollHand(), b2 = rollHand(), c = rollHand();
                cs.add(new ChoiceScreen.Option("Read Widely", "3 hands +1",
                    a.label + ", " + b2.label + " and " + c.label + " each gain a level.", Pal.CHIPS, "^")
                    .act(() -> { run().levelUp(a, 1); run().levelUp(b2, 1); run().levelUp(c, 1); }));
                cs.add(new ChoiceScreen.Option("Read Deeply", a.label + " +3",
                    "One hand, three levels. " + a.label + " reaches level " + (run.level(a) + 3) + ".",
                    a.color, "^")
                    .act(() -> run().levelUp(a, 3)));
                break;
            }
            case 5: {
                cs = new ChoiceScreen(node, "WANDERING MERCHANT",
                    "\"Everything here is honest. Most of it, anyway.\"", Pal.GOLD);
                Relic r = Relics.random(run, rnd(), 2);
                final Card fancy = rollCard(false);
                fancy.ed = Card.Edition.POLY;
                cs.add(new ChoiceScreen.Option(r.name, "$7", r.desc, r.color, r.glyph)
                    .showRelic(r).lock(run.gold >= 7 && !run.full())
                    .act(() -> { run().gold -= 7; run().addRelic(r); }));
                cs.add(new ChoiceScreen.Option("Polychrome Card", "$9",
                    "Adds a Polychrome " + fancy.name() + " to your deck. X1.5 Mult whenever it scores.",
                    Pal.PINK, "+")
                    .showCard(fancy).lock(run.gold >= 9)
                    .act(() -> { run().gold -= 9; run().deck.add(fancy); }));
                cs.add(new ChoiceScreen.Option("Two Red Seals", "$11",
                    "Stamps two random deck cards with a Red Seal so they trigger twice.", Pal.RED, "O")
                    .lock(run.gold >= 11)
                    .act(() -> { run().gold -= 11; sealRandom(Card.Seal.RED, 2); }));
                break;
            }
            case 6: {
                cs = new ChoiceScreen(node, "SCRAP HEAP",
                    "A deck is sharper for what you take out of it.", Pal.STONE);
                cs.add(new ChoiceScreen.Option("Thin the Deck", "-4 cards",
                    "Removes four random cards. A smaller deck draws its good cards more often.",
                    Pal.STEEL, "-")
                    .lock(run.deck.size() > 26)
                    .act(() -> removeRandom(4)));
                cs.add(new ChoiceScreen.Option("Pack it In", "+2 Stone, $8",
                    "Two random cards become Stone: +60 Chips, no rank, no suit. And you find some coin.",
                    Pal.STONE, "#")
                    .act(() -> { enhanceRandom(Card.Enhance.STONE, 2); run().gold += 8; }));
                break;
            }
            default: {
                cs = new ChoiceScreen(node, "THE FOUNT",
                    "It takes payment in whatever you have most of.", Pal.GREEN);
                cs.add(new ChoiceScreen.Option("Drink", "+1 hand, -$10",
                    "Permanently play one more hand in every seal.", Pal.GOLD, "H")
                    .lock(run.gold >= 10)
                    .act(() -> { run().gold -= 10; run().baseHands++; }));
                cs.add(new ChoiceScreen.Option("Bathe", "+3 cards in hand, -1 discard",
                    "A much wider hand, at the cost of one discard for the rest of the run.",
                    Pal.CHIPS, "B")
                    .lock(run.baseDiscards > 1)
                    .act(() -> { run().baseHandSize += 3; run().baseDiscards--; }));
                cs.add(new ChoiceScreen.Option("Bottle It", "+1 discard",
                    "Permanently gain one extra discard in every seal.", Pal.MULT, "D")
                    .act(() -> run().baseDiscards++));
                break;
            }
        }
        cs.skipLabel = I18n.t("choice.walkaway");
        return cs;
    }
}
