# ANTEVAULT

<img src="docs/icon.png" alt="" width="96" align="right">

A poker roguelite. Play poker hands to break **seals**, collect **items** and **charms** that bend the
scoring rules, and climb a map whose doors only open once you have won the right **sigil**.

**Play it in your browser: <https://gpojani.me/antevault/>**

It is written in Java with [libGDX](https://libgdx.com/), and runs on the web, desktop and Android from
one codebase. There are no image assets for the cards, panels or backdrop: they are drawn in code.

## How it plays

- Pick up to five cards and play them. A hand scores **Chips × Mult**; beat the seal's target before
  your hands run out.
- Hand types level up, cards carry enhancements, editions and wax seals, and 38 items rewrite the
  scoring. Charms are single-use consumables that reshape the cards in front of you.
- The world map is one connected graph you walk both ways. Four **sigils** are each a power with
  charges inside a seal *and* the key to one kind of sealed door, so winning a region lets you
  return for what you walked past.
- Press `I` at any time for your deck, hand levels and loadout. A guided first seal is under
  **Tutorial**. The interface is available in English, Italian, German and French.

| Input | Action |
| --- | --- |
| click / `1`–`9` | select a card, or walk to a node on the map |
| `Enter` / `X` | play / discard the selected cards |
| `Tab` | sort the hand by rank or suit |
| `Q` `W` `E` `R` | use a sigil |
| `Z` `C` | use the first or second charm |
| `I` | run overview |
| `F1` | cycle visual effects |

## Running it

You need JDK 17 to build (Gradle 9), and Java 11 or newer to run the desktop jar.

```bash
./gradlew lwjgl3:run                # desktop, from source
./gradlew lwjgl3:jar                # desktop: lwjgl3/build/libs/antevault-1.0.0.jar
./gradlew html:dist                 # browser: html/build/dist (a static site)
./gradlew android:assembleDebug     # Android APK, needs the Android SDK (local.properties)
```

On Windows, `PLAY ANTEVAULT.bat` builds and launches the desktop version. If a screen ever throws,
the game recovers to the map and writes the stack trace to `antevault-crash.log`.

The site at gpojani.me is built and deployed by `.github/workflows/pages.yml` on every push to `main`.

## Layout

| Module | What it is |
| --- | --- |
| `core` | the whole game, shared by every platform |
| `lwjgl3` | desktop launcher |
| `html` | browser launcher (GWT) and the page that hosts it |
| `android` | Android launcher |
| `tools` | build-time helpers: `tools:run` bakes the fonts, `make_icons.py` draws the icons |

Inside `core`: `battle` (scoring and seal rules), `cards`, `run` (items, charms, sigils),
`world` (the map), `screens`, `gfx` (drawing, post-processing, effects) and `i18n`.

Fonts are baked once from `tools/fonts` into ordinary bitmap fonts in `assets/font`, so no platform
needs FreeType at runtime. That is also what lets the same text render in the browser.

## Credits

Fonts, icons and libraries are listed in [THIRD_PARTY.md](THIRD_PARTY.md), with their licenses in
[licenses/](licenses/).

No license has been chosen for the game's own code yet, so by default all rights are reserved.
