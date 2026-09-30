package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.game.GameScreen;
import com.mygdx.game.Main;
import com.mygdx.game.battle.*;
import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandType;
import com.mygdx.game.gfx.*;
import com.mygdx.game.i18n.I18n;
import com.mygdx.game.run.Relic;
import com.mygdx.game.run.Run;
import com.mygdx.game.run.Sigil;
import com.mygdx.game.world.WorldMap;

import java.util.HashMap;
import java.util.List;

/**
 * Playing a seal: select cards, play the hand, watch the counter climb.
 *
 * Layout rule for this screen: every readout is drawn with {@code textBoxC} into a rectangle
 * that the layout cursor handed out, so a value can never overflow its box or drift onto the
 * row below it, however long the number gets.

 */
public class SealScreen implements GameScreen, Battle.Fx {

    private final Main g = Main.I;
    private final WorldMap.Node node;
    private final Battle battle;
    private float t;

    /** Per-card animation state keyed by card uid: x, y, lift, flash, tilt. */
    private final HashMap<Integer, float[]> anim = new HashMap<>();
    private final float[] relicX = new float[12];
    private final float[] relicY = new float[12];
    private final float[] relicFlash = new float[12];

    private double showChips, showMult;
    private float chipPop, multPop, scorePop;
    private double shownScore;
    private String toast;
    private Color toastCol = Pal.INK;
    private float toastT, outcomeT;

    // panel geometry
    private static final float PX = 18, PW = 272, PY = 92, PH = 564;
    private static final float PAD = 12;
    private static final float HAND_CX = 700, HAND_Y = 40;
    private static final float PLAY_Y = 322;
    private static final float RAIL_W = 232;

    private float scoreBoxCx = PX + PW / 2f, scoreBoxCy = 470;

    public SealScreen(WorldMap.Node node) {
        this.node = node;
        this.battle = new Battle(Main.I.run, node.blind);
        this.battle.fx = this;
    }

    @Override
    public void enter() {
        g.juice.clear();
        // cinematic 4: announce the seal with a title card wipe
        g.cine.titleCard(battle.blind.name, battle.blind.tierLabel());
        g.cine.rack(0.7f);
    }

    private float[] st(Card c) {
        float[] s = anim.get(c.uid);
        if (s == null) { s = new float[]{ HAND_CX, -160, 0, 0, 0 }; anim.put(c.uid, s); }
        return s;
    }

    // ------------------------------------------------------------------ Fx

    @Override
    public void onEvent(ScoreCtx.Ev e) {
        showChips = e.chips;
        showMult = e.mult;
        float fx = PX + PW / 2f, fy = 440;
        if (e.card != null) {
            float[] s = st(e.card);
            s[3] = 1f;
            s[2] = Math.max(s[2], 26);
            fx = s[0] + CardArt.W / 2f;
            fy = s[1] + CardArt.H + 14;
        } else if (e.relicIdx >= 0 && e.relicIdx < relicX.length) {
            relicFlash[e.relicIdx] = 1f;
            fx = relicX[e.relicIdx];
            fy = relicY[e.relicIdx] - 18;
        }
        if (e.text != null) g.juice.floater(fx, fy, e.text, e.color, e.punchy);
        if (e.color == Pal.CHIPS) chipPop = 1f;
        else if (e.color == Pal.MULT) multPop = 1f;
        if (e.punchy) {
            g.juice.shake(7);
            g.juice.burst(fx, fy, 16, e.color, 210, 7);
            g.juice.shatter(fx, fy, 10, e.color, 200);
            g.juice.smoke(fx, fy, 5, Pal.mix(e.color, Pal.PANEL_LO, 0.4f), 60, 16);
            g.cine.impact(fx, fy, 0.55f, e.color);
        } else {
            g.juice.burst(fx, fy, 5, e.color, 110, 4);
        }
        // the chips visibly get drawn into the running total
        g.juice.implode(fx, fy, scoreBoxCx, scoreBoxCy, e.punchy ? 12 : 6, e.color, 5f);
        if (e.card != null) {
            g.juice.embers(fx, fy - 18, 3, Pal.mix(e.color, Pal.GOLD, 0.4f), 14);
            // cinematic 2: camera leans toward the card that is scoring
            g.cine.pushTo(fx, fy, 0.55f);
        }
        // cinematic 5: rack focus so the played hand pops off the backdrop
        g.cine.rack(0.45f);
    }

    @Override
    public void onTally(long amount, long newScore, boolean cleared) {
        scorePop = 1f;
        g.juice.shake(cleared ? 16 : 9);
        g.juice.floater(scoreBoxCx, scoreBoxCy + 40, "+" + ScoreCtx.big(amount), Pal.GOLD, true);
        g.juice.burst(scoreBoxCx, scoreBoxCy, 30, Pal.GOLD, 260, 8);
        g.juice.orbit(scoreBoxCx, scoreBoxCy, 14, Pal.GOLD, 70, 9);
        g.juice.embers(scoreBoxCx, scoreBoxCy - 20, 14, Pal.ORANGE, 44);
        for (int i = 0; i < 6; i++) {
            float a = MathUtils.random(MathUtils.PI2);
            g.juice.comet(scoreBoxCx, scoreBoxCy, MathUtils.cos(a) * 320, MathUtils.sin(a) * 320,
                Pal.GOLD, 7f, 0.5f);
        }
        if (cleared) {
            // the full cinematic treatment: slow motion, ray burst, speed lines, grade push
            g.cine.finale(scoreBoxCx, scoreBoxCy, Pal.GREEN);
            g.juice.confetti(scoreBoxCx, scoreBoxCy + 40, 90, Pal.GOLD, Pal.GREEN, 340);
        } else {
            g.cine.impact(scoreBoxCx, scoreBoxCy, 0.9f, Pal.GOLD);
        }
    }

    @Override
    public void onToast(String text, Color c) {
        toast = text; toastCol = c; toastT = 1.9f;
        g.cine.impact(Main.VW / 2f, 610, 0.6f, c);
    }

    @Override
    public void onWin() {
        outcomeT = 0;
        g.juice.shake(20);
        g.cine.letterbox(true);
        g.cine.releasePush();
        g.juice.confetti(Main.VW / 2f, Main.VH + 20, 140, Pal.GOLD, Pal.PINK, 220);
        g.juice.embers(Main.VW / 2f, 60, 40, Pal.ORANGE, 420);
    }
    @Override public void onLose() { outcomeT = 0; g.juice.shake(20); g.cine.letterbox(true); }

    // ------------------------------------------------------------------ frame

    @Override
    public void draw(SpriteBatch b, float dt) {
        t += dt;
        Art a = g.art;

        toastT = Math.max(0, toastT - dt);
        chipPop = Math.max(0, chipPop - dt * 4f);
        multPop = Math.max(0, multPop - dt * 4f);
        scorePop = Math.max(0, scorePop - dt * 1.6f);
        for (int i = 0; i < relicFlash.length; i++) relicFlash[i] = Math.max(0, relicFlash[i] - dt * 3f);

        if (battle.phase == Battle.Phase.IDLE) { showChips = 0; showMult = 0; }
        shownScore += (battle.score - shownScore) * Math.min(1, dt * 9f);
        if (Math.abs(battle.score - shownScore) < 1) shownScore = battle.score;

        battle.update(dt);
        if (battle.phase == Battle.Phase.WON || battle.phase == Battle.Phase.LOST) outcomeT += dt;

        input();

        drawSealPanel(b, a, dt);
        drawTopStrip(b, a, dt);
        drawSigilRail(b, a, dt);
        drawCharmRail(b, a, dt);
        drawPlayed(b, a, dt);
        drawHand(b, a, dt);
        drawControls(b, a, dt);

        if (toastT > 0) {
            float k = Math.min(1, toastT * 3f);
            float h = 48;
            float w = a.textW(a.big, toast) + 64;
            float x = Main.VW / 2 - w / 2;
            a.bloom(b, x, 596, w, h, toastCol, 0.5f * k, 18);
            a.panel(b, x, 596, w, h, Pal.PANEL_LO, 0.94f * k);
            a.outline(b, x, 596, w, h, toastCol, 0.9f * k);
            a.textBoxC(b, a.big, toast, x, 596, w, h, toastCol, k, 1f, 22);
        }

        if (g.run.tut != null && !g.run.tut.finished) {
            g.run.tut.update(battle, dt);
            drawTutorial(b, a, dt);
        }

        if (battle.phase == Battle.Phase.WON) drawOutcome(b, a, dt, true);
        else if (battle.phase == Battle.Phase.LOST) drawOutcome(b, a, dt, false);
    }

    private void input() {
        if (battle.phase == Battle.Phase.WON || battle.phase == Battle.Phase.LOST) return;
        if (!battle.idle()) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) battle.playHand();
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) || Gdx.input.isKeyJustPressed(Input.Keys.X))
            battle.discardSelected();
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) { battle.sortBySuit = !battle.sortBySuit; battle.sortHand(); }
        for (int i = 0; i < 9; i++)
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i) && i < battle.hand.size())
                battle.toggle(battle.hand.get(i));
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) battle.useSigil(Sigil.KEY);
        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) battle.useSigil(Sigil.TIDE);
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) battle.useSigil(Sigil.ECHO);
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) battle.useSigil(Sigil.PRISM);
        // Z and C spend the first and second charm without reaching for the mouse
        if (Gdx.input.isKeyJustPressed(Input.Keys.Z) && !g.run.charms.isEmpty())
            battle.useCharm(g.run.charms.get(0));
        if (Gdx.input.isKeyJustPressed(Input.Keys.C) && g.run.charms.size() > 1)
            battle.useCharm(g.run.charms.get(1));
    }

    // ------------------------------------------------------------------ left panel

    private void drawSealPanel(SpriteBatch b, Art a, float dt) {
        Blind bl = battle.blind;
        a.shadow(b, PX, PY - 6, PW, PH, 0.5f, 14);
        a.panel(b, PX, PY, PW, PH, Pal.PANEL_LO, 0.96f);
        a.outline(b, PX, PY, PW, PH, Pal.LINE, 0.5f);

        float iw = PW - PAD * 2;
        float ix = PX + PAD;
        Color tone = bl.tier >= 3 ? Pal.GOLD : bl.tier == 2 ? Pal.MULT : Pal.CHIPS;

        Layout L = new Layout(PY + PH - PAD, PY + PAD);

        // --- 1: which seal this is, and the number to beat
        float rTier = 18, rName = 26, rCap = 16, rTarget = 34;
        float h1 = 10 + rTier + rName + 4 + rCap + rTarget + 10;
        float y1 = L.block(h1, 9);
        a.panelSmall(b, ix, y1, iw, h1, Pal.PANEL, 0.9f);
        float top1 = y1 + h1 - 10;
        a.textBoxC(b, a.small, bl.tierLabel(), ix, top1 - rTier, iw, rTier, tone, 1f, 1f, 8);
        a.textBoxC(b, a.bodyB, bl.name, ix, top1 - rTier - rName, iw, rName, Pal.INK, 1f, 1f, 10);
        a.textBoxC(b, a.small, I18n.t("seal.scoreatleast"), ix, y1 + 10 + rTarget, iw, rCap, Pal.DIM, 1f, 1f, 8);
        a.textBoxC(b, a.num, ScoreCtx.big(bl.target), ix, y1 + 10, iw, rTarget, Pal.GOLD, 1f, 0.95f, 12);

        // --- 2: the running total
        float rLabel = 16, rScore = 54, rBar = 14, rLeft = 16;
        float h2 = 10 + rLabel + rScore + 6 + rBar + 6 + rLeft + 10;
        float y2 = L.block(h2, 9);
        a.panelSmall(b, ix, y2, iw, h2, Pal.PANEL_LO, 0.55f);
        float top2 = y2 + h2 - 10;
        a.textBoxC(b, a.small, I18n.t("seal.yourscore"), ix, top2 - rLabel, iw, rLabel, Pal.DIM, 1f, 1f, 8);

        float scoreY = top2 - rLabel - rScore;
        Color scoreCol = battle.score >= bl.target ? Pal.GREEN : Pal.INK;
        if (scorePop > 0.01f) a.bloom(b, ix + 6, scoreY, iw - 12, rScore, Pal.GOLD, scorePop * 0.35f, 12);
        a.textBoxC(b, a.numHuge, ScoreCtx.big((long) shownScore), ix, scoreY, iw, rScore,
            scoreCol, 1f, 1f + scorePop * 0.10f, 14);
        scoreBoxCx = ix + iw / 2f;
        scoreBoxCy = scoreY + rScore / 2f;

        float barY = y2 + 10 + rLeft + 6;
        a.bar(b, ix + 10, barY, iw - 20, rBar, battle.progress(), Pal.PANEL, tone);
        a.textBoxC(b, a.small,
            battle.remaining() > 0 ? I18n.t("seal.stillneeded", ScoreCtx.big(battle.remaining())) : I18n.t("seal.targetmet"),
            ix, y2 + 10, iw, rLeft, battle.remaining() > 0 ? Pal.DIM : Pal.GREEN, 1f, 1f, 8);

        // --- 3: what the current hand is worth
        HandType ht = battle.phase == Battle.Phase.IDLE ? battle.previewHand()
            : (battle.ctx != null ? battle.ctx.hand : null);
        int lvl = ht == null ? 1 : (bl.mod == BossMod.INHIBITOR ? 1 : g.run.level(ht));

        float rHand = 30, rBoxes = 48, rWorth = 16, rTotal = 32;
        float h3 = rHand + 8 + rBoxes + 8 + rWorth + rTotal;
        float y3 = L.block(h3, 9);

        float handY = y3 + h3 - rHand;
        a.panelSmall(b, ix, handY, iw, rHand, Pal.PANEL, 0.9f);
        if (ht == null) {
            a.textBoxC(b, a.small, I18n.t("seal.select"), ix, handY, iw, rHand, Pal.DIMMER, 1f, 1f, 10);
        } else {
            float lvW = 44;
            a.textBoxC(b, a.bodyB, I18n.hand(ht), ix, handY, iw - lvW - 8, rHand, ht.color, 1f, 1f, 10);
            a.panelSmall(b, ix + iw - lvW, handY + 4, lvW, rHand - 8,
                Pal.mix(ht.color, Pal.PANEL_LO, 0.4f), 1f);
            a.textBoxC(b, a.small, "lv" + lvl, ix + iw - lvW, handY + 4, lvW, rHand - 8, Pal.INK, 1f, 1f, 4);
        }

        double chips = battle.phase == Battle.Phase.IDLE ? (ht == null ? 0 : ht.chipsAt(lvl)) : showChips;
        double mult  = battle.phase == Battle.Phase.IDLE ? (ht == null ? 0 : ht.multAt(lvl)) : showMult;

        float boxesY = handY - 8 - rBoxes;
        float xGap = 24;
        float half = (iw - xGap) / 2f;
        a.panelSmall(b, ix, boxesY, half, rBoxes,
            Pal.mix(Pal.CHIPS, Pal.PANEL_LO, 0.30f + 0.34f * (1 - chipPop)), 1f);
        a.textBoxC(b, a.num, ScoreCtx.fmt(Math.floor(chips)), ix, boxesY, half, rBoxes,
            Pal.INK, 1f, 1f + chipPop * 0.10f, 8);
        a.panelSmall(b, ix + iw - half, boxesY, half, rBoxes,
            Pal.mix(Pal.MULT, Pal.PANEL_LO, 0.30f + 0.34f * (1 - multPop)), 1f);
        a.textBoxC(b, a.num, ScoreCtx.fmt(Math.round(mult * 100) / 100.0), ix + iw - half, boxesY,
            half, rBoxes, Pal.INK, 1f, 1f + multPop * 0.10f, 8);
        a.textBoxC(b, a.bodyB, "X", ix + half, boxesY, xGap, rBoxes, Pal.INK, 0.9f, 1f, 1);

        a.textBoxC(b, a.small, battle.phase == Battle.Phase.IDLE ? I18n.t("seal.wouldscore") : I18n.t("seal.thishand"),
            ix, y3 + rTotal, iw, rWorth, Pal.DIM, 1f, 1f, 8);
        a.textBoxC(b, a.num, ScoreCtx.big((long) (chips * mult)), ix, y3, iw, rTotal, Pal.GOLD, 1f, 1f, 12);

        // --- 4: hands and discards
        float h4 = 56;
        float y4 = L.block(h4, 9);
        float bGap = 10;
        float bHalf = (iw - bGap) / 2f;
        budget(b, a, ix, y4, bHalf, h4, I18n.t("seal.hands"), String.valueOf(battle.handsLeft), Pal.CHIPS,
            I18n.t("seal.handsTip"));
        budget(b, a, ix + iw - bHalf, y4, bHalf, h4, I18n.t("seal.discards"), String.valueOf(battle.discardsLeft),
            Pal.MULT, I18n.t("seal.discardsTip"));

        // --- 5: boss rule, only when there is one and there is room for it
        if (bl.mod != BossMod.NONE) {
            BossMod m = bl.mod;
            float descH = a.wrapH(a.small, I18n.bossDesc(m), iw - 16);
            float h5 = 8 + 16 + 2 + descH + 8;
            if (L.remaining() >= h5) {
                float y5 = L.block(h5);
                a.panelSmall(b, ix, y5, iw, h5, Pal.mix(m.color, Pal.PANEL_LO, 0.62f), 1f);
                a.textBoxC(b, a.small, I18n.boss(m).toUpperCase(), ix, y5 + h5 - 8 - 16, iw, 16, m.color, 1f, 1f, 8);
                a.textWrap(b, a.small, I18n.bossDesc(m), ix + 8, y5 + 8 + descH, iw - 16, Pal.INK, 0.9f);
                if (g.ui.hovering(ix, y5, iw, h5)) {
                    g.ui.tipWidth(300);
                    g.ui.tip(I18n.boss(m), I18n.bossDesc(m) + (bl.debuffSuit != null
                        ? "\n\n" + I18n.suit(bl.debuffSuit) : ""), m.color);
                }
            }
        }
    }

    private void budget(SpriteBatch b, Art a, float x, float y, float w, float h, String label,
                        String value, Color c, String tip) {
        a.panelSmall(b, x, y, w, h, Pal.PANEL, 0.88f);
        float labelH = 18;
        a.textBoxC(b, a.small, label, x, y + h - labelH - 4, w, labelH, Pal.DIM, 1f, 1f, 4);
        a.textBoxC(b, a.num, value, x, y + 4, w, h - labelH - 10, c, 1f, 1f, 8);
        if (g.ui.hovering(x, y, w, h)) g.ui.tip(label, tip, c);
    }

    // ------------------------------------------------------------------ chrome

    private void drawTopStrip(SpriteBatch b, Art a, float dt) {
        float h = 46, y = Main.VH - 18 - h;
        a.panel(b, PX, y, Main.VW - PX * 2, h, Pal.PANEL_LO, 0.9f);
        a.outline(b, PX, y, Main.VW - PX * 2, h, Pal.LINE, 0.4f);

        a.textBoxC(b, a.small, node.regionName, PX + 12, y + h / 2, 140, h / 2 - 2, Pal.DIM, 1f, 1f, 4);
        a.textBoxC(b, a.bodyB, "ANTE " + battle.blind.ante, PX + 12, y + 4, 140, h / 2 - 4, Pal.INK, 1f, 1f, 4);

        float gx = Main.VW - PX - 120;
        a.icon(b, Icon.COINS, gx + 16, y + h / 2, 20, Pal.GOLD, 0.95f);
        a.textBoxC(b, a.bodyB, "$" + g.run.gold, gx + 32, y + 6, 84, h - 12, Pal.GOLD, 1f, 1f, 4);

        int slots = g.run.slots();
        float rw = 36, gap = 6;
        float rx = PX + 168;
        for (int i = 0; i < slots; i++) {
            float x = rx + i * (rw + gap);
            relicX[Math.min(i, relicX.length - 1)] = x + rw / 2;
            relicY[Math.min(i, relicY.length - 1)] = y;
            if (i < g.run.relics.size()) {
                Relic rel = g.run.relics.get(i);
                float fl = relicFlash[Math.min(i, relicFlash.length - 1)];
                float hv = g.ui.hover("srelic" + i, x, y + 5, rw, rw, dt);
                if (fl > 0.01f) a.bloom(b, x, y + 5, rw, rw, rel.color, fl, 13);
                a.panelSmall(b, x, y + 5, rw, rw,
                    Pal.mix(Pal.mix(rel.color, Pal.PANEL_LO, 0.42f), Color.WHITE, fl * 0.6f), 1f);
                a.glyph(b, rel.glyph, x + rw / 2, y + 5 + rw / 2, 20, rel.color, 1f);
                if (rel.rarity > 0) a.disc(b, x + rw - 6, y + 5 + rw - 6, 3f, rel.rarityColor(), 1f);
                if (hv > 0.5f) {
                    g.ui.tipWidth(300);
                    g.ui.tip(rel.name + "   " + rel.rarityName(), rel.liveDesc(), rel.color);
                }
            } else {
                a.panelSmall(b, x, y + 5, rw, rw, Pal.PANEL, 0.3f);
            }
        }
    }

    private void drawSigilRail(SpriteBatch b, Art a, float dt) {
        float x = Main.VW - 18 - RAIL_W;
        float rowH = 40, gap = 6;
        float top = 486;
        a.textBoxC(b, a.small, I18n.t("seal.sigils"), x, top, RAIL_W, 18, Pal.DIM, 1f, 1f, 4);

        for (Sigil s : Sigil.values()) {
            float y = top - (s.ordinal() + 1) * (rowH + gap);
            boolean have = g.run.has(s);
            int ch = battle.charges[s.ordinal()];
            boolean ready = battle.canUse(s);
            float hv = g.ui.hover("sig" + s.name(), x, y, RAIL_W, rowH, dt);
            Color base = have ? Pal.mix(s.color, Pal.PANEL_LO, ready ? 0.34f : 0.74f) : Pal.PANEL;

            if (ready) a.bloom(b, x, y, RAIL_W, rowH, s.color, 0.22f + hv * 0.3f, 10);
            a.panelSmall(b, x, y, RAIL_W, rowH, base, have ? 1f : 0.42f);

            a.panelSmall(b, x + 5, y + 5, 30, 30, have ? Pal.PANEL_LO : Pal.PANEL, have ? 0.8f : 0.4f);
            a.icon(b, s.icon, x + 20, y + 20, 17, have ? s.color : Pal.DIMMER, have ? 1f : 0.6f);

            a.textBoxC(b, a.small, have ? I18n.sigilShort(s) : "-",
                x + 40, y + rowH / 2 - 1, 96, rowH / 2, have ? Pal.INK : Pal.DIMMER, 1f, 1f, 2);
            if (have) {
                int max = Math.max(1, s.charges(g.run.rank(s)));
                for (int i = 0; i < max; i++)
                    a.disc(b, x + 46 + i * 13, y + 11, 4.5f, i < ch ? s.color : Pal.PANEL_LO, i < ch ? 1f : 0.7f);
            }
            a.textBoxC(b, a.small, "[" + s.key + "]", x + RAIL_W - 34, y + 4, 30, rowH - 8,
                have ? Pal.DIM : Pal.DIMMER, 1f, 1f, 2);

            if (hv > 0.5f && have) {
                g.ui.tipWidth(310);
                g.ui.tip(I18n.sigil(s) + "  [" + s.key + "]",
                    I18n.sigilDesc(s, g.run.rank(s)) + "\n\n" + I18n.t("seal.charges", ch)
                        + "\n" + I18n.gate(s), s.color);
                if (ready && g.ui.justUp) battle.useSigil(s);
            }
        }

        if (battle.echoArmed > 0 || battle.prismArmed > 1) {
            String txt = battle.echoArmed > 0 ? "ECHO ARMED  +" + battle.echoArmed
                : "PRISM ARMED  X" + ScoreCtx.fmt(battle.prismArmed);
            Color c = battle.echoArmed > 0 ? Sigil.ECHO.color : Sigil.PRISM.color;
            float w2 = a.textW(a.small, txt) + 24;
            a.panelSmall(b, Main.VW - 18 - w2, top + 24, w2, 24, Pal.mix(c, Pal.PANEL_LO, 0.5f), 1f);
            a.textBoxC(b, a.small, txt, Main.VW - 18 - w2, top + 24, w2, 24, Pal.INK, 1f, 1f, 6);
        }
    }

    /** The charm pouch, sitting above the sigil rail. */
    private void drawCharmRail(SpriteBatch b, Art a, float dt) {
        Run run = g.run;
        float x = Main.VW - 18 - RAIL_W;
        float top = 566, slot = 46, gap = 8;
        a.textBoxC(b, a.small, "CHARMS", x, top, RAIL_W, 18, Pal.DIM, 1f, 1f, 4);

        int slots = run.charmSlots();
        for (int i = 0; i < slots; i++) {
            float sx = x + i * (slot + gap);
            float sy = top - slot - 6;
            if (i >= run.charms.size()) {
                a.panelSmall(b, sx, sy, slot, slot, Pal.PANEL, 0.30f);
                continue;
            }
            com.mygdx.game.run.Charm c = run.charms.get(i);
            String blocked = c.blocker(battle, battle.selected.size());
            boolean ready = blocked == null;
            float hv = g.ui.hover("charm" + i, sx, sy, slot, slot, dt);
            float lift = hv * 4f;

            if (ready) a.bloom(b, sx, sy + lift, slot, slot, c.color, 0.22f + hv * 0.32f, 10);
            a.panelSmall(b, sx, sy + lift, slot, slot,
                Pal.mix(c.color, Pal.PANEL_LO, ready ? 0.42f : 0.74f), ready ? 1f : 0.55f);
            a.icon(b, c.icon, sx + slot / 2, sy + lift + slot / 2, 22, c.color, ready ? 1f : 0.6f);
            if (c.rarity > 0) a.disc(b, sx + slot - 7, sy + lift + slot - 7, 3.5f, c.rarityColor(), 1f);

            if (hv > 0.5f) {
                g.ui.tipWidth(300);
                g.ui.tip(c.name + "   " + c.rarityName(),
                    c.desc + "\n\n(" + c.requirement() + ")"
                        + (blocked != null ? "\n\n" + blocked : "\n\nClick to use."), c.color);
                if (ready && g.ui.justUp) {
                    String err = battle.useCharm(c);
                    if (err == null) {
                        g.juice.burst(sx + slot / 2, sy + slot / 2, 20, c.color, 220, 6);
                        g.cine.impact(sx + slot / 2, sy + slot / 2, 0.6f, c.color);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ cards

    private void drawPlayed(SpriteBatch b, Art a, float dt) {
        List<Card> p = battle.playing;
        if (p.isEmpty()) return;
        float w = CardArt.W * 0.98f, h = CardArt.H * 0.98f;
        float step = w + 10;
        float x0 = HAND_CX - (p.size() * step - 14) / 2f;
        for (int i = 0; i < p.size(); i++) {
            Card c = p.get(i);
            float[] s = st(c);
            boolean scores = battle.ctx != null && battle.ctx.scoring.contains(c);
            s[0] = MathUtils.lerp(s[0], x0 + i * step, Math.min(1, dt * 13f));
            s[1] = MathUtils.lerp(s[1], PLAY_Y, Math.min(1, dt * 13f));
            s[2] = MathUtils.lerp(s[2], 0, Math.min(1, dt * 6f));
            s[3] = Math.max(0, s[3] - dt * 3.2f);
            s[4] = MathUtils.lerp(s[4], 0, Math.min(1, dt * 8f));
            CardArt.draw(b, a, c, s[0], s[1] + s[2], w, h, s[4], false, false, s[3],
                scores ? Pal.GOLD : Pal.DIMMER, scores ? 0.35f + s[3] * 0.6f : 0f,
                scores ? 1f : 0.42f, t);
        }
    }

    private void drawHand(SpriteBatch b, Art a, float dt) {
        List<Card> h = battle.hand;
        int n = h.size();
        boolean fog = battle.blind.mod == BossMod.FOG_OF_WAR;

        float pileW = CardArt.W * 0.50f, pileH = CardArt.H * 0.50f;
        CardArt.back(b, a, 300, HAND_Y + 16, pileW, pileH, 0.92f, t);
        a.textBoxC(b, a.small, I18n.t("seal.draw", battle.drawPile.size()), 300, HAND_Y - 8, pileW, 18, Pal.DIM, 1f, 1f, 0);
        float usedX = Main.VW - 18 - RAIL_W - 14 - pileW;
        a.panel(b, usedX, HAND_Y + 16, pileW, pileH, Pal.PANEL, 0.4f);
        a.textBoxC(b, a.small, I18n.t("seal.used", battle.discardPile.size()), usedX, HAND_Y - 8, pileW, 18, Pal.DIM, 1f, 1f, 0);

        if (n == 0) return;
        float avail = 690;
        float step = Math.min(CardArt.W + 10, avail / n);
        float x0 = HAND_CX - (n * step - (step - CardArt.W)) / 2f;

        int hoverIdx = -1;
        for (int i = n - 1; i >= 0; i--) {
            float x = x0 + i * step;
            float wSlice = (i == n - 1) ? CardArt.W : step;
            if (g.ui.hovering(x, HAND_Y, wSlice, CardArt.H)) { hoverIdx = i; break; }
        }

        for (int i = 0; i < n; i++) {
            Card c = h.get(i);
            float[] s = st(c);
            boolean sel = battle.selected.contains(c);
            boolean hov = i == hoverIdx && battle.idle();
            float mid = (n - 1) / 2f;
            float off = n <= 1 ? 0 : (i - mid) / mid;
            float tx = x0 + i * step;
            float ty = HAND_Y - Math.abs(off) * 8f;
            float tilt = -off * 5f;
            float lift = (sel ? 34 : 0) + (hov ? 15 : 0);

            s[0] = MathUtils.lerp(s[0], tx, Math.min(1, dt * 16f));
            s[1] = MathUtils.lerp(s[1], ty, Math.min(1, dt * 16f));
            s[2] = MathUtils.lerp(s[2], lift, Math.min(1, dt * 14f));
            s[3] = Math.max(0, s[3] - dt * 3.2f);
            s[4] = MathUtils.lerp(s[4], sel ? 0 : tilt, Math.min(1, dt * 12f));

            boolean inPreview = battle.idle() && sel && battle.previewScoring().contains(c);
            CardArt.draw(b, a, c, s[0], s[1] + s[2], CardArt.W, CardArt.H, s[4], sel, fog, s[3],
                inPreview ? Pal.GOLD : Pal.CHIPS, inPreview ? 0.5f : (hov ? 0.32f : 0f), 1f, t);

            if (hov) {
                g.ui.tipWidth(260);
                StringBuilder sb = new StringBuilder();
                sb.append("+").append(c.chipValue()).append(" Chips when scored.");
                if (c.enh != Card.Enhance.NONE) sb.append("\n").append(c.enh.label).append(": ").append(c.enh.desc);
                if (c.ed != Card.Edition.NONE) sb.append("\n").append(c.ed.label).append(": ").append(c.ed.desc);
                if (c.seal != Card.Seal.NONE) sb.append("\n").append(c.seal.label).append(": ").append(c.seal.desc);
                g.ui.tip(fog ? c.rank.label + " of ?????" : c.name(), sb.toString(), Pal.INK);
                if (g.ui.justUp) battle.toggle(c);
            }
        }
    }

    private void drawControls(SpriteBatch b, Art a, float dt) {
        float x = Main.VW - 18 - RAIL_W;
        String blocker = battle.playBlocker();
        boolean canPlay = battle.idle() && blocker == null;
        boolean canDiscard = battle.idle() && !battle.selected.isEmpty() && battle.discardsLeft > 0;

        // sits in its own strip above the PLAY button, never over it
        if (blocker != null && battle.idle() && !battle.selected.isEmpty())
            a.textBoxC(b, a.small, blocker, x, 196, RAIL_W, 22, Pal.RED, 1f, 1f, 6);

        if (g.ui.button(b, a, "play", I18n.t("seal.play"), x, 132, RAIL_W, 56,
            Pal.mix(Pal.CHIPS, Pal.PANEL, 0.2f), canPlay, dt).clicked) battle.playHand();
        if (g.ui.button(b, a, "disc", I18n.t("seal.discard"), x, 72, RAIL_W, 46,
            Pal.mix(Pal.MULT, Pal.PANEL, 0.3f), canDiscard, dt).clicked) battle.discardSelected();
        if (g.ui.chip(b, a, "sort", battle.sortBySuit ? I18n.t("seal.sortsuit") : I18n.t("seal.sortrank"), x, 38, 112, 26,
            Pal.PANEL_HI, battle.idle(), dt)) { battle.sortBySuit = !battle.sortBySuit; battle.sortHand(); }
        if (g.ui.chip(b, a, "clear", I18n.t("seal.clear"), x + 120, 38, 112, 26, Pal.PANEL_HI,
            battle.idle() && !battle.selected.isEmpty(), dt)) battle.selected.clear();
        a.textBoxC(b, a.small, I18n.t("seal.keyhint"), x, 12, RAIL_W, 20, Pal.DIMMER, 1f, 1f, 4);
    }

    // ------------------------------------------------------------------ tutorial

    /** Where the current step is pointing, in screen space. */
    private float[] focusPoint(Tutorial.Focus f) {
        switch (f) {
            case TARGET:         return new float[]{ PX + PW / 2f, PY + PH - 60, 90 };
            case SCORE_PANEL:    return new float[]{ PX + PW / 2f, PY + PH - 190, 110 };
            case HAND:           return new float[]{ HAND_CX, HAND_Y + CardArt.H / 2f, 210 };
            case PLAY_BUTTON:    return new float[]{ Main.VW - 18 - RAIL_W / 2f, 160, 120 };
            case DISCARD_BUTTON: return new float[]{ Main.VW - 18 - RAIL_W / 2f, 95, 110 };
            default:             return null;
        }
    }

    private void drawTutorial(SpriteBatch b, Art a, float dt) {
        Tutorial tut = g.run.tut;
        String text = tut.text();
        if (text == null) return;

        float[] f = focusPoint(tut.focus());
        if (f != null) {
            float pulse = 0.55f + 0.45f * MathUtils.sin(t * 3.4f);
            a.halo(b, f[0], f[1], f[2] * 1.15f, Pal.GOLD, 0.10f + 0.06f * pulse);
            a.ring(b, f[0], f[1], f[2] * (0.72f + 0.05f * pulse), Pal.GOLD, 0.35f + 0.3f * pulse);
        }

        float w = 620, h = 78;
        float x = (Main.VW - w) / 2f, y = 556;
        float pop = 1f + tut.pop * 0.05f;
        a.shadow(b, x, y - 6, w, h, 0.55f, 16);
        a.bloom(b, x, y, w, h, Pal.GOLD, 0.18f + tut.pop * 0.3f, 14);
        a.panel(b, x, y, w, h, Pal.PANEL_LO, 0.97f);
        a.outline(b, x, y, w, h, Pal.GOLD, 0.7f);

        a.icon(b, Icon.SCROLL, x + 32, y + h / 2f, 26, Pal.GOLD, 0.95f);
        a.textBoxC(b, a.small, I18n.t("tut.title"), x + 8, y + h - 22, 52, 16, Pal.GOLD, 0.8f, 1f, 2);
        a.textWrap(b, a.body, text, x + 62, y + h - 16, w - 150, Pal.INK, 1f * pop);

        if (g.ui.chip(b, a, "tutskip", I18n.t("tut.skip"), x + w - 84, y + 10, 74, 24,
            Pal.PANEL_HI, true, dt)) tut.skip();
    }

    // ------------------------------------------------------------------ outcome

    private void drawOutcome(SpriteBatch b, Art a, float dt, boolean won) {
        float k = Math.min(1, outcomeT * 2f);
        b.setColor(0, 0, 0, 0.8f * k);
        b.draw(a.white, 0, 0, Main.VW, Main.VH);
        b.setColor(Color.WHITE);

        float w = 560, x = (Main.VW - w) / 2;
        float pad = 26, rowH = 28, titleH = 46, btnH = 48;
        int rows = won ? 5 : 4;
        float grantH = (won && node.grants != null) ? 26 : 0;
        float h = pad + titleH + 12 + rows * rowH + grantH + 18 + btnH + pad;
        float y = (Main.VH - h) / 2;

        a.bloom(b, x, y, w, h, won ? Pal.GOLD : Pal.RED, 0.4f * k, 24);
        a.panel(b, x, y, w, h, Pal.PANEL_LO, 0.98f * k);
        a.outline(b, x, y, w, h, won ? Pal.GOLD : Pal.RED, 0.9f * k);

        float cur = y + h - pad - titleH;
        a.textBoxC(b, a.big, won ? I18n.t("seal.broken") : I18n.t("seal.holds"), x + pad, cur, w - pad * 2, titleH,
            won ? Pal.GOLD : Pal.RED, k, 1f, 10);
        cur -= 12;

        cur = row(b, a, x, w, pad, cur, rowH, I18n.t("seal.finalscore"), ScoreCtx.big(battle.score));
        cur = row(b, a, x, w, pad, cur, rowH, I18n.t("seal.required"), ScoreCtx.big(battle.blind.target));
        if (won) {
            cur = row(b, a, x, w, pad, cur, rowH, I18n.t("seal.reward"), "$" + battle.blind.reward);
            cur = row(b, a, x, w, pad, cur, rowH, I18n.t("seal.unused"), "$" + Math.max(0, battle.handsLeft));
            cur = row(b, a, x, w, pad, cur, rowH, I18n.t("seal.banked"), "$" + battle.goldEarned);
            if (node.grants != null) {
                a.textBoxC(b, a.bodyB, I18n.t("seal.takes", I18n.sigil(node.grants)), x + pad, cur - 26,
                    w - pad * 2, 26, node.grants.color, k, 1f, 8);
            }
        } else {
            cur = row(b, a, x, w, pad, cur, rowH, I18n.t("seal.shortby"), ScoreCtx.big(battle.remaining()));
            cur = row(b, a, x, w, pad, cur, rowH, I18n.t("seal.sealsbroken"), String.valueOf(g.run.blindsCleared));
        }

        if (outcomeT > 0.45f) {
            if (g.ui.button(b, a, "cont", won ? I18n.t("seal.continue") : I18n.t("seal.endrun"), x + w / 2 - 110, y + pad, 220, btnH,
                won ? Pal.PURPLE : Pal.PANEL_HI, true, dt).clicked
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                g.cine.letterbox(false);
                if (won) advance(); else g.set(new EndScreen(false));
            }
        }
    }

    /** Draws one label/value row inside its own band and returns the next band's top. */
    private float row(SpriteBatch b, Art a, float px, float pw, float pad, float top, float h,
                      String l, String r) {
        float y = top - h;
        float labelW = (pw - pad * 2) * 0.55f;
        float valueW = (pw - pad * 2) - labelW;
        a.textBoxC(b, a.body, l, px + pad + 18, y, labelW, h, Pal.DIM, 1f, 1f, 0);
        a.textBoxC(b, a.bodyB, r, px + pad + labelW, y, valueW - 18, h, Pal.INK, 1f, 1f, 0);
        return y;
    }

    private void advance() {
        Run run = g.run;
        run.cleared.add(node.id);
        if (node.grants != null) run.grant(node.grants);
        if (node.kind == WorldMap.Kind.HEART) {
            run.won = true;
            g.set(new EndScreen(true));
            return;
        }
        g.set(Content.reward(node));
    }
}
