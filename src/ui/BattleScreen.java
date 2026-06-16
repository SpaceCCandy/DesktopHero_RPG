package ui;

import game.GameMap;
import processing.core.PApplet;
import processing.core.PImage;
import rpg.CardDefinition;
import rpg.CardInventory;
import rpg.CheatSheetDefinition;

import java.util.HashSet;
import java.util.Set;

public class BattleScreen {

    public enum Result {
        NONE,
        PLAYER_WON,
        PLAYER_LOST
    }

    private static final int PLAYER_MAX_HEALTH = 100;
    private static final int MAX_ENERGY = 100;
    private static final int SKIP_ENERGY_GAIN = 30;

    private final CardInventory inventory;

    private int playerHealth;
    private int enemyHealth;
    private int enemyMaxHealth;
    private int energy;
    private String message;
    private String enemyName = "Enemy";
    private Result result;
    private GameMap.EnemyType enemyType;
    private PImage playerSprite;
    private PImage enemySprite;
    private String[] weaknessSubjects;
    private final Set<String> usedCheatSheets = new HashSet<>();
    private String cheatSheetPopup;
    private int cheatSheetPopupFrames;

    public BattleScreen(CardInventory inventory) {
        this.inventory = inventory;
        reset();
    }

    public void reset() {
        reset(100, GameMap.EnemyType.GEEK);
    }

    public GameMap.EnemyType getDefeatedEnemyType() {
        return enemyType;
    }

    public void reset(int enemyMaxHealth, GameMap.EnemyType type) {
        reset(enemyMaxHealth, type, null, null);
    }

    public void reset(int enemyMaxHealth, GameMap.EnemyType type, PImage playerSprite, PImage enemySprite) {
        reset(enemyMaxHealth, type, playerSprite, enemySprite, null);
    }

    public void reset(int enemyMaxHealth, GameMap.EnemyType type, PImage playerSprite, PImage enemySprite,
                      String[] weaknessSubjects) {
        String raw = type.name(); // "GEEK", "ACE", "JOCK"
        enemyName = raw.charAt(0) + raw.substring(1).toLowerCase(); // "Geek", "Ace", "Jock"
        playerHealth = PLAYER_MAX_HEALTH;
        energy = MAX_ENERGY;
        this.enemyMaxHealth = enemyMaxHealth;
        enemyHealth = enemyMaxHealth;
        this.enemyType = type;  // NEW
        this.playerSprite = playerSprite;
        this.enemySprite = enemySprite;
        this.weaknessSubjects = weaknessSubjects;
        usedCheatSheets.clear();
        cheatSheetPopup = null;
        cheatSheetPopupFrames = 0;
        message = "Choose an attack";
        result = Result.NONE;
    }

    public void showGumballReward(int amount) {
        message = "You won! +" + amount + " gumballs! Returning to map...";
    }

    public void draw(PApplet p) {
        p.background(245);

        p.fill(255);
        p.stroke(70);
        p.rect(55, 35, 850, 490, 4);

        drawHealthBar(p, "Player", 90, 70, playerHealth, PLAYER_MAX_HEALTH);
        drawEnergyBar(p, 90, 112);
        drawHealthBar(p, enemyName, 680, 420, enemyHealth, enemyMaxHealth);

        drawBattleSprite(p, playerSprite, 300, 310, 150, true);
        drawBattleSprite(p, enemySprite, 690, 310, 150, false);

        p.fill(20);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(18);
        p.text(message, 480, 365);

        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.textSize(15);
        p.text("Attacks", 90, 420);

        CardDefinition[] cards = inventory.getEquippedCards();

        for (int i = 0; i < cards.length; i++) {
            drawCard(p, cards[i], 90 + i * 122, 445);
        }

        drawSkipButton(p, 610, 445);
        drawCheatSheets(p, 760, 398);
        drawCheatSheetPopup(p);
    }

    public void mousePressed(int mx, int my) {
        if (result != Result.NONE) {
            return;
        }

        CardDefinition[] cards = inventory.getEquippedCards();

        for (int i = 0; i < cards.length; i++) {
            int x = 90 + i * 122;
            int y = 445;

            if (cards[i] != null && inside(mx, my, x, y, 110, 58)) {
                playTurn(cards[i]);
                return;
            }
        }

        if (inside(mx, my, 610, 445, 130, 70)) {
            skipTurn();
            return;
        }

        CheatSheetDefinition[] sheets = inventory.getCheatSheets().toArray(new CheatSheetDefinition[0]);
        for (int i = 0; i < sheets.length; i++) {
            int cx = 760;
            int cy = 412 + i * 76;
            if (inside(mx, my, cx, cy, 150, 68)) {
                useCheatSheet(sheets[i]);
                return;
            }
        }
    }

