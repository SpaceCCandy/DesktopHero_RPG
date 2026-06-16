package rpg;

import processing.core.PApplet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CardInventory {

    private static final String SAVE_FILE = "data/card_inventory.csv";
    private static final String SAVE_VERSION = "V,2";

    private final PApplet app;
    private final List<CardDefinition> backpack = new ArrayList<>();
    private final List<CheatSheetDefinition> cheatSheets = new ArrayList<>();
    private final CardDefinition[] equipped = new CardDefinition[4];
    private int selectedSlot;
    private int gumballs;

    public CardInventory(PApplet app) {
        this.app = app;
        load();
    }

    public CardDefinition[] getEquippedCards() {
        return equipped;
    }

    public List<CardDefinition> getBackpackCards() {
        return backpack;
    }

    public List<CheatSheetDefinition> getCheatSheets() {
        return cheatSheets;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public int getGumballs() {
        return gumballs;
    }

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
        for (int i = 0; i < equipped.length; i++) {
            if (equipped[i] == null) {
                equipped[i] = card;
                save();
                return;
            }
        }

        backpack.add(card);
        save();
    }

    public void addCheatSheet(String name) {
        if (addCheatSheetIfMissing(name)) {
            save();
        }
    }

    private boolean addCheatSheetIfMissing(String name) {
        CheatSheetDefinition sheet = CheatSheetDefinition.findByName(name);
        for (CheatSheetDefinition owned : cheatSheets) {
            if (owned.name.equals(sheet.name)) {
                return false;
            }
        }
        cheatSheets.add(sheet);
        return true;
    }

    public void selectSlot(int slot) {
        if (slot >= 0 && slot < equipped.length) {
            selectedSlot = slot;
        }
    }

    public void swapWithBackpack(int backpackIndex) {
        if (backpackIndex < 0 || backpackIndex >= backpack.size()) {
            return;
        }

        CardDefinition backpackCard = backpack.get(backpackIndex);
        CardDefinition equippedCard = equipped[selectedSlot];

        equipped[selectedSlot] = backpackCard;

        if (equippedCard == null) {
            backpack.remove(backpackIndex);
        }
        else {
            backpack.set(backpackIndex, equippedCard);
        }

        save();
    }

    private void load() {
        File saveFile = new File(app.sketchPath(SAVE_FILE));

        if (!saveFile.exists()) {
            save();
            return;
        }

        String[] lines = app.loadStrings(saveFile.getAbsolutePath());

        if (lines == null) {
            save();
            return;
        }

        // Version check — wipe save if it's from an older version (different starting state)
        if (lines.length == 0 || !SAVE_VERSION.equals(lines[0])) {
            save();
            return;
        }

        for (String line : lines) {
            String[] parts = PApplet.split(line, ',');

            if (parts.length < 2) {
                continue;
            }

            if ("V".equals(parts[0])) {
                continue; // skip version line
            } else if ("G".equals(parts[0])) {
                gumballs = PApplet.parseInt(parts[1]);
            }
            else if ("B".equals(parts[0])) {
                backpack.add(CardDefinition.findByName(parts[1]));
            }
            else if ("E".equals(parts[0]) && parts.length >= 3) {
                int slot = PApplet.parseInt(parts[1]);

                if (slot >= 0 && slot < equipped.length) {
                    equipped[slot] = CardDefinition.findByName(parts[2]);
                }
            }
            else if ("S".equals(parts[0])) {
                addCheatSheetIfMissing(parts[1]);
            }
        }

        save();
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        File saveFile = new File(app.sketchPath(SAVE_FILE));
        File parentFolder = saveFile.getParentFile();

        if (parentFolder != null) {
            parentFolder.mkdirs();
        }

        lines.add(SAVE_VERSION);
        lines.add("G," + gumballs);

        for (CardDefinition card : backpack) {
            lines.add("B," + card.name);
        }

        for (CheatSheetDefinition sheet : cheatSheets) {
            lines.add("S," + sheet.name);
        }

        for (int i = 0; i < equipped.length; i++) {
            if (equipped[i] != null) {
                lines.add("E," + i + "," + equipped[i].name);
            }
        }

        app.saveStrings(saveFile.getAbsolutePath(), lines.toArray(new String[0]));
    }
}