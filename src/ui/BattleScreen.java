package ui;

import game.AssetManager;
import game.GameMap;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import rpg.CardDefinition;
import rpg.CardInventory;
import rpg.CheatSheetDefinition;
import rpg.EnemyDefinition;

import java.util.ArrayList;
import java.util.List;

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

    // Active-effect badges, shown under the player's health/energy bars
    private static final int EFFECT_BADGE_X = 90, EFFECT_BADGE_Y = 152, EFFECT_BADGE_GAP = 6;

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
    private EnemyDefinition enemyDef; // resolved weak/resist/special-move profile for the current enemy
    private String enemySpriteId; // optional override: shows the exact NPC sprite fought
    private boolean cheatUsedThisTurn = false;  // resets each turn

    /**
     * A multi-turn effect currently active on the player, applied from a
     * cheat sheet. turnsRemaining counts down by 1 at the end of every
     * player turn (after either an attack or a skip); the effect expires
     * once it reaches 0.
     */
    private static class ActiveEffect {
        final CheatSheetDefinition.EffectType type;
        final float magnitude;
        int turnsRemaining;
        ActiveEffect(CheatSheetDefinition.EffectType type, float magnitude, int turnsRemaining) {
            this.type = type;
            this.magnitude = magnitude;
            this.turnsRemaining = turnsRemaining;
        }
    }

    private final List<ActiveEffect> activeEffects = new ArrayList<>();

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
        enemyType      = type;
        enemyDef       = EnemyDefinition.findByName(enemyName);
        // Named generic types (Geek/Ace/Jock) have null baseHealth in the
        // table — meaning "use whatever was passed in" (the distance-scaled
        // random hallway health). Only fixed-health enemies override it.
        this.enemyMaxHealth = (enemyDef.baseHealth != null) ? enemyDef.baseHealth : enemyMaxHealth;
        enemyHealth    = this.enemyMaxHealth;
        enemySpriteId  = null; // cleared by default; set explicitly for story/named NPCs
        message        = "Choose an attack!";
        result         = Result.NONE;
        cheatUsedThisTurn = false;
        activeEffects.clear();
    }

    /**
     * Overrides the enemy display name (e.g. "Math Test", "Jamie") and
     * re-resolves its combat profile (weak/resist subjects, fixed health if
     * the chart specifies one, and any special move) from EnemyDefinition.
     * Call this after reset() for any named story NPC so the real profile —
     * not the generic Geek/Ace/Jock fallback — applies to the fight.
     */
    public void setEnemyName(String name) {
        this.enemyName = name;
        this.enemyDef  = EnemyDefinition.findByName(name);
        if (enemyDef.baseHealth != null) {
            this.enemyMaxHealth = enemyDef.baseHealth;
            this.enemyHealth    = enemyDef.baseHealth;
        }
    }

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

        // Active effect badges (multi-turn cheat sheet buffs/debuffs in play)
        drawActiveEffectBadges(p);

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

        float dmg = card.damage;
        float typeMult = effectivenessMultiplier(card);
        dmg *= typeMult;

        // Apply any active player-damage-boosting effects (e.g. Industrial Revolution)
        float playerMult = currentPlayerDamageMultiplier();
        dmg *= playerMult;

        int finalDmg = Math.round(dmg);
        enemyHealth -= finalDmg;
        if (enemyHealth <= 0) {
            enemyHealth = 0;
            message     = "You won! Heading back to the halls...";
            result      = Result.PLAYER_WON;
            return;
        }

        String bonus = typeMult > 1f ? " (SUPER EFFECTIVE!)" : typeMult < 1f ? " (resisted...)" : "";
        String boost = playerMult > 1f ? " (BOOSTED!)" : playerMult < 1f ? " (weakened!)" : "";
        tickActiveEffects();
        enemyAttack(SKIP_ENERGY_GAIN / 2, "Dealt " + finalDmg + bonus + boost + ".  ");
    }

    private void skipTurn() {
        tickActiveEffects();
        enemyAttack(SKIP_ENERGY_GAIN, "Skipped turn — recovered energy.  ");
    }

    private void enemyAttack(int energyGain, String prefix) {
        cheatUsedThisTurn = false;  // new player turn starts after enemy attacks
        energy = Math.min(MAX_ENERGY, energy + energyGain);

        float dmg = 14 + (int)(Math.random() * 13);
        float enemyMult = currentEnemyDamageMultiplier();
        dmg *= enemyMult;
        int finalDmg = Math.round(dmg);

        playerHealth -= finalDmg;
        String debuff = enemyMult < 1f ? " (weakened!)" : "";

        // Special move roll — happens alongside the normal attack above, not
        // instead of it, per a 10% chance each of the enemy's attack turns.
        String specialMsg = maybeTriggerSpecialMove();

        if (playerHealth <= 0) {
            playerHealth = 0;
            message      = "You lost! Heading back...";
            result       = Result.PLAYER_LOST;
            return;
        }
        message = prefix + enemyName + " dealt " + finalDmg + debuff + ".  Energy +" + energyGain + "." + specialMsg;
    }

    /**
     * Rolls EnemyDefinition.SPECIAL_MOVE_CHANCE (10%) for each special move
     * the current enemy has. On a hit, applies that move's effect and
     * returns a short message describing it; returns an empty string if the
     * enemy has no special moves or none triggered this turn.
     */
    private String maybeTriggerSpecialMove() {
        if (enemyDef == null || enemyDef.specialMoves.length == 0) return "";
        StringBuilder msg = new StringBuilder();
        for (EnemyDefinition.SpecialMove move : enemyDef.specialMoves) {
            if (Math.random() >= EnemyDefinition.SPECIAL_MOVE_CHANCE) continue;
            switch (move.type) {
                case SELF_HEAL:
                    int healAmt = Math.round(move.magnitude);
                    enemyHealth = Math.min(enemyMaxHealth, enemyHealth + healAmt);
                    msg.append("  ").append(enemyName).append(" healed ").append(healAmt).append(" HP!");
                    break;
                case SELF_DAMAGE_BUFF:
                    activeEffects.add(new ActiveEffect(
                            CheatSheetDefinition.EffectType.ENEMY_DAMAGE_MULT, move.magnitude, move.duration));
                    msg.append("  ").append(enemyName).append(" is pumped up! Damage x")
                            .append(move.magnitude).append(" for ").append(move.duration).append(" turns!");
                    break;
                case WEAKEN_PLAYER:
                    activeEffects.add(new ActiveEffect(
                            CheatSheetDefinition.EffectType.PLAYER_DAMAGE_MULT, move.magnitude, move.duration));
                    msg.append("  ").append(enemyName).append(" rattled you! Your damage x")
                            .append(move.magnitude).append(" for ").append(move.duration).append(" turns!");
                    break;
            }
        }
        return msg.toString();
    }

    /**
     * Returns the damage multiplier for a card against the current enemy,
     * based on the bidirectional weak/resist table in EnemyDefinition:
     * 1.25x if the card's subject is something the enemy is weak to,
     * 0.75x if it's something the enemy resists, 1.0x otherwise. Falls back
     * to a neutral 1.0x if no enemy profile has been resolved yet.
     */
    private float effectivenessMultiplier(CardDefinition card) {
        if (enemyDef == null) return 1f;
        if (enemyDef.weakTo.contains(card.subject)) return 1.25f;
        if (enemyDef.resistantTo.contains(card.subject)) return 0.75f;
        return 1f;
    }

    // ── Status effects (multi-turn cheat sheet buffs/debuffs) ──────────────────

    /** Combined multiplier from all active PLAYER_DAMAGE_MULT effects (1.0 if none active). */
    private float currentPlayerDamageMultiplier() {
        float mult = 1f;
        for (ActiveEffect e : activeEffects) {
            if (e.type == CheatSheetDefinition.EffectType.PLAYER_DAMAGE_MULT) mult *= e.magnitude;
        }
        return mult;
    }

    /** Combined multiplier from all active ENEMY_DAMAGE_MULT effects (1.0 if none active). */
    private float currentEnemyDamageMultiplier() {
        float mult = 1f;
        for (ActiveEffect e : activeEffects) {
            if (e.type == CheatSheetDefinition.EffectType.ENEMY_DAMAGE_MULT) mult *= e.magnitude;
        }
        return mult;
    }

    /**
     * Applies heal-over-time healing for this turn, then counts every active
     * effect's remaining duration down by one and removes any that expire.
     * Call once per player turn (attack or skip), after damage/effects for
     * that turn have already been resolved.
     */
    private void tickActiveEffects() {
        int totalHeal = 0;
        for (ActiveEffect e : activeEffects) {
            if (e.type == CheatSheetDefinition.EffectType.HEAL_OVER_TIME) {
                totalHeal += Math.round(e.magnitude);
            }
        }
        if (totalHeal > 0) {
            playerHealth = Math.min(PLAYER_MAX_HEALTH, playerHealth + totalHeal);
        }

        for (int i = activeEffects.size() - 1; i >= 0; i--) {
            ActiveEffect e = activeEffects.get(i);
            e.turnsRemaining--;
            if (e.turnsRemaining <= 0) activeEffects.remove(i);
        }
    }

    // ── Cheat sheet logic ────────────────────────────────────────────────────────

    private void useCheatSheet() {
        if (cheatUsedThisTurn) { message = "Already used a cheat sheet this turn!"; return; }
        List<CheatSheetDefinition> sheets = inventory.getCheatSheets();
        if (sheets.isEmpty()) { message = "No cheat sheets in your backpack!"; return; }
        // Pick a random sheet from what the player owns
        int idx = (int)(Math.random() * sheets.size());
        CheatSheetDefinition sheet = inventory.useCheatSheet(idx);
        if (sheet == null) return;
        cheatUsedThisTurn = true;
        applyCheatSheetEffect(sheet);
    }

    private void applyCheatSheetEffect(CheatSheetDefinition sheet) {
        switch (sheet.effect) {
            case ENERGY_GAIN:
                energy = Math.min(MAX_ENERGY, energy + Math.round(sheet.magnitude));
                message = "Cheat Sheet: " + sheet.name + "! +" + Math.round(sheet.magnitude) + " energy.";
                break;

            case DAMAGE:
                int dmg = Math.round(sheet.magnitude);
                enemyHealth = Math.max(0, enemyHealth - dmg);
                message = "Cheat Sheet: " + sheet.name + "! Dealt " + dmg + " damage.";
                if (enemyHealth == 0) { message += "  You won!"; result = Result.PLAYER_WON; }
                break;

            case HEAL_OVER_TIME:
                // Heal immediately for this turn, then keep healing each turn
                // for the remaining duration (registered as an active effect).
                int healNow = Math.round(sheet.magnitude);
                playerHealth = Math.min(PLAYER_MAX_HEALTH, playerHealth + healNow);
                if (sheet.duration > 1) {
                    activeEffects.add(new ActiveEffect(sheet.effect, sheet.magnitude, sheet.duration - 1));
                    message = "Cheat Sheet: " + sheet.name + "! Healed " + healNow
                            + " HP now, +" + healNow + " for " + (sheet.duration - 1) + " more turns.";
                } else {
                    message = "Cheat Sheet: " + sheet.name + "! Healed " + healNow + " HP.";
                }
                break;

            case PLAYER_DAMAGE_MULT:
                activeEffects.add(new ActiveEffect(sheet.effect, sheet.magnitude, sheet.duration));
                message = "Cheat Sheet: " + sheet.name + "! Your damage x" + sheet.magnitude
                        + " for " + sheet.duration + " turn" + (sheet.duration == 1 ? "" : "s") + ".";
                break;

            case ENEMY_DAMAGE_MULT:
                activeEffects.add(new ActiveEffect(sheet.effect, sheet.magnitude, sheet.duration));
                message = "Cheat Sheet: " + sheet.name + "! " + enemyName + "'s damage x" + sheet.magnitude
                        + " for " + sheet.duration + " turn" + (sheet.duration == 1 ? "" : "s") + ".";
                break;
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

    private void drawActiveEffectBadges(PApplet p) {
        if (activeEffects.isEmpty()) return;

        int bx = EFFECT_BADGE_X;
        int by = EFFECT_BADGE_Y;
        int rowHeight = 24;
        int maxRowX = 250; // stay clear of the player fighter sprite drawn around x=290
        p.textFont(font); p.textSize(10);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);

        for (ActiveEffect e : activeEffects) {
            String label;
            int badgeColor;
            switch (e.type) {
                case PLAYER_DAMAGE_MULT:
                    label = "DMG x" + e.magnitude + " (" + e.turnsRemaining + ")";
                    badgeColor = 0xFFFFD9A0;
                    break;
                case ENEMY_DAMAGE_MULT:
                    label = enemyName + " DMG x" + e.magnitude + " (" + e.turnsRemaining + ")";
                    badgeColor = 0xFFD7C8FF;
                    break;
                case HEAL_OVER_TIME:
                    label = "Healing +" + Math.round(e.magnitude) + " (" + e.turnsRemaining + ")";
                    badgeColor = 0xFFC8F5C8;
                    break;
                default:
                    continue; // instant effects never linger and never reach here
            }
            float w = p.textWidth(label) + 16;
            if (bx + w > maxRowX) {
                bx = EFFECT_BADGE_X;
                by += rowHeight;
            }
            p.fill(badgeColor); p.stroke(120); p.strokeWeight(1);
            p.rect(bx, by, w, 20, 8);
            p.noStroke();
            p.fill(40);
            p.text(label, bx + 8, by + 10);
            bx += w + EFFECT_BADGE_GAP;
        }
        p.strokeWeight(1);
    }

    private void drawCheatSheetButton(PApplet p, int x, int y) {
        List<CheatSheetDefinition> sheets = inventory.getCheatSheets();
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
        drawFittedCardName(p, card.name, x + CARD_W / 2f, y + 23, CARD_W - 10, 18, 11);
        p.fill(110);
        p.textSize(10);
        p.text(card.subject, x + CARD_W / 2f, y + 40, CARD_W - 8, 18);
        p.fill(25);
        p.textSize(11);
        p.text(card.damage + " dmg", x + CARD_W / 2f - 22, y + 60);
        p.fill(80, 60, 140);
        p.text(card.energyCost + " ⚡", x + CARD_W / 2f + 30, y + 60);
    }

    /**
     * Draws a card title centered in a box, shrinking the font size until it
     * fits on one line, or wrapping it within the box if even the smallest
     * allowed size still doesn't fit — so long names never spill past the
     * card's edges.
     */
    private void drawFittedCardName(PApplet p, String text, float cx, float cy,
                                    float maxWidth, float maxHeight, int startSize) {
        int minSize = 8;
        p.textAlign(PApplet.CENTER, PApplet.CENTER);

        int size = startSize;
        p.textSize(size);
        while (size > minSize && p.textWidth(text) > maxWidth) {
            size--;
            p.textSize(size);
        }

        if (p.textWidth(text) <= maxWidth) {
            p.text(text, cx, cy);
        } else {
            p.text(text, cx, cy, maxWidth, maxHeight);
        }
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