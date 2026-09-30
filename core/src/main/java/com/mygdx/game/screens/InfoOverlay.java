package com.mygdx.game.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.game.Main;
import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandType;
import com.mygdx.game.gfx.*;
import com.mygdx.game.i18n.I18n;
import com.mygdx.game.run.Charm;
import com.mygdx.game.run.Relic;
import com.mygdx.game.run.Run;
import com.mygdx.game.run.Sigil;

/**
 * Everything the player needs to make an informed decision, on one panel: hand levels, deck
 * composition, and what is currently equipped.
 *
 * Drawn on top of whichever screen is running, so it works during a seal as well as on the map.
 */
public class InfoOverlay {

    private final Main g = Main.I;
    public boolean open;
    private float t, appear;

    public void toggle() { open = !open; }

    public void draw(SpriteBatch b, float dt) {
        appear = Math.min(1f, Math.max(0f, appear + (open ? dt * 6f : -dt * 8f)));
        if (appear <= 0.001f) return;
        t += dt;

        Art a = g.art;
        Run run = g.run;
        if (run == null) return;

        float k = appear * appear * (3 - 2 * appear);
        b.setColor(0, 0, 0, 0.80f * k);
        b.draw(a.white, 0, 0, Main.VW, Main.VH);
        b.setColor(Color.WHITE);

        float m = 40;
        float x = m, y = m, w = Main.VW - m * 2, h = Main.VH - m * 2;
        float slide = (1f - k) * 40f;

        a.shadow(b, x, y - slide - 6, w, h, 0.5f * k, 18);
        a.panel(b, x, y - slide, w, h, Pal.PANEL_LO, 0.98f * k);
        a.outline(b, x, y - slide, w, h, Pal.GOLD, 0.7f * k);
        y -= slide;

        a.textBoxC(b, a.title, "RUN", x, y + h - 62, w, 48, Pal.GOLD, k, 0.5f, 20);
        a.textBoxC(b, a.small, "press I or Esc to close", x, y + h - 82, w, 18, Pal.DIMMER, k, 1f, 20);

        float colGap = 22;
        float colW = (w - 60 - colGap * 2) / 3f;
        float colY = y + 84;
        float colH = h - 84 - 96;
        float cx0 = x + 30;

        drawHandLevels(b, a, run, cx0, colY, colW, colH, k);
        drawDeck(b, a, run, cx0 + colW + colGap, colY, colW, colH, k);
        drawLoadout(b, a, run, cx0 + (colW + colGap) * 2, colY, colW, colH, k, dt);
    }

    private void header(SpriteBatch b, Art a, String s, float x, float y, float w, float k, Color c) {
        a.panelSmall(b, x, y, w, 26, Pal.PANEL, 0.9f * k);
        a.textBoxC(b, a.small, s, x, y, w, 26, c, k, 1f, 8);
    }

    // ---------------------------------------------------------------- columns

    private void drawHandLevels(SpriteBatch b, Art a, Run run, float x, float y, float w, float h, float k) {
        a.panelSmall(b, x, y, w, h, Pal.PANEL_LO, 0.55f * k);
        header(b, a, "HAND LEVELS", x, y + h - 26, w, k, Pal.CHIPS);

        float rowH = 30;
        float cur = y + h - 32 - rowH;
        for (HandType ht : HandType.values()) {
            if (ht.secret() && !run.discovered.contains(ht)) continue;
            if (cur < y + 6) break;
            int lvl = run.level(ht);

            a.panelSmall(b, x + 6, cur, w - 12, rowH - 4, Pal.PANEL, (lvl > 1 ? 0.85f : 0.45f) * k);
            a.textBoxC(b, a.small, I18n.hand(ht), x + 12, cur, w * 0.50f, rowH - 4, ht.color, k, 1f, 4);

            a.panelSmall(b, x + w * 0.53f, cur + 3, 34, rowH - 10,
                Pal.mix(ht.color, Pal.PANEL_LO, 0.45f), k);
            a.textBoxC(b, a.small, "lv" + lvl, x + w * 0.53f, cur + 3, 34, rowH - 10, Pal.INK, k, 1f, 2);

            a.textBoxC(b, a.small, ht.chipsAt(lvl) + " x " + ht.multAt(lvl),
                x + w * 0.53f + 38, cur, w * 0.47f - 46, rowH - 4, Pal.DIM, k, 1f, 2);
            cur -= rowH;
        }
    }

