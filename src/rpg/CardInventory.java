package rpg;

import processing.core.PApplet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CardInventory {

    private static final String SAVE_FILE    = "data/card_inventory.csv";
    private static final String SAVE_VERSION = "V,3";  // bump to wipe old saves

    private final PApplet app;
    private final List<CardDefinition> backpack = new ArrayList<>();
    private final CardDefinition[] equipped = new CardDefinition[4];
    private int selectedSlot;
    private int gumballs;

    public CardInventory(PApplet app) {
        this.app = app;
        load();
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
            } else if ("E".equals(p[0]) && p.length >= 3) {
                int slot = PApplet.parseInt(p[1]);
                if (slot >= 0 && slot < equipped.length)
                    equipped[slot] = CardDefinition.findByName(p[2]);
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

        app.saveStrings(f.getAbsolutePath(), lines.toArray(new String[0]));
    }

    public List<CheatSheetDefinition> getCheatSheets() {
        return new ArrayList<>(); // placeholder until cheat sheets are implemented
    }
}