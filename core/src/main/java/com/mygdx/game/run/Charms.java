package com.mygdx.game.run;

import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandType;
import com.mygdx.game.gfx.Icon;
import com.mygdx.game.gfx.Pal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The charm catalogue. Same factory pattern as {@link Relics} so each one is a fresh instance. */
public final class Charms {

    public interface Factory { Charm make(); }

    private static final List<Factory> FACTORIES = new ArrayList<>();
    private static final List<Charm> PROTOS = new ArrayList<>();

    private Charms() {}

    private static void def(Factory f) {
        FACTORIES.add(f);
        PROTOS.add(f.make());
    }

    public static Charm make(int i) { return FACTORIES.get(i).make(); }

    /** Rarity-weighted draw. Duplicates are fine - charms are consumed. */
    public static Charm random(Random rnd, int maxRarity) {
        List<Integer> bag = new ArrayList<>();
        for (int i = 0; i < PROTOS.size(); i++) {
            Charm p = PROTOS.get(i);
            if (p.rarity > maxRarity) continue;
            int w = p.rarity == 0 ? 10 : p.rarity == 1 ? 5 : 2;
            for (int k = 0; k < w; k++) bag.add(i);
        }
        if (bag.isEmpty()) return make(rnd.nextInt(PROTOS.size()));
        return make(bag.get(rnd.nextInt(bag.size())));
    }

    private static Card.Rank up(Card.Rank r) {
        Card.Rank[] all = Card.Rank.values();
        return all[Math.min(all.length - 1, r.ordinal() + 1)];
    }

    private static Card.Rank down(Card.Rank r) {
        Card.Rank[] all = Card.Rank.values();
        return all[Math.max(0, r.ordinal() - 1)];
    }

    // ------------------------------------------------------------------ table

