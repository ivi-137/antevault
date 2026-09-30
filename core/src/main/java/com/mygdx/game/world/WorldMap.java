package com.mygdx.game.world;

import com.badlogic.gdx.graphics.Color;
import com.mygdx.game.battle.Blind;
import com.mygdx.game.gfx.Pal;
import com.mygdx.game.run.Run;
import com.mygdx.game.run.Sigil;

import java.util.*;

/**
 * The world is a single interconnected graph you climb in both directions, not a one-way
 * ladder. Some edges are sealed behind a Sigil you only earn from a later Warden, so
 * clearing a region hands you the key to a door you already walked past.
 */
public class WorldMap {

    public enum Kind {
        START ("Camp",     Pal.GREEN),
        BATTLE("Seal",     Pal.CHIPS),
        ELITE ("Champion", Pal.MULT),
        WARDEN("Warden",   Pal.GOLD),
        SHOP  ("Bazaar",   Pal.TEAL),
        SHRINE("Shrine",   Pal.PURPLE),
        CACHE ("Cache",    Pal.ORANGE),
        REST  ("Sanctum",  Pal.GREEN),
        HEART ("The Heart", Pal.PINK);

        public final String label;
        public final Color color;
        Kind(String l, Color c) { label = l; color = c; }
    }

    public static class Node {
        public String id;
        public Kind kind;
        public float x, y;
        public int ante, tier;
        public Sigil grants;
        public String regionName = "";
        public Blind blind;
        public final List<Edge> edges = new ArrayList<>();

        public Node(String id, Kind k, float x, float y, int ante, int tier) {
            this.id = id; this.kind = k; this.x = x; this.y = y; this.ante = ante; this.tier = tier;
        }
    }

    public static class Edge {
        public Node a, b;
        public Sigil gate;
        public Edge(Node a, Node b, Sigil g) { this.a = a; this.b = b; gate = g; }
        public Node other(Node n) { return n == a ? b : a; }
    }

    public final LinkedHashMap<String, Node> nodes = new LinkedHashMap<>();
    public final List<Edge> edges = new ArrayList<>();
    public final String[] regionNames = {
        "THE OUTER WARD", "THE SUNKEN GALLERY", "THE VAULT", "THE UNDERTOW", "THE HEART"
    };

    private final Random rnd;

    public WorldMap(long seed) {
        rnd = new Random(seed);
        build();
        for (Node n : nodes.values()) {
            n.regionName = regionNames[Math.min(4, n.ante - 1)];
            if (n.kind == Kind.BATTLE || n.kind == Kind.ELITE || n.kind == Kind.WARDEN)
                n.blind = Blind.make(n.ante, n.tier, n.kind == Kind.WARDEN ? wardenName(n.ante) : null, rnd);
            if (n.kind == Kind.HEART) n.blind = Blind.heart();
        }
    }

    private static String wardenName(int ante) {
        switch (ante) {
            case 1: return "WARDEN OF THE OUTER SEAL";
            case 2: return "THE DROWNED MARSHAL";
            case 3: return "KEEPER OF THE VAULT";
            default: return "THE UNDERTOW SOVEREIGN";
        }
    }

    private Node n(String id, Kind k, float x, float y, int ante, int tier) {
        Node node = new Node(id, k, x, y, ante, tier);
        nodes.put(id, node);
        return node;
    }

    private void e(String a, String b) { e(a, b, null); }

    private void e(String a, String b, Sigil gate) {
        Node na = nodes.get(a), nb = nodes.get(b);
        Edge edge = new Edge(na, nb, gate);
        edges.add(edge);
        na.edges.add(edge);
        nb.edges.add(edge);
    }

