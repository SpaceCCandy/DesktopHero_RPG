package ui;

import game.GameMap;
import processing.core.PApplet;
import rpg.CardDefinition;
import rpg.CardInventory;

public class BattleScreen {

    public enum Result {
        NONE,
        PLAYER_WON,
        PLAYER_LOST
    }

    private static final int PLAYER_MAX_HEALTH = 100;
    private static final int MAX_ENERGY = 100;
    private static final int ATTACK_ENERGY_GAIN = 15;
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
        String raw = type.name(); // "GEEK", "ACE", "JOCK"
        enemyName = raw.charAt(0) + raw.substring(1).toLowerCase(); // "Geek", "Ace", "Jock"
        playerHealth = PLAYER_MAX_HEALTH;
        energy = MAX_ENERGY;
        this.enemyMaxHealth = enemyMaxHealth;
        enemyHealth = enemyMaxHealth;
        this.enemyType = type;  // NEW
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

        drawStickFigure(p, 300, 245);
        drawStickFigure(p, 690, 245);

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

        // Check type effectiveness
        boolean effective = false;
        if (enemyType == GameMap.EnemyType.GEEK &&
                (card.subject.equals("Comp Sci") || card.subject.equals("Math") || card.subject.equals("Science"))) {
            effective = true;
        } else if (enemyType == GameMap.EnemyType.ACE &&
                (card.subject.equals("History") || card.subject.equals("English"))) {
            effective = true;
        } else if (enemyType == GameMap.EnemyType.JOCK && card.subject.equals("Gym")) {
            effective = true;
        }

        if (effective) dmg = (int)(dmg * 1.25f);

        enemyHealth -= dmg;

        if (enemyHealth <= 0) {
            enemyHealth = 0;
            message = "You won! Returning to map...";
            result = Result.PLAYER_WON;
            return;
        }

        String bonus = effective ? " (EFFECTIVE!)" : "";
        enemyAttackAndRegainEnergy(ATTACK_ENERGY_GAIN,
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

        message = prefix + "Enemy dealt " + enemyDamage + ". Energy +" + energyGain + ".";
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

    private float randomEnemyDamage() {
        return 10 + Math.round(Math.random() * 10);
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

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
