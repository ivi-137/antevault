package com.mygdx.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * Screen shake, particles and floating numbers.
 *
 * Particles are one pooled struct with a {@code kind} discriminator rather than a class per
 * effect, so emitters stay cheap and a heavy frame never allocates.
 */
public class Juice {

    public static final int SPARK = 0, SQUARE = 1, RING = 2,
        CONFETTI = 3, EMBER = 4, SHARD = 5, SMOKE = 6, COMET = 7, ORBIT = 8, STAR = 9;

    public static class P implements Pool.Poolable {
        public float x, y, vx, vy, life, max, size, rot, spin, grav, drag;
        /** Homing / orbit target, used by imploding and spiralling particles. */
        public float tx, ty, pull, orbitR, orbitA, orbitSpd;
        public float px, py;          // previous position, for comet tails
        public int kind, variant;
        public boolean additive;
        public final Color col = new Color();
        @Override public void reset() { life = 0; pull = 0; additive = false; variant = 0; }
    }

    public static class F implements Pool.Poolable {
        public float x, y, vy, life, max, scale;
        public String text;
        public final Color col = new Color();
        public boolean bigFont;
        @Override public void reset() { life = 0; text = null; }
    }

    private final Pool<P> pPool = new Pool<P>(512) { @Override protected P newObject() { return new P(); } };
    private final Pool<F> fPool = new Pool<F>(64) { @Override protected F newObject() { return new F(); } };
    private final Array<P> parts = new Array<>(false, 1024);
    private final Array<F> floats = new Array<>(false, 64);

    private float shake;
    public float shakeX, shakeY;
    public boolean shakeEnabled = true;

    public void shake(float amount) {
        if (!shakeEnabled) return;
        shake = Math.max(shake, amount);
    }

    // ---------------------------------------------------------------- emitters

    public P part() {
        P p = pPool.obtain();
        p.life = p.max = 0.6f; p.size = 5; p.rot = 0; p.spin = 0; p.grav = 0; p.drag = 0.9f;
        p.kind = SQUARE; p.col.set(Color.WHITE);
        p.vx = p.vy = 0; p.pull = 0; p.additive = false;
        parts.add(p);
        return p;
    }

    /** Classic outward burst of sparks and chips. */
    public void burst(float x, float y, int n, Color c, float speed, float size) {
        for (int i = 0; i < n; i++) {
            P p = part();
            p.x = p.px = x; p.y = p.py = y;
            float a = MathUtils.random(MathUtils.PI2);
            float s = speed * MathUtils.random(0.35f, 1.2f);
            p.vx = MathUtils.cos(a) * s; p.vy = MathUtils.sin(a) * s;
            p.life = p.max = MathUtils.random(0.35f, 0.8f);
            p.size = size * MathUtils.random(0.5f, 1.3f);
            p.spin = MathUtils.random(-420f, 420f);
            p.grav = -420f; p.drag = 0.90f;
            p.col.set(c);
            p.kind = MathUtils.randomBoolean(0.6f) ? SQUARE : SPARK;
        }
    }

    public void ring(float x, float y, Color c, float size) {
        P p = part();
        p.x = x; p.y = y;
        p.life = p.max = 0.35f; p.size = size; p.kind = RING; p.col.set(c); p.drag = 1f;
    }

    /** Paper confetti: flat strips that flutter and tumble as they fall. */
    public void confetti(float x, float y, int n, Color a, Color b, float speed) {
        for (int i = 0; i < n; i++) {
            P p = part();
            p.x = x; p.y = y;
            float ang = MathUtils.random(MathUtils.PI * 0.15f, MathUtils.PI * 0.85f);
            float s = speed * MathUtils.random(0.6f, 1.4f);
            p.vx = MathUtils.cos(ang) * s;
            p.vy = MathUtils.sin(ang) * s;
            p.life = p.max = MathUtils.random(1.4f, 2.6f);
            p.size = MathUtils.random(5f, 11f);
            p.spin = MathUtils.random(-720f, 720f);
            p.grav = -300f; p.drag = 0.985f;
            p.kind = CONFETTI;
            p.variant = MathUtils.random(0, 1000);
            p.col.set(MathUtils.randomBoolean() ? a : b);
        }
    }

    /** Rising embers that flicker and drift, drawn additively so they read as light. */
    public void embers(float x, float y, int n, Color c, float spread) {
        for (int i = 0; i < n; i++) {
            P p = part();
            p.x = x + MathUtils.random(-spread, spread);
            p.y = y + MathUtils.random(-spread * 0.4f, spread * 0.4f);
            p.vx = MathUtils.random(-18f, 18f);
            p.vy = MathUtils.random(34f, 96f);
            p.life = p.max = MathUtils.random(0.9f, 2.0f);
            p.size = MathUtils.random(1.6f, 3.8f);
            p.grav = 26f; p.drag = 0.99f;
            p.kind = EMBER; p.additive = true;
            p.variant = MathUtils.random(0, 1000);
            p.col.set(c);
        }
    }

    /** Glass shatter: angular shards that spin fast and fall hard. */
    public void shatter(float x, float y, int n, Color c, float speed) {
        for (int i = 0; i < n; i++) {
            P p = part();
            p.x = x; p.y = y;
            float a = MathUtils.random(MathUtils.PI2);
            float s = speed * MathUtils.random(0.4f, 1.3f);
            p.vx = MathUtils.cos(a) * s; p.vy = MathUtils.sin(a) * s;
            p.life = p.max = MathUtils.random(0.6f, 1.2f);
            p.size = MathUtils.random(4f, 10f);
            p.spin = MathUtils.random(-900f, 900f);
            p.grav = -900f; p.drag = 0.97f;
            p.kind = SHARD;
            p.col.set(c);
        }
    }

    /** Soft expanding puffs, good under impacts and card slams. */
    public void smoke(float x, float y, int n, Color c, float speed, float size) {
        for (int i = 0; i < n; i++) {
            P p = part();
            p.x = x + MathUtils.random(-6f, 6f);
            p.y = y + MathUtils.random(-6f, 6f);
            float a = MathUtils.random(MathUtils.PI2);
            p.vx = MathUtils.cos(a) * speed * MathUtils.random(0.2f, 1f);
            p.vy = MathUtils.sin(a) * speed * MathUtils.random(0.2f, 1f) + 12f;
            p.life = p.max = MathUtils.random(0.7f, 1.5f);
            p.size = size * MathUtils.random(0.7f, 1.6f);
            p.drag = 0.93f; p.grav = 8f;
            p.kind = SMOKE;
            p.col.set(c);
        }
    }

    /** A comet that streaks from a point, leaving a fading tail behind it. */
    public void comet(float x, float y, float vx, float vy, Color c, float size, float life) {
        P p = part();
        p.x = p.px = x; p.y = p.py = y;
        p.vx = vx; p.vy = vy;
        p.life = p.max = life;
        p.size = size; p.drag = 0.97f; p.grav = 0;
        p.kind = COMET; p.additive = true;
        p.col.set(c);
    }

    /**
     * Particles that fly out from a source then home into a target. Used to show chips being
     * drawn into the score readout, so the number visibly eats what the card produced.
     */
    public void implode(float fromX, float fromY, float toX, float toY, int n, Color c, float size) {
        for (int i = 0; i < n; i++) {
            P p = part();
            p.x = fromX + MathUtils.random(-14f, 14f);
            p.y = fromY + MathUtils.random(-14f, 14f);
            float a = MathUtils.random(MathUtils.PI2);
            float s = MathUtils.random(90f, 210f);
            p.vx = MathUtils.cos(a) * s;
            p.vy = MathUtils.sin(a) * s;
            p.tx = toX; p.ty = toY;
            p.pull = MathUtils.random(9f, 15f);
            p.life = p.max = MathUtils.random(0.45f, 0.8f);
            p.size = size * MathUtils.random(0.6f, 1.2f);
            p.drag = 0.97f;
            p.kind = STAR; p.additive = true;
            p.col.set(c);
        }
    }

    /** Sparks that spiral outward around a point. */
    public void orbit(float x, float y, int n, Color c, float radius, float size) {
        for (int i = 0; i < n; i++) {
            P p = part();
            p.tx = x; p.ty = y;
            p.orbitA = i / (float) n * MathUtils.PI2;
            p.orbitR = radius * MathUtils.random(0.5f, 1f);
            p.orbitSpd = MathUtils.random(1.6f, 3.4f) * (MathUtils.randomBoolean() ? 1 : -1);
            p.life = p.max = MathUtils.random(0.8f, 1.4f);
            p.size = size;
            p.kind = ORBIT; p.additive = true;
            p.col.set(c);
        }
    }

    public void floater(float x, float y, String text, Color c, boolean bigFont) {
        F f = fPool.obtain();
        f.x = x; f.y = y; f.vy = 62f; f.life = f.max = 1.0f; f.scale = 1f;
        f.text = text; f.col.set(c); f.bigFont = bigFont;
        floats.add(f);
    }

    // ---------------------------------------------------------------- update

    public void update(float dt) {
        if (shake > 0) {
            shake = Math.max(0, shake - dt * (6f + shake * 3f));
            shakeX = MathUtils.random(-shake, shake);
            shakeY = MathUtils.random(-shake, shake);
        } else {
            shakeX = shakeY = 0;
        }

        for (int i = parts.size - 1; i >= 0; i--) {
            P p = parts.get(i);
            p.life -= dt;
            if (p.life <= 0) { parts.removeIndex(i); pPool.free(p); continue; }
            p.px = p.x; p.py = p.y;

            if (p.kind == ORBIT) {
                p.orbitA += p.orbitSpd * dt;
                float t = 1f - p.life / p.max;
                float r = p.orbitR * (0.3f + t * 1.1f);
                p.x = p.tx + MathUtils.cos(p.orbitA) * r;
                p.y = p.ty + MathUtils.sin(p.orbitA) * r * 0.6f;
                continue;
            }

            if (p.pull > 0) {
                // ease toward the target, accelerating as it closes
                p.vx += (p.tx - p.x) * p.pull * dt;
                p.vy += (p.ty - p.y) * p.pull * dt;
            }
            p.vy += p.grav * dt;
            float d = (float) Math.pow(p.drag, dt * 60f);
            p.vx *= d; p.vy *= d;
            p.x += p.vx * dt; p.y += p.vy * dt;
            p.rot += p.spin * dt;
        }

        for (int i = floats.size - 1; i >= 0; i--) {
            F f = floats.get(i);
            f.life -= dt;
            if (f.life <= 0) { floats.removeIndex(i); fPool.free(f); continue; }
            f.y += f.vy * dt;
            f.vy *= (float) Math.pow(0.93f, dt * 60f);
        }
    }

    // ---------------------------------------------------------------- draw

    public void draw(Batch b, Art art) {
        int srcFunc = b.getBlendSrcFunc(), dstFunc = b.getBlendDstFunc();
        boolean additiveOn = false;

        for (int i = 0; i < parts.size; i++) {
            P p = parts.get(i);
            float t = p.life / p.max;
            float a = Math.min(1, t > 0.75f ? (1 - t) * 4f : t / 0.75f);

            if (p.additive != additiveOn) {
                b.flush();
                if (p.additive) b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                else b.setBlendFunction(srcFunc, dstFunc);
                additiveOn = p.additive;
            }

            switch (p.kind) {
                case RING:
                    art.ring(b, p.x, p.y, p.size * (1.4f - t) * 1.6f, p.col, a * 0.9f);
                    break;
                case SPARK: {
                    float len = p.size * (0.6f + t * 2.2f);
                    float ang = (float) Math.toDegrees(Math.atan2(p.vy, p.vx));
                    b.setColor(p.col.r, p.col.g, p.col.b, a);
                    b.draw(art.white, p.x, p.y - p.size * 0.16f, 0, p.size * 0.16f,
                        len, p.size * 0.32f, 1, 1, ang);
                    b.setColor(Color.WHITE);
                    break;
                }
                case CONFETTI: {
                    // flutter: the strip narrows and widens as it tumbles
                    float flutter = MathUtils.sin((p.max - p.life) * 9f + p.variant);
                    float w = p.size * (0.25f + 0.75f * Math.abs(flutter));
                    b.setColor(p.col.r, p.col.g, p.col.b, a);
                    b.draw(art.white, p.x - w / 2, p.y - p.size / 2, w / 2, p.size / 2,
                        w, p.size * 0.62f, 1, 1, p.rot);
                    b.setColor(Color.WHITE);
                    break;
                }
                case EMBER: {
                    float flick = 0.55f + 0.45f * MathUtils.sin((p.max - p.life) * 14f + p.variant);
                    art.halo(b, p.x, p.y, p.size * 4.5f, p.col, a * 0.28f * flick);
                    art.disc(b, p.x, p.y, p.size * (0.5f + t * 0.6f), p.col, a * flick);
                    break;
                }
                case SHARD: {
                    float w = p.size, h = p.size * 0.38f;
                    b.setColor(p.col.r, p.col.g, p.col.b, a);
                    b.draw(art.white, p.x - w / 2, p.y - h / 2, w / 2, h / 2, w, h, 1, 1, p.rot);
                    b.draw(art.white, p.x - h / 2, p.y - w / 2, h / 2, w / 2, h, w, 1, 1, p.rot);
                    b.setColor(Color.WHITE);
                    break;
                }
                case SMOKE:
                    art.halo(b, p.x, p.y, p.size * (0.6f + (1 - t) * 2.4f), p.col, a * 0.30f);
                    break;
                case COMET: {
                    float dx = p.x - p.px, dy = p.y - p.py;
                    float len = (float) Math.sqrt(dx * dx + dy * dy) * 3.2f + p.size;
                    float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
                    b.setColor(p.col.r, p.col.g, p.col.b, a * 0.55f);
                    b.draw(art.white, p.x, p.y - p.size * 0.25f, 0, p.size * 0.25f,
                        -len, p.size * 0.5f, 1, 1, ang);
                    b.setColor(Color.WHITE);
                    art.halo(b, p.x, p.y, p.size * 3.2f, p.col, a * 0.5f);
                    art.disc(b, p.x, p.y, p.size * 0.6f, p.col, a);
                    break;
                }
                case ORBIT:
                case STAR: {
                    float s = p.size * (0.5f + t * 0.9f);
                    b.setColor(p.col.r, p.col.g, p.col.b, a);
                    b.draw(art.star, p.x - s, p.y - s, s * 2, s * 2);
                    b.setColor(Color.WHITE);
                    break;
                }
                default: {
                    float s = p.size * (0.35f + t * 0.9f);
                    b.setColor(p.col.r, p.col.g, p.col.b, a);
                    b.draw(art.white, p.x - s / 2, p.y - s / 2, s / 2, s / 2, s, s, 1, 1, p.rot);
                    b.setColor(Color.WHITE);
                }
            }
        }
        if (additiveOn) {
            b.flush();
            b.setBlendFunction(srcFunc, dstFunc);
        }
    }

    public void drawFloaters(Batch b, Art art) {
        for (int i = 0; i < floats.size; i++) {
            F f = floats.get(i);
            float t = f.life / f.max;
            float a = t > 0.8f ? (1 - t) * 5f : Math.min(1, t * 2.4f);
            BitmapFont font = f.bigFont ? art.numBig : art.num;
            float pop = 1f + (1f - Math.min(1f, (f.max - f.life) * 7f)) * 0.45f;
            art.textScaled(b, font, f.text, f.x, f.y, f.col, a, pop * (f.bigFont ? 0.8f : 0.62f));
        }
    }

    public void clear() {
        for (P p : parts) pPool.free(p);
        parts.clear();
        for (F f : floats) fPool.free(f);
        floats.clear();
        shake = 0; shakeX = shakeY = 0;
    }
}
