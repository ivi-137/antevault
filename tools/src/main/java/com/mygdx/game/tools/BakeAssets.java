package com.mygdx.game.tools;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeBitmapFontData;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter.FontInfo;
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.mygdx.game.gfx.Fonts;

import java.io.File;

/**
 * Bakes every font in {@link Fonts#ALL} from its TTF into a BMFont file plus a PNG page.
 *
 * Run from the project root with {@code gradlew tools:run}. The output in {@code assets/font}
 * is committed, so a normal build never needs FreeType.
 */
public final class BakeAssets {

    private static final int[] PAGE_SIZES = { 128, 256, 512, 1024, 2048 };

    public static void main(String[] args) {
        GdxNativesLoader.load();
        FileHandle root = new FileHandle(new File("."));
        FileHandle out = root.child("assets/font");
        out.mkdirs();
        for (Fonts.Spec s : Fonts.ALL) bake(s, root.child("tools/fonts/" + s.ttf + ".ttf"), out);
        System.out.println("Baked " + Fonts.ALL.length + " fonts into " + out.file().getAbsolutePath());
    }

    private static void bake(Fonts.Spec s, FileHandle ttf, FileHandle out) {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(ttf);
        try {
            // Smallest single page that holds every glyph; small fonts should not cost a 1024px texture.
            for (int size : PAGE_SIZES) {
                PixmapPacker packer = new PixmapPacker(size, size, Pixmap.Format.RGBA8888, 2, false);
                FreeTypeBitmapFontData data = gen.generateData(parameter(s, packer));
                if (packer.getPages().size > 1) { packer.dispose(); continue; }

                String[] pages = BitmapFontWriter.writePixmaps(packer.getPages(), out, s.name);
                BitmapFontWriter.writeFont(data, pages, out.child(s.name + ".fnt"),
                    new FontInfo(s.name, s.size), size, size);
                System.out.printf("  %-8s %2dpx  %4dx%-4d  %d glyphs%n",
                    s.name, s.size, size, size, s.chars().length());
                packer.dispose();
                return;
            }
            throw new IllegalStateException(s.name + " does not fit a single 2048px page");
        } finally {
            gen.dispose();
        }
    }

    private static FreeTypeFontParameter parameter(Fonts.Spec s, PixmapPacker packer) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = s.size;
        p.characters = s.chars();
        p.shadowOffsetX = s.shadow;
        p.shadowOffsetY = s.shadow;
        p.shadowColor = new Color(0, 0, 0, 0.45f);
        p.hinting = FreeTypeFontGenerator.Hinting.Full;
        p.packer = packer;
        return p;
    }
}