    static {
        // ---- card marks -----------------------------------------------------
        def(() -> new Charm("ashen", "Ashen Mark", "Stamps up to 2 selected cards as Bonus: +40 Chips each.", 4)
            .look(Icon.GEM, Pal.CHIPS).cards(1, 2)
            .act((b, run, sel) -> Charm.forEach(sel, 2, c -> c.enh = Card.Enhance.BONUS)));

        def(() -> new Charm("blood", "Bloodmark", "Stamps up to 2 selected cards as Mult: +5 Mult each.", 4)
            .look(Icon.FIRE, Pal.MULT).cards(1, 2)
            .act((b, run, sel) -> Charm.forEach(sel, 2, c -> c.enh = Card.Enhance.MULT)));

        def(() -> new Charm("steel", "Steelmark", "Stamps up to 2 selected cards as Steel: X1.5 Mult while held.", 5)
            .look(Icon.SEAL, Pal.STEEL).cards(1, 2)
            .act((b, run, sel) -> Charm.forEach(sel, 2, c -> c.enh = Card.Enhance.STEEL)));

        def(() -> new Charm("gold", "Goldmark", "Stamps up to 2 selected cards as Gold: $3 each if held at the end.", 5)
            .look(Icon.COINS, Pal.GOLD).cards(1, 2)
            .act((b, run, sel) -> Charm.forEach(sel, 2, c -> c.enh = Card.Enhance.GOLD)));

        def(() -> new Charm("glass", "Glassmark", "Turns 1 selected card to Glass: X2 Mult, but it can shatter.", 5)
            .look(Icon.DIAMOND, Pal.TEAL).cards(1, 1).rare(1)
            .act((b, run, sel) -> Charm.forEach(sel, 1, c -> c.enh = Card.Enhance.GLASS)));

        def(() -> new Charm("wild", "Wildmark", "Turns 1 selected card Wild: it counts as every suit.", 6)
            .look(Icon.SHAPES, Pal.PINK).cards(1, 1).rare(1)
            .act((b, run, sel) -> Charm.forEach(sel, 1, c -> c.enh = Card.Enhance.WILD)));

        // ---- editions and seals ---------------------------------------------
        def(() -> new Charm("foil", "Foilwright", "Makes 1 selected card Foil: +60 Chips.", 5)
            .look(Icon.STAR, Pal.CHIPS).cards(1, 1)
            .act((b, run, sel) -> Charm.forEach(sel, 1, c -> c.ed = Card.Edition.FOIL)));

        def(() -> new Charm("holo", "Holowright", "Makes 1 selected card Holographic: +12 Mult.", 6)
            .look(Icon.STAR, Pal.MULT).cards(1, 1).rare(1)
            .act((b, run, sel) -> Charm.forEach(sel, 1, c -> c.ed = Card.Edition.HOLO)));

        def(() -> new Charm("prismdust", "Prism Dust", "Makes 1 selected card Polychrome: X1.5 Mult.", 8)
            .look(Icon.PRISM, Pal.PINK).cards(1, 1).rare(2)
            .act((b, run, sel) -> Charm.forEach(sel, 1, c -> c.ed = Card.Edition.POLY)));

        def(() -> new Charm("redwax", "Red Wax", "Presses a Red Seal onto 1 selected card so it triggers twice.", 7)
            .look(Icon.SEALWAX, Pal.RED).cards(1, 1).rare(1)
            .act((b, run, sel) -> Charm.forEach(sel, 1, c -> c.seal = Card.Seal.RED)));

        def(() -> new Charm("goldwax", "Gold Wax", "Presses a Gold Seal onto 1 selected card: $4 when it scores.", 6)
            .look(Icon.SEALWAX, Pal.GOLD).cards(1, 1)
            .act((b, run, sel) -> Charm.forEach(sel, 1, c -> c.seal = Card.Seal.GOLD)));

        // ---- reshaping --------------------------------------------------------
        def(() -> new Charm("suitshift", "Suit Shift",
            "Turns up to 3 selected cards into the suit of the first one. Flushes made to order.", 6)
            .look(Icon.COMPASS, Pal.TEAL).cards(2, 3).rare(1)
            .act((b, run, sel) -> {
                if (sel.isEmpty()) return;
                Card.Suit target = sel.get(0).suit;
                Charm.forEach(sel, 3, c -> c.suit = target);
            }));

        def(() -> new Charm("rankup", "Rising Mark", "Raises up to 3 selected cards by one rank.", 6)
            .look(Icon.UP, Pal.GREEN).cards(1, 3)
            .act((b, run, sel) -> Charm.forEach(sel, 3, c -> c.rank = up(c.rank))));

        def(() -> new Charm("rankdown", "Falling Mark", "Lowers up to 3 selected cards by one rank.", 4)
            .look(Icon.MINUS, Pal.STEEL).cards(1, 3)
            .act((b, run, sel) -> Charm.forEach(sel, 3, c -> c.rank = down(c.rank))));

        def(() -> new Charm("twin", "Twinned", "Copies 1 selected card, marks and all, into your deck.", 7)
            .look(Icon.PLUS, Pal.PURPLE).cards(1, 1).rare(1)
            .act((b, run, sel) -> {
                if (sel.isEmpty()) return;
                run.deck.add(sel.get(0).copy());
            }));

        def(() -> new Charm("ash", "Ash", "Burns up to 2 selected cards out of your deck for good.", 4)
            .look(Icon.TRASH, Pal.STONE).cards(1, 2)
            .act((b, run, sel) -> {
                int n = Math.min(2, sel.size());
                for (int i = 0; i < n; i++) {
                    Card c = sel.get(i);
                    run.deck.remove(c);
                    if (b != null) { b.hand.remove(c); b.drawPile.remove(c); b.discardPile.remove(c); }
                }
                if (b != null) { b.selected.clear(); b.drawUp(); }
            }));

        // ---- no selection needed ------------------------------------------------
        def(() -> new Charm("codex", "Codex Fragment",
            "Levels up the hand you currently have selected. Select the cards first.", 6)
            .look(Icon.BOOK, Pal.CHIPS).cards(1, 5).rare(1)
            .act((b, run, sel) -> {
                if (b == null) return;
                HandType h = b.previewHand();
                if (h != null) run.levelUp(h, 1);
            }));

        def(() -> new Charm("wind", "Second Wind", "Grants one extra hand for this seal.", 7)
            .look(Icon.HOURGLASS, Pal.GOLD).rare(1)
            .act((b, run, sel) -> { if (b != null) b.handsLeft++; }));

        def(() -> new Charm("breath", "Deep Breath", "Grants two extra discards for this seal.", 5)
            .look(Icon.ROTATE, Pal.MULT)
            .act((b, run, sel) -> { if (b != null) b.discardsLeft += 2; }));

        def(() -> new Charm("fortune", "Fortune", "Pays out $9 on the spot.", 4)
            .look(Icon.MONEY, Pal.GOLD)
            .act((b, run, sel) -> run.gold += 9));

        def(() -> new Charm("shuffle", "Reshuffle", "Throws your hand back and draws a completely new one, free.", 5)
            .look(Icon.DICE, Pal.PURPLE)
            .act((b, run, sel) -> {
                if (b == null) return;
                b.discardPile.addAll(b.hand);
                b.hand.clear();
                b.selected.clear();
                b.drawUp();
            }));
    }
}
