package ui;

import game.AssetManager;
import game.GameMap;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import rpg.CardDefinition;
import rpg.CardInventory;

public class BattleScreen {

    public enum Result { NONE, PLAYER_WON, PLAYER_LOST }

    private static final int PLAYER_MAX_HEALTH = 100;
    private static final int MAX_ENERGY        = 100;
    private static final int SKIP_ENERGY_GAIN  = 30;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int PANEL_X = 55, PANEL_Y = 30, PANEL_W = 850, PANEL_H = 500;
    private static final int CARD_W  = 115, CARD_H = 80, CARD_GAP = 10;
    private static final int CARDS_START_X = 90, CARDS_Y = 435;

    // Cheat Sheet button — bottom-right corner of the panel, clear of the
    // message box above and the attack cards/skip button to its left.
    private static final int CHEAT_BTN_W = 145, CHEAT_BTN_H = 40, CHEAT_BTN_MARGIN = 15;
    private static final int CHEAT_BTN_X = PANEL_X + PANEL_W - CHEAT_BTN_W - CHEAT_BTN_MARGIN;
    private static final int CHEAT_BTN_Y = PANEL_Y + PANEL_H - CHEAT_BTN_H - CHEAT_BTN_MARGIN;

    private final CardInventory inventory;
    private AssetManager assets;
    private PFont font;

    private int    playerHealth;
    private int    enemyHealth;
    private int    enemyMaxHealth;
    private int    energy;
    private String message;
    private String enemyName = "Enemy";
    private Result result;
    private GameMap.EnemyType enemyType;
    private String enemySpriteId; // optional override: shows the exact NPC sprite fought
    private boolean cheatUsedThisTurn = false;  // resets each turn

    public BattleScreen(CardInventory inventory) {
        this.inventory = inventory;
        reset();
    }

    public void setAssets(AssetManager assets) { this.assets = assets; }

    public void reset() { reset(100, GameMap.EnemyType.GEEK); }

    public void reset(int enemyMaxHealth, GameMap.EnemyType type) {
        String raw = type.name();
        enemyName      = raw.charAt(0) + raw.substring(1).toLowerCase();
        playerHealth   = PLAYER_MAX_HEALTH;
        energy         = MAX_ENERGY;
        this.enemyMaxHealth = enemyMaxHealth;
        enemyHealth    = enemyMaxHealth;
        enemyType      = type;
        enemySpriteId  = null; // cleared by default; set explicitly for story/named NPCs
        message        = "Choose an attack!";
        result         = Result.NONE;
        cheatUsedThisTurn = false;
    }

    /** Override the enemy display name (e.g. "Math Test"). */
    public void setEnemyName(String name) { this.enemyName = name; }

    /**
     * Override which sprite is shown for the enemy in battle, using the same
     * spriteId keys as AssetManager.storySprite (e.g. "mathtest", "rico",
     * "stacey"). When set, this takes priority over the generic GEEK/ACE/JOCK
     * sprite so the battle screen always matches the NPC the player just
     * interacted with. Pass null to fall back to the generic type sprite.
     */
    public void setEnemySpriteId(String spriteId) { this.enemySpriteId = spriteId; }

    public void showGumballReward(int amount) {
        message = "You won! +" + amount + " gumballs!  Heading back...";
    }

    public GameMap.EnemyType getDefeatedEnemyType() { return enemyType; }
    public Result getResult() { return result; }

    // ── Draw ──────────────────────────────────────────────────────────────────

    public void draw(PApplet p) {
        ensureFont(p);

        // Background
        p.background(240, 238, 232);

        // Main panel
        p.fill(255, 253, 248);
        p.stroke(160, 148, 130);
        p.strokeWeight(2);
        p.rect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 10);
        p.strokeWeight(1);

        // Title banner
        p.fill(80, 60, 100);
        p.noStroke();
        p.rect(PANEL_X, PANEL_Y, PANEL_W, 44, 10, 10, 0, 0);
        p.fill(255);
        p.textFont(font); p.textSize(20);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.text("⚔  Battle!", PANEL_X + PANEL_W / 2f, PANEL_Y + 22);