    private void drawDeck(SpriteBatch b, Art a, Run run, float x, float y, float w, float h, float k) {
        a.panelSmall(b, x, y, w, h, Pal.PANEL_LO, 0.55f * k);
        header(b, a, "DECK  ·  " + run.deck.size() + " CARDS", x, y + h - 26, w, k, Pal.GOLD);

        // suit tallies
        float cur = y + h - 34;
        float cellW = (w - 24) / 4f;
        for (Card.Suit s : Card.Suit.values()) {
            float sx = x + 12 + s.ordinal() * cellW;
            // the card ink colours are tuned for an ivory face, not this dark panel
            CardArt.pip(b, a, s, sx + cellW / 2, cur - 16, 18,
                s.red() ? Pal.SUIT_RED : Pal.INK, k);
            a.textBoxC(b, a.small, String.valueOf(run.deckCountSuit(s)), sx, cur - 44, cellW, 18, Pal.INK, k, 1f, 2);
        }
        cur -= 58;

        // rank histogram, so you can see what the deck actually holds
        header(b, a, "BY RANK", x + 6, cur - 26, w - 12, k, Pal.DIM);
        cur -= 34;

        int[] byRank = new int[15];
        int enhanced = 0, editioned = 0, sealed = 0;
        for (Card c : run.deck) {
            if (!c.isStone()) byRank[c.rank.order]++;
            if (c.enh != Card.Enhance.NONE) enhanced++;
            if (c.ed != Card.Edition.NONE) editioned++;
            if (c.seal != Card.Seal.NONE) sealed++;
        }
        int max = 1;
        for (int v : byRank) max = Math.max(max, v);

        float barW = (w - 24) / 13f;
        float barMax = Math.max(20f, cur - y - 96);
        for (int i = 0; i < 13; i++) {
            Card.Rank r = Card.Rank.values()[i];
            int n = byRank[r.order];
            float bx = x + 12 + i * barW;
            float bh = barMax * (n / (float) max);
            a.panelSmall(b, bx + 1, cur - barMax, barW - 2, barMax, Pal.PANEL, 0.4f * k);
            if (n > 0) a.panelSmall(b, bx + 1, cur - barMax, barW - 2, Math.max(3, bh), Pal.CHIPS, 0.85f * k);
            a.textBoxC(b, a.small, r.label, bx, cur - barMax - 18, barW, 16, Pal.DIM, k, 1f, 0);
        }
        cur -= barMax + 24;

        stat(b, a, x + 12, cur - 20, w - 24, "Enhanced", String.valueOf(enhanced), k);
        stat(b, a, x + 12, cur - 40, w - 24, "Editions", String.valueOf(editioned), k);
        stat(b, a, x + 12, cur - 60, w - 24, "Sealed", String.valueOf(sealed), k);
    }

    private void drawLoadout(SpriteBatch b, Art a, Run run, float x, float y, float w, float h,
                             float k, float dt) {
        a.panelSmall(b, x, y, w, h, Pal.PANEL_LO, 0.55f * k);
        header(b, a, "CARRIED", x, y + h - 26, w, k, Pal.PURPLE);

        float cur = y + h - 38;

        // items
        a.textBoxC(b, a.small, "ITEMS  " + run.relics.size() + " / " + run.slots(),
            x + 10, cur - 18, w - 20, 18, Pal.DIM, k, 1f, 2);
        cur -= 22;
        for (Relic r : run.relics) {
            float rowH = 34;
            a.panelSmall(b, x + 8, cur - rowH, w - 16, rowH - 3, Pal.mix(r.color, Pal.PANEL_LO, 0.72f), k);
            a.glyph(b, r.glyph, x + 26, cur - rowH / 2 - 1, 18, r.color, k);
            a.textBoxC(b, a.small, r.name, x + 42, cur - rowH, w - 56, rowH - 3, Pal.INK, k, 1f, 2);
            if (g.ui.hovering(x + 8, cur - rowH, w - 16, rowH - 3)) {
                g.ui.tipWidth(300);
                g.ui.tip(r.name + "   " + r.rarityName(), r.liveDesc(), r.color);
            }
            cur -= rowH;
        }

        // charms
        cur -= 8;
        a.textBoxC(b, a.small, "CHARMS  " + run.charms.size() + " / " + run.charmSlots(),
            x + 10, cur - 18, w - 20, 18, Pal.DIM, k, 1f, 2);
        cur -= 22;
        for (Charm c : run.charms) {
            float rowH = 34;
            a.panelSmall(b, x + 8, cur - rowH, w - 16, rowH - 3, Pal.mix(c.color, Pal.PANEL_LO, 0.72f), k);
            a.icon(b, c.icon, x + 26, cur - rowH / 2 - 1, 18, c.color, k);
            a.textBoxC(b, a.small, c.name, x + 42, cur - rowH, w - 56, rowH - 3, Pal.INK, k, 1f, 2);
            if (g.ui.hovering(x + 8, cur - rowH, w - 16, rowH - 3)) {
                g.ui.tipWidth(300);
                g.ui.tip(c.name, c.desc + "\n\n(" + c.requirement() + ")", c.color);
            }
            cur -= rowH;
        }

        // sigils
        cur -= 8;
        a.textBoxC(b, a.small, I18n.t("seal.sigils"), x + 10, cur - 18, w - 20, 18, Pal.DIM, k, 1f, 2);
        cur -= 26;
        float sx = x + 10;
        for (Sigil s : Sigil.values()) {
            boolean have = run.has(s);
            a.panelSmall(b, sx, cur - 34, 34, 34, have ? Pal.mix(s.color, Pal.PANEL_LO, 0.4f) : Pal.PANEL,
                (have ? 1f : 0.35f) * k);
            a.icon(b, s.icon, sx + 17, cur - 17, 17, have ? Pal.INK : Pal.DIMMER, k);
            if (have)
                a.textBoxC(b, a.small, String.valueOf(run.rank(s)), sx + 22, cur - 34, 14, 14, Pal.GOLD, k, 1f, 0);
            if (g.ui.hovering(sx, cur - 34, 34, 34)) {
                g.ui.tipWidth(300);
                g.ui.tip(I18n.sigil(s), have ? I18n.sigilDesc(s, run.rank(s)) : "-", s.color);
            }
            sx += 40;
        }
        cur -= 46;

        stat(b, a, x + 12, cur - 20, w - 24, I18n.t("seal.hands"), String.valueOf(run.baseHands), k);
        stat(b, a, x + 12, cur - 40, w - 24, I18n.t("seal.discards"), String.valueOf(run.baseDiscards), k);
        stat(b, a, x + 12, cur - 60, w - 24, I18n.t("end.handsize"), String.valueOf(run.handSize()), k);
    }

    private void stat(SpriteBatch b, Art a, float x, float y, float w, String l, String r, float k) {
        a.textBoxC(b, a.small, l, x, y, w * 0.6f, 18, Pal.DIM, k, 1f, 0);
        a.textBoxC(b, a.small, r, x + w * 0.6f, y, w * 0.4f, 18, Pal.INK, k, 1f, 0);
    }
}
