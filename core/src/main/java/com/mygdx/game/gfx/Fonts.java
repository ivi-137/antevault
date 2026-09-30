package com.mygdx.game.gfx;

/**
 * The game's bitmap fonts, one row per face.
 *
 * Fonts are baked once from the TTFs in {@code tools/fonts} into plain BMFont files under
 * {@code assets/font} (run {@code gradlew tools:run}). The runtime only ever loads those files,
 * so every platform - desktop, Android and the browser - renders identical text with no
 * FreeType dependency. This table is shared with the baker so the two cannot drift apart.
 */
public final class Fonts {

    private Fonts() {}

    /** @param shadow drop-shadow offset in pixels, baked into the glyphs. */
    public static final class Spec {
        public final String name, ttf;
        public final int size, shadow;
        public final boolean icons;

        Spec(String name, String ttf, int size, int shadow, boolean icons) {
            this.name = name; this.ttf = ttf; this.size = size; this.shadow = shadow; this.icons = icons;
        }

        public String path() { return "font/" + name + ".fnt"; }
        public String chars() { return icons ? Icon.ALL : TEXT; }
    }

    private static Spec text(String name, String ttf, int size, int shadow) {
        return new Spec(name, ttf, size, shadow, false);
    }

    private static Spec icons(String name, int size, int shadow) {
        return new Spec(name, "icons", size, shadow, true);
    }

    public static final Spec SMALL   = text("small",   "body",     15, 0);
    public static final Spec BODY    = text("body",    "body",     18, 0);
    public static final Spec BODY_B  = text("bodyb",   "bodybold", 18, 0);
    public static final Spec HEAD    = text("head",    "bodybold", 21, 0);
    public static final Spec BIG     = text("big",     "display",  32, 2);
    public static final Spec HUGE    = text("huge",    "display",  68, 3);
    public static final Spec NUM     = text("num",     "cond",     34, 0);
    public static final Spec NUM_BIG = text("numbig",  "cond",     46, 1);
    public static final Spec NUM_HUGE= text("numhuge", "cond",     62, 2);
    public static final Spec TITLE   = text("title",   "title",    62, 3);
    public static final Spec ICON    = icons("icon",   20, 0);
    public static final Spec ICON_LG = icons("iconlg", 30, 1);
    public static final Spec ICON_XL = icons("iconxl", 46, 2);

    public static final Spec[] ALL = {
        SMALL, BODY, BODY_B, HEAD, BIG, HUGE, NUM, NUM_BIG, NUM_HUGE, TITLE, ICON, ICON_LG, ICON_XL
    };

    /** Printable ASCII, the Latin-1 letters Italian/German/French need, and a few symbols. */
    public static final String TEXT = buildText();

    private static String buildText() {
        StringBuilder sb = new StringBuilder();
        for (char c = 32; c <= 126; c++) sb.append(c);
        for (char c = 0xA1; c <= 0xFF; c++) sb.append(c);
        sb.append('\u2022').append('\u20AC');
        return sb.toString();
    }
}
