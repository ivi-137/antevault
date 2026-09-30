package com.mygdx.game.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.game.Main;
import com.mygdx.game.battle.ScoreCtx;
import com.mygdx.game.cards.Card;
import com.mygdx.game.gfx.Art;
import com.mygdx.game.gfx.Icon;
import com.mygdx.game.gfx.Pal;
import com.mygdx.game.gfx.UI;
import com.mygdx.game.i18n.I18n;
import com.mygdx.game.run.Relic;
import com.mygdx.game.run.Run;
import com.mygdx.game.run.Sigil;

/** Shared chrome: the run strip, item rack and sigil rack, drawn on several screens. */
public final class Hud {

    private Hud() {}

    public static void runStrip(SpriteBatch b, float x, float y, float w, float h, float dt) {
        Main g = Main.I;
        Art a = g.art;
        Run run = g.run;
        a.panel(b, x, y, w, h, Pal.PANEL_LO, 0.92f);
        a.outline(b, x, y, w, h, Pal.LINE, 0.45f);

        float cy = y + h / 2f;
        float px = x + 16;
        float halfH = (h - 12) / 2f;

        // gold
        a.icon(b, Icon.COINS, px + 15, cy, 22, Pal.GOLD, 0.95f);
        a.textBoxC(b, a.num, "$" + run.gold, px + 32, y + 6, 92, h - 12, Pal.GOLD, 1f, 0.8f, 4);
        if (g.ui.hovering(px, y, 130, h))
            g.ui.tip(I18n.t("hud.gold"), I18n.t("hud.goldTip"), Pal.GOLD);
        px += 140;

        px = stat(b, a, px, y, 66, h, halfH, I18n.t("hud.ante"), String.valueOf(run.ante), null);
        px = stat(b, a, px, y, 74, h, halfH, I18n.t("hud.deck"), String.valueOf(run.deck.size()), deckTip(run));
        px = stat(b, a, px, y, 74, h, halfH, I18n.t("hud.seals"), String.valueOf(run.blindsCleared), null);

        sigilRack(b, px + 6, y + 8, h - 16, dt);
    }

    /** One label-over-value cell. Returns the x for the next cell. */
    private static float stat(SpriteBatch b, Art a, float x, float y, float w, float h, float halfH,
                              String label, String value, String tip) {
        Main g = Main.I;
        a.textBoxC(b, a.small, label, x, y + h - halfH - 4, w, halfH, Pal.DIM, 1f, 1f, 2);
        a.textBoxC(b, a.bodyB, value, x, y + 6, w, halfH, Pal.INK, 1f, 1f, 2);
        if (tip != null && g.ui.hovering(x, y, w, h)) g.ui.tip(label, tip, Pal.INK);
        return x + w;
    }

    private static String deckTip(Run run) {
        StringBuilder sb = new StringBuilder();
        for (Card.Suit s : Card.Suit.values())
            sb.append(I18n.suit(s)).append(": ").append(run.deckCountSuit(s)).append("   ");
        return sb.toString();
    }

    public static void sigilRack(SpriteBatch b, float x, float y, float size, float dt) {
        Main g = Main.I;
        Art a = g.art;
        Run run = g.run;
        for (Sigil s : Sigil.values()) {
            float sx = x + s.ordinal() * (size + 7);
            boolean have = run.has(s);
            if (have) a.bloom(b, sx, y, size, size, s.color, 0.20f, 7);
            a.panelSmall(b, sx, y, size, size, have ? Pal.mix(s.color, Pal.PANEL_LO, 0.38f) : Pal.PANEL,
                have ? 1f : 0.4f);
            a.icon(b, s.icon, sx + size / 2, y + size / 2, size * 0.55f,
                have ? Pal.INK : Pal.DIMMER, have ? 1f : 0.5f);
            if (g.ui.hovering(sx, y, size, size)) {
                g.ui.tipWidth(300);
                g.ui.tip(I18n.sigil(s) + (have ? "   rank " + run.rank(s) : "   LOCKED"),
                    (have ? I18n.sigilDesc(s, run.rank(s)) + "\n" + s.charges(run.rank(s)) + " charge(s) per seal."
                          : "Not yet found. Wardens carry them.")
                        + "\n\n" + I18n.gate(s), s.color);
            }
        }
    }

    /** Draws the item rack; returns the index the mouse is over, or -1. */
    public static int relicRack(SpriteBatch b, float x, float y, float slot, float gap, float dt, boolean showSell) {
        Main g = Main.I;
        Art a = g.art;
        Run run = g.run;
        UI ui = g.ui;
        int hovered = -1;
        int slots = run.slots();
        for (int i = 0; i < slots; i++) {
            float sx = x + i * (slot + gap);
            if (i < run.relics.size()) {
                Relic r = run.relics.get(i);
                float hv = ui.hover("relic" + i, sx, y, slot, slot, dt);
                float lift = hv * 5f;
                a.bloom(b, sx, y + lift, slot, slot, r.color, 0.22f + hv * 0.4f, 10);
                a.panelSmall(b, sx, y + lift, slot, slot, Pal.mix(r.color, Pal.PANEL_LO, 0.42f), 1f);
                a.panelSmall(b, sx + 3, y + lift + 3, slot - 6, slot - 6, Pal.PANEL_LO, 0.5f);
                a.glyph(b, r.glyph, sx + slot / 2, y + lift + slot / 2, slot * 0.5f, r.color, 1f);
                if (r.rarity > 0) a.disc(b, sx + slot - 7, y + lift + slot - 7, 3.5f, r.rarityColor(), 1f);
                if (hv > 0.5f) {
                    hovered = i;
                    ui.tipWidth(300);
                    ui.tip(r.name + "   " + r.rarityName(),
                        r.liveDesc() + (showSell ? "\n\nRight-click to sell for $" + r.sellValue() + "." : ""),
                        r.color);
                }
            } else {
                a.panelSmall(b, sx, y, slot, slot, Pal.PANEL, 0.32f);
                a.textC(b, a.small, "-", sx + slot / 2, y + slot / 2 + 4, Pal.DIMMER);
            }
        }
        return hovered;
    }

    public static void pulseTitle(SpriteBatch b, String text, float cx, float y, Color c, float time) {
        Art a = Main.I.art;
        a.textScaled(b, a.big, text, cx, y, c, 1f, 1f + MathUtils.sin(time * 2.4f) * 0.02f);
    }

    public static String num(long v) { return ScoreCtx.big(v); }
}
