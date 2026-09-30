package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.game.GameScreen;
import com.mygdx.game.Main;
import com.mygdx.game.cards.Card;
import com.mygdx.game.gfx.*;
import com.mygdx.game.i18n.I18n;
import com.mygdx.game.run.Relic;
import com.mygdx.game.world.WorldMap;

import java.util.ArrayList;
import java.util.List;

/** One screen shape reused for rewards, shrines, caches and wellsprings. */
public class ChoiceScreen implements GameScreen {

    public static class Option {
        public String title = "", sub = "", desc = "";
        public Color color = Pal.PURPLE;
        public String glyph = "*";
        public Card card;
        public Relic relic;
        public boolean enabled = true;
        public Runnable action;

        public Option(String title, String sub, String desc, Color c, String glyph) {
            this.title = title; this.sub = sub; this.desc = desc; this.color = c; this.glyph = glyph;
        }
        public Option act(Runnable r) { action = r; return this; }
        public Option showCard(Card c) { card = c; return this; }
        public Option showRelic(Relic r) { relic = r; glyph = r.glyph; color = r.color; return this; }
        public Option lock(boolean ok) { enabled = ok; return this; }
    }

    private final Main g = Main.I;
    public final WorldMap.Node node;
    public String header, flavour = "";
    public Color headerColor = Pal.GOLD;
    public final List<Option> options = new ArrayList<>();
    public String skipLabel = I18n.t("choice.moveon");
    public boolean allowSkip = true;
    private float t;

    public ChoiceScreen(WorldMap.Node node, String header, String flavour, Color c) {
        this.node = node; this.header = header; this.flavour = flavour; this.headerColor = c;
    }

    public ChoiceScreen add(Option o) { options.add(o); return this; }

    @Override
    public void draw(SpriteBatch b, float dt) {
        t += dt;
        Art a = g.art;
        UI ui = g.ui;

        Hud.pulseTitle(b, header, Main.VW / 2f, 648, headerColor, t);
        a.textC(b, a.body, flavour, Main.VW / 2f, 616, Pal.DIM);

        int n = options.size();
        float w = Math.min(288, (Main.VW - 140) / Math.max(1, n) - 18);
        float h = 340;
        float total = n * w + (n - 1) * 18;
        float x0 = (Main.VW - total) / 2f;
        float y = 200;

        for (int i = 0; i < n; i++) {
            Option o = options.get(i);
            float x = x0 + i * (w + 18);
            float hv = ui.hover("opt" + i, x, y, w, h, dt);
            float lift = o.enabled ? hv * 8 : 0;
            float alpha = o.enabled ? 1f : 0.45f;

            if (o.enabled) a.bloom(b, x, y + lift, w, h, o.color, 0.18f + hv * 0.35f, 14);
            a.panel(b, x, y + lift, w, h, Pal.PANEL_LO, 0.96f * alpha);
            a.outline(b, x, y + lift, w, h, o.color, (0.4f + hv * 0.5f) * alpha);

            // banded top-down: art, title, subtitle, description - each in its own rect
            float by = y + lift;
            float pad = 14, artH = 132, titleH = 28, subH = 18;
            float cur = by + h - pad;

            float artCy = cur - artH / 2f;
            if (o.card != null) {
                float ch = Math.min(artH * 0.96f, (w - 40) * (CardArt.H / CardArt.W));
                float cw = ch * (CardArt.W / CardArt.H);
                CardArt.draw(b, a, o.card, x + w / 2 - cw / 2, artCy - ch / 2, cw, ch,
                    0, false, false, 0, o.color, 0.3f, alpha, t);
            } else {
                a.halo(b, x + w / 2, artCy, 54, o.color, 0.35f * alpha);
                a.panelSmall(b, x + w / 2 - 34, artCy - 34, 68, 68, Pal.mix(o.color, Pal.PANEL_LO, 0.42f), alpha);
                a.glyph(b, o.glyph, x + w / 2, artCy, 42, o.color, alpha);
            }
            cur -= artH + 4;

            a.textBoxC(b, a.bodyB, o.title, x + 10, cur - titleH, w - 20, titleH,
                o.enabled ? Pal.INK : Pal.DIMMER, alpha, 1f, 6);
            cur -= titleH;
            a.textBoxC(b, a.small, o.sub, x + 10, cur - subH, w - 20, subH, o.color, alpha, 1f, 6);
            cur -= subH + 8;

            if (cur - (by + pad) > 16)
                a.textWrap(b, a.body, o.desc, x + 16, cur, w - 32, o.enabled ? Pal.DIM : Pal.DIMMER, alpha);

            if (o.relic != null && hv > 0.5f) {
                ui.tipWidth(290);
                ui.tip(o.relic.name + "   " + o.relic.rarityName(), o.relic.liveDesc(), o.relic.color);
            }

            if (o.enabled && hv > 0.5f && ui.justUp) {
                if (o.action != null) o.action.run();
                MapScreen.finish(node);
                return;
            }
        }

        Hud.runStrip(b, 10, 8, Main.VW - 20, 62, dt);
        Hud.relicRack(b, Main.VW - 20 - 5 * 46, 96, 40, 6, dt, false);

        if (allowSkip) {
            if (ui.button(b, a, "skip", skipLabel, Main.VW / 2f - 110, 96, 220, 44, Pal.PANEL_HI, true, dt).clicked
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                MapScreen.finish(node);
            }
        }
    }
}