        // Health / energy bars
        drawHealthBar(p, "You",        90,  90, playerHealth, PLAYER_MAX_HEALTH, 0xFF4BC86A, 0xFFE03030);
        drawEnergyBar(p,               90, 132);
        drawHealthBar(p, enemyName,   680, 90, enemyHealth,  enemyMaxHealth,     0xFF4BC86A, 0xFFE03030);

        // Fighters
        drawFighter(p, 290, 270, false);
        drawFighter(p, 680, 270, true);

        // Message box
        p.fill(245, 240, 225);
        p.stroke(180, 165, 140);
        p.rect(PANEL_X + 20, 355, PANEL_W - 40, 52, 6);
        p.fill(40);
        p.textFont(font); p.textSize(15);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.text(message, PANEL_X + PANEL_W / 2f, 381);

        // Section label
        p.fill(80, 60, 100);
        p.textFont(font); p.textSize(14);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.text("Your Cards", CARDS_START_X, CARDS_Y - 14);

        // Cards
        CardDefinition[] cards = inventory.getEquippedCards();
        for (int i = 0; i < cards.length; i++) {
            drawCard(p, cards[i], CARDS_START_X + i * (CARD_W + CARD_GAP), CARDS_Y);
        }

        // Skip button
        drawSkipButton(p, CARDS_START_X + cards.length * (CARD_W + CARD_GAP) + 12, CARDS_Y);

