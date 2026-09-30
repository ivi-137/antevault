package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.game.GameScreen;
import com.mygdx.game.Main;
import com.mygdx.game.battle.ScoreCtx;
import com.mygdx.game.gfx.Art;
import com.mygdx.game.gfx.Pal;
import com.mygdx.game.i18n.I18n;
import com.mygdx.game.run.Relic;
import com.mygdx.game.run.Run;
import com.mygdx.game.run.Sigil;

public class EndScreen implements GameScreen {

    private final Main g = Main.I;
    private final boolean won;
    private float t;

    public EndScreen(boolean won) { this.won = won; }

    @Override
    public void enter() {
        if (won) for (int i = 0; i < 60; i++)
            g.juice.burst(MathUtils.random(200, Main.VW - 200), MathUtils.random(200, 600),
                6, i % 2 == 0 ? Pal.GOLD : Pal.PINK, 260, 8);
    }

    @Override
    public void draw(SpriteBatch b, float dt) {
        t += dt;
        Art a = g.art;
        Run run = g.run;

        if (won && MathUtils.randomBoolean(dt * 5f))
            g.juice.burst(MathUtils.random(120, Main.VW - 120), Main.VH + 20, 5, Pal.GOLD, 200, 7);

        a.textC(b, a.huge, won ? I18n.t("end.win") : I18n.t("end.lose"), Main.VW / 2f, 622,
            won ? Pal.GOLD : Pal.RED);
        a.textC(b, a.body, won
                ? "You broke every seal between the camp and the thing at the bottom."
                : "The seal held. Somebody else will have to break it.",
            Main.VW / 2f, 588, Pal.DIM);

        float w = 580, x = (Main.VW - w) / 2f, y = 186, h = 348;
        a.shadow(b, x, y - 6, w, h, 0.5f, 16);
        a.panel(b, x, y, w, h, Pal.PANEL_LO, 0.96f);
        a.outline(b, x, y, w, h, won ? Pal.GOLD : Pal.LINE, 0.7f);

        float ly = y + h - 40;
        stat(b, a, x, w, ly, I18n.t("seal.sealsbroken"), String.valueOf(run.blindsCleared)); ly -= 30;
        stat(b, a, x, w, ly, I18n.t("end.handsplayed"), String.valueOf(run.handsPlayed)); ly -= 30;
        stat(b, a, x, w, ly, I18n.t("end.best"), ScoreCtx.big(run.bestScore)); ly -= 30;
        stat(b, a, x, w, ly, I18n.t("end.goldbanked"), "$" + run.gold); ly -= 30;
        stat(b, a, x, w, ly, I18n.t("end.decksize"), String.valueOf(run.deck.size())); ly -= 30;
        stat(b, a, x, w, ly, I18n.t("end.handsize"), String.valueOf(run.handSize())); ly -= 44;

        a.text(b, a.small, I18n.t("seal.sigils"), x + 36, ly, Pal.DIM);
        float sx = x + 36;
        for (Sigil s : Sigil.values()) {
            boolean have = run.has(s);
            a.panelSmall(b, sx, ly - 46, 36, 36, have ? Pal.mix(s.color, Pal.PANEL_LO, 0.38f) : Pal.PANEL,
                have ? 1f : 0.35f);
            a.icon(b, s.icon, sx + 18, ly - 28, 18, have ? Pal.INK : Pal.DIMMER, have ? 1f : 0.5f);
            if (g.ui.hovering(sx, ly - 46, 36, 36))
                g.ui.tip(I18n.sigil(s), have ? I18n.sigilDesc(s, run.rank(s)) : "-", s.color);
            sx += 42;
        }
        a.text(b, a.small, I18n.t("end.build"), x + 300, ly, Pal.DIM);
        float rx = x + 300;
        for (Relic r : run.relics) {
            a.panelSmall(b, rx, ly - 46, 36, 36, Pal.mix(r.color, Pal.PANEL_LO, 0.42f), 1f);
            a.glyph(b, r.glyph, rx + 18, ly - 28, 18, r.color, 1f);
            if (g.ui.hovering(rx, ly - 46, 36, 36)) g.ui.tip(r.name, r.liveDesc(), r.color);
            rx += 42;
        }

        if (g.ui.button(b, a, "again", I18n.t("menu.newrun"), Main.VW / 2f - 232, 96, 224, 52, Pal.PURPLE, true, dt).clicked
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            g.newRun();
        }
        if (g.ui.button(b, a, "title", "TITLE", Main.VW / 2f + 8, 96, 224, 52, Pal.PANEL_HI, true, dt).clicked) {
            g.set(new TitleScreen());
        }
    }

    private void stat(SpriteBatch b, Art a, float x, float w, float y, String l, String r) {
        a.text(b, a.body, l, x + 36, y, Pal.DIM);
        a.textR(b, a.bodyB, r, x + w - 36, y, Pal.INK);
    }
}
