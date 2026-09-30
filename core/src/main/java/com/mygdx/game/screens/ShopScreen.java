package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.game.GameScreen;
import com.mygdx.game.Main;
import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandType;
import com.mygdx.game.gfx.*;
import com.mygdx.game.i18n.I18n;
import com.mygdx.game.run.Charm;
import com.mygdx.game.run.Charms;
import com.mygdx.game.run.Relic;
import com.mygdx.game.run.Relics;
import com.mygdx.game.run.Run;
import com.mygdx.game.world.WorldMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The item shop. Bazaars stay open forever, so coming back richer is a real play. */
public class ShopScreen implements GameScreen {

    private static final int ITEM = 0, CODEX = 1, CARD = 2, CHARM = 3;

    public static class Offer {
        int kind;
        String title, sub, desc, glyph;
        Color color;
        Relic relic;
        Charm charm;
        Card card;
        HandType hand;
        int price;
        boolean sold;
    }

    /** Held on the Run so a bazaar remembers what you already bought. */
    public static class Stock {
        final List<Offer> offers = new ArrayList<>();
        int rerollCost = 4;
        int excised;
    }

    private final Main g = Main.I;
    private final WorldMap.Node node;
    private final Stock stock;
    private float t;
    private String note = "";
    private float noteT;

    public ShopScreen(WorldMap.Node node) {
        this.node = node;
        Object o = g.run.nodeState.get("shop:" + node.id);
        if (o instanceof Stock) {
            stock = (Stock) o;
        } else {
            stock = new Stock();
            restock(true);
            g.run.nodeState.put("shop:" + node.id, stock);
        }
    }

    private void restock(boolean full) {
        Run run = g.run;
        stock.offers.clear();
        int maxRarity = node.ante >= 3 ? 3 : 2;
        for (int i = 0; i < 3; i++) {
            Relic r = Relics.random(run, run.rnd, maxRarity);
            Offer o = new Offer();
            o.kind = ITEM; o.relic = r; o.title = r.name; o.desc = r.desc;
            o.sub = r.rarityName(); o.color = r.color; o.glyph = r.glyph;
            o.price = Math.max(3, r.cost + node.ante - 1);
            stock.offers.add(o);
        }
        for (int i = 0; i < 2; i++) {
            Charm c = Charms.random(run.rnd, node.ante >= 3 ? 2 : 1);
            Offer o = new Offer();
            o.kind = CHARM; o.charm = c; o.title = c.name; o.sub = c.rarityName() + " charm";
            o.desc = c.desc; o.color = c.color; o.glyph = String.valueOf(c.icon);
            o.price = Math.max(2, c.cost + node.ante - 2);
            stock.offers.add(o);
        }
        for (int i = 0; i < 2; i++) {
            HandType h = Content.rollHand();
            Offer o = new Offer();
            o.kind = CODEX; o.hand = h; o.title = "Codex: " + I18n.hand(h); o.sub = "+1 level";
            o.desc = "Raises " + I18n.hand(h) + " permanently.";
            o.color = h.color; o.glyph = "^"; o.price = 5 + node.ante;
            stock.offers.add(o);
        }
        for (int i = 0; i < 2; i++) {
            Card c = Content.rollCard(true);
            Offer o = new Offer();
            o.kind = CARD; o.card = c; o.title = c.rank.label + " " + I18n.suit(c.suit); o.sub = "add to deck";
            StringBuilder sb = new StringBuilder();
            if (c.enh != Card.Enhance.NONE) sb.append(c.enh.desc);
            if (c.ed != Card.Edition.NONE) { if (sb.length() > 0) sb.append("\n"); sb.append(c.ed.desc); }
            if (c.seal != Card.Seal.NONE) { if (sb.length() > 0) sb.append("\n"); sb.append(c.seal.desc); }
            if (sb.length() == 0) sb.append("A plain card, but it thickens the odds you want.");
            o.desc = sb.toString();
            o.color = Pal.CHIPS; o.glyph = "+";
            o.price = 3 + (c.enh != Card.Enhance.NONE ? 3 : 0) + (c.ed != Card.Edition.NONE ? 3 : 0)
                + (c.seal != Card.Seal.NONE ? 3 : 0);
            stock.offers.add(o);
        }
        if (full) stock.rerollCost = 4;
    }

    private void buy(Offer o) {
        Run run = g.run;
        if (o.sold || run.gold < o.price) return;
        switch (o.kind) {
            case ITEM:
                if (run.full()) { toast("Item slots are full. Sell one first."); return; }
                run.addRelic(o.relic);
                break;
            case CODEX: run.levelUp(o.hand, 1); break;
            case CHARM:
                if (run.charmsFull()) { toast("Your charm pouch is full."); return; }
                run.addCharm(o.charm);
                break;
            default:    run.deck.add(o.card); break;
        }
        run.gold -= o.price;
        o.sold = true;
        g.juice.burst(g.ui.mouse.x, g.ui.mouse.y, 18, Pal.GOLD, 220, 6);
        toast("Bought " + o.title + ".");
    }

    private void toast(String s) { note = s; noteT = 2.4f; }

    @Override
    public void draw(SpriteBatch b, float dt) {
        t += dt;
        Art a = g.art;
        UI ui = g.ui;
        Run run = g.run;
        noteT = Math.max(0, noteT - dt);

        a.textBoxC(b, a.title, I18n.t("shop.title"), Main.VW / 2f - 400, 648, 800, 54,
            Pal.TEAL, 1f, 0.52f, 20);
        a.textBoxC(b, a.body, I18n.t("shop.sub"), Main.VW / 2f - 420, 614, 840, 24, Pal.DIM, 1f, 1f, 10);

        float y = 396, w = 252, h = 208, gap = 22;
        float x0 = (Main.VW - (3 * w + 2 * gap)) / 2f;
        for (int i = 0; i < 3; i++) drawOffer(b, a, stock.offers.get(i), x0 + i * (w + gap), y, w, h, dt, true);

        float sw = 146, sh = 150, sgap = 14;
        float sx0 = (Main.VW - (7 * sw + 6 * sgap)) / 2f;
        for (int i = 0; i < 6; i++) drawOffer(b, a, stock.offers.get(3 + i), sx0 + i * (sw + sgap), 212, sw, sh, dt, false);
        drawExcise(b, a, sx0 + 6 * (sw + sgap), 212, sw, sh, dt);

        a.text(b, a.small, I18n.t("shop.yourbuild"), 44, 176, Pal.DIM);
        int hovered = Hud.relicRack(b, 44, 104, 52, 8, dt, true);
        if (hovered >= 0 && ui.rightJustDown && hovered < run.relics.size()) {
            Relic r = run.relics.get(hovered);
            run.gold += r.sellValue();
            run.relics.remove(hovered);
            toast("Sold " + r.name + " for $" + r.sellValue() + ".");
            g.juice.burst(ui.mouse.x, ui.mouse.y, 16, Pal.GOLD, 200, 6);
        }

        boolean canRoll = run.gold >= stock.rerollCost;
        if (ui.button(b, a, "reroll", I18n.t("shop.reroll") + "  $" + stock.rerollCost, Main.VW - 480, 104, 210, 48,
            Pal.mix(Pal.PURPLE, Pal.PANEL, 0.3f), canRoll, dt).clicked) {
            run.gold -= stock.rerollCost;
            stock.rerollCost += 3;
            restock(false);
            g.juice.shake(4);
            toast("Restocked.");
        }
        if (ui.button(b, a, "leave", I18n.t("shop.leave"), Main.VW - 254, 104, 210, 48, Pal.PANEL_HI, true, dt).clicked
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            MapScreen.finish(node);
            return;
        }

        if (noteT > 0) a.textC(b, a.bodyB, note, Main.VW / 2f, 82, Pal.GREEN, Math.min(1, noteT * 2));

        Hud.runStrip(b, 18, 12, Main.VW - 36, 62, dt);
    }

    private void drawOffer(SpriteBatch b, Art a, Offer o, float x, float y, float w, float h, float dt, boolean bigOne) {
        UI ui = g.ui;
        Run run = g.run;
        boolean afford = run.gold >= o.price && !o.sold;
        boolean blocked = (o.kind == ITEM && run.full()) || (o.kind == CHARM && run.charmsFull());
        float hv = o.sold ? 0 : ui.hover("shop" + o.title + x, x, y, w, h, dt);
        float lift = hv * 7;
        float alpha = o.sold ? 0.28f : (afford ? 1f : 0.62f);

        if (!o.sold && afford) a.bloom(b, x, y + lift, w, h, o.color, 0.16f + hv * 0.32f, 14);
        a.shadow(b, x, y + lift - 5, w, h, 0.42f * alpha, 11);
        a.panel(b, x, y + lift, w, h, Pal.PANEL_LO, 0.97f * alpha);
        a.outline(b, x, y + lift, w, h, o.color, (0.35f + hv * 0.5f) * alpha);

        // Banded from the top down: art, title, subtitle, description, price. Each band owns its
        // own rectangle, so the price tag can never end up on the text above it.
        float by = y + lift;
        float pad = 10, priceH = 28, titleH = 24, subH = 16;
        float artH = bigOne ? 74 : 58;
        float cur = by + h - pad;

        float artCy = cur - artH / 2f;
        if (o.card != null) {
            // derive the preview size from the band so it can never grow past its panel
            float ch = Math.min(artH * 0.96f, (w - 24) * (CardArt.H / CardArt.W));
            float cw = ch * (CardArt.W / CardArt.H);
            CardArt.draw(b, a, o.card, x + w / 2 - cw / 2, artCy - ch / 2, cw, ch,
                0, false, false, 0, o.color, 0.25f, alpha, t);
        } else {
            a.halo(b, x + w / 2, artCy, 42, o.color, 0.32f * alpha);
            a.panelSmall(b, x + w / 2 - 27, artCy - 27, 54, 54, Pal.mix(o.color, Pal.PANEL_LO, 0.40f), alpha);
            a.glyph(b, o.glyph, x + w / 2, artCy, 34, o.color, alpha);
        }
        cur -= artH + 4;

        a.textBoxC(b, a.bodyB, o.title, x + 8, cur - titleH, w - 16, titleH, Pal.INK, alpha, 1f, 6);
        cur -= titleH;
        a.textBoxC(b, a.small, o.sub, x + 8, cur - subH, w - 16, subH, o.color, alpha, 1f, 6);
        cur -= subH + 6;

        float priceY = by + pad;
        if (bigOne) {
            float descTop = cur;
            float descBottom = priceY + priceH + 8;
            if (descTop - descBottom > 16)
                a.textWrap(b, a.body, o.desc, x + 14, descTop, w - 28, Pal.DIM, alpha);
        }

        float pw = 82;
        float px = x + w / 2 - pw / 2;
        a.panelSmall(b, px, priceY, pw, priceH, o.sold ? Pal.PANEL
            : (afford ? Pal.mix(Pal.GOLD, Pal.PANEL_LO, 0.32f) : Pal.PANEL), alpha);
        a.textBoxC(b, a.bodyB, o.sold ? I18n.t("shop.sold") : "$" + o.price, px, priceY, pw, priceH,
            o.sold ? Pal.DIMMER : (afford ? Pal.GOLD : Pal.DIMMER), alpha, 1f, 6);

        if (hv > 0.5f && !o.sold) {
            ui.tipWidth(300);
            String extra = blocked
                ? (o.kind == CHARM ? "\n\nYour charm pouch is full - use one first."
                                   : "\n\nYour item slots are full - sell something first.")
                : (!afford ? "\n\nYou cannot afford this." : "");
            ui.tip(o.title, o.desc + extra, o.color);
            if (ui.justUp) {
                if (blocked) toast(o.kind == CHARM ? "Charm pouch is full." : "Item slots are full.");
                else if (!afford) toast("Not enough gold.");
                else buy(o);
            }
        }
    }

