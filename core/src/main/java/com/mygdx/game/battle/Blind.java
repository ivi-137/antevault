package com.mygdx.game.battle;

import com.mygdx.game.cards.Card;
import com.mygdx.game.i18n.I18n;

import java.util.Random;

/** One scored encounter: a score to reach, a budget of hands, and maybe a rule bent against you. */
public class Blind {

    public String name = "Seal";
    public int ante = 1;
    /** 0 = lesser, 1 = greater, 2 = champion, 3 = warden, 4 = the Heart. */
    public int tier = 0;

    public long target = 300;
    public int hands = 4;
    public int discards = 3;
    public int reward = 5;

    public BossMod mod = BossMod.NONE;
    public Card.Suit debuffSuit = null;

    private static final String[] MINOR = {
        "The Cracked Seal", "Silt Ward", "Ash Picket", "Rust Sigil", "Low Bastion",
        "Weeping Arch", "Salt Lock", "The Quiet Door"
    };
    private static final String[] MAJOR = {
        "Iron Curtain", "The Long Count", "Bleeding Spire", "Hollow Bulwark",
        "Choke of Bells", "Gilded Breach", "The Second Ledger"
    };
    private static final String[] ELITE = {
        "Champion of Ash", "The Overrun", "Blade Ascendant", "The Cinder Ledger"
    };

    private static final long[] BASE = { 0, 300, 950, 3000, 9500, 32000 };
    private static final double[] TIER_MUL = { 1.0, 1.5, 2.2, 3.0, 1.0 };

    public static Blind make(int ante, int tier, String forcedName, Random rnd) {
        Blind e = new Blind();
        e.ante = Math.max(1, Math.min(5, ante));
        e.tier = tier;
        e.target = (long) (BASE[e.ante] * TIER_MUL[Math.min(tier, TIER_MUL.length - 1)]);
        e.hands = 4;
        e.discards = 3;
        e.reward = 4 + tier * 3 + ante;

        if (forcedName != null) e.name = forcedName;
        else if (tier == 0) e.name = MINOR[rnd.nextInt(MINOR.length)];
        else if (tier == 1) e.name = MAJOR[rnd.nextInt(MAJOR.length)];
        else if (tier == 2) e.name = ELITE[rnd.nextInt(ELITE.length)];

        if (tier >= 2) {
            e.mod = BossMod.POOL[rnd.nextInt(BossMod.POOL.length)];
            if (e.mod == BossMod.NULL_ZONE) e.debuffSuit = Card.Suit.values()[rnd.nextInt(4)];
            if (e.mod == BossMod.EXECUTIONER) e.hands -= 1;
            if (e.mod == BossMod.DRAIN) e.discards -= 1;
            e.reward += 2;
        }
        return e;
    }

    public static Blind heart() {
        Blind e = new Blind();
        e.name = "THE HEART";
        e.ante = 5;
        e.tier = 4;
        e.target = BASE[5];
        e.hands = 6;
        e.discards = 4;
        e.reward = 0;
        e.mod = BossMod.INHIBITOR;
        return e;
    }

    public String tierLabel() {
        switch (tier) {
            case 1:  return I18n.t("tier.greater");
            case 2:  return I18n.t("tier.champion");
            case 3:  return I18n.t("tier.warden");
            case 4:  return I18n.t("tier.heart");
            default: return I18n.t("tier.lesser");
        }
    }
}
