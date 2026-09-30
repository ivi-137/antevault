package com.mygdx.game.run;

import com.badlogic.gdx.graphics.Color;
import com.mygdx.game.battle.Battle;
import com.mygdx.game.battle.ScoreCtx;
import com.mygdx.game.cards.Card;
import com.mygdx.game.gfx.Pal;

/**
 * A held item - this game's joker slot. Behaviour is plugged in with small lambdas so the whole
 * catalogue reads as a table in {@link Relics}.
 */
public class Relic {

    public interface CardHook { void run(ScoreCtx c, Card card); }
    public interface CtxHook  { void run(ScoreCtx c); }
    public interface BattleHook { void run(Battle b); }

    public final String id, name;
    public String desc;
    public String glyph = "*";
    public Color color = Pal.PURPLE;
    public int cost = 5;
    public int rarity = 0;          // 0 common, 1 uncommon, 2 rare, 3 legendary

    /** Passive stat mods, folded into the run while equipped. */
    public int extraHands, extraDiscards, extraHandSize, extraSlots, extraCharmSlots;
    public int flushSize = 5, straightSize = 5;
    public boolean gappedStraights;

    /** Live state for stacking items; cleared at the start of each seal when {@code resets}. */
    public double counter;
    public boolean resets;

    public CardHook onScored, onRetrigger;
    public CtxHook onIndependent;
    public BattleHook onBlindStart, onHandEnd, onDiscard;

    public Relic(String id, String name, String desc, int cost) {
        this.id = id; this.name = name; this.desc = desc; this.cost = cost;
    }

    public Relic look(String glyph, Color c) { this.glyph = glyph; this.color = c; return this; }
    public Relic rare(int r) { this.rarity = r; return this; }
    public Relic scored(CardHook h) { onScored = h; return this; }
    public Relic retrigger(CardHook h) { onRetrigger = h; return this; }
    public Relic indep(CtxHook h) { onIndependent = h; return this; }
    public Relic mods(int hands, int discards, int handSize) {
        extraHands = hands; extraDiscards = discards; extraHandSize = handSize; return this;
    }

    public Relic gapped() { gappedStraights = true; return this; }
    public Relic fingers() { flushSize = 4; straightSize = 4; return this; }
    public Relic elder() { extraSlots = 1; extraHands = 1; extraHandSize = 1; return this; }
    public Relic pouch(int n) { extraCharmSlots = n; return this; }

    // ---- stateful shorthands -------------------------------------------------

    /** Gains flat Mult for every hand played this seal. */
    public Relic ledger(int per) {
        resets = true;
        onHandEnd = b -> counter += per;
        onIndependent = c -> { if (counter > 0) c.addMult(counter, null, ScoreCtx.EV_RELIC); };
        return this;
    }

    /** Ramps its multiplier with every hand played this seal. */
    public Relic march(double per) {
        resets = true;
        onBlindStart = b -> counter = 1;
        onHandEnd = b -> counter = Math.round((counter + per) * 100) / 100.0;
        onIndependent = c -> { if (counter > 1) c.xMult(counter, null, ScoreCtx.EV_RELIC); };
        return this;
    }

    /** Gains Mult for every card discarded this seal. */
    public Relic ashes(int per) {
        resets = true;
        onDiscard = b -> counter += per * b.lastDiscardCount;
        onIndependent = c -> { if (counter > 0) c.addMult(counter, null, ScoreCtx.EV_RELIC); };
        return this;
    }

    public int sellValue() { return Math.max(1, cost / 2); }

    /** Description with {@code #} replaced by the live counter. */
    public String liveDesc() {
        if (desc.indexOf('#') < 0) return desc;
        return desc.replace("#", ScoreCtx.fmt(counter));
    }

    public Color rarityColor() {
        switch (rarity) {
            case 1: return Pal.CHIPS;
            case 2: return Pal.MULT;
            case 3: return Pal.GOLD;
            default: return Pal.DIM;
        }
    }

    public String rarityName() {
        switch (rarity) {
            case 1: return "Uncommon";
            case 2: return "Rare";
            case 3: return "Legendary";
            default: return "Common";
        }
    }
}
