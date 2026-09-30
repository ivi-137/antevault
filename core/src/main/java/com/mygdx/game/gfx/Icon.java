package com.mygdx.game.gfx;

/**
 * Named glyphs from the bundled icon face (Font Awesome 6 Free Solid; the font is SIL OFL,
 * the icons CC BY 4.0). Only these codepoints are rasterised, so the atlas stays small.
 */
public final class Icon {

    private Icon() {}

    public static final char CAMP      = '\uF6BB';  // campground
    public static final char SEAL      = '\uF3ED';  // shield-halved
    public static final char CHAMPION  = '\uF54C';  // skull
    public static final char WARDEN    = '\uF521';  // crown
    public static final char SHOP      = '\uF54E';  // store
    public static final char SHRINE    = '\uF059';  // circle-question
    public static final char CACHE     = '\uF187';  // box-archive
    public static final char SANCTUM   = '\uF0C3';  // flask
    public static final char HEART     = '\uF004';  // heart
    public static final char KEY       = '\uF084';  // key
    public static final char TIDE      = '\uF773';  // water
    public static final char ECHO      = '\uF83E';  // wave-square
    public static final char PRISM     = '\uE2CA';  // wand-magic-sparkles
    public static final char COINS     = '\uF51E';  // coins
    public static final char PURSE     = '\uF81D';  // sack-dollar
    public static final char DECK      = '\uF5FD';  // layer-group
    public static final char TROPHY    = '\uF091';  // trophy
    public static final char LOCK      = '\uF023';  // lock
    public static final char UNLOCK    = '\uF3C1';  // lock-open
    public static final char CHECK     = '\uF058';  // circle-check
    public static final char CROSS     = '\uF00D';  // xmark
    public static final char UP        = '\uF062';  // arrow-up
    public static final char PLUS      = '\uF067';  // plus
    public static final char MINUS     = '\uF068';  // minus
    public static final char BOOK      = '\uF02D';  // book
    public static final char SCROLL    = '\uF70E';  // scroll
    public static final char DICE      = '\uF522';  // dice
    public static final char ROTATE    = '\uF2F1';  // rotate
    public static final char DOOR      = '\uF52B';  // door-open
    public static final char MAP       = '\uF279';  // map
    public static final char COMPASS   = '\uF14E';  // compass
    public static final char HOURGLASS = '\uF254';  // hourglass
    public static final char BOLT      = '\uF0E7';  // bolt
    public static final char FIRE      = '\uF06D';  // fire
    public static final char GEM       = '\uF3A5';  // gem
    public static final char STAR      = '\uF005';  // star
    public static final char EYE       = '\uF06E';  // eye
    public static final char GAVEL     = '\uF0E3';  // gavel
    public static final char RING      = '\uF70B';  // ring
    public static final char FEATHER   = '\uF52D';  // feather
    public static final char ANCHOR    = '\uF13D';  // anchor
    public static final char MOON      = '\uF186';  // moon
    public static final char SUN       = '\uF185';  // sun
    public static final char MOUNTAIN  = '\uF6FC';  // mountain
    public static final char TOWER     = '\uE586';  // tower-observation
    public static final char BURST     = '\uE4DC';  // burst
    public static final char SEALWAX   = '\uF0A3';  // certificate
    public static final char SHAPES    = '\uF61F';  // shapes
    public static final char DIAMOND   = '\uF219';  // diamond
    public static final char CHESS     = '\uF447';  // chess-rook
    public static final char DUNGEON   = '\uF6D9';  // dungeon
    public static final char SUITCASE  = '\uF0F2';  // suitcase
    public static final char MONEY     = '\uF0D6';  // money-bill
    public static final char HAND      = '\uE05D';  // hand-sparkles
    public static final char TRASH     = '\uF1F8';  // trash
    public static final char GEAR      = '\uF013';  // gear
    public static final char PLAY      = '\uF04B';  // play
    public static final char HOUSE     = '\uF015';  // house
    public static final char SHOP2     = '\uF54F';  // shop

    /** Every glyph the game rasterises. */
    public static final String ALL = ""
        + CAMP + SEAL + CHAMPION + WARDEN + SHOP
        + SHRINE + CACHE + SANCTUM + HEART + KEY
        + TIDE + ECHO + PRISM + COINS + PURSE
        + DECK + TROPHY + LOCK + UNLOCK + CHECK
        + CROSS + UP + PLUS + MINUS + BOOK
        + SCROLL + DICE + ROTATE + DOOR + MAP
        + COMPASS + HOURGLASS + BOLT + FIRE + GEM
        + STAR + EYE + GAVEL + RING + FEATHER
        + ANCHOR + MOON + SUN + MOUNTAIN + TOWER
        + BURST + SEALWAX + SHAPES + DIAMOND + CHESS
        + DUNGEON + SUITCASE + MONEY + HAND + TRASH
        + GEAR + PLAY + HOUSE + SHOP2;

    public static String s(char c) { return String.valueOf(c); }
}