    public Result getResult() {
        return result;
    }

    private void playTurn(CardDefinition card) {
        if (energy < card.energyCost) {
            message = "Not enough energy for " + card.name + ".";
            return;
        }

        energy -= card.energyCost;

        int dmg = card.damage;

        boolean effective = isEffective(card.subject);

        if (effective) dmg = (int)(dmg * 1.25f);

        enemyHealth -= dmg;

        if (enemyHealth <= 0) {
            enemyHealth = 0;
            message = "You won! Returning to map...";
            result = Result.PLAYER_WON;
            return;
        }

        String bonus = effective ? " (EFFECTIVE!)" : "";
        enemyAttackAndRegainEnergy(0,
                "You dealt " + dmg + bonus + ". ");
    }

    private void skipTurn() {
        enemyAttackAndRegainEnergy(SKIP_ENERGY_GAIN, "You skipped and recovered energy. ");
    }

    private void enemyAttackAndRegainEnergy(int energyGain, String prefix) {
        energy = Math.min(MAX_ENERGY, energy + energyGain);

        int enemyDamage = (int) randomEnemyDamage();
        playerHealth -= enemyDamage;

        if (playerHealth <= 0) {
            playerHealth = 0;
            message = "You lost! Returning to map...";
            result = Result.PLAYER_LOST;
            return;
        }

        String energyText = energyGain > 0 ? " Energy +" + energyGain + "." : "";
        message = prefix + "Enemy dealt " + enemyDamage + "." + energyText;
    }

    private boolean isEffective(String subject) {
        if (weaknessSubjects != null && weaknessSubjects.length > 0) {
            for (String weakness : weaknessSubjects) {
                if (weakness.equals(subject)) {
                    return true;
                }
            }
            return false;
        }

        if (enemyType == GameMap.EnemyType.GEEK) {
            return subject.equals("Comp Sci") || subject.equals("Math") || subject.equals("Science");
        }
        if (enemyType == GameMap.EnemyType.ACE) {
            return subject.equals("History") || subject.equals("English");
        }
        return enemyType == GameMap.EnemyType.JOCK && subject.equals("Gym");
    }

    private void useCheatSheet(CheatSheetDefinition sheet) {
        if (usedCheatSheets.contains(sheet.name)) {
            cheatSheetPopup = sheet.name + " already used this battle.";
            cheatSheetPopupFrames = 120;
            return;
        }

        usedCheatSheets.add(sheet.name);
        if ("Extra Time".equals(sheet.name)) {
            energy = Math.min(MAX_ENERGY, energy + 35);
            message = "Extra Time restored 35 energy.";
        } else if ("Process of Elimination".equals(sheet.name)) {
            enemyHealth = Math.max(0, enemyHealth - 30);
            message = "Process of Elimination dealt 30 damage.";
            if (enemyHealth == 0) {
                result = Result.PLAYER_WON;
            }
        } else if ("Curve".equals(sheet.name)) {
            playerHealth = Math.min(PLAYER_MAX_HEALTH, playerHealth + 30);
            message = "Curve restored 30 HP.";
        }

        cheatSheetPopup = sheet.name + ": " + sheet.description;
        cheatSheetPopupFrames = 150;
    }

    private void drawHealthBar(PApplet p, String label, int x, int y, int health, int maxHealth) {
        p.fill(30);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.textSize(13);
        p.text(label, x, y - 16);
        p.textAlign(PApplet.RIGHT, PApplet.CENTER);
        p.text(health + "/" + maxHealth, x + 150, y - 16);

        p.fill(210);
        p.stroke(80);
        p.rect(x, y, 150, 16, 3);

        float fillWidth = 150 * (health / (float) maxHealth);
        p.fill(75, 200, 105);
        p.rect(x, y, fillWidth, 16, 3);

        p.fill(235, 65, 35);
        p.rect(x + fillWidth, y, 150 - fillWidth, 16, 3);
    }

    private void drawEnergyBar(PApplet p, int x, int y) {
        p.fill(30);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.textSize(13);
        p.text("Energy", x, y - 16);
        p.textAlign(PApplet.RIGHT, PApplet.CENTER);
        p.text(energy + "/" + MAX_ENERGY, x + 150, y - 16);

        p.fill(215);
        p.stroke(80);
        p.rect(x, y, 150, 16, 3);

        float fillWidth = 150 * (energy / (float) MAX_ENERGY);
        p.fill(80, 150, 255);
        p.rect(x, y, fillWidth, 16, 3);
    }

    private void drawStickFigure(PApplet p, int x, int y) {
        p.noFill();
        p.stroke(25);
        p.strokeWeight(4);
        p.ellipse(x, y - 72, 46, 46);
        p.line(x, y - 48, x, y + 20);
        p.line(x - 44, y - 25, x + 44, y - 25);
        p.line(x, y + 20, x - 28, y + 62);
        p.line(x, y + 20, x + 28, y + 62);
        p.strokeWeight(1);
    }

    private void drawBattleSprite(PApplet p, PImage sprite, int centerX, int footY, int maxH, boolean faceRight) {
        if (sprite == null || sprite.width <= 0 || sprite.height <= 0) {
            drawStickFigure(p, centerX, footY - 65);
            return;
        }

        float h = maxH;
        float w = h * sprite.width / (float) sprite.height;
        float x = centerX - w / 2f;
        float y = footY - h;

        p.pushMatrix();
        if (!faceRight) {
            p.translate(x + w, y);
            p.scale(-1, 1);
            p.image(sprite, 0, 0, w, h);
        } else {
            p.image(sprite, x, y, w, h);
        }
        p.popMatrix();
    }

    private float randomEnemyDamage() {
        return 20 + Math.round(Math.random() * 16);
    }

    private void drawCard(PApplet p, CardDefinition card, int x, int y) {
        if (card == null) {
            p.fill(230); p.stroke(150);
            p.rect(x, y, 110, 70, 5);
            return;
        }

        // White body, colored border only
        p.fill(255);
        p.stroke(card.strokeColor);
        p.strokeWeight(3);
        p.rect(x, y, 110, 70, 5);
        p.strokeWeight(1);

        p.fill(30);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(11);
        p.text(card.name, x + 55, y + 18);       // Name
        p.textSize(10);
        p.fill(100);
        p.text(card.subject, x + 55, y + 36);    // Subject
        p.fill(30);
        p.textSize(12);
        p.text(card.damage + " dmg / " + card.energyCost + " energy", x + 55, y + 54);
    }

    private void drawSkipButton(PApplet p, int x, int y) {
        p.fill(235);
        p.stroke(80);
        p.rect(x, y, 130, 70, 5);

        p.fill(20);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(15);
        p.text("Skip Turn", x + 65, y + 26);
        p.textSize(12);
        p.text("+" + SKIP_ENERGY_GAIN + " energy", x + 65, y + 48);
    }

    private void drawCheatSheets(PApplet p, int x, int labelY) {
        p.fill(20);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.textSize(14);
        p.text("Cheat Sheets", x, labelY);

        CheatSheetDefinition[] sheets = inventory.getCheatSheets().toArray(new CheatSheetDefinition[0]);
        if (sheets.length == 0) {
            p.fill(110);
            p.textSize(11);
            p.text("None", x, labelY + 20);
            return;
        }

        int cardW = 150;
        int cardH = 68;
        int gap = 8;
        for (int i = 0; i < sheets.length; i++) {
            int cx = x;
            int cy = labelY + 14 + i * (cardH + gap);
            boolean used = usedCheatSheets.contains(sheets[i].name);

            p.fill(used ? 215 : sheets[i].fillColor);
            p.stroke(used ? 160 : 90);
            p.strokeWeight(1.5f);
            p.rect(cx, cy, cardW, cardH, 5);
            p.strokeWeight(1);

            // Name at top
            p.fill(used ? 130 : 20);
            p.textAlign(PApplet.CENTER, PApplet.CENTER);
            p.textSize(11);
            p.text(sheets[i].name, cx + cardW / 2f, cy + 18);

            // Divider line
            p.stroke(used ? 160 : 120);
            p.line(cx + 8, cy + 30, cx + cardW - 8, cy + 30);

            // Description at bottom
            p.fill(used ? 130 : 60);
            p.textSize(10);
            p.text(sheets[i].description, cx + 6, cy + 36, cardW - 12, 26);
        }
    }

    private void drawCheatSheetPopup(PApplet p) {
        if (cheatSheetPopup == null || cheatSheetPopupFrames <= 0) {
            return;
        }

        cheatSheetPopupFrames--;
        p.fill(255, 250, 215);
        p.stroke(65);
        p.rect(330, 170, 300, 92, 5);
        p.fill(25);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(14);
        p.text(cheatSheetPopup, 350, 188, 260, 55);
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}