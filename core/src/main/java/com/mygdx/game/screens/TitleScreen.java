package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.game.GameScreen;
import com.mygdx.game.Main;
import com.mygdx.game.cards.Card;
import com.mygdx.game.gfx.*;
import com.mygdx.game.i18n.I18n;

public class TitleScreen implements GameScreen {

    private final Main g = Main.I;
    private static final String CREDITS =
        "Built with libGDX  ·  Cinzel and Barlow fonts (SIL OFL)  ·  Icons: Font Awesome Free (CC BY 4.0)";

    private boolean help;
    private float t;

    private static final String[] RULES = {
        "PLAY POKER HANDS TO BREAK SEALS.",
        "Every seal asks for a score. Select up to five cards and play them: the hand's Chips are",
        "multiplied by its Mult and added to your total. Beat the number before your hands run out.",
        "",
        "THE HAND IS THE ENGINE.",
        "Only the cards that form the hand score. Enhancements, editions and wax seals change what",
        "each card is worth, and hand levels raise the base Chips and Mult of a whole hand type.",
        "",
        "ITEMS ARE YOUR BUILD.",
        "Five slots, bought with gold at the Bazaar. They rewrite scoring: extra Mult, retriggers,",
        "shorter flushes, payouts. Sell one to make room. Interest pays $1 per $5 banked, up to $5.",
        "",
        "SIGILS ARE KEYS AND POWERS.",
        "Each Warden you break grants a Sigil. Inside a seal it is a power with charges: draw extra",
        "cards, redraw your hand, retrigger everything, or double your Mult. On the world map the",
        "same Sigil opens one class of sealed door.",
        "",
        "THE MAP LOCKS ITSELF, THEN HANDS YOU THE KEY.",
        "The world is one connected graph you walk in both directions. Doors you could not open on",
        "the way down open on the way back, so go back for what you left behind.",
        "",
        "F1 cycles the post-processing.   Esc opens the menu."
    };

    @Override
    public void draw(SpriteBatch b, float dt) {
        t += dt;
        Art a = g.art;
        UI ui = g.ui;
        float cx = Main.VW / 2f;

        for (int i = 0; i < 5; i++) {
            float ph = t * 0.7f + i * 1.15f;
            float x = cx - 246 + i * 123 + MathUtils.sin(ph) * 7;
            float y = 356 + MathUtils.sin(ph * 0.8f + i) * 15;
            float rot = MathUtils.sin(ph * 0.55f + i * 2) * 7;
            Card c = DEMO[i];
            CardArt.draw(b, a, c, x - CardArt.W / 2, y, CardArt.W * 1.12f, CardArt.H * 1.12f,
                rot, false, false, 0, c.suit.color,
                0.30f + 0.22f * MathUtils.sin(t * 2 + i), 1f, t);
        }

        a.textBoxC(b, a.title, "ANTEVAULT", cx - 460, 588, 920, 88, Pal.GOLD, 1f, 1f, 20);
        a.textBoxC(b, a.body, I18n.t("title.tagline"),
            cx - 400, 560, 800, 24, Pal.DIM, 1f, 1f, 10);

        if (!help) {
            if (ui.button(b, a, "start", I18n.t("menu.newrun"), cx - 140, 262, 280, 54, Pal.PURPLE, true, dt).clicked
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                g.newRun();
            }
            if (ui.button(b, a, "tut", I18n.t("menu.tutorial"), cx - 140, 206, 280, 48,
                Pal.mix(Pal.GOLD, Pal.PANEL, 0.45f), true, dt).clicked) {
                g.newRun(true);
            }
            if (ui.button(b, a, "help", I18n.t("menu.howtoplay"), cx - 140, 154, 280, 44,
                Pal.PANEL_HI, true, dt).clicked) {
                help = true;
            }
            if (ui.button(b, a, "settings", I18n.t("menu.settings"), cx - 140, 104, 280, 44, Pal.PANEL_HI, true, dt).clicked) {
                g.set(new SettingsScreen(new TitleScreen()));
            }
            if (ui.button(b, a, "quit", I18n.t("menu.quit"), cx - 140, 54, 280, 44, Pal.PANEL, true, dt).clicked) {
                Gdx.app.exit();
            }
            a.textBoxC(b, a.small, I18n.t("title.footer"),
                cx - 360, 40, 720, 20, Pal.DIMMER, 1f, 1f, 10);
            // credit is owed wherever these are shown, not only in the repository
            a.textBoxC(b, a.small, CREDITS, cx - 400, 14, 800, 18, Pal.DIMMER, 0.7f, 0.9f, 10);
        } else {
            a.shadow(b, 84, 54, Main.VW - 168, 486, 0.55f, 18);
            a.panel(b, 84, 54, Main.VW - 168, 486, Pal.PANEL_LO, 0.97f);
            a.outline(b, 84, 54, Main.VW - 168, 486, Pal.LINE, 0.8f);
            float y = 508;
            for (String line : RULES) {
                boolean headline = line.length() > 0 && line.equals(line.toUpperCase());
                a.text(b, headline ? a.bodyB : a.body, line, 116, y, headline ? Pal.GOLD : Pal.INK);
                y -= headline ? 24 : 19;
            }
            if (ui.button(b, a, "back", I18n.t("menu.back"), cx - 84, 66, 168, 42, Pal.PANEL_HI, true, dt).clicked
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                help = false;
            }
        }
    }

    private static final Card[] DEMO = {
        card(Card.Suit.SPADES, Card.Rank.ACE, Card.Enhance.NONE, Card.Edition.POLY),
        card(Card.Suit.HEARTS, Card.Rank.KING, Card.Enhance.MULT, Card.Edition.NONE),
        card(Card.Suit.CLUBS, Card.Rank.TEN, Card.Enhance.NONE, Card.Edition.FOIL),
        card(Card.Suit.DIAMONDS, Card.Rank.SEVEN, Card.Enhance.GLASS, Card.Edition.NONE),
        card(Card.Suit.SPADES, Card.Rank.QUEEN, Card.Enhance.NONE, Card.Edition.NONE),
    };

    private static Card card(Card.Suit s, Card.Rank r, Card.Enhance e, Card.Edition ed) {
        Card c = new Card(s, r);
        c.enh = e; c.ed = ed;
        return c;
    }
}
