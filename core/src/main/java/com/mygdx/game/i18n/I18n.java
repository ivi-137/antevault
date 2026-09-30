package com.mygdx.game.i18n;

import com.mygdx.game.battle.BossMod;
import com.mygdx.game.cards.Card;
import com.mygdx.game.cards.HandType;
import com.mygdx.game.run.Sigil;

import java.util.HashMap;
import java.util.Map;

/**
 * Translations. The table is one row per key, columns ordered to match {@link Lang}, so adding
 * a language means adding a column rather than touching any screen.
 *
 * {@link #t} falls back to English for any key a language has not filled in yet, so a partial
 * translation degrades to readable English instead of showing a raw key.
 */
public final class I18n {

    public enum Lang {
        EN("English"), IT("Italiano"), DE("Deutsch"), FR("Francais");
        public final String label;
        Lang(String l) { label = l; }
    }

    private static Lang current = Lang.EN;
    private static final Map<String, String[]> MAP = new HashMap<>();

    private I18n() {}

    public static Lang lang() { return current; }

    public static void set(Lang l) { current = l; }

    public static String t(String key) {
        String[] row = MAP.get(key);
        if (row == null) return key;
        int i = current.ordinal();
        if (i < row.length && row[i] != null && !row[i].isEmpty()) return row[i];
        return row[0];
    }

