package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.game.cards.Card;
import com.mygdx.game.gfx.*;
import com.mygdx.game.run.Run;
import com.mygdx.game.screens.*;
import com.mygdx.game.world.WorldMap;

/** ANTEVAULT - a poker roguelite. Application entry point and global services. */
public class Main extends ApplicationAdapter {

    public static final float VW = 1280, VH = 720;
    public static Main I;

    public SpriteBatch batch;
    public OrthographicCamera cam;
    public FitViewport vp;
    public Art art;
    public UI ui;
    public Juice juice;
    public PostFx post;
    public Cine cine;
    public Backdrop backdrop;
    public com.mygdx.game.screens.InfoOverlay info;

    public Run run;
    public WorldMap map;

    public GameScreen screen;
    private GameScreen next;
    public float time;
    public boolean vsync = true;
    public float fade = 1f;
    private float fadeDir = -1;

    private String hint;
    private float hintT;

    /** Where recovered screen failures are reported. Platforms replace this (desktop writes a log file). */
    public interface ErrorSink { void report(String where, Throwable t); }

    public ErrorSink errors = new ErrorSink() {
        @Override public void report(String where, Throwable t) { Gdx.app.error("antevault", where, t); }
    };

    /** Shown when a screen threw and the game recovered instead of dying. */
    private String errorBanner;
    private float errorT;

    /**
     * Work deferred to after the frame is submitted.
     *
     * Changing the window mode makes GLFW fire its resize callback synchronously, which
     * re-enters render() - so anything that resizes the window has to run outside the batch.
     */
    private final com.badlogic.gdx.utils.Array<Runnable> postFrame = new com.badlogic.gdx.utils.Array<>();

    private final float[][] blobs = new float[7][6];
    /** Parallax motes: x, y, speed, size, depth, phase. */
    private final float[][] motes = new float[70][6];
    private final Color tmp = new Color();

    @Override
    public void create() {
        I = this;
        batch = new SpriteBatch(3000);
        cam = new OrthographicCamera();
        vp = new FitViewport(VW, VH, cam);
        vp.apply(true);
        art = new Art();
        ui = new UI();
        juice = new Juice();
        cine = new Cine();
        backdrop = new Backdrop();
        info = new com.mygdx.game.screens.InfoOverlay();
        post = new PostFx();
        post.cine = cine;

        for (int i = 0; i < blobs.length; i++) {
            blobs[i][0] = MathUtils.random(0, VW);
            blobs[i][1] = MathUtils.random(0, VH);
            blobs[i][2] = MathUtils.random(-10f, 10f);
            blobs[i][3] = MathUtils.random(-7f, 7f);
            blobs[i][4] = MathUtils.random(170f, 470f);
            blobs[i][5] = MathUtils.random(0f, 6.28f);
        }
        for (float[] m : motes) {
            m[4] = MathUtils.random(0.25f, 1f);          // depth: 1 = near
            m[0] = MathUtils.random(-40, VW + 40);
            m[1] = MathUtils.random(-40, VH + 40);
            m[2] = MathUtils.random(6f, 22f) * m[4];     // rise speed
            m[3] = MathUtils.random(1.2f, 3.6f) * m[4];  // radius
            m[5] = MathUtils.random(0f, 6.28f);
        }

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean scrolled(float amountX, float amountY) {
                ui.scrollAccum += amountY;
                return false;
            }

            @Override public boolean touchDown(int x, int y, int pointer, int button) {
                if (pointer == 0 && button == Input.Buttons.LEFT) ui.pressEvent = true;
                return false;
            }

            @Override public boolean touchUp(int x, int y, int pointer, int button) {
                if (pointer == 0 && button == Input.Buttons.LEFT) ui.releaseEvent = true;
                return false;
            }
        });
        set(new TitleScreen());
    }

    public void set(GameScreen s) {
        next = s;
        fadeDir = 1;
        cine.defocus(1.2f);
    }

    private void swap() {
        if (screen != null) screen.exit();
        screen = next;
        next = null;
        cine.reset();
        cine.defocus(1.2f);
        screen.enter();
        fadeDir = -1;
    }

    public void newRun() { newRun(false); }

    public void newRun(boolean tutorial) {
        run = new Run();
        run.tutorial = tutorial;
        if (tutorial) run.tut = new com.mygdx.game.battle.Tutorial();
        map = new WorldMap(TimeUtils.nanoTime());
        run.node = "n0";
        juice.clear();
        set(new MapScreen());
    }

    public void hint(String s) { hint = s; hintT = 2.2f; }

    /** Queues work to run after this frame has been submitted. */
    public void afterFrame(Runnable r) { postFrame.add(r); }

    /** Records a screen failure, tells the player, and returns to safe ground. */
    private void recover(Throwable ex) {
        String where = screen == null ? "?" : screen.getClass().getSimpleName();
        errorBanner = where + ": " + ex;
        errorT = 8f;
        errors.report(where, ex);
        // back out to somewhere that is known to render
        afterFrame(new Runnable() {
            @Override public void run() {
                if (run != null && map != null) set(new MapScreen());
                else set(new TitleScreen());
            }
        });
    }

    @Override
    public void render() {
        float raw = Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f);
        cine.update(raw);
        // hit-stop and slow motion: game logic crawls, the director keeps full speed
        float dt = raw * cine.timeScale();
        time += raw;
        hintT = Math.max(0, hintT - raw);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) hint(post.cycle());
        if (run != null && Gdx.input.isKeyJustPressed(Input.Keys.I)) info.toggle();
        if (info.open && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) info.open = false;

        // picking always uses the un-shaken, un-zoomed camera so aiming never drifts
        cam.zoom = 1f;
        cam.position.set(VW / 2f, VH / 2f, 0);
        cam.update();
        vp.apply();
        ui.begin(vp, raw);
        juice.update(dt);

        fade = MathUtils.clamp(fade + fadeDir * raw * 4.5f, 0, 1);
        if (fadeDir > 0 && fade >= 1f && next != null) swap();
        if (fade > 0.02f) cine.defocus(fade * 1.4f);

        // clear the whole window first so the letterbox bars are a clean colour
        ScreenUtils.clear(Pal.BG0.r, Pal.BG0.g, Pal.BG0.b, 1f);
        boolean captured = post.captureBegin(vp);

        cam.zoom = 1f - cine.zoom;
        // cinematic camera push toward whatever the director is looking at
        cam.position.set(VW / 2f + juice.shakeX + cine.pushX,
            VH / 2f + juice.shakeY + cine.pushY, 0);
        cam.update();
        batch.setProjectionMatrix(cam.combined);
        batch.enableBlending();
        batch.begin();

        if (screen == null || !screen.customBackground()) drawBackground(raw);
        // A failure in one screen should not take the whole game down with no explanation.
        // Log it, say so on screen, and fall back to somewhere safe.
        try {
            // while the info panel is up, park the pointer off screen so nothing underneath
            // reacts to hovers or clicks meant for the overlay
            float mx = ui.mouse.x, my = ui.mouse.y;
            if (info.open) ui.mouse.set(-9999, -9999);
            if (screen != null) screen.draw(batch, dt);
            ui.mouse.set(mx, my);
            info.draw(batch, raw);
        } catch (Throwable ex) {
            recover(ex);
        }

        juice.draw(batch, art);
        juice.drawFloaters(batch, art);
        ui.drawTip(batch, art, VW, VH);

        // full screen colour flash from the director
        if (cine.flash > 0.002f) {
            batch.setColor(cine.flashCol.r, cine.flashCol.g, cine.flashCol.b, cine.flash);
            batch.draw(art.white, -90, -90, VW + 180, VH + 180);
            batch.setColor(Color.WHITE);
        }
        // cinematic: anticipation speed lines converging on a point
        if (cine.lines > 0.01f) drawSpeedLines(cine.linesX * VW, cine.linesY * VH, cine.lines);
        // cinematic: title card wipe
        if (cine.card > 0.01f) drawTitleCard();
        // cinematic letterbox bars
        if (cine.letterbox > 0.005f) {
            float bar = 54f * cine.letterbox;
            art.rect(batch, -90, VH - bar, VW + 180, bar + 90, Color.BLACK, 0.92f);
            art.rect(batch, -90, -90, VW + 180, bar + 90, Color.BLACK, 0.92f);
        }

        errorT = Math.max(0, errorT - raw);
        if (errorT > 0 && errorBanner != null) {
            float bh = 46;
            art.panel(batch, 20, VH / 2f - bh / 2f, VW - 40, bh, Pal.PANEL_LO, 0.96f);
            art.outline(batch, 20, VH / 2f - bh / 2f, VW - 40, bh, Pal.RED, 0.9f);
            art.textBoxC(batch, art.small, "Something went wrong and the game recovered.",
                20, VH / 2f - bh / 2f, VW - 40, bh, Pal.INK, 1f, 1f, 20);
        }
        if (hintT > 0) {
            float a = Math.min(1, hintT * 2.5f);
            float wpx = art.textW(art.small, hint) + 30;
            art.panelSmall(batch, VW - 20 - wpx, VH - 36, wpx, 26, Pal.PANEL_LO, 0.9f * a);
            art.textC(batch, art.small, hint, VW - 20 - wpx / 2, VH - 17, Pal.DIM, a);
        }
        if (fade > 0.001f) {
            batch.setColor(0, 0, 0, fade);
            batch.draw(art.white, -90, -90, VW + 180, VH + 180);
            batch.setColor(Color.WHITE);
        }
        batch.end();

        if (captured) post.renderToScreen(raw);

        // safe point to resize the window, swap display mode, and so on
        if (postFrame.size > 0) {
            com.badlogic.gdx.utils.Array<Runnable> pending =
                new com.badlogic.gdx.utils.Array<>(postFrame);
            postFrame.clear();
            for (Runnable r : pending) r.run();
        }
    }

    /** Streaks converging on a point, drawn behind the UI for anticipation. */
    private void drawSpeedLines(float cx, float cy, float amount) {
        int n = 34;
        for (int i = 0; i < n; i++) {
            float a = i / (float) n * MathUtils.PI2 + time * 0.6f;
            float seed = (i * 0.618f) % 1f;
            float inner = 190f + seed * 240f - amount * 130f;
            float outer = inner + 90f + seed * 170f;
            float alpha = amount * (0.10f + 0.18f * seed);
            float ca = MathUtils.cos(a), sa = MathUtils.sin(a);
            art.line(batch, cx + ca * inner, cy + sa * inner,
                cx + ca * outer, cy + sa * outer, 2.2f + seed * 2.4f, Pal.GOLD, alpha);
        }
    }

    /** Diagonal wipe that announces a new seal. */
    private void drawTitleCard() {
        float p = 1f - cine.card;                       // 0 -> 1 over the card's life
        float ease = p < 0.5f ? p * 2f : 1f;
        float fade = cine.card < 0.35f ? cine.card / 0.35f : 1f;
        float h = 118;
        float y = VH / 2f - h / 2f;
        float w = VW * ease;
        art.rect(batch, 0, y, w, h, Pal.PANEL_LO, 0.90f * fade);
        art.rect(batch, 0, y + h - 3, w, 3, Pal.GOLD, 0.85f * fade);
        art.rect(batch, 0, y, w, 3, Pal.GOLD, 0.85f * fade);
        if (ease > 0.55f && cine.cardTitle != null) {
            float k = Math.min(1, (ease - 0.55f) / 0.35f) * fade;
            art.textBoxC(batch, art.title, cine.cardTitle, 0, y + 46, VW, 58, Pal.GOLD, k, 0.62f, 40);
            if (cine.cardSub != null)
                art.textBoxC(batch, art.body, cine.cardSub, 0, y + 16, VW, 26, Pal.DIM, k, 1f, 40);
        }
    }

    public void drawBackground(float dt) {
        int region = run != null ? Math.max(1, Math.min(5, run.ante)) : 1;
        backdrop.draw(batch, art, region, time, dt, VW, VH, time * 5f, 1f);

        for (float[] bl : blobs) {
            bl[0] += bl[2] * dt;
            bl[1] += bl[3] * dt;
            if (bl[0] < -bl[4]) bl[0] = VW + bl[4];
            if (bl[0] > VW + bl[4]) bl[0] = -bl[4];
            if (bl[1] < -bl[4]) bl[1] = VH + bl[4];
            if (bl[1] > VH + bl[4]) bl[1] = -bl[4];
            float pulse = 0.5f + 0.5f * MathUtils.sin(time * 0.5f + bl[5]);
            Pal.mix(tmp, Pal.region(region), Pal.PURPLE, pulse);
            art.halo(batch, bl[0], bl[1], bl[4], tmp, 0.045f + pulse * 0.025f);
        }

        // drifting suit watermarks, far layer
        for (int i = 0; i < 8; i++) {
            float t = time * 0.045f + i * 0.618f;
            float x = ((t % 1f) * (VW + 240)) - 120;
            float y = (VH * ((i * 0.191f) % 1f)) + MathUtils.sin(time * 0.4f + i) * 24;
            Card.Suit s = Card.Suit.values()[i % 4];
            CardArt.pip(batch, art, s, x, y, 46, Pal.PANEL_HI, 0.24f);
        }

        // parallax embers rising through three depth layers
        for (float[] m : motes) {
            m[1] += m[2] * dt;
            m[0] += MathUtils.sin(time * 0.6f + m[5]) * 8f * dt;
            if (m[1] > VH + 30) { m[1] = -30; m[0] = MathUtils.random(-40, VW + 40); }
            float tw = 0.45f + 0.55f * MathUtils.sin(time * 2.1f + m[5]);
            Pal.mix(tmp, Pal.ORANGE, Pal.GOLD, m[4]);
            art.halo(batch, m[0], m[1], m[3] * 6f, tmp, 0.06f * m[4] * tw);
            art.disc(batch, m[0], m[1], m[3], tmp, (0.16f + 0.26f * m[4]) * tw);
        }
    }

    @Override
    public void resize(int width, int height) {
        vp.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        art.dispose();
        post.dispose();
    }
}