        // Cheat Sheet button (only if available and not yet used this turn)
        if (inventory instanceof rpg.CardInventory) {
            drawCheatSheetButton(p, CHEAT_BTN_X, CHEAT_BTN_Y);
        }
    }

    public void mousePressed(int mx, int my) {
        if (result != Result.NONE) return;
        // Cheat Sheet button (bottom-right corner of the panel)
        if (inside(mx, my, CHEAT_BTN_X, CHEAT_BTN_Y, CHEAT_BTN_W, CHEAT_BTN_H)) {
            useCheatSheet();
            return;
        }
        CardDefinition[] cards = inventory.getEquippedCards();
        for (int i = 0; i < cards.length; i++) {
            int x = CARDS_START_X + i * (CARD_W + CARD_GAP);
            if (cards[i] != null && inside(mx, my, x, CARDS_Y, CARD_W, CARD_H)) {
                playTurn(cards[i]);
                return;
            }
        }
        int skipX = CARDS_START_X + cards.length * (CARD_W + CARD_GAP) + 12;
        if (inside(mx, my, skipX, CARDS_Y, 130, CARD_H)) skipTurn();
    }

    // ── Turn logic ────────────────────────────────────────────────────────────

    private void playTurn(CardDefinition card) {
        if (energy < card.energyCost) {
            message = "Not enough energy for " + card.name + "!";
            return;
        }
        energy -= card.energyCost;

        int dmg = card.damage;
        boolean effective = isEffective(card);
        if (effective) dmg = (int)(dmg * 1.25f);

        enemyHealth -= dmg;
        if (enemyHealth <= 0) {
            enemyHealth = 0;
            message     = "You won! Heading back to the halls...";
            result      = Result.PLAYER_WON;
            return;
        }

        String bonus = effective ? " (SUPER EFFECTIVE!)" : "";
        enemyAttack(SKIP_ENERGY_GAIN / 2, "Dealt " + dmg + bonus + ".  ");
    }

    private void skipTurn() {
        enemyAttack(SKIP_ENERGY_GAIN, "Skipped turn — recovered energy.  ");
    }

    private void enemyAttack(int energyGain, String prefix) {
        cheatUsedThisTurn = false;  // new player turn starts after enemy attacks
        energy = Math.min(MAX_ENERGY, energy + energyGain);
        int dmg = 14 + (int)(Math.random() * 13);
        playerHealth -= dmg;
        if (playerHealth <= 0) {
            playerHealth = 0;
            message      = "You lost! Heading back...";
            result       = Result.PLAYER_LOST;
            return;
        }
        message = prefix + enemyName + " dealt " + dmg + ".  Energy +" + energyGain + ".";
    }

    private boolean isEffective(CardDefinition card) {
        if (enemyType == GameMap.EnemyType.GEEK)
            return card.subject.equals("Comp Sci") || card.subject.equals("Math") || card.subject.equals("Science");
        if (enemyType == GameMap.EnemyType.ACE)
            return card.subject.equals("History") || card.subject.equals("English");
        if (enemyType == GameMap.EnemyType.JOCK)
            return card.subject.equals("Gym");
        return false;
    }

    // ── Cheat sheet logic ────────────────────────────────────────────────────────

    private void useCheatSheet() {
        if (cheatUsedThisTurn) { message = "Already used a cheat sheet this turn!"; return; }
        java.util.List<rpg.CheatSheetDefinition> sheets = inventory.getCheatSheets();
        if (sheets.isEmpty()) { message = "No cheat sheets in your backpack!"; return; }
        // Pick a random sheet from what the player owns
        int idx = (int)(Math.random() * sheets.size());
        rpg.CheatSheetDefinition sheet = inventory.useCheatSheet(idx);
        if (sheet == null) return;
        cheatUsedThisTurn = true;
        // Apply effect based on sheet
        if ("Extra Time".equals(sheet.name)) {
            energy = Math.min(MAX_ENERGY, energy + 35);
            message = "Cheat Sheet: Extra Time! +35 energy.";
        } else if ("Process of Elimination".equals(sheet.name)) {
            enemyHealth = Math.max(0, enemyHealth - 30);
            message = "Cheat Sheet: Process of Elimination! Dealt 30 damage.";
            if (enemyHealth == 0) { message += "  You won!"; result = Result.PLAYER_WON; }
        } else if ("Curve".equals(sheet.name)) {
            playerHealth = Math.min(PLAYER_MAX_HEALTH, playerHealth + 30);
            message = "Cheat Sheet: Curve! Healed 30 HP.";
        } else {
            // Generic random effect for any future sheets
            int roll = (int)(Math.random() * 3);
            if (roll == 0) { energy = Math.min(MAX_ENERGY, energy + 35); message = "Cheat Sheet: +35 energy!"; }
            else if (roll == 1) { enemyHealth = Math.max(0, enemyHealth - 30); message = "Cheat Sheet: 30 damage!"; }
            else { playerHealth = Math.min(PLAYER_MAX_HEALTH, playerHealth + 30); message = "Cheat Sheet: +30 HP!"; }
        }
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────

    private void ensureFont(PApplet p) {
        if (font == null) font = p.createFont("Comic Sans MS", 14, true);
    }

    private void drawHealthBar(PApplet p, String label, int x, int y,
                               int health, int maxHealth, int fillCol, int emptyCol) {
        p.textFont(font); p.textSize(13);
        p.fill(40);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.text(label, x, y - 16);
        p.textAlign(PApplet.RIGHT, PApplet.CENTER);
        p.text(health + "/" + maxHealth, x + 160, y - 16);

        p.fill(220); p.stroke(120); p.rect(x, y, 160, 18, 4);
        float fw = 160 * (health / (float) maxHealth);
        p.fill(fillCol);  p.noStroke(); p.rect(x,      y, fw,        18, 4, 0, 0, 4);
        p.fill(emptyCol); p.noStroke(); p.rect(x + fw, y, 160 - fw,  18, 0, 4, 4, 0);
    }

    private void drawEnergyBar(PApplet p, int x, int y) {
        p.textFont(font); p.textSize(13);
        p.fill(40);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.text("Energy", x, y - 16);
        p.textAlign(PApplet.RIGHT, PApplet.CENTER);
        p.text(energy + "/" + MAX_ENERGY, x + 160, y - 16);

        p.fill(220); p.stroke(120); p.rect(x, y, 160, 18, 4);
        float fw = 160 * (energy / (float) MAX_ENERGY);
        p.fill(0xFF4F96F0); p.noStroke(); p.rect(x, y, fw, 18, 4);
    }

    private void drawFighter(PApplet p, int x, int y, boolean enemy) {
        PImage sprite = null;
        if (assets != null) {
            if (enemy) {
                // Prefer the exact NPC sprite (e.g. the math test, Rico, Stacey) when one
                // was supplied; otherwise fall back to the generic Geek/Ace/Jock sprite.
                sprite = (enemySpriteId != null) ? assets.storySprite(enemySpriteId)
                        : assets.spriteForType(enemyType);
            } else {
                sprite = assets.playerIdle;
            }
        }
        if (sprite != null && sprite.width > 0 && sprite.height > 0) {
            int sprH = 130;
            int sprW = (int)(sprH * sprite.width / (float) sprite.height);
            if (!enemy) {
                p.image(sprite, x - sprW / 2f, y - sprH, sprW, sprH);
            } else {
                p.pushMatrix();
                p.translate(x + sprW / 2f, y - sprH);
                p.scale(-1, 1);
                p.image(sprite, 0, 0, sprW, sprH);
                p.popMatrix();
            }
        } else {
            // Fallback stick figure
            p.noFill(); p.stroke(enemy ? 0xFF883030 : 0xFF305088);
            p.strokeWeight(4);
            p.ellipse(x, y - 72, 46, 46);
            p.line(x, y - 48, x, y + 20);
            p.line(x - 44, y - 25, x + 44, y - 25);
            p.line(x, y + 20, x - 28, y + 62);
            p.line(x, y + 20, x + 28, y + 62);
            p.strokeWeight(1);
        }
    }

    private void drawCheatSheetButton(PApplet p, int x, int y) {
        java.util.List<rpg.CheatSheetDefinition> sheets = inventory.getCheatSheets();
        boolean hasSheets = !sheets.isEmpty();
        boolean canUse    = hasSheets && !cheatUsedThisTurn && result == Result.NONE;

        p.fill(canUse ? 0xFFFFF8C2 : 0xFFDDD8C0);
        p.stroke(canUse ? 0xFFBBA820 : 0xFF999080);
        p.strokeWeight(1.5f);
        p.rect(x, y, CHEAT_BTN_W, CHEAT_BTN_H, 6);
        p.strokeWeight(1);

        p.fill(canUse ? 0xFF5A4800 : 0xFF888070);
        p.textFont(font); p.textSize(12);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        String label = hasSheets
                ? (cheatUsedThisTurn ? "Cheat Used ✓" : "📋 Use Cheat (" + sheets.size() + ")")
                : "No Cheat Sheets";
        p.text(label, x + CHEAT_BTN_W / 2f, y + CHEAT_BTN_H / 2f);
    }

    private void drawCard(PApplet p, CardDefinition card, int x, int y) {
        if (card == null) {
            p.fill(225); p.stroke(160); p.rect(x, y, CARD_W, CARD_H, 6);
            return;
        }
        // Card body
        p.fill(255, 253, 248);
        p.stroke(card.strokeColor);
        p.strokeWeight(2);
        p.rect(x, y, CARD_W, CARD_H, 6);
        p.strokeWeight(1);

        // Colored rarity strip
        p.fill(card.fillColor);
        p.noStroke();
        p.rect(x + 1, y + 1, CARD_W - 2, 9, 5, 5, 0, 0);

        p.textFont(font);
        p.fill(25);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(11);
        p.text(card.name, x + CARD_W / 2f, y + 23);
        p.fill(110);
        p.textSize(10);
        p.text(card.subject, x + CARD_W / 2f, y + 40);
        p.fill(25);
        p.textSize(11);
        p.text(card.damage + " dmg", x + CARD_W / 2f - 22, y + 60);
        p.fill(80, 60, 140);
        p.text(card.energyCost + " ⚡", x + CARD_W / 2f + 30, y + 60);
    }

    private void drawSkipButton(PApplet p, int x, int y) {
        p.fill(235, 230, 220);
        p.stroke(140, 125, 100);
        p.strokeWeight(1.5f);
        p.rect(x, y, 130, CARD_H, 6);
        p.strokeWeight(1);
        p.fill(60, 50, 80);
        p.textFont(font); p.textSize(14);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.text("Skip Turn", x + 65, y + 30);
        p.fill(80, 120, 80);
        p.textSize(12);
        p.text("+" + SKIP_ENERGY_GAIN + " ⚡", x + 65, y + 56);
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}