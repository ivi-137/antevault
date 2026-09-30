package com.mygdx.game.battle;

import com.badlogic.gdx.graphics.Color;
import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandEval;
import com.mygdx.game.cards.HandType;
import com.mygdx.game.run.Relic;
import com.mygdx.game.run.Run;
import com.mygdx.game.run.Sigil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * One seal. Owns the deck piles, the hand/discard budget and the running score, and steps the
 * scoring animation on a timer so the screen only has to draw state.
 */
public class Battle {

    public enum Phase { IDLE, SCORING, TALLY, WON, LOST }

    /** Hooks the screen implements so the rules never need to know about pixels. */
    public interface Fx {
        void onEvent(ScoreCtx.Ev e);
        void onTally(long amount, long newScore, boolean cleared);
        void onToast(String text, Color c);
        void onWin();
        void onLose();
    }

    public static final float EV_STEP = 0.14f;

    public final Run run;
    public final Blind blind;
    public Fx fx = new Fx() {
        public void onEvent(ScoreCtx.Ev e) {}
        public void onTally(long a, long s, boolean c) {}
        public void onToast(String t, Color c) {}
        public void onWin() {}
        public void onLose() {}
    };

    public final List<Card> drawPile = new ArrayList<>();
    public final List<Card> hand = new ArrayList<>();
    public final List<Card> discardPile = new ArrayList<>();
    public final List<Card> selected = new ArrayList<>();
    public final List<Card> playing = new ArrayList<>();

    public int handsLeft, discardsLeft;
    public long score;
    public int goldEarned;
    public int lastDiscardCount;
    public int handsThisBlind;
    public boolean sortBySuit;

    public final int[] charges = new int[Sigil.values().length];
    /** Extra retriggers armed by the Echo sigil, spent on the next hand played. */
    public int echoArmed;
    /** Mult armed by the Prism sigil, spent on the next hand played. */
    public double prismArmed;
    /** Set by the Mirror boss so the same hand type cannot repeat. */
    public HandType lastHandType;

    public Phase phase = Phase.IDLE;
    public ScoreCtx ctx;
    public int evIdx;
    private float evT;
    private float pauseT;
    public long pendingScore;
    public float tallyT;

    public Battle(Run run, Blind blind) {
        this.run = run;
        this.blind = blind;
        handsLeft = run.hands(blind);
        discardsLeft = run.discards(blind);

        for (Card c : run.deck) { c.debuffed = false; drawPile.add(c); }
        Collections.shuffle(drawPile, run.rnd);

        for (Sigil s : Sigil.values()) {
            int n = run.has(s) ? s.charges(run.rank(s)) : 0;
            if (blind.mod == BossMod.JAMMER) n = Math.max(0, n - 1);
            charges[s.ordinal()] = n;
        }

        for (Relic r : run.relics) {
            if (r.resets) r.counter = 0;
            if (r.onBlindStart != null) r.onBlindStart.run(this);
        }
        drawUp();
    }

    // ---------------------------------------------------------------- helpers

    public int handSize() {
        int n = run.handSize();
        if (blind.mod == BossMod.SUMP) n -= 2;
        return Math.max(3, n);
    }

    public boolean idle() { return phase == Phase.IDLE && pauseT <= 0; }

    public float progress() { return blind.target <= 0 ? 1f : Math.min(1f, (float) ((double) score / blind.target)); }

    public long remaining() { return Math.max(0, blind.target - score); }

    public void sortHand() {
        Collections.sort(hand, new Comparator<Card>() {
            @Override public int compare(Card a, Card b) {
                if (sortBySuit) {
                    int s = a.suit.ordinal() - b.suit.ordinal();
                    if (s != 0) return s;
                    return b.rank.order - a.rank.order;
                }
                int r = b.rank.order - a.rank.order;
                if (r != 0) return r;
                return a.suit.ordinal() - b.suit.ordinal();
            }
        });
    }

    public void drawUp() { drawTo(handSize()); }

    private void drawTo(int want) {
        while (hand.size() < want) {
            if (drawPile.isEmpty()) {
                if (discardPile.isEmpty()) break;
                drawPile.addAll(discardPile);
                discardPile.clear();
                Collections.shuffle(drawPile, run.rnd);
            }
            hand.add(drawPile.remove(drawPile.size() - 1));
        }
        sortHand();
    }

    public HandType previewHand() {
        if (selected.isEmpty()) return null;
        return HandEval.eval(selected, run.flushSize(), run.straightSize(), run.gaps()).type;
    }

    public List<Card> previewScoring() {
        if (selected.isEmpty()) return new ArrayList<>();
        return HandEval.eval(selected, run.flushSize(), run.straightSize(), run.gaps()).scoring;
    }

    public void toggle(Card c) {
        if (!idle()) return;
        if (selected.contains(c)) selected.remove(c);
        else if (selected.size() < 5) selected.add(c);
    }