    private void build() {
        // ---- Region 1: The Outer Ward -------------------------------------
        n("n0",  Kind.START,  700,  120, 1, 0);
        n("n1",  Kind.BATTLE, 600,  285, 1, 0);
        n("n2",  Kind.BATTLE, 810,  285, 1, 0);
        n("n3",  Kind.SHOP,   700,  450, 1, 0);
        n("n4",  Kind.CACHE,  935,  450, 1, 0);
        n("n5",  Kind.BATTLE, 600,  615, 1, 1);
        n("n6",  Kind.SHRINE, 830,  615, 1, 0);
        n("n7",  Kind.WARDEN, 710,  780, 1, 3).grants = Sigil.KEY;
        e("n0", "n1"); e("n0", "n2"); e("n1", "n3"); e("n2", "n3");
        e("n2", "n4", Sigil.KEY);
        e("n3", "n5"); e("n3", "n6"); e("n4", "n6"); e("n5", "n7"); e("n6", "n7");

        // ---- Region 2: The Sunken Gallery --------------------------------------
        n("n8",  Kind.REST,   710,  945, 2, 0);
        n("n9",  Kind.BATTLE, 590, 1105, 2, 0);
        n("n10", Kind.BATTLE, 830, 1105, 2, 0);
        n("n11", Kind.ELITE,  480, 1265, 2, 2);
        n("n12", Kind.SHOP,   710, 1265, 2, 0);
        n("n13", Kind.CACHE,  950, 1265, 2, 0);
        n("n14", Kind.SHRINE, 590, 1425, 2, 0);
        n("n15", Kind.BATTLE, 830, 1425, 2, 1);
        n("n16", Kind.WARDEN, 710, 1585, 2, 3).grants = Sigil.TIDE;
        e("n7", "n8"); e("n8", "n9"); e("n8", "n10");
        e("n9", "n11"); e("n9", "n12"); e("n10", "n12");
        e("n10", "n13", Sigil.TIDE);
        e("n11", "n14"); e("n12", "n14"); e("n12", "n15"); e("n13", "n15");
        e("n14", "n16"); e("n15", "n16");

        // ---- Region 3: The Vault (upper branch) -----------------------------
        n("n17", Kind.REST,   900, 1745,  3, 0);
        n("n18", Kind.BATTLE, 800, 1905,  3, 0);
        n("n19", Kind.BATTLE, 1030, 1905, 3, 0);
        n("n20", Kind.SHOP,   690, 2065,  3, 0);
        n("n21", Kind.ELITE,  910, 2065,  3, 2);
        n("n22", Kind.CACHE,  1140, 2065, 3, 0);
        n("n23", Kind.SHRINE, 800, 2225,  3, 0);
        n("n24", Kind.BATTLE, 1030, 2225, 3, 1);
        n("n25", Kind.WARDEN, 910, 2385,  3, 3).grants = Sigil.ECHO;
        e("n16", "n17");
        e("n17", "n18"); e("n17", "n19");
        e("n18", "n20"); e("n18", "n21"); e("n19", "n21");
        e("n19", "n22", Sigil.PRISM);
        e("n20", "n23"); e("n21", "n23"); e("n21", "n24"); e("n22", "n24");
        e("n23", "n25"); e("n24", "n25");

        // ---- Region 4: The Undertow (lower branch, sealed by the Echo Sigil) --------
        n("n26", Kind.REST,   430, 1745, 4, 0);
        n("n27", Kind.BATTLE, 320, 1905, 4, 0);
        n("n28", Kind.BATTLE, 545, 1905, 4, 0);
        n("n29", Kind.ELITE,  210, 2065, 4, 2);
        n("n30", Kind.SHOP,   440, 2065, 4, 0);
        n("n31", Kind.SHRINE, 320, 2225, 4, 0);
        n("n32", Kind.BATTLE, 545, 2225, 4, 1);
        n("n33", Kind.WARDEN, 430, 2385, 4, 3).grants = Sigil.PRISM;
        e("n16", "n26", Sigil.ECHO);
        e("n26", "n27"); e("n26", "n28");
        e("n27", "n29"); e("n27", "n30"); e("n28", "n30");
        e("n29", "n31"); e("n30", "n31"); e("n30", "n32"); e("n31", "n32");
        e("n31", "n33"); e("n32", "n33");

        // ---- The Heart ---------------------------------------------------------
        n("n34", Kind.HEART, 670, 2620, 5, 4);
        e("n25", "n34", Sigil.PRISM);
        e("n33", "n34");
    }

    public Node get(String id) { return nodes.get(id); }

    public List<Node> neighbours(Node n) {
        List<Node> out = new ArrayList<>();
        for (Edge e : n.edges) out.add(e.other(n));
        return out;
    }

    public Edge edgeBetween(Node a, Node b) {
        for (Edge e : a.edges) if (e.other(a) == b) return e;
        return null;
    }

    public boolean canTravel(Run run, Node from, Node to) {
        Edge e = edgeBetween(from, to);
        if (e == null) return false;
        return e.gate == null || run.has(e.gate);
    }

    /** Nodes that still have something to do (used for the "unfinished business" hint). */
    public int remaining(Run run) {
        int c = 0;
        for (Node n : nodes.values()) {
            if (n.kind == Kind.START || n.kind == Kind.HEART) continue;
            if (!run.cleared.contains(n.id)) c++;
        }
        return c;
    }

    public float minX() { float v = 1e9f; for (Node n : nodes.values()) v = Math.min(v, n.x); return v; }
    public float maxX() { float v = -1e9f; for (Node n : nodes.values()) v = Math.max(v, n.x); return v; }
    public float minY() { float v = 1e9f; for (Node n : nodes.values()) v = Math.min(v, n.y); return v; }
    public float maxY() { float v = -1e9f; for (Node n : nodes.values()) v = Math.max(v, n.y); return v; }
}
