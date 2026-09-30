package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.game.GameScreen;
import com.mygdx.game.Main;
import com.mygdx.game.battle.Blind;
import com.mygdx.game.gfx.Art;
import com.mygdx.game.gfx.Icon;
import com.mygdx.game.gfx.Pal;
import com.mygdx.game.gfx.UI;
import com.mygdx.game.i18n.I18n;
import com.mygdx.game.run.Run;
import com.mygdx.game.run.Sigil;
import com.mygdx.game.world.WorldMap;

import java.util.List;

/** The interconnected world graph. Walk in any direction; sealed edges need the right Sigil. */
public class MapScreen implements GameScreen {

    private final Main g = Main.I;
    private float camX, camY, zoom = 0.62f;
    private float targetX, targetY;
    private float t;
    private boolean dragging;
    private float dragSX, dragSY, dragCX, dragCY, dragDist;

    private WorldMap.Node travelFrom, travelTo;
    private float travelT;
    private boolean menu;

    private static final float MAP_Y0 = 84, MAP_Y1 = 630;
    /** The camera looks this far ahead of the player, so the road upward stays in view. */
    private static final float LEAD = 170;

    @Override
    public void enter() {
        WorldMap.Node n = g.map.get(g.run.node);
        camX = targetX = n.x;
        camY = targetY = n.y + LEAD;
        g.run.ante = n.ante;
    }

    private float sx(float wx) { return (wx - camX) * zoom + Main.VW / 2f; }
    private float sy(float wy) { return (wy - camY) * zoom + (MAP_Y0 + MAP_Y1) / 2f; }

    @Override
    public void draw(SpriteBatch b, float dt) {
        t += dt;
        Art a = g.art;
        UI ui = g.ui;
        Run run = g.run;
        WorldMap map = g.map;
        WorldMap.Node here = map.get(run.node);

        // ---- camera -------------------------------------------------------
        if (Math.abs(ui.scroll) > 0.01f) zoom = MathUtils.clamp(zoom - ui.scroll * 0.07f, 0.40f, 1.05f);
        if (travelTo == null && !menu) {
            if (ui.justDown && ui.mouse.y > MAP_Y0) {
                dragging = true; dragDist = 0;
                dragSX = ui.mouse.x; dragSY = ui.mouse.y; dragCX = targetX; dragCY = targetY;
            }
            if (dragging && ui.down) {
                float dx = (ui.mouse.x - dragSX) / zoom, dy = (ui.mouse.y - dragSY) / zoom;
                dragDist = Math.max(dragDist, Math.abs(dx) + Math.abs(dy));
                targetX = dragCX - dx; targetY = dragCY - dy;
            }
            if (!ui.down) dragging = false;
        }
        float pad = 260;
        targetX = MathUtils.clamp(targetX, map.minX() - pad, map.maxX() + pad);
        targetY = MathUtils.clamp(targetY, map.minY() - pad, map.maxY() + pad);
        camX = MathUtils.lerp(camX, targetX, Math.min(1, dt * 8f));
        camY = MathUtils.lerp(camY, targetY, Math.min(1, dt * 8f));

        // ---- travel animation ----------------------------------------------
        if (travelTo != null) {
            travelT += dt * 2.6f;
            targetX = MathUtils.lerp(travelFrom.x, travelTo.x, Math.min(1, travelT));
            targetY = MathUtils.lerp(travelFrom.y, travelTo.y, Math.min(1, travelT)) + LEAD;
            if (travelT >= 1f) {
                WorldMap.Node dest = travelTo;
                travelTo = null;
                run.node = dest.id;
                run.ante = dest.ante;
                arrive(dest);
                return;
            }
        }

        // ---- region banners --------------------------------------------------
        drawRegionLabel(b, a, 1, 1130, 450);
        drawRegionLabel(b, a, 2, 1190, 1265);
        drawRegionLabel(b, a, 3, 1330, 2065);
        drawRegionLabel(b, a, 4, 70, 2065);
        drawRegionLabel(b, a, 5, 670, 2760);

        // ---- edges ------------------------------------------------------------
        for (WorldMap.Edge e : map.edges) {
            float x1 = sx(e.a.x), y1 = sy(e.a.y), x2 = sx(e.b.x), y2 = sy(e.b.y);
            boolean open = e.gate == null || run.has(e.gate);
            boolean lit = (e.a == here || e.b == here) && open;
            Color col = e.gate == null ? Pal.LINE : e.gate.color;
            float alpha = open ? (lit ? 0.95f : 0.45f) : 0.55f;
            if (open) {
                a.line(b, x1, y1, x2, y2, lit ? 4.5f : 3f, lit ? Pal.INK : col, alpha);
            } else {
                // sealed: dashed
                int seg = 9;
                for (int i = 0; i < seg; i++) {
                    if (i % 2 == 1) continue;
                    float f0 = i / (float) seg, f1 = (i + 0.85f) / seg;
                    a.line(b, MathUtils.lerp(x1, x2, f0), MathUtils.lerp(y1, y2, f0),
                        MathUtils.lerp(x1, x2, f1), MathUtils.lerp(y1, y2, f1), 3.5f, col, alpha);
                }
            }
            if (e.gate != null) {
                float mx = (x1 + x2) / 2f, my = (y1 + y2) / 2f;
                float pulse = open ? 0.6f : 0.85f + 0.15f * MathUtils.sin(t * 3f);
                a.disc(b, mx, my, 13 * zoom + 6, Pal.PANEL_LO, 0.95f);
                a.ring(b, mx, my, 13 * zoom + 6, e.gate.color, pulse);
                a.icon(b, open ? e.gate.icon : Icon.LOCK, mx, my, 13, open ? e.gate.color : Pal.INK, 1f);
                if (ui.hovering(mx - 14, my - 14, 28, 28)) {
                    ui.tip(I18n.gate(e.gate), open
                        ? I18n.t("map.gateOpen", I18n.sigil(e.gate))
                        : I18n.t("map.gateSealed", I18n.sigil(e.gate)),
                        e.gate.color);
                }
            }
        }

        // ---- nodes -------------------------------------------------------------
        List<WorldMap.Node> adj = map.neighbours(here);
        WorldMap.Node clicked = null;
        for (WorldMap.Node n : map.nodes.values()) {
            float x = sx(n.x), y = sy(n.y);
            if (x < -60 || x > Main.VW + 60 || y < MAP_Y0 - 60 || y > MAP_Y1 + 60) continue;
            boolean cleared = run.cleared.contains(n.id);
            boolean reachable = adj.contains(n) && map.canTravel(run, here, n) && travelTo == null && !menu;
            boolean isHere = n == here;
            float r = 20 * zoom + 10;

            float hv = ui.hover("node" + n.id, x - r, y - r, r * 2, r * 2, dt);
            Color col = n.kind.color;
            if (cleared) col = Pal.mix(col, Pal.PANEL, 0.6f);

            if (reachable) {
                float p = 0.35f + 0.25f * MathUtils.sin(t * 4f);
                a.halo(b, x, y, r * 2.6f, col, 0.30f + p * 0.25f);
                a.ring(b, x, y, r + 7 + MathUtils.sin(t * 4f) * 2, Pal.INK, 0.55f);
            }
            if (isHere) a.halo(b, x, y, r * 3f, Pal.GOLD, 0.35f);

            a.disc(b, x, y, r + hv * 3, Pal.PANEL_LO, 1f);
            a.disc(b, x, y, r - 3 + hv * 3, col, cleared ? 0.55f : 1f);
            a.ring(b, x, y, r + hv * 3, cleared ? Pal.DIMMER : Color.WHITE, cleared ? 0.5f : 0.35f);
            a.icon(b, glyph(n), x, y, r * 1.05f, cleared ? Pal.DIMMER : Pal.PANEL_LO, cleared ? 0.8f : 1f);

            if (cleared) a.textC(b, a.small, I18n.t("map.cleared"), x, y - r - 8, Pal.DIMMER);
            else a.textC(b, a.small, n.kind.label, x, y - r - 8, reachable ? Pal.INK : Pal.DIM);

            if (n.grants != null && !run.has(n.grants)) {
                a.textC(b, a.small, I18n.sigil(n.grants), x, y + r + 16, n.grants.color);
            }

            if (hv > 0.5f) {
                ui.tipWidth(300);
                ui.tip(title(n), describe(n, cleared, reachable, map, run, here), n.kind.color);
                if (reachable && ui.justUp && dragDist < 8) clicked = n;
            }
        }

        // ---- player token ----------------------------------------------------------
        float px, py;
        if (travelTo != null) {
            float f = MathUtils.clamp(travelT, 0, 1);
            f = f * f * (3 - 2 * f);
            px = sx(MathUtils.lerp(travelFrom.x, travelTo.x, f));
            py = sy(MathUtils.lerp(travelFrom.y, travelTo.y, f));
        } else {
            px = sx(here.x); py = sy(here.y);
        }
        py += 34 + MathUtils.sin(t * 3f) * 3;
        a.halo(b, px, py, 26, Pal.GOLD, 0.5f);
        b.setColor(Pal.GOLD);
        b.draw(a.white, px - 8, py - 8, 8, 8, 16, 16, 1, 1, 45 + t * 40);
        b.setColor(Color.WHITE);

        // ---- chrome ----------------------------------------------------------------
        b.setColor(Pal.BG0.r, Pal.BG0.g, Pal.BG0.b, 0.92f);
        b.draw(a.white, 0, 0, Main.VW, MAP_Y0 - 6);
        b.setColor(Color.WHITE);
        Hud.runStrip(b, 10, 8, Main.VW - 20, 62, dt);

        // scrim so the header never fights the nodes underneath it
        for (int i = 0; i < 16; i++) {
            float band = 6f;
            a.rect(b, 0, Main.VH - (i + 1) * band, Main.VW, band, Pal.BG0, 0.96f * (1f - i / 16f));
        }
        a.textBoxC(b, a.big, here.regionName, 0, Main.VH - 46, Main.VW, 40, Pal.GOLD, 0.95f, 1f, 30);
        int left = map.remaining(run);
        a.textBoxC(b, a.small, I18n.t("map.unexplored", left), 0, Main.VH - 66, Main.VW, 18, Pal.DIM, 1f, 1f, 30);

        Hud.relicRack(b, Main.VW - 20 - 5 * 46, MAP_Y1 - 46, 40, 6, dt, false);

        if (g.ui.button(b, a, "mapmenu", I18n.t("map.menu"), 16, Main.VH - 46, 92, 34, Pal.PANEL_HI, true, dt).clicked
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            menu = !menu;
        }

        if (clicked == null && travelTo == null && !menu) clicked = keyPick(here, adj, run, map);

        if (clicked != null) {
            travelFrom = here;
            travelTo = clicked;
            travelT = 0;
        }

        if (menu) drawMenu(b, a, dt);
    }

    /** Number keys walk to the Nth open neighbour, listed bottom-to-top. */
    private WorldMap.Node keyPick(WorldMap.Node here, List<WorldMap.Node> adj, Run run, WorldMap map) {
        List<WorldMap.Node> open = new java.util.ArrayList<>();
        for (WorldMap.Node n : adj) if (map.canTravel(run, here, n)) open.add(n);
        java.util.Collections.sort(open, new java.util.Comparator<WorldMap.Node>() {
            @Override public int compare(WorldMap.Node p, WorldMap.Node q) {
                int d = Float.compare(q.y, p.y);
                return d != 0 ? d : Float.compare(p.x, q.x);
            }
        });
        for (int i = 0; i < open.size() && i < 9; i++)
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) return open.get(i);
        return null;
    }

    private void drawMenu(SpriteBatch b, Art a, float dt) {
        b.setColor(0, 0, 0, 0.72f);
        b.draw(a.white, 0, 0, Main.VW, Main.VH);
        b.setColor(Color.WHITE);
        float w = 340, h = 262, x = (Main.VW - w) / 2, y = (Main.VH - h) / 2;
        a.panel(b, x, y, w, h, Pal.PANEL_LO, 0.98f);
        a.outline(b, x, y, w, h, Pal.LINE, 0.8f);
        a.textBoxC(b, a.big, I18n.t("menu.paused"), x, y + h - 56, w, 44, Pal.INK, 1f, 1f, 20);
        if (g.ui.button(b, a, "mresume", I18n.t("menu.resume"), x + 40, y + 150, w - 80, 42, Pal.PURPLE, true, dt).clicked) menu = false;
        if (g.ui.button(b, a, "msettings", I18n.t("menu.settings"), x + 40, y + 100, w - 80, 42, Pal.PANEL_HI, true, dt).clicked) {
            g.set(new SettingsScreen(new MapScreen()));
        }
        if (g.ui.button(b, a, "mabandon", I18n.t("menu.abandon"), x + 40, y + 50, w - 80, 42, Pal.PANEL_HI, true, dt).clicked) {
            g.set(new TitleScreen());
        }
        if (g.ui.button(b, a, "mquit", I18n.t("menu.quitgame"), x + 40, y + 4, w - 80, 38, Pal.PANEL, true, dt).clicked) {
            Gdx.app.exit();
        }
    }

    private void drawRegionLabel(SpriteBatch b, Art a, int region, float wx, float wy) {
        float x = sx(wx), y = sy(wy);
        if (x < -200 || x > Main.VW + 200) return;
        a.textC(b, a.bodyB, g.map.regionNames[region - 1], x, y, Pal.mix(Pal.LINE, Pal.INK, 0.25f), 0.55f);
    }

    private static char glyph(WorldMap.Node n) {
        switch (n.kind) {
            case START:  return Icon.CAMP;
            case BATTLE: return Icon.SEAL;
            case ELITE:  return Icon.CHAMPION;
            case WARDEN: return Icon.WARDEN;
            case SHOP:   return Icon.SHOP;
            case SHRINE: return Icon.SHRINE;
            case CACHE:  return Icon.CACHE;
            case REST:   return Icon.SANCTUM;
            default:     return Icon.HEART;
        }
    }

    private static String title(WorldMap.Node n) {
        if (n.blind != null) return n.blind.name;
        return n.kind.label;
    }

    private String describe(WorldMap.Node n, boolean cleared, boolean reachable,
                            WorldMap map, Run run, WorldMap.Node here) {
        StringBuilder sb = new StringBuilder();
        Blind e = n.blind;
        if (e != null) {
            sb.append(e.tierLabel()).append("  ·  score ").append(Hud.num(e.target));
            sb.append("\n").append(e.hands).append(" hands, ").append(e.discards).append(" discards");
            sb.append("\nReward $").append(e.reward);
            if (e.mod != com.mygdx.game.battle.BossMod.NONE)
                sb.append("\n\n").append(I18n.boss(e.mod)).append(": ").append(I18n.bossDesc(e.mod));
        } else {
            switch (n.kind) {
                case SHOP:   sb.append("Buy items, codices and cards. Bazaars stay open, so come back richer."); break;
                case SHRINE: sb.append("An old bargain. Something is given, something is taken."); break;
                case CACHE:  sb.append("A sealed cache. Take one thing."); break;
                case REST:   sb.append("A quiet room. Take one permanent upgrade."); break;
                case START:  sb.append("Where you started."); break;
                default:     break;
            }
        }
        if (n.grants != null) sb.append("\n\n").append(I18n.t("map.grants", I18n.sigil(n.grants)));
        if (cleared) sb.append("\n\nAlready cleared. You can walk through freely.");
        else if (!reachable && !map.neighbours(here).contains(n)) sb.append("\n\nNot connected to where you stand.");
        else if (!reachable) {
            WorldMap.Edge ed = map.edgeBetween(here, n);
            if (ed != null && ed.gate != null) sb.append("\n\n").append(I18n.t("map.sealedBy", I18n.gate(ed.gate)));
        }
        return sb.toString();
    }

    /** Runs whatever the node holds. */
    public static void arrive(WorldMap.Node n) {
        Main g = Main.I;
        Run run = g.run;
        if (n.kind == WorldMap.Kind.SHOP) { g.set(new ShopScreen(n)); return; }
        if (run.cleared.contains(n.id)) return;
        switch (n.kind) {
            case BATTLE: case ELITE: case WARDEN: case HEART:
                g.set(new SealScreen(n));
                break;
            case SHRINE: g.set(Content.shrine(n)); break;
            case CACHE:  g.set(Content.cache(n)); break;
            case REST:   g.set(Content.rest(n)); break;
            default:     run.cleared.add(n.id); break;
        }
    }

    /** Called after a node's content resolves. */
    public static void finish(WorldMap.Node n) {
        Main g = Main.I;
        if (n.kind != WorldMap.Kind.SHOP) g.run.cleared.add(n.id);
        g.set(new MapScreen());
    }

}