    /** Substitutes {@code %s} / {@code %d} in order. (String.format is not available in the browser build.) */
    public static String t(String key, Object... args) {
        String s = t(key);
        StringBuilder out = new StringBuilder(s.length() + 8);
        int next = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean slot = c == '%' && i + 1 < s.length() && next < args.length
                && (s.charAt(i + 1) == 's' || s.charAt(i + 1) == 'd');
            if (slot) { out.append(args[next++]); i++; }
            else out.append(c);
        }
        return out.toString();
    }

    public static String hand(HandType h) { return t("hand." + h.name().toLowerCase()); }
    public static String suit(Card.Suit s) { return t("suit." + s.name().toLowerCase()); }
    public static String sigil(Sigil s) { return t("sigil." + s.name().toLowerCase()); }
    public static String sigilShort(Sigil s) { return t("sigilshort." + s.name().toLowerCase()); }
    public static String boss(BossMod m) { return t("boss." + m.name().toLowerCase()); }
    public static String bossDesc(BossMod m) { return t("bossdesc." + m.name().toLowerCase()); }
    public static String gate(Sigil s) { return t("gate." + s.name().toLowerCase()); }

    /** Sigil effect text, with the scaling value already substituted. */
    public static String sigilDesc(Sigil s, int rank) {
        switch (s) {
            case KEY:   return t("sigildesc.key", s.keyDraw(rank));
            case ECHO:  return t("sigildesc.echo", rank);
            case PRISM: return t("sigildesc.prism", s.prismText(rank));
            default:    return t("sigildesc.tide");
        }
    }

    // ---------------------------------------------------------------- table
    // columns: key, EN, IT, DE, FR

    private static final String[][] T = {
        // --- menus and chrome
        {"menu.newrun",    "NEW RUN", "NUOVA PARTITA", "NEUER LAUF", "NOUVELLE PARTIE"},
        {"menu.tutorial",  "TUTORIAL", "TUTORIAL", "TUTORIAL", "TUTORIEL"},
        {"menu.howtoplay", "HOW TO PLAY", "COME SI GIOCA", "SPIELANLEITUNG", "COMMENT JOUER"},
        {"menu.settings",  "SETTINGS", "IMPOSTAZIONI", "EINSTELLUNGEN", "REGLAGES"},
        {"menu.quit",      "QUIT", "ESCI", "BEENDEN", "QUITTER"},
        {"menu.back",      "BACK", "INDIETRO", "ZURUCK", "RETOUR"},
        {"menu.resume",    "RESUME", "RIPRENDI", "FORTSETZEN", "REPRENDRE"},
        {"menu.abandon",   "ABANDON RUN", "ABBANDONA", "LAUF AUFGEBEN", "ABANDONNER"},
        {"menu.quitgame",  "QUIT GAME", "ESCI DAL GIOCO", "SPIEL BEENDEN", "QUITTER LE JEU"},
        {"menu.paused",    "PAUSED", "IN PAUSA", "PAUSIERT", "EN PAUSE"},
        {"title.tagline",  "a poker roguelite  ·  break the seals, open the vault",
                           "un roguelite di poker  ·  spezza i sigilli, apri la cripta",
                           "ein Poker-Roguelite  ·  brich die Siegel, offne das Gewolbe",
                           "un roguelite de poker  ·  brise les sceaux, ouvre la voute"},
        {"title.footer",   "deal, score, descend  ·  a vault that locks itself  ·  keys that reopen it",
                           "distribuisci, segna, scendi  ·  una cripta che si chiude  ·  chiavi che la riaprono",
                           "geben, werten, absteigen  ·  ein Gewolbe das sich verschliesst  ·  Schlussel die es offnen",
                           "distribuer, marquer, descendre  ·  une voute qui se ferme  ·  des cles qui la rouvrent"},

        // --- settings
        {"settings.title",      "SETTINGS", "IMPOSTAZIONI", "EINSTELLUNGEN", "REGLAGES"},
        {"settings.resolution", "Resolution", "Risoluzione", "Auflosung", "Resolution"},
        {"settings.resolutionTip",
            "The game renders at a fixed 1280x720 layout and scales to fit, so every size stays sharp.",
            "Il gioco usa un layout fisso 1280x720 e lo adatta, quindi ogni dimensione resta nitida.",
            "Das Spiel nutzt ein festes 1280x720 Layout und skaliert es, jede Grosse bleibt scharf.",
            "Le jeu utilise une mise en page fixe 1280x720 et l'adapte, chaque taille reste nette."},
        {"settings.fullscreen", "Fullscreen", "Schermo intero", "Vollbild", "Plein ecran"},
        {"settings.fullscreenTip", "Fullscreen at your desktop resolution.",
            "Schermo intero alla risoluzione del desktop.",
            "Vollbild in der Auflosung deines Desktops.",
            "Plein ecran a la resolution de votre bureau."},
        {"settings.effects",    "Effects", "Effetti", "Effekte", "Effets"},
        {"settings.effectsTip", "Bloom, god rays, shockwaves and grain. Turn it down for a flatter image.",
            "Bagliore, raggi, onde d'urto e grana. Abbassalo per un'immagine piu piatta.",
            "Leuchten, Lichtstrahlen, Schockwellen und Korn. Reduziere es fur ein flacheres Bild.",
            "Halo, rayons, ondes de choc et grain. Reduisez pour une image plus plate."},
        {"settings.shake",      "Screen shake", "Vibrazione schermo", "Bildschirmruttlen", "Secousse d'ecran"},
        {"settings.shakeTip",   "Camera kick on big scoring moments.",
            "Scossa della camera nei momenti di punteggio alto.",
            "Kamerastoss bei grossen Wertungsmomenten.",
            "Secousse de camera lors des gros scores."},
        {"settings.framecap",   "Frame cap", "Limite FPS", "Bildratenlimit", "Limite FPS"},
        {"settings.framecapTip", "V-Sync matches your monitor and avoids tearing.",
            "Il V-Sync segue il monitor ed evita il tearing.",
            "V-Sync folgt deinem Monitor und vermeidet Tearing.",
            "La V-Sync suit votre ecran et evite le dechirement."},
        {"settings.language",   "Language", "Lingua", "Sprache", "Langue"},
        {"settings.languageTip", "Interface language.", "Lingua dell'interfaccia.",
            "Sprache der Oberflache.", "Langue de l'interface."},
        {"settings.on",         "On", "Si", "An", "Oui"},
        {"settings.off",        "Off", "No", "Aus", "Non"},
        {"settings.windowed",   "Windowed", "Finestra", "Fenster", "Fenetre"},
        {"settings.vsync",      "V-Sync", "V-Sync", "V-Sync", "V-Sync"},
        {"settings.uncapped",   "Uncapped", "Illimitato", "Unbegrenzt", "Illimite"},
        {"fx.off",              "Off", "No", "Aus", "Non"},
        {"fx.clean",            "Clean", "Pulito", "Klar", "Sobre"},
        {"fx.cinematic",        "Cinematic", "Cinematico", "Filmisch", "Cinematique"},

        // --- the seal screen
        {"seal.scoreatleast", "SCORE AT LEAST", "PUNTEGGIO MINIMO", "MINDESTENS", "SCORE MINIMUM"},
        {"seal.yourscore",    "YOUR SCORE", "IL TUO PUNTEGGIO", "DEIN PUNKTESTAND", "VOTRE SCORE"},
        {"seal.stillneeded",  "%s still needed", "mancano %s", "%s noch notig", "il manque %s"},
        {"seal.targetmet",    "target met", "obiettivo raggiunto", "Ziel erreicht", "objectif atteint"},
        {"seal.select",       "select up to five cards", "seleziona fino a cinque carte",
                              "wahle bis zu funf Karten", "choisissez jusqu'a cinq cartes"},
        {"seal.wouldscore",   "this hand would score", "questa mano varrebbe",
                              "diese Hand wurde geben", "cette main vaudrait"},
        {"seal.thishand",     "this hand", "questa mano", "diese Hand", "cette main"},
        {"seal.hands",        "HANDS", "MANI", "HANDE", "MAINS"},
        {"seal.discards",     "DISCARDS", "SCARTI", "ABWURFE", "DEFAUSSES"},
        {"seal.handsTip",     "Hands left. Miss the target before they run out and the run is over.",
            "Mani rimaste. Se finiscono senza raggiungere l'obiettivo, la partita finisce.",
            "Verbleibende Hande. Gehen sie aus, bevor das Ziel erreicht ist, endet der Lauf.",
            "Mains restantes. Si elles s'epuisent avant l'objectif, la partie est finie."},
        {"seal.discardsTip",  "Discards left. Discarding never costs a hand.",
            "Scarti rimasti. Scartare non costa mai una mano.",
            "Verbleibende Abwurfe. Abwerfen kostet nie eine Hand.",
            "Defausses restantes. Defausser ne coute jamais une main."},
        {"seal.play",         "PLAY HAND", "GIOCA MANO", "HAND SPIELEN", "JOUER LA MAIN"},
        {"seal.discard",      "DISCARD", "SCARTA", "ABWERFEN", "DEFAUSSER"},
        {"seal.sortrank",     "SORT: RANK", "ORDINA: VALORE", "SORT: WERT", "TRI: VALEUR"},
        {"seal.sortsuit",     "SORT: SUIT", "ORDINA: SEME", "SORT: FARBE", "TRI: COULEUR"},
        {"seal.clear",        "CLEAR", "PULISCI", "LEEREN", "EFFACER"},
        {"seal.keyhint",      "Enter play · X discard · Tab sort",
                              "Invio gioca · X scarta · Tab ordina",
                              "Enter spielen · X abwerfen · Tab sortieren",
                              "Entree jouer · X defausser · Tab trier"},
        {"seal.sigils",       "SIGILS", "SIGILLI", "SIEGEL", "SCEAUX"},
        {"seal.draw",         "draw %d", "mazzo %d", "Stapel %d", "pioche %d"},
        {"seal.used",         "used %d", "usate %d", "benutzt %d", "utilisees %d"},
        {"seal.broken",       "SEAL BROKEN", "SIGILLO SPEZZATO", "SIEGEL GEBROCHEN", "SCEAU BRISE"},
        {"seal.holds",        "THE SEAL HOLDS", "IL SIGILLO REGGE", "DAS SIEGEL HALT", "LE SCEAU TIENT"},
        {"seal.finalscore",   "Final score", "Punteggio finale", "Endstand", "Score final"},
        {"seal.required",     "Required", "Richiesto", "Benotigt", "Requis"},
        {"seal.reward",       "Seal reward", "Ricompensa", "Belohnung", "Recompense"},
        {"seal.unused",       "Unused hands", "Mani non usate", "Ungenutzte Hande", "Mains inutilisees"},
        {"seal.banked",       "Banked this seal", "Guadagnato", "Verdient", "Gagne"},
        {"seal.shortby",      "Short by", "Mancavano", "Es fehlten", "Il manquait"},
        {"seal.sealsbroken",  "Seals broken", "Sigilli spezzati", "Gebrochene Siegel", "Sceaux brises"},
        {"seal.continue",     "CONTINUE", "CONTINUA", "WEITER", "CONTINUER"},
        {"seal.endrun",       "END RUN", "FINE PARTITA", "LAUF BEENDEN", "FIN DE PARTIE"},
        {"seal.takes",        "You take the %s.", "Ottieni il %s.", "Du erhaltst das %s.", "Vous obtenez le %s."},
        {"seal.charges",      "%d charge(s) left this seal.", "%d carica/che in questo sigillo.",
                              "%d Ladung(en) in diesem Siegel.", "%d charge(s) pour ce sceau."},
        {"tier.lesser",       "LESSER SEAL", "SIGILLO MINORE", "KLEINES SIEGEL", "SCEAU MINEUR"},
        {"tier.greater",      "GREATER SEAL", "SIGILLO MAGGIORE", "GROSSES SIEGEL", "SCEAU MAJEUR"},
        {"tier.champion",     "CHAMPION SEAL", "SIGILLO CAMPIONE", "CHAMPION-SIEGEL", "SCEAU CHAMPION"},
        {"tier.warden",       "WARDEN", "CUSTODE", "WACHTER", "GARDIEN"},
        {"tier.heart",        "THE HEART", "IL CUORE", "DAS HERZ", "LE COEUR"},

        // --- map, shop, choices, hud, end
        {"map.unexplored",  "%d site(s) unexplored  ·  click a lit node, or press 1-9  ·  scroll to zoom",
            "%d luoghi inesplorati  ·  clicca un nodo acceso, o premi 1-9  ·  rotella per zoomare",
            "%d Orte unerforscht  ·  klicke einen hellen Knoten oder drucke 1-9  ·  Rad zum Zoomen",
            "%d lieux inexplores  ·  cliquez un noeud eclaire, ou 1-9  ·  molette pour zoomer"},
        {"map.cleared",     "cleared", "completato", "erledigt", "termine"},
        {"map.gateOpen",    "Open. Your %s carries you across.", "Aperta. Il tuo %s ti porta oltre.",
            "Offen. Dein %s bringt dich hinuber.", "Ouverte. Votre %s vous fait passer."},
        {"map.gateSealed",  "Sealed. You need the %s to pass. Wardens carry them.",
            "Sigillata. Serve il %s per passare. Li portano i Custodi.",
            "Versiegelt. Du brauchst das %s. Wachter tragen sie.",
            "Scellee. Il faut le %s pour passer. Les Gardiens les portent."},
        {"map.grants",      "Grants the %s.", "Concede il %s.", "Gewahrt das %s.", "Accorde le %s."},
        {"map.sealedBy",    "Sealed by: %s.", "Sigillata da: %s.", "Versiegelt durch: %s.", "Scellee par : %s."},
        {"map.menu",        "MENU", "MENU", "MENU", "MENU"},
        {"shop.title",      "THE BAZAAR", "IL BAZAR", "DER BASAR", "LE BAZAR"},
        {"shop.sub",        "Five item slots is the whole build. Choose what you carry.",
            "Cinque slot oggetto sono tutta la build. Scegli cosa portare.",
            "Funf Gegenstandsplatze sind der ganze Build. Wahle mit Bedacht.",
            "Cinq emplacements d'objets, c'est tout le build. Choisissez bien."},
        {"shop.reroll",     "REROLL", "RIGENERA", "NEU MISCHEN", "RELANCER"},
        {"shop.leave",      "LEAVE", "ESCI", "VERLASSEN", "PARTIR"},
        {"shop.yourbuild",  "YOUR BUILD  ·  right-click an item to sell it",
            "LA TUA BUILD  ·  tasto destro su un oggetto per venderlo",
            "DEIN BUILD  ·  Rechtsklick auf einen Gegenstand zum Verkaufen",
            "VOTRE BUILD  ·  clic droit sur un objet pour le vendre"},
        {"shop.sold",       "SOLD", "VENDUTO", "VERKAUFT", "VENDU"},
        {"choice.moveon",   "MOVE ON", "PROSEGUI", "WEITER", "CONTINUER"},
        {"choice.walkaway", "WALK AWAY", "VAI VIA", "WEGGEHEN", "S'EN ALLER"},
        {"hud.ante",        "ANTE", "PUNTATA", "EINSATZ", "MISE"},
        {"hud.deck",        "DECK", "MAZZO", "DECK", "PAQUET"},
        {"hud.seals",       "SEALS", "SIGILLI", "SIEGEL", "SCEAUX"},
        {"hud.gold",        "Gold", "Oro", "Gold", "Or"},
        {"hud.goldTip",     "Interest pays $1 per $5 banked, up to $5, every time you break a seal.",
            "Gli interessi danno $1 ogni $5, fino a $5, ogni volta che spezzi un sigillo.",
            "Zinsen zahlen $1 pro $5, bis zu $5, jedes Mal wenn du ein Siegel brichst.",
            "Les interets rapportent $1 par $5, jusqu'a $5, a chaque sceau brise."},
        {"end.win",         "THE HEART FALLS", "IL CUORE CADE", "DAS HERZ FALLT", "LE COEUR TOMBE"},
        {"end.lose",        "RUN OVER", "PARTITA FINITA", "LAUF VORBEI", "PARTIE TERMINEE"},
        {"end.handsplayed", "Hands played", "Mani giocate", "Gespielte Hande", "Mains jouees"},
        {"end.best",        "Best single hand", "Miglior mano", "Beste Hand", "Meilleure main"},
        {"end.goldbanked",  "Gold banked", "Oro accumulato", "Gold gesammelt", "Or accumule"},
        {"end.decksize",    "Deck size", "Dimensione mazzo", "Deckgrosse", "Taille du paquet"},
        {"end.handsize",    "Hand size", "Carte in mano", "Handkartenzahl", "Cartes en main"},
        {"end.build",       "BUILD", "BUILD", "BUILD", "BUILD"},

        // --- poker hands
        {"hand.high_card",       "High Card", "Carta Alta", "Hohe Karte", "Carte Haute"},
        {"hand.pair",            "Pair", "Coppia", "Paar", "Paire"},
        {"hand.two_pair",        "Two Pair", "Doppia Coppia", "Zwei Paare", "Deux Paires"},
        {"hand.three_kind",      "Three of a Kind", "Tris", "Drilling", "Brelan"},
        {"hand.straight",        "Straight", "Scala", "Strasse", "Suite"},
        {"hand.flush",           "Flush", "Colore", "Flush", "Couleur"},
        {"hand.full_house",      "Full House", "Full", "Full House", "Full"},
        {"hand.four_kind",       "Four of a Kind", "Poker", "Vierling", "Carre"},
        {"hand.straight_flush",  "Straight Flush", "Scala Colore", "Strassenflush", "Quinte Flush"},
        {"hand.five_kind",       "Five of a Kind", "Cinque Uguali", "Funfling", "Cinq Identiques"},
        {"hand.flush_house",     "Flush House", "Full Colore", "Flush House", "Full Couleur"},
        {"hand.flush_five",      "Flush Five", "Cinque Colore", "Flush Funf", "Cinq Couleur"},

        // --- suits
        {"suit.spades",   "Spades", "Picche", "Pik", "Pique"},
        {"suit.hearts",   "Hearts", "Cuori", "Herz", "Coeur"},
        {"suit.diamonds", "Diamonds", "Quadri", "Karo", "Carreau"},
        {"suit.clubs",    "Clubs", "Fiori", "Kreuz", "Trefle"},

        // --- sigils
        {"sigil.key",   "Key Sigil", "Sigillo Chiave", "Schlusselsiegel", "Sceau de la Cle"},
        {"sigil.tide",  "Tide Sigil", "Sigillo Marea", "Flutsiegel", "Sceau de la Maree"},
        {"sigil.echo",  "Echo Sigil", "Sigillo Eco", "Echosiegel", "Sceau de l'Echo"},
        {"sigil.prism", "Prism Sigil", "Sigillo Prisma", "Prismensiegel", "Sceau du Prisme"},
        {"sigilshort.key",   "Key", "Chiave", "Schlussel", "Cle"},
        {"sigilshort.tide",  "Tide", "Marea", "Flut", "Maree"},
        {"sigilshort.echo",  "Echo", "Eco", "Echo", "Echo"},
        {"sigilshort.prism", "Prism", "Prisma", "Prisma", "Prisme"},
        {"sigildesc.key",   "Draw %d extra cards into your hand right now.",
            "Pesca subito %d carte in piu.", "Ziehe sofort %d zusatzliche Karten.",
            "Piochez %d cartes supplementaires maintenant."},
        {"sigildesc.tide",  "Throw your whole hand away and draw a fresh one. Costs no discard.",
            "Butta tutta la mano e pescane una nuova. Non costa scarti.",
            "Wirf die ganze Hand weg und ziehe neu. Kostet keinen Abwurf.",
            "Jetez toute votre main et repiochez. Ne coute aucune defausse."},
        {"sigildesc.echo",  "Your next hand retriggers every scoring card %d extra time(s).",
            "La prossima mano riattiva ogni carta che segna %d volta/e in piu.",
            "Deine nachste Hand lost jede wertende Karte %d mal zusatzlich aus.",
            "Votre prochaine main redeclenche chaque carte %d fois de plus."},
        {"sigildesc.prism", "Your next hand gains X%s Mult.",
            "La prossima mano guadagna X%s Molt.", "Deine nachste Hand erhalt X%s Mult.",
            "Votre prochaine main gagne X%s Mult."},
        {"gate.key",   "Sealed Door", "Porta Sigillata", "Verschlossene Tur", "Porte Scellee"},
        {"gate.tide",  "Mire", "Palude", "Sumpf", "Bourbier"},
        {"gate.echo",  "Chasm", "Abisso", "Abgrund", "Gouffre"},
        {"gate.prism", "Rubble", "Macerie", "Trummer", "Decombres"},

        // --- boss rules
        {"boss.inhibitor",   "Inhibitor", "Inibitore", "Hemmer", "Inhibiteur"},
        {"boss.fog_of_war",  "Fog of War", "Nebbia di Guerra", "Nebel des Krieges", "Brouillard de Guerre"},
        {"boss.sump",        "The Sump", "Il Pozzo", "Der Schacht", "Le Puisard"},
        {"boss.null_zone",   "Null Zone", "Zona Nulla", "Nullzone", "Zone Nulle"},
        {"boss.executioner", "Executioner", "Boia", "Henker", "Bourreau"},
        {"boss.drain",       "The Drain", "Lo Scarico", "Der Abfluss", "La Vidange"},
        {"boss.bramble",     "Bramble Ward", "Rovi", "Dornenwacht", "Ronces"},
        {"boss.tithe",       "The Tithe", "La Decima", "Der Zehnt", "La Dime"},
        {"boss.shackle",     "Shackle", "Ceppi", "Fessel", "Entrave"},
        {"boss.mirror",      "The Mirror", "Lo Specchio", "Der Spiegel", "Le Miroir"},
        {"boss.jammer",      "The Jammer", "Il Disturbatore", "Der Storer", "Le Brouilleur"},
        {"bossdesc.inhibitor",   "Every poker hand scores as if it were level 1.",
            "Ogni mano di poker vale come se fosse livello 1.",
            "Jede Pokerhand wertet als ware sie Stufe 1.",
            "Chaque main compte comme si elle etait niveau 1."},
        {"bossdesc.fog_of_war",  "You cannot read the suits of cards in your hand.",
            "Non puoi vedere i semi delle carte in mano.",
            "Du kannst die Farben deiner Handkarten nicht lesen.",
            "Vous ne voyez pas les couleurs de vos cartes."},
        {"bossdesc.sump",        "Your hand size is reduced by 2.",
            "Le carte in mano sono ridotte di 2.", "Deine Handkartenzahl sinkt um 2.",
            "Votre nombre de cartes en main baisse de 2."},
        {"bossdesc.null_zone",   "One suit is debuffed and scores nothing.",
            "Un seme e depotenziato e non segna nulla.",
            "Eine Farbe ist geschwacht und wertet nicht.",
            "Une couleur est affaiblie et ne compte pas."},
        {"bossdesc.executioner", "You get one fewer hand.", "Hai una mano in meno.",
            "Du hast eine Hand weniger.", "Vous avez une main de moins."},
        {"bossdesc.drain",       "You get one fewer discard.", "Hai uno scarto in meno.",
            "Du hast einen Abwurf weniger.", "Vous avez une defausse de moins."},
        {"bossdesc.bramble",     "The first card scored in each hand gives no Chips.",
            "La prima carta che segna in ogni mano non da Fiche.",
            "Die erste wertende Karte jeder Hand gibt keine Chips.",
            "La premiere carte comptee de chaque main ne donne pas de Jetons."},
        {"bossdesc.tithe",       "Every hand you play costs $2.", "Ogni mano giocata costa $2.",
            "Jede gespielte Hand kostet $2.", "Chaque main jouee coute $2."},
        {"bossdesc.shackle",     "Hands of fewer than three cards score nothing.",
            "Le mani con meno di tre carte non segnano.",
            "Hande mit weniger als drei Karten werten nicht.",
            "Les mains de moins de trois cartes ne comptent pas."},
        {"bossdesc.mirror",      "The same hand type cannot be played twice in a row.",
            "Non puoi giocare lo stesso tipo di mano due volte di fila.",
            "Derselbe Handtyp kann nicht zweimal hintereinander gespielt werden.",
            "Le meme type de main ne peut pas etre joue deux fois de suite."},
        {"bossdesc.jammer",      "Every Sigil has one fewer charge.",
            "Ogni Sigillo ha una carica in meno.", "Jedes Siegel hat eine Ladung weniger.",
            "Chaque Sceau a une charge de moins."},

        // --- tutorial
        {"tut.title",   "TUTORIAL", "TUTORIAL", "TUTORIAL", "TUTORIEL"},
        {"tut.skip",    "SKIP TUTORIAL", "SALTA TUTORIAL", "TUTORIAL UBERSPRINGEN", "PASSER LE TUTORIEL"},
        {"tut.s1",      "This is a Seal. Reach the score at the top left before your hands run out.",
            "Questo e un Sigillo. Raggiungi il punteggio in alto a sinistra prima di finire le mani.",
            "Das ist ein Siegel. Erreiche den Punktestand oben links, bevor die Hande ausgehen.",
            "Ceci est un Sceau. Atteignez le score en haut a gauche avant d'epuiser vos mains."},
        {"tut.s2",      "Click two cards of the SAME NUMBER to select them. That makes a Pair.",
            "Clicca due carte con lo STESSO NUMERO per selezionarle. Fanno una Coppia.",
            "Klicke zwei Karten mit derselben ZAHL. Das ergibt ein Paar.",
            "Cliquez deux cartes du MEME NUMERO. Cela fait une Paire."},
        {"tut.s2b",     "No pair in hand? Select any two cards and use DISCARD to draw new ones.",
            "Nessuna coppia? Seleziona due carte e usa SCARTA per pescarne altre.",
            "Kein Paar? Wahle zwei Karten und nutze ABWERFEN fur neue.",
            "Pas de paire ? Selectionnez deux cartes et DEFAUSSEZ pour repiocher."},
        {"tut.s3",      "The left panel shows Chips X Mult. That product is what the hand scores.",
            "Il pannello a sinistra mostra Fiche X Molt. Quel prodotto e il punteggio della mano.",
            "Das linke Feld zeigt Chips X Mult. Dieses Produkt ist der Wert der Hand.",
            "Le panneau gauche montre Jetons X Mult. Ce produit est le score de la main."},
        {"tut.s4",      "Now press PLAY HAND.", "Ora premi GIOCA MANO.",
            "Jetzt HAND SPIELEN drucken.", "Appuyez sur JOUER LA MAIN."},
        {"tut.s5",      "Each scoring card adds Chips, then everything is multiplied. Keep going.",
            "Ogni carta che segna aggiunge Fiche, poi si moltiplica tutto. Continua.",
            "Jede wertende Karte gibt Chips, dann wird alles multipliziert. Weiter so.",
            "Chaque carte ajoute des Jetons, puis tout est multiplie. Continuez."},
        {"tut.s6",      "Discards cost you nothing but a discard. Use them to hunt better hands.",
            "Gli scarti costano solo uno scarto. Usali per cercare mani migliori.",
            "Abwurfe kosten nur einen Abwurf. Nutze sie fur bessere Hande.",
            "Les defausses ne coutent qu'une defausse. Cherchez de meilleures mains."},
        {"tut.s7",      "Break the seal to finish. Bigger hands score far more than more cards.",
            "Spezza il sigillo per finire. Le mani migliori valgono molto piu di tante carte.",
            "Brich das Siegel zum Abschluss. Bessere Hande zahlen weit mehr als viele Karten.",
            "Brisez le sceau pour finir. Les meilleures mains valent bien plus que beaucoup de cartes."},
        {"tut.done",    "That is the whole game. Everything else is items that bend these rules.",
            "Questo e tutto il gioco. Il resto sono oggetti che piegano queste regole.",
            "Das ist das ganze Spiel. Alles andere sind Gegenstande, die diese Regeln beugen.",
            "Voila tout le jeu. Le reste, ce sont des objets qui plient ces regles."},
    };

    static {
        for (String[] row : T) {
            String[] vals = new String[row.length - 1];
            System.arraycopy(row, 1, vals, 0, vals.length);
            MAP.put(row[0], vals);
        }
    }
}
