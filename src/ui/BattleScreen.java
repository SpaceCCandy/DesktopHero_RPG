package ui;

import game.GameMap;
import processing.core.PApplet;
import processing.core.PFont;
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

    private final CardInventory inventory;
    private PFont font;

    private int    playerHealth;
    private int    enemyHealth;
    private int    enemyMaxHealth;
    private int    energy;
    private String message;
    private String enemyName = "Enemy";
    private Result result;
    private GameMap.EnemyType enemyType;

    public BattleScreen(CardInventory inventory) {
        this.inventory = inventory;
        reset();
    }

    public void reset() { reset(100, GameMap.EnemyType.GEEK); }

    public void reset(int enemyMaxHealth, GameMap.EnemyType type) {
        String raw = type.name();
        enemyName      = raw.charAt(0) + raw.substring(1).toLowerCase();
        playerHealth   = PLAYER_MAX_HEALTH;
        energy         = MAX_ENERGY;
        this.enemyMaxHealth = enemyMaxHealth;
        enemyHealth    = enemyMaxHealth;
        enemyType      = type;
        message        = "Choose an attack!";
        result         = Result.NONE;
    }

    /** Override the enemy display name (e.g. "Math Test"). */
    public void setEnemyName(String name) { this.enemyName = name; }

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
    }

    public void mousePressed(int mx, int my) {
        if (result != Result.NONE) return;
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
        p.noFill(); p.stroke(enemy ? 0xFF883030 : 0xFF305088);
        p.strokeWeight(4);
        p.ellipse(x, y - 72, 46, 46);
        p.line(x, y - 48, x, y + 20);
        p.line(x - 44, y - 25, x + 44, y - 25);
        p.line(x, y + 20, x - 28, y + 62);
        p.line(x, y + 20, x + 28, y + 62);
        p.strokeWeight(1);
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