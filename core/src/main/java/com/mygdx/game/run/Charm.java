package com.mygdx.game.run;

import com.badlogic.gdx.graphics.Color;
import com.mygdx.game.battle.Battle;
import com.mygdx.game.cards.Card;
import com.mygdx.game.gfx.Pal;

import java.util.List;

/**
 * A single-use consumable, held in a small pouch and spent during a seal.
 *
 * Most charms reshape cards you have selected, which makes them the tactical counterpart to
 * items: items change how scoring works for the whole run, charms change the cards in front of
 * you right now.
 */
public class Charm {

    public interface Use { void run(Battle b, Run run, List<Card> selected); }

    public final String id, name;
    public String desc;
    public char icon = com.mygdx.game.gfx.Icon.SCROLL;
    public Color color = Pal.PURPLE;
    public int cost = 4;
    public int rarity;

    /** How many selected cards this charm needs. Zero means it ignores the selection. */
    public int minCards, maxCards;
    public Use use;

    public Charm(String id, String name, String desc, int cost) {
        this.id = id; this.name = name; this.desc = desc; this.cost = cost;
    }

    public Charm look(char icon, Color c) { this.icon = icon; this.color = c; return this; }
    public Charm rare(int r) { rarity = r; return this; }
    public Charm cards(int min, int max) { minCards = min; maxCards = max; return this; }
    public Charm act(Use u) { use = u; return this; }

    /** Null when the charm can be used, otherwise why it cannot. */
    public String blocker(Battle b, int selected) {
        if (b == null) return "Only usable during a seal.";
        if (!b.idle()) return "Wait for the hand to finish scoring.";
        if (minCards > 0 && selected < minCards)
            return minCards == 1 ? "Select a card first." : "Select at least " + minCards + " cards.";
        return null;
    }

    public String requirement() {
        if (minCards <= 0) return "no selection needed";
        if (minCards == maxCards) return "select " + minCards;
        return "select " + minCards + " to " + maxCards;
    }

    public int sellValue() { return Math.max(1, cost / 2); }

    public Color rarityColor() {
        return rarity >= 2 ? Pal.MULT : rarity == 1 ? Pal.CHIPS : Pal.DIM;
    }

    public String rarityName() {
        return rarity >= 2 ? "Rare" : rarity == 1 ? "Uncommon" : "Common";
    }

    /** Applies the charm to at most {@link #maxCards} of the selection. */
    public static void forEach(List<Card> sel, int max, CardOp op) {
        int n = Math.min(max, sel.size());
        for (int i = 0; i < n; i++) op.run(sel.get(i));
    }

    public interface CardOp { void run(Card c); }
}