    /** Deck thinning: strips the two least useful plain cards. */
    private void drawExcise(SpriteBatch b, Art a, float x, float y, float w, float h, float dt) {
        Run run = g.run;
        int price = 6 + stock.excised * 5;
        boolean afford = run.gold >= price && run.deck.size() > 20;
        float hv = g.ui.hover("excise", x, y, w, h, dt);
        float lift = hv * 7;
        if (afford) a.bloom(b, x, y + lift, w, h, Pal.STEEL, 0.16f + hv * 0.3f, 14);
        a.shadow(b, x, y + lift - 5, w, h, 0.42f, 11);
        a.panel(b, x, y + lift, w, h, Pal.PANEL_LO, 0.97f);
        a.outline(b, x, y + lift, w, h, Pal.STEEL, 0.35f + hv * 0.5f);

        float by = y + lift;
        float pad = 10, priceH = 28, titleH = 24, subH = 16, artH = 56;
        float cur = by + h - pad;

        float artCy = cur - artH / 2f;
        a.halo(b, x + w / 2, artCy, 42, Pal.STEEL, 0.3f);
        a.panelSmall(b, x + w / 2 - 27, artCy - 27, 54, 54, Pal.mix(Pal.STEEL, Pal.PANEL_LO, 0.42f), 1f);
        a.icon(b, Icon.TRASH, x + w / 2, artCy, 30, Pal.STEEL, 1f);
        cur -= artH + 4;

        a.textBoxC(b, a.bodyB, "Excise", x + 8, cur - titleH, w - 16, titleH, Pal.INK, 1f, 1f, 6);
        cur -= titleH;
        a.textBoxC(b, a.small, "remove 2 low cards", x + 8, cur - subH, w - 16, subH, Pal.STEEL, 1f, 1f, 6);

        float pw = 82, px = x + w / 2 - pw / 2, py = by + pad;
        a.panelSmall(b, px, py, pw, priceH, afford ? Pal.mix(Pal.GOLD, Pal.PANEL_LO, 0.32f) : Pal.PANEL, 1f);
        a.textBoxC(b, a.bodyB, "$" + price, px, py, pw, priceH, afford ? Pal.GOLD : Pal.DIMMER, 1f, 1f, 6);

        if (hv > 0.5f) {
            g.ui.tipWidth(290);
            g.ui.tip("Excise",
                "Strips the two lowest unmodified cards out of your deck (" + run.deck.size()
                    + " cards). A thinner deck draws its good cards far more often."
                    + (afford ? "" : "\n\nNot available right now."), Pal.STEEL);
            if (g.ui.justUp && afford) {
                excise(2);
                run.gold -= price;
                stock.excised++;
                toast("Deck is now " + run.deck.size() + " cards.");
                g.juice.burst(g.ui.mouse.x, g.ui.mouse.y, 18, Pal.STEEL, 220, 6);
            }
        }
    }

    private void excise(int n) {
        Run run = g.run;
        List<Card> plain = new ArrayList<>();
        for (Card c : run.deck)
            if (c.enh == Card.Enhance.NONE && c.ed == Card.Edition.NONE && c.seal == Card.Seal.NONE)
                plain.add(c);
        plain.sort(new Comparator<Card>() {
            @Override public int compare(Card p, Card q) { return p.rank.order - q.rank.order; }
        });
        for (int i = 0; i < n && i < plain.size() && run.deck.size() > 20; i++) run.deck.remove(plain.get(i));
    }
}