    /** Why the current selection cannot be played, or null if it can. */
    public String playBlocker() {
        if (selected.isEmpty()) return "Select up to five cards.";
        if (handsLeft <= 0) return "No hands left.";
        if (blind.mod == BossMod.SHACKLE && selected.size() < 3) return "Shackle: play at least three cards.";
        if (blind.mod == BossMod.MIRROR && lastHandType != null && previewHand() == lastHandType)
            return "Mirror: you just played " + lastHandType.label + ".";
        return null;
    }

    // ---------------------------------------------------------------- actions

    public void playHand() {
        if (!idle() || playBlocker() != null) return;
        playing.clear();
        for (Card c : hand) if (selected.contains(c)) playing.add(c);
        hand.removeAll(playing);
        selected.clear();
        run.handsPlayed++;
        handsThisBlind++;

        if (blind.mod == BossMod.TITHE) run.gold = Math.max(0, run.gold - 2);

        ctx = Scorer.score(this, playing);
        lastHandType = ctx.hand;
        echoArmed = 0;
        prismArmed = 0;
        pendingScore = ctx.total();
        phase = Phase.SCORING;
        evIdx = -1;
        evT = 0;
    }

    /** Spends a charm on the current selection. Returns why it failed, or null on success. */
    public String useCharm(com.mygdx.game.run.Charm c) {
        if (c == null || !run.charms.contains(c)) return "Not carried.";
        String blocked = c.blocker(this, selected.size());
        if (blocked != null) return blocked;
        java.util.List<Card> sel = new ArrayList<>(selected);
        if (c.use != null) c.use.run(this, run, sel);
        run.charms.remove(c);
        // the selection may no longer be valid after cards were burned or redrawn
        selected.retainAll(hand);
        sortHand();
        fx.onToast(c.name, c.color);
        return null;
    }

    public void discardSelected() {
        if (!idle() || selected.isEmpty() || discardsLeft <= 0) return;
        discardsLeft--;
        lastDiscardCount = selected.size();
        List<Card> gone = new ArrayList<>(selected);
        hand.removeAll(gone);
        discardPile.addAll(gone);
        selected.clear();
        for (Relic r : run.relics) if (r.onDiscard != null) r.onDiscard.run(this);
        drawUp();
    }

    public boolean canUse(Sigil s) {
        return idle() && run.has(s) && charges[s.ordinal()] > 0;
    }

    public void useSigil(Sigil s) {
        if (!canUse(s)) return;
        int rank = run.rank(s);
        charges[s.ordinal()]--;
        switch (s) {
            case KEY:
                drawTo(hand.size() + s.keyDraw(rank));
                fx.onToast("KEY  +" + s.keyDraw(rank) + " cards", s.color);
                break;
            case TIDE:
                lastDiscardCount = hand.size();
                discardPile.addAll(hand);
                hand.clear();
                selected.clear();
                drawUp();
                fx.onToast("TIDE  fresh hand", s.color);
                break;
            case ECHO:
                echoArmed += rank;
                fx.onToast("ECHO ARMED  +" + echoArmed + " retrigger", s.color);
                break;
            default:
                prismArmed = s.prismMult(rank);
                fx.onToast("PRISM ARMED  X" + s.prismText(rank) + " Mult", s.color);
                break;
        }
    }

    // ---------------------------------------------------------------- update

    public void update(float dt) {
        if (pauseT > 0) { pauseT -= dt; return; }
        if (phase == Phase.SCORING) updateScoring(dt);
        else if (phase == Phase.TALLY) updateTally(dt);
    }

    private void updateScoring(float dt) {
        evT -= dt;
        while (evT <= 0 && evIdx < ctx.events.size() - 1) {
            evIdx++;
            evT += EV_STEP;
            fx.onEvent(ctx.events.get(evIdx));
        }
        if (evIdx >= ctx.events.size() - 1 && evT <= 0) {
            run.gold += ctx.money;
            goldEarned += ctx.money;
            for (Card c : ctx.destroyed) run.deck.remove(c);
            pendingScore = ctx.total();
            phase = Phase.TALLY;
            tallyT = 0;
        }
    }

    private void updateTally(float dt) {
        tallyT += dt;
        if (tallyT < 0.55f) return;
        score += pendingScore;
        if (score > run.bestScore) run.bestScore = score;
        boolean cleared = score >= blind.target;
        fx.onTally(pendingScore, score, cleared);
        discardPile.addAll(playing);
        playing.clear();
        handsLeft--;

        for (Relic r : run.relics) if (r.onHandEnd != null) r.onHandEnd.run(this);

        if (cleared) { win(); return; }
        if (handsLeft <= 0) { lose(); return; }
        drawUp();
        phase = Phase.IDLE;
        pauseT = 0.15f;
    }

    private void win() {
        int bonus = 0;
        for (Card c : hand) if (c.enh == Card.Enhance.GOLD) bonus += 3;
        int unused = Math.max(0, handsLeft);
        int interest = run.interest();
        int total = blind.reward + bonus + unused + interest;
        run.gold += total;
        goldEarned += total;
        run.blindsCleared++;
        phase = Phase.WON;
        fx.onWin();
    }

    private void lose() {
        phase = Phase.LOST;
        fx.onLose();
    }
}
