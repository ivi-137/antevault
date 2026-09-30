package com.mygdx.game.battle;

import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandType;
import com.mygdx.game.i18n.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * A scripted first seal that teaches by playing rather than by reading.
 *
 * Steps never block input - each one watches the battle for the thing it asked for and advances
 * when it happens, so a player who already knows the game can simply play and the prompts keep
 * pace. Nothing here can soft-lock: {@link #update} only ever moves forward.
 */
public class Tutorial {

    /** Where the prompt should point, so the screen can draw an arrow at the right thing. */
    public enum Focus { NONE, TARGET, HAND, SCORE_PANEL, PLAY_BUTTON, DISCARD_BUTTON }

    public static class Step {
        public final String key;
        public final Focus focus;
        Step(String key, Focus focus) { this.key = key; this.focus = focus; }
    }

    private final List<Step> steps = new ArrayList<>();
    private int index;
    public boolean finished;
    /** Rises when a step changes so the screen can pop the banner. */
    public float pop;

    public Tutorial() {
        steps.add(new Step("tut.s1", Focus.TARGET));
        steps.add(new Step("tut.s2", Focus.HAND));
        steps.add(new Step("tut.s3", Focus.SCORE_PANEL));
        steps.add(new Step("tut.s4", Focus.PLAY_BUTTON));
        steps.add(new Step("tut.s5", Focus.SCORE_PANEL));
        steps.add(new Step("tut.s6", Focus.DISCARD_BUTTON));
        steps.add(new Step("tut.s7", Focus.TARGET));
        steps.add(new Step("tut.done", Focus.NONE));
    }

    public Step step() { return finished ? null : steps.get(Math.min(index, steps.size() - 1)); }

    public String text() {
        Step s = step();
        return s == null ? null : I18n.t(s.key);
    }

    public Focus focus() {
        Step s = step();
        return s == null ? Focus.NONE : s.focus;
    }

    public void skip() { finished = true; }

    private void advance() {
        index++;
        pop = 1f;
        if (index >= steps.size()) finished = true;
    }

    /** True when the player holds two cards of matching rank, the pair the tutorial asks for. */
    private static boolean hasPairInHand(Battle b) {
        int[] count = new int[15];
        for (Card c : b.hand) if (!c.isStone()) count[c.rank.order]++;
        for (int i = 2; i <= 14; i++) if (count[i] >= 2) return true;
        return false;
    }

    private static boolean selectionIsPair(Battle b) {
        if (b.selected.size() < 2) return false;
        HandType h = b.previewHand();
        return h != null && h != HandType.HIGH_CARD;
    }

    public void update(Battle b, float dt) {
        if (finished) return;
        pop = Math.max(0, pop - dt * 2f);
        Step s = step();
        if (s == null) return;

        switch (s.key) {
            case "tut.s1":
                // give the player a beat to read, then move on
                if (pop <= 0 && b.handsThisBlind == 0) { if (waited(dt, 2.6f)) advance(); }
                break;
            case "tut.s2":
                // if there is genuinely no pair to find, re-point at discarding instead
                if (!hasPairInHand(b) && b.discardsLeft > 0 && !nudged) {
                    steps.set(index, new Step("tut.s2b", Focus.DISCARD_BUTTON));
                    nudged = true;
                    pop = 1f;
                    break;
                }
                if (selectionIsPair(b) || b.handsThisBlind > 0) advance();
                break;
            case "tut.s2b":
                if (hasPairInHand(b) || selectionIsPair(b) || b.handsThisBlind > 0) advance();
                break;
            case "tut.s3":
                if (waited(dt, 3.2f) || b.handsThisBlind > 0) advance();
                break;
            case "tut.s4":
                if (b.handsThisBlind > 0) advance();
                break;
            case "tut.s5":
                if (waited(dt, 3.6f) || b.handsThisBlind > 1) advance();
                break;
            case "tut.s6":
                if (b.discardsLeft < b.run.discards(b.blind) || b.handsThisBlind > 1) advance();
                break;
            case "tut.s7":
                if (b.handsThisBlind > 2 || b.phase == Battle.Phase.WON) advance();
                break;
            default:
                if (waited(dt, 5f) || b.phase == Battle.Phase.WON) finished = true;
                break;
        }
    }

    private boolean nudged;
    private float timer;

    private boolean waited(float dt, float seconds) {
        timer += dt;
        if (timer >= seconds) { timer = 0; return true; }
        return false;
    }
}
