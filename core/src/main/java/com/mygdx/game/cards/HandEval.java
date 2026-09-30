package com.mygdx.game.cards;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Poker hand evaluation with the roguelite extensions: wild suits, rank-less Stone cards,
 * five-of-a-kind families, and relic-driven rule bends (shorter flushes/straights, gapped straights).
 */
public final class HandEval {

    public static class Result {
        public HandType type = HandType.HIGH_CARD;
        public final List<Card> scoring = new ArrayList<>();
        public final List<Card> played = new ArrayList<>();
    }

    private HandEval() {}

    public static Result eval(List<Card> played) { return eval(played, 5, 5, false); }

    /**
     * @param flushSize    cards needed for a flush (relics can lower it to 4)
     * @param straightSize cards needed for a straight
     * @param gaps         allow one-rank gaps inside a straight
     */
    public static Result eval(List<Card> played, int flushSize, int straightSize, boolean gaps) {
        Result r = new Result();
        r.played.addAll(played);
        if (played.isEmpty()) return r;

        List<Card> h = new ArrayList<>();
        List<Card> stones = new ArrayList<>();
        for (Card c : played) {
            if (c.isStone()) stones.add(c); else h.add(c);
        }

        // ---- flush ------------------------------------------------------
        Card.Suit bestSuit = null;
        int bestSuitCount = 0;
        for (Card.Suit s : Card.Suit.values()) {
            int n = 0;
            for (Card c : h) if (c.suitIs(s)) n++;
            if (n > bestSuitCount) { bestSuitCount = n; bestSuit = s; }
        }
        boolean flush = bestSuitCount >= flushSize;
        List<Card> flushCards = new ArrayList<>();
        if (flush) {
            for (Card c : h) if (c.suitIs(bestSuit)) flushCards.add(c);
        }

        // ---- rank multiplicity ------------------------------------------
        int[] count = new int[15];
        for (Card c : h) count[c.rank.order]++;
        int max = 0, maxRank = 0, second = 0, secondRank = 0;
        for (int o = 14; o >= 2; o--) {
            if (count[o] > max) { second = max; secondRank = maxRank; max = count[o]; maxRank = o; }
            else if (count[o] > second) { second = count[o]; secondRank = o; }
        }
        boolean fullHouse = max >= 3 && second >= 2;
        boolean twoPair = max >= 2 && second >= 2;

        // ---- straight ----------------------------------------------------
        List<Integer> straightOrders = findStraight(h, straightSize, gaps);
        boolean straight = straightOrders != null;

        // ---- classify -----------------------------------------------------
        if (max >= 5 && flush)        { r.type = HandType.FLUSH_FIVE;    addRank(r, h, maxRank); }
        else if (fullHouse && flush)  { r.type = HandType.FLUSH_HOUSE;   r.scoring.addAll(h); }
        else if (max >= 5)            { r.type = HandType.FIVE_KIND;     addRank(r, h, maxRank); }
        else if (straight && flush)   { r.type = HandType.STRAIGHT_FLUSH; addStraight(r, h, straightOrders); }
        else if (max >= 4)            { r.type = HandType.FOUR_KIND;     addRank(r, h, maxRank); }
        else if (fullHouse)           { r.type = HandType.FULL_HOUSE;    r.scoring.addAll(h); }
        else if (flush)               { r.type = HandType.FLUSH;         r.scoring.addAll(flushCards); }
        else if (straight)            { r.type = HandType.STRAIGHT;      addStraight(r, h, straightOrders); }
        else if (max >= 3)            { r.type = HandType.THREE_KIND;    addRank(r, h, maxRank); }
        else if (twoPair)             { r.type = HandType.TWO_PAIR;      addRank(r, h, maxRank); addRank(r, h, secondRank); }
        else if (max >= 2)            { r.type = HandType.PAIR;          addRank(r, h, maxRank); }
        else {
            r.type = HandType.HIGH_CARD;
            Card best = null;
            for (Card c : h) if (best == null || c.rank.order > best.rank.order) best = c;
            if (best != null) r.scoring.add(best);
        }

        // Stone cards never help the hand type but always score.
        r.scoring.addAll(stones);

        // Keep scoring order the same as the played order so triggers read left to right.
        List<Card> ordered = new ArrayList<>();
        for (Card c : played) if (r.scoring.contains(c)) ordered.add(c);
        r.scoring.clear();
        r.scoring.addAll(ordered);
        return r;
    }

    private static void addRank(Result r, List<Card> h, int order) {
        for (Card c : h) if (c.rank.order == order && !r.scoring.contains(c)) r.scoring.add(c);
    }

    private static void addStraight(Result r, List<Card> h, List<Integer> orders) {
        HashSet<Integer> want = new HashSet<>(orders);
        for (Card c : h) {
            int o = c.rank.order;
            if (want.contains(o) || (o == 14 && want.contains(1))) {
                if (!r.scoring.contains(c)) r.scoring.add(c);
            }
        }
    }

    /** Returns the rank orders forming the best straight, or null. Ace can be low (order 1). */
    private static List<Integer> findStraight(List<Card> h, int size, boolean gaps) {
        if (h.size() < size) return null;
        boolean[] have = new boolean[16];
        for (Card c : h) {
            have[c.rank.order] = true;
            if (c.rank == Card.Rank.ACE) have[1] = true;
        }
        List<Integer> best = null;
        for (int start = 14; start >= 1; start--) {
            List<Integer> run = new ArrayList<>();
            int cur = start;
            while (run.size() < size && cur >= 1) {
                if (have[cur]) { run.add(cur); cur--; }
                else if (gaps && cur - 1 >= 1 && have[cur - 1] && run.size() > 0) { cur--; }
                else break;
            }
            if (run.size() >= size) {
                if (best == null) best = run;
                break;
            }
        }
        return best;
    }
}
