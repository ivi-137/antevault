# Third-party material

ANTEVAULT's own code and art are not covered by anything below. What it builds on:

## Fonts (SIL Open Font License 1.1)

The TTFs in `tools/fonts` are baked into the bitmap fonts in `assets/font`.

| Font | Used for | Authors | License |
| --- | --- | --- | --- |
| [Cinzel](https://github.com/NDISCOVER/Cinzel) | headings | The Cinzel Project Authors | [licenses/Cinzel-OFL.txt](licenses/Cinzel-OFL.txt) |
| [Cinzel Decorative](https://fonts.google.com/specimen/Cinzel+Decorative) | the title | Natanael Gama | [licenses/CinzelDecorative-OFL.txt](licenses/CinzelDecorative-OFL.txt) |
| [Barlow](https://github.com/jpt/barlow) | body text | The Barlow Project Authors | [licenses/Barlow-OFL.txt](licenses/Barlow-OFL.txt) |
| [Barlow Condensed](https://github.com/jpt/barlow) | numbers | The Barlow Project Authors | [licenses/BarlowCondensed-OFL.txt](licenses/BarlowCondensed-OFL.txt) |

## Icons

[Font Awesome Free](https://fontawesome.com) 6, by Fonticons, Inc. The icons are CC BY 4.0, the font
files SIL OFL 1.1 and the code MIT. See [licenses/FontAwesome-LICENSE.txt](licenses/FontAwesome-LICENSE.txt).
Only the glyphs listed in `Icon.java` are used.

## Code

- [libGDX](https://libgdx.com) and its GWT backend, Apache License 2.0
  ([licenses/libGDX-Apache-2.0.txt](licenses/libGDX-Apache-2.0.txt)).
- `tools/src/main/java/com/badlogic/gdx/tools/bmfont/BitmapFontWriter.java` is copied unchanged from
  libGDX 1.14.2 (Apache License 2.0). It writes the baked bitmap fonts.
- [GWT](https://www.gwtproject.org/) compiles the game for the browser, Apache License 2.0.
