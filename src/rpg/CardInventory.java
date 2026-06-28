package rpg;

import processing.core.PApplet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardInventory {

    private static final String SAVE_FILE    = "data/card_inventory.csv";
    private static final String SAVE_VERSION = "V,4";  // bump to wipe old saves

    /** How often the cheat-sheet shop's offerings rotate, in real-world milliseconds. */
    public static final long SHOP_REFRESH_INTERVAL_MS = 20L * 60L * 1000L; // 20 minutes
    /** How many cheat sheets are shown in the shop at once. */
    public static final int SHOP_SLOT_COUNT = 4;

    private final PApplet app;
    private final List<CardDefinition> backpack = new ArrayList<>();
    private final CardDefinition[] equipped = new CardDefinition[4];
    private final List<CheatSheetDefinition> cheatSheets = new ArrayList<>();
    private int selectedSlot;
    private int gumballs;

    // Shop rotation state. lastShopRefreshMillis uses real wall-clock time
    // (System.currentTimeMillis(), NOT Processing's millis()) specifically
    // because it needs to keep counting down correctly across app restarts —
    // millis() resets to 0 every time the sketch launches, which would make
    // "refresh every 20 real minutes" meaningless after closing and
    // reopening the game.
    private long lastShopRefreshMillis;
    private final List<CheatSheetDefinition> shopOffers = new ArrayList<>();

    public CardInventory(PApplet app) {
        this.app = app;
        load();
        ensureShopOffersCurrent();
    }

    public CardDefinition[] getEquippedCards() { return equipped; }
    public List<CardDefinition> getBackpackCards() { return backpack; }
    public int getSelectedSlot() { return selectedSlot; }
    public int getGumballs()     { return gumballs; }

    public void addGumballs(int amount) {
        gumballs += amount;
        save();
    }

    public CardDefinition buyRandomCard() {
        if (gumballs < 20) return null;
        gumballs -= 20;
        CardDefinition card = CardDefinition.randomCard(app);
        addCard(card);
        return card;
    }

    /** Buys a specific cheat sheet at its fixed gumball cost. Returns false if not enough gumballs. */
    public boolean buyCheatSheet(CheatSheetDefinition sheet) {
        if (sheet == null || gumballs < sheet.cost) return false;
        gumballs -= sheet.cost;
        addCheatSheet(sheet);
        return true;
    }

    // ── Cheat sheet shop rotation ────────────────────────────────────────────

    /**
     * Returns the cheat sheets currently available to buy in the shop (a
     * rotating subset of SHOP_SLOT_COUNT, refreshed every
     * SHOP_REFRESH_INTERVAL_MS of real time). Call this instead of
     * CheatSheetDefinition.ALL when drawing the shop.
     */
    public List<CheatSheetDefinition> getShopOffers() {
        ensureShopOffersCurrent();
        return shopOffers;
    }

    /** Milliseconds of real time remaining until the shop offers next rotate. */
    public long getShopRefreshMillisRemaining() {
        ensureShopOffersCurrent();
        long elapsed = System.currentTimeMillis() - lastShopRefreshMillis;
        return Math.max(0, SHOP_REFRESH_INTERVAL_MS - elapsed);
    }

    /** Re-rolls the shop offers right now, regardless of how much time has passed. */
    private void rollNewShopOffers() {
        List<CheatSheetDefinition> pool = new ArrayList<>(java.util.Arrays.asList(CheatSheetDefinition.ALL));
        Collections.shuffle(pool);
        shopOffers.clear();
        for (int i = 0; i < Math.min(SHOP_SLOT_COUNT, pool.size()); i++) {
            shopOffers.add(pool.get(i));
        }
        lastShopRefreshMillis = System.currentTimeMillis();
        save();
    }

    /** Rolls new offers if none exist yet or the refresh interval has elapsed. */
    private void ensureShopOffersCurrent() {
        boolean expired = (System.currentTimeMillis() - lastShopRefreshMillis) >= SHOP_REFRESH_INTERVAL_MS;
        if (shopOffers.isEmpty() || expired) {
            rollNewShopOffers();
        }
    }

    public void addCard(CardDefinition card) {
        backpack.add(card);
        save();
    }

    /** Add a card directly to the equipped deck in the given slot (0-3). */
    public void addCardToSlot(CardDefinition card, int slot) {
        if (slot >= 0 && slot < equipped.length) {
            if (equipped[slot] == null) {
                equipped[slot] = card;
            } else {
                backpack.add(card);
            }
            save();
        }
    }

    public void selectSlot(int slot) {
        if (slot >= 0 && slot < equipped.length) selectedSlot = slot;
    }

    public void swapWithBackpack(int backpackIndex) {
        if (backpackIndex < 0 || backpackIndex >= backpack.size()) return;
        CardDefinition bp  = backpack.get(backpackIndex);
        CardDefinition eq  = equipped[selectedSlot];
        equipped[selectedSlot] = bp;
        if (eq == null) backpack.remove(backpackIndex);
        else            backpack.set(backpackIndex, eq);
        save();
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    private void load() {
        File f = new File(app.sketchPath(SAVE_FILE));
        if (!f.exists()) { save(); return; }

        String[] lines = app.loadStrings(f.getAbsolutePath());
        if (lines == null || lines.length == 0 || !SAVE_VERSION.equals(lines[0])) {
            // Old version — wipe and start clean
            save();
            return;
        }

        for (String line : lines) {
            String[] p = PApplet.split(line, ',');
            if (p.length < 2) continue;
            if ("V".equals(p[0])) {
                // version line — already checked
            } else if ("G".equals(p[0])) {
                gumballs = PApplet.parseInt(p[1]);
            } else if ("B".equals(p[0])) {
                backpack.add(CardDefinition.findByName(p[1]));
            } else if ("CS".equals(p[0]) && p.length >= 2) {
                cheatSheets.add(CheatSheetDefinition.findByName(p[1]));
            } else if ("E".equals(p[0]) && p.length >= 3) {
                int slot = PApplet.parseInt(p[1]);
                if (slot >= 0 && slot < equipped.length)
                    equipped[slot] = CardDefinition.findByName(p[2]);
            } else if ("SHOPT".equals(p[0])) {
                try { lastShopRefreshMillis = Long.parseLong(p[1]); }
                catch (NumberFormatException ignored) { lastShopRefreshMillis = 0; }
            } else if ("SHOPS".equals(p[0])) {
                shopOffers.add(CheatSheetDefinition.findByName(p[1]));
            }
        }
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        File f = new File(app.sketchPath(SAVE_FILE));
        if (f.getParentFile() != null) f.getParentFile().mkdirs();

        lines.add(SAVE_VERSION);
        lines.add("G," + gumballs);
        for (CardDefinition c : backpack)        lines.add("B," + c.name);
        for (int i = 0; i < equipped.length; i++)
            if (equipped[i] != null) lines.add("E," + i + "," + equipped[i].name);
        for (CheatSheetDefinition s : cheatSheets) lines.add("CS," + s.name);
        lines.add("SHOPT," + lastShopRefreshMillis);
        for (CheatSheetDefinition s : shopOffers) lines.add("SHOPS," + s.name);

        app.saveStrings(f.getAbsolutePath(), lines.toArray(new String[0]));
    }

    public List<CheatSheetDefinition> getCheatSheets() { return cheatSheets; }

    public void addCheatSheet(CheatSheetDefinition sheet) {
        cheatSheets.add(sheet);
        save();
    }

    public CheatSheetDefinition useCheatSheet(int index) {
        if (index < 0 || index >= cheatSheets.size()) return null;
        CheatSheetDefinition sheet = cheatSheets.remove(index);
        save();
        return sheet;
    }
}