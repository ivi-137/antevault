package com.mygdx.game.battle;

import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandEval;
import com.mygdx.game.gfx.Pal;
import com.mygdx.game.i18n.I18n;
import com.mygdx.game.run.Relic;
import com.mygdx.game.run.Run;
import com.mygdx.game.run.Sigil;

import java.util.List;

/**
 * The scoring pipeline: base hand value, then every scoring card left to right, then cards held
 * in hand, then items. Each step appends an event so the screen can replay it beat by beat.
 */
public final class Scorer {

    private Scorer() {}

    public static ScoreCtx score(Battle b, List<Card> played) {
        Run run = b.run;
        ScoreCtx c = new ScoreCtx();
        c.b = b;
        c.run = run;
        c.played.addAll(played);

        HandEval.Result res = HandEval.eval(played, run.flushSize(), run.straightSize(), run.gaps());
        c.hand = res.type;
        c.scoring.addAll(res.scoring);
        c.held.addAll(b.hand);
        run.discovered.add(c.hand);

        for (Card card : played) {
            card.debuffed = b.blind.debuffSuit != null && card.suitIs(b.blind.debuffSuit);
        }

        c.handLevel = b.blind.mod == BossMod.INHIBITOR ? 1 : run.level(c.hand);

        c.chips = c.hand.chipsAt(c.handLevel);
        c.mult = c.hand.multAt(c.handLevel);
        ScoreCtx.Ev base = new ScoreCtx.Ev(ScoreCtx.EV_BASE,
            c.hand.label + "  Lv" + c.handLevel, c.hand.color);
        base.chips = c.chips;
        base.mult = c.mult;
        c.events.add(base);

        boolean bramble = b.blind.mod == BossMod.BRAMBLE;
        boolean shackled = b.blind.mod == BossMod.SHACKLE && played.size() < 3;

        int index = 0;
        for (Card card : c.scoring) {
            if (card.debuffed) {
                c.note("DEBUFFED", Pal.RED, card, ScoreCtx.EV_CARD);
                index++;
                continue;
            }
            int triggers = 1 + (card.seal == Card.Seal.RED ? 1 : 0) + b.echoArmed;
            for (int i = 0; i < run.relics.size(); i++) {
                Relic r = run.relics.get(i);
                if (r.onRetrigger != null) {
                    c.relicIdx = i;
                    c.retrig = 0;
                    r.onRetrigger.run(c, card);
                    triggers += c.retrig;
                }
            }
            c.relicIdx = -1;
            for (int t = 0; t < triggers; t++) scoreCard(c, card, index, bramble);
            index++;
        }

        for (Card card : c.held) {
            if (card.enh == Card.Enhance.STEEL) c.xMult(1.5, card, ScoreCtx.EV_HELD);
        }

        for (int i = 0; i < run.relics.size(); i++) {
            Relic r = run.relics.get(i);
            if (r.onIndependent != null) { c.relicIdx = i; r.onIndependent.run(c); }
        }
        c.relicIdx = -1;

        if (b.prismArmed > 1) {
            c.note(I18n.sigil(Sigil.PRISM), Sigil.PRISM.color, null, ScoreCtx.EV_SIGIL);
            c.xMult(b.prismArmed, null, ScoreCtx.EV_SIGIL);
        }

        if (shackled) {
            c.chips = 0;
            c.note("SHACKLED - no score", Pal.RED, null, ScoreCtx.EV_BASE);
        }

        if (c.mult < 0) c.mult = 0;
        if (c.chips < 0) c.chips = 0;
        return c;
    }

    private static void scoreCard(ScoreCtx c, Card card, int index, boolean bramble) {
        c.cardsScored++;
        Run run = c.run;

        if (!(bramble && index == 0)) c.addChips(card.chipValue(), card, ScoreCtx.EV_CARD);
        else c.note("JAMMED", Pal.GREEN, card, ScoreCtx.EV_CARD);

        switch (card.enh) {
            case BONUS: c.addChips(40, card, ScoreCtx.EV_CARD); break;
            case MULT:  c.addMult(5, card, ScoreCtx.EV_CARD); break;
            case GLASS:
                c.xMult(2, card, ScoreCtx.EV_CARD);
                if (run.rnd.nextInt(4) == 0 && !c.destroyed.contains(card)) {
                    c.destroyed.add(card);
                    c.note("SHATTERED", Pal.TEAL, card, ScoreCtx.EV_CARD);
                }
                break;
            case LUCKY:
                if (run.rnd.nextInt(5) == 0) c.addMult(20, card, ScoreCtx.EV_CARD);
                if (run.rnd.nextInt(15) == 0) c.cash(20, card, ScoreCtx.EV_CARD);
                break;
            default: break;
        }

        switch (card.ed) {
            case FOIL: c.addChips(60, card, ScoreCtx.EV_CARD); break;
            case HOLO: c.addMult(12, card, ScoreCtx.EV_CARD); break;
            case POLY: c.xMult(1.5, card, ScoreCtx.EV_CARD); break;
            default: break;
        }

        if (card.seal == Card.Seal.GOLD) c.cash(4, card, ScoreCtx.EV_CARD);
        if (card.seal == Card.Seal.BLUE) {
            run.levelUp(c.hand, 1);
            c.note("LEVEL UP", Pal.CHIPS, card, ScoreCtx.EV_CARD);
        }

        for (int i = 0; i < run.relics.size(); i++) {
            Relic r = run.relics.get(i);
            if (r.onScored != null) { c.relicIdx = i; r.onScored.run(c, card); }
        }
        c.relicIdx = -1;
    }
}
