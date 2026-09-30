package com.mygdx.game.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Full screen post processing, built on plain FrameBuffers and inline GLSL.
 *
 * Passes: bright threshold -> separable blur (quarter res) -> composite. The composite folds in
 * god rays, a shockwave ripple, a radial punch blur, chromatic aberration, defocus, a vignette
 * and animated grain, all driven by {@link Cine}.
 */
public class PostFx implements Disposable {

    private static final String VERT =
        "attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "void main() {\n" +
        "  v_color = a_color;\n" +
        "  v_color.a = v_color.a * (255.0/254.0);\n" +
        "  v_texCoords = a_texCoord0;\n" +
        "  gl_Position = u_projTrans * a_position;\n" +
        "}\n";

    private static final String FRAG_BRIGHT =
        "#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
        "varying vec4 v_color;\nvarying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform float u_threshold;\n" +
        "void main() {\n" +
        "  vec3 c = texture2D(u_texture, v_texCoords).rgb;\n" +
        "  float l = dot(c, vec3(0.2126, 0.7152, 0.0722));\n" +
        "  float k = smoothstep(u_threshold, u_threshold + 0.30, l);\n" +
        "  gl_FragColor = vec4(c * k, 1.0);\n" +
        "}\n";

    private static final String FRAG_BLUR =
        "#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
        "varying vec4 v_color;\nvarying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform vec2 u_dir;\n" +
        "void main() {\n" +
        "  vec3 sum = texture2D(u_texture, v_texCoords).rgb * 0.227027;\n" +
        "  sum += texture2D(u_texture, v_texCoords + u_dir * 1.3846).rgb * 0.316216;\n" +
        "  sum += texture2D(u_texture, v_texCoords - u_dir * 1.3846).rgb * 0.316216;\n" +
        "  sum += texture2D(u_texture, v_texCoords + u_dir * 3.2308).rgb * 0.070270;\n" +
        "  sum += texture2D(u_texture, v_texCoords - u_dir * 3.2308).rgb * 0.070270;\n" +
        "  gl_FragColor = vec4(sum, 1.0);\n" +
        "}\n";

    private static final String FRAG_COMP =
        "#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
        "varying vec4 v_color;\nvarying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform sampler2D u_bloom;\n" +
        "uniform float u_time;\n" +
        "uniform float u_bloomAmt;\n" +
        "uniform float u_aberration;\n" +
        "uniform float u_vignette;\n" +
        "uniform float u_grain;\n" +
        "uniform float u_scan;\n" +
        "uniform float u_rays;\n" +
        "uniform float u_punch;\n" +
        "uniform float u_defocus;\n" +
        "uniform vec3  u_shock;\n" +      // x, y, radius
        "uniform float u_shockAmt;\n" +
        "uniform float u_haze;\n" +
        "uniform vec3  u_burst;\n" +      // x, y, strength
        "uniform vec4  u_grade;\n" +      // rgb tint, a = amount
        "uniform float u_edge;\n" +
        "uniform vec2  u_res;\n" +
        "float hash(vec2 p) {\n" +
        "  return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);\n" +
        "}\n" +
        "vec3 grab(vec2 uv, float ab) {\n" +
        "  if (ab < 0.0005) return texture2D(u_texture, uv).rgb;\n" +
        "  vec2 d = uv - vec2(0.5);\n" +
        "  vec2 off = d * dot(d, d) * ab;\n" +
        "  return vec3(texture2D(u_texture, uv + off).r,\n" +
        "              texture2D(u_texture, uv).g,\n" +
        "              texture2D(u_texture, uv - off).b);\n" +
        "}\n" +
        "void main() {\n" +
        "  vec2 uv = v_texCoords;\n" +
        // --- 3: shockwave ripple
        "  if (u_shockAmt > 0.0001) {\n" +
        "    vec2 sd = uv - u_shock.xy;\n" +
        "    sd.x *= u_res.x / u_res.y;\n" +
        "    float dist = length(sd);\n" +
        "    float ring = smoothstep(u_shock.z - 0.09, u_shock.z, dist)\n" +
        "               - smoothstep(u_shock.z, u_shock.z + 0.09, dist);\n" +
        "    uv += normalize(sd + vec2(0.0001)) * ring * u_shockAmt;\n" +
        "  }\n" +
        // --- special: heat haze, a travelling shimmer across the frame
        "  if (u_haze > 0.001) {\n" +
        "    uv.x += sin(uv.y * 46.0 + u_time * 7.0) * 0.0024 * u_haze;\n" +
        "    uv.y += cos(uv.x * 38.0 + u_time * 5.5) * 0.0019 * u_haze;\n" +
        "  }\n" +
        // --- 5: chromatic aberration
        "  vec3 col = grab(uv, u_aberration);\n" +
        // --- 7: defocus (cheap 4 tap ring)
        "  if (u_defocus > 0.001) {\n" +
        "    float r = u_defocus * 0.012;\n" +
        "    vec3 blurred = grab(uv + vec2(r, 0.0), u_aberration)\n" +
        "                 + grab(uv - vec2(r, 0.0), u_aberration)\n" +
        "                 + grab(uv + vec2(0.0, r), u_aberration)\n" +
        "                 + grab(uv - vec2(0.0, r), u_aberration);\n" +
        "    col = mix(col, blurred * 0.25, min(1.0, u_defocus));\n" +
        "  }\n" +
        // --- 4: radial punch blur toward the centre
        "  if (u_punch > 0.0005) {\n" +
        "    vec2 dir = vec2(0.5) - uv;\n" +
        "    vec3 acc = col;\n" +
        "    acc += grab(uv + dir * u_punch * 0.25, u_aberration);\n" +
        "    acc += grab(uv + dir * u_punch * 0.50, u_aberration);\n" +
        "    acc += grab(uv + dir * u_punch * 0.75, u_aberration);\n" +
        "    acc += grab(uv + dir * u_punch, u_aberration);\n" +
        "    col = acc * 0.2;\n" +
        "  }\n" +
        // --- 1: bloom
        "  col += texture2D(u_bloom, uv).rgb * u_bloomAmt;\n" +
        // --- 2: volumetric god rays marched through the bright buffer
        "  if (u_rays > 0.001) {\n" +
        "    vec2 origin = vec2(0.5, 1.06);\n" +
        "    vec2 step = (uv - origin) / 14.0;\n" +
        "    vec2 p = uv;\n" +
        "    float decay = 1.0;\n" +
        "    vec3 shaft = vec3(0.0);\n" +
        "    for (int i = 0; i < 14; i++) {\n" +
        "      p -= step;\n" +
        "      shaft += texture2D(u_bloom, p).rgb * decay;\n" +
        "      decay *= 0.90;\n" +
        "    }\n" +
        "    col += shaft * (u_rays / 22.0);\n" +
        "  }\n" +
        // --- special: radial ray burst thrown out from a point
        "  if (u_burst.z > 0.001) {\n" +
        "    vec2 bstep = (uv - u_burst.xy) / 16.0;\n" +
        "    vec2 q = uv;\n" +
        "    float bdec = 1.0;\n" +
        "    vec3 acc2 = vec3(0.0);\n" +
        "    for (int i = 0; i < 16; i++) {\n" +
        "      q -= bstep;\n" +
        "      acc2 += texture2D(u_bloom, q).rgb * bdec;\n" +
        "      bdec *= 0.87;\n" +
        "    }\n" +
        "    col += acc2 * (u_burst.z / 13.0);\n" +
        "  }\n" +
        "  col = col / (col + vec3(1.35)) * 2.35;\n" +
        // --- special: colour grade push toward a hue
        "  if (u_grade.a > 0.001) {\n" +
        "    float glum = dot(col, vec3(0.2126, 0.7152, 0.0722));\n" +
        "    col = mix(col, u_grade.rgb * (0.35 + glum * 1.15), u_grade.a);\n" +
        "  }\n" +
        // --- 6: vignette, scanlines, grain
        "  vec2 vd = v_texCoords - vec2(0.5);\n" +
        "  float vig = smoothstep(0.92, 0.20, dot(vd, vd) * 2.0);\n" +
        "  col *= mix(1.0, vig, u_vignette);\n" +
        // --- special: frame edges ignite
        "  if (u_edge > 0.001) {\n" +
        "    float ex = max(abs(vd.x) * 2.0, abs(vd.y) * 2.0);\n" +
        "    col += u_grade.rgb * pow(clamp(ex, 0.0, 1.0), 6.0) * u_edge * 1.5;\n" +
        "  }\n" +
        "  col *= 1.0 - u_scan * 0.5 * (0.5 + 0.5 * sin(v_texCoords.y * u_res.y * 3.14159));\n" +
        "  col += (hash(v_texCoords * u_res + vec2(u_time * 91.7, u_time * 47.3)) - 0.5) * u_grain;\n" +
        "  gl_FragColor = vec4(col, 1.0);\n" +
        "}\n";

    private FrameBuffer scene, blurA, blurB;
    private ShaderProgram bright, blur, comp;
    private final SpriteBatch fsBatch;
    private final OrthographicCamera fsCam = new OrthographicCamera();
    private final TextureRegion sceneReg = new TextureRegion();
    private final TextureRegion blurReg = new TextureRegion();

    private int w, h, bw, bh;
    private Viewport viewport;
    public boolean enabled = true;
    public float bloomThreshold = 0.74f, bloomAmount = 0.30f;
    public float baseAberration = 0f, vignette = 0.45f, grain = 0.014f, scanlines = 0f;
    public float rays = 0.5f;
    public int blurPasses = 2;
    private int quality = 2;
    private float time;

    /** Dynamic values pushed in by the director each frame. */
    public Cine cine;

    public PostFx() {
        ShaderProgram.pedantic = false;
        fsBatch = new SpriteBatch(8);
        bright = compile(FRAG_BRIGHT, "bright");
        blur = compile(FRAG_BLUR, "blur");
        comp = compile(FRAG_COMP, "composite");
        if (bright == null || blur == null || comp == null) {
            enabled = false;
            Gdx.app.error("PostFx", "shader compilation failed; post processing disabled");
        }
    }

    private ShaderProgram compile(String frag, String name) {
        ShaderProgram s = new ShaderProgram(VERT, frag);
        if (!s.isCompiled()) {
            Gdx.app.error("PostFx", name + ": " + s.getLog());
            s.dispose();
            return null;
        }
        return s;
    }

    private void ensure(int width, int height) {
        if (scene != null && width == w && height == h) return;
        free(scene); free(blurA); free(blurB);
        w = Math.max(2, width);
        h = Math.max(2, height);
        bw = Math.max(2, w / 4);
        bh = Math.max(2, h / 4);
        scene = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        blurA = new FrameBuffer(Pixmap.Format.RGBA8888, bw, bh, false);
        blurB = new FrameBuffer(Pixmap.Format.RGBA8888, bw, bh, false);
        for (FrameBuffer f : new FrameBuffer[]{ scene, blurA, blurB })
            f.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private static void free(FrameBuffer f) { if (f != null) f.dispose(); }

    /**
     * Begins capturing the frame. Returns false when post processing is off.
     *
     * The scene goes into a buffer the size of the viewport's fitted area only, and is composited
     * back into exactly that area. Sizing it to the whole window instead would stretch the picture
     * in any window that is not the game's aspect ratio, instead of letterboxing it.
     */
    public boolean captureBegin(Viewport viewport) {
        if (!enabled) return false;
        this.viewport = viewport;
        float hidpi = Gdx.graphics.getBackBufferWidth() / (float) Gdx.graphics.getWidth();
        ensure(Math.round(viewport.getScreenWidth() * hidpi), Math.round(viewport.getScreenHeight() * hidpi));
        scene.begin();
        Gdx.gl.glClearColor(Pal.BG0.r, Pal.BG0.g, Pal.BG0.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        return true;
    }

    public void renderToScreen(float dt) {
        if (!enabled || scene == null) return;
        time += dt;
        scene.end();

        sceneReg.setRegion(scene.getColorBufferTexture());
        sceneReg.flip(false, true);

        drawFs(blurA, bright, bw, bh, sceneReg, new Runnable() {
            @Override public void run() { bright.setUniformf("u_threshold", bloomThreshold); }
        });

        for (int i = 0; i < blurPasses; i++) {
            final float radius = 1f + i;
            blurReg.setRegion(blurA.getColorBufferTexture());
            blurReg.flip(false, true);
            drawFs(blurB, blur, bw, bh, blurReg, new Runnable() {
                @Override public void run() { blur.setUniformf("u_dir", radius / bw, 0f); }
            });
            blurReg.setRegion(blurB.getColorBufferTexture());
            blurReg.flip(false, true);
            drawFs(blurA, blur, bw, bh, blurReg, new Runnable() {
                @Override public void run() { blur.setUniformf("u_dir", 0f, radius / bh); }
            });
        }

        float ab = baseAberration + (cine != null ? cine.chroma : 0);
        float punch = cine != null ? cine.punch : 0;
        float defocus = cine != null ? Math.max(cine.defocus, cine.rack * 0.55f) : 0;
        float shockAmt = cine != null ? cine.shockAmt : 0;

        // the passes above reset the GL viewport to the whole window; composite into the fitted area
        HdpiUtils.glViewport(viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight());
        fsCam.setToOrtho(false, w, h);
        fsBatch.setProjectionMatrix(fsCam.combined);
        fsBatch.setShader(comp);
        blurA.getColorBufferTexture().bind(1);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        fsBatch.begin();
        comp.setUniformi("u_bloom", 1);
        comp.setUniformf("u_time", time);
        comp.setUniformf("u_bloomAmt", bloomAmount);
        comp.setUniformf("u_aberration", ab);
        comp.setUniformf("u_vignette", vignette);
        comp.setUniformf("u_grain", grain);
        comp.setUniformf("u_scan", scanlines);
        comp.setUniformf("u_rays", rays);
        comp.setUniformf("u_punch", punch);
        comp.setUniformf("u_defocus", defocus);
        comp.setUniformf("u_shock", cine != null ? cine.shockX : 0.5f,
            cine != null ? cine.shockY : 0.5f, cine != null ? cine.shockRadius : 0f);
        comp.setUniformf("u_shockAmt", shockAmt);
        comp.setUniformf("u_haze", cine != null ? cine.haze : 0f);
        comp.setUniformf("u_burst", cine != null ? cine.burstX : 0.5f,
            cine != null ? cine.burstY : 0.5f, cine != null ? cine.burst : 0f);
        if (cine != null) comp.setUniformf("u_grade", cine.gradeCol.r, cine.gradeCol.g, cine.gradeCol.b, cine.grade);
        else comp.setUniformf("u_grade", 1f, 1f, 1f, 0f);
        comp.setUniformf("u_edge", cine != null ? cine.edge : 0f);
        comp.setUniformf("u_res", w, h);
        fsBatch.setColor(Color.WHITE);
        fsBatch.draw(sceneReg, 0, 0, w, h);
        fsBatch.end();
        fsBatch.setShader(null);
    }

    private void drawFs(FrameBuffer target, ShaderProgram shader, int tw, int th,
                        TextureRegion src, Runnable uniforms) {
        target.begin();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        fsCam.setToOrtho(false, tw, th);
        fsBatch.setProjectionMatrix(fsCam.combined);
        fsBatch.setShader(shader);
        fsBatch.begin();
        uniforms.run();
        fsBatch.setColor(Color.WHITE);
        fsBatch.draw(src, 0, 0, tw, th);
        fsBatch.end();
        target.end();
    }

    /** Cycles cinematic -> clean -> off -> cinematic. */
    public String cycle() {
        quality = (quality + 1) % 3;
        switch (quality) {
            case 0:
                enabled = false;
                return "effects: off";
            case 1:
                enabled = true;
                bloomAmount = 0.16f; rays = 0f; grain = 0.008f; vignette = 0.3f;
                baseAberration = 0f; scanlines = 0f; blurPasses = 1;
                return "effects: clean";
            default:
                enabled = true;
                bloomAmount = 0.30f; rays = 0.5f; grain = 0.014f; vignette = 0.45f;
                baseAberration = 0f; scanlines = 0f; blurPasses = 2;
                return "effects: cinematic";
        }
    }

    @Override
    public void dispose() {
        free(scene); free(blurA); free(blurB);
        if (bright != null) bright.dispose();
        if (blur != null) blur.dispose();
        if (comp != null) comp.dispose();
        fsBatch.dispose();
    }
}
