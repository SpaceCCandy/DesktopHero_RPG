package ui;

import processing.core.PApplet;
import processing.core.PFont;
import rpg.CardDefinition;
import rpg.CardInventory;

import java.util.List;

public class MenuScreen {

    private enum Window { NONE, TRADE, INVENTORY, SETTINGS }

    // ── Mini toggle ──────────────────────────────────────────────────────────
    private static final int MINI_X = 10, MINI_Y = 10, MINI_W = 95, MINI_H = 34;

    // ── Main menu ────────────────────────────────────────────────────────────
    private static final int MENU_TITLE_Y     = 44,  MENU_GUMBALLS_Y  = 82;
    private static final int MENU_BTN_W       = 200, MENU_BTN_H       = 48;
    private static final int MENU_BTN_TRADE_Y = 124, MENU_BTN_INV_Y   = 192;
    private static final int MENU_BTN_SET_Y   = 260, MENU_BTN_PLAY_Y  = 328;

    // ── Trade window ─────────────────────────────────────────────────────────
    private static final int TRADE_W = 580, TRADE_H = 340;
    private static final int TRADE_LEGEND_X      = 24,  TRADE_COMMON_Y  = 66;
    private static final int TRADE_RARE_Y        = 84,  TRADE_LEGEND_Y  = 102;
    private static final int TRADE_CYBER_Y       = 120, TRADE_GUMBALLS_Y = 158;
    private static final int TRADE_BTN_Y         = 182, TRADE_BTN_W     = 130, TRADE_BTN_H = 38;
    private static final int TRADE_MESSAGE_Y     = 260;
    private static final int TRADE_CARDS_X       = 215, TRADE_CARDS_Y   = 58;
    private static final int TRADE_CARD_COL_W    = 66,  TRADE_CARD_ROW_H = 116;
    private static final int TRADE_CARD_W        = 56,  TRADE_CARD_H     = 96;
    private static final int TRADE_CARDS_PER_ROW = 5;

    // ── Inventory window ─────────────────────────────────────────────────────
    private static final int INVENTORY_W = 700, INVENTORY_H = 430;
    private static final int INV_SECTION_Y       = 76;
    private static final int INV_DECK_X          = 32;
    private static final int INV_DECK_COL_W      = 92,  INV_DECK_ROW_H   = 106;
    private static final int INV_DECK_START_Y    = 104;
    private static final int INV_DECK_W          = 74,  INV_DECK_H       = 92;
    private static final int INV_SELECT_BORDER   = 4;
    private static final int INV_PACK_X          = 252;
    private static final int INV_PACK_COL_W      = 96,  INV_PACK_ROW_H   = 98;
    private static final int INV_PACK_START_Y    = 104;
    private static final int INV_PACK_W          = 78,  INV_PACK_H       = 82;
    private static final int INV_PACK_COLS       = 4,   INV_PACK_ROWS    = 3;
    private static final int INV_SCROLL_X_OFF    = 38;
    private static final int INV_SCROLL_PAD_TOP  = 104, INV_SCROLL_PAD_BOT = 146;

    // ── Settings ─────────────────────────────────────────────────────────────
    private static final int SETTINGS_W = 320, SETTINGS_H = 320;

    // ── Card rendering ────────────────────────────────────────────────────────
    private static final int   CARD_STRIP_H      = 9;
    private static final int   CARD_RADIUS       = 6;
    private static final float CARD_NAME_POS     = 0.30f;
    private static final float CARD_SUBJ_POS     = 0.58f;
    private static final float CARD_DMG_POS      = 0.82f;
    private static final int   CARD_NAME_SIZE    = 11;
    private static final int   CARD_SUBJ_SIZE    = 10;
    private static final int   CARD_DMG_SIZE     = 11;

    // ── Window chrome ─────────────────────────────────────────────────────────
    private static final int WIN_TITLE_SIZE   = 21;
    private static final int WIN_TITLE_X      = 18, WIN_TITLE_Y = 26;
    private static final int WIN_CLOSE_MARGIN = 42, WIN_CLOSE_SIZE = 28, WIN_CLOSE_PAD = 11;
    private static final int SCROLLBAR_W     = 14,  SCROLLBAR_MIN   = 36, SCROLLBAR_R = 8;

    // ── Colors (softer palette) ───────────────────────────────────────────────
    private static final int C_BG         = 0xFFF5F2EC;   // warm off-white
    private static final int C_PANEL      = 0xFFEEEAE0;
    private static final int C_ACCENT     = 0xFF6B4E8A;   // soft purple
    private static final int C_ACCENT2    = 0xFF4E7A8A;   // teal accent
    private static final int C_TEXT       = 0xFF2A2030;
    private static final int C_TEXT_MED   = 0xFF4A4060;
    private static final int C_TEXT_SOFT  = 0xFF8A80A0;
    private static final int C_GUMBALLS   = 0xFF1A7DCC;
    private static final int C_COMMON     = 0xFF505050;
    private static final int C_RARE_R     = 40,  C_RARE_G  = 160, C_RARE_B  = 70;
    private static final int C_LEG_R      = 30,  C_LEG_G   = 140, C_LEG_B   = 210;
    private static final int C_CYBER_R    = 110, C_CYBER_G = 60,  C_CYBER_B = 220;
    private static final int C_SEL_R      = 80,  C_SEL_G   = 50,  C_SEL_B   = 160;
    private static final int C_CARD_STR   = 180;
    private static final int C_SCROLL_BG  = 240, C_SCROLL_FG = 140;
    private static final int C_BTN_FILL   = 0xFFE8E2F5;
    private static final int C_BTN_STR    = 0xFF9080B8;
    private static final int C_WIN_STR    = 0xFFA090C0;
    private static final int C_MINI_FILL  = 0xFFDED8F0;

    // ── State ─────────────────────────────────────────────────────────────────
    private final CardInventory inventory;
    private PFont font;
    private boolean open           = true;
    private Window  activeWindow   = Window.NONE;
    private String  tradeMessage   = "Spend gumballs to draw new cards!";
    private int     screenW        = 960, screenH = 580;
    private int     inventoryScroll;

    public MenuScreen(CardInventory inventory) {
        this.inventory = inventory;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void draw(PApplet p) {
        ensureFont(p);
        screenW = p.width; screenH = p.height;

        drawMiniButton(p);
        if (open) drawMainMenu(p);

        if      (activeWindow == Window.TRADE)     drawTradeWindow(p);
        else if (activeWindow == Window.INVENTORY) drawInventoryWindow(p);
        else if (activeWindow == Window.SETTINGS)  drawSettingsWindow(p);
    }

    public void mousePressed(int mx, int my) {
        if (inside(mx, my, MINI_X, MINI_Y, MINI_W, MINI_H)) { open = !open; return; }

        if (activeWindow == Window.TRADE     && handleTradeClick(mx, my))     return;
        if (activeWindow == Window.INVENTORY && handleInventoryClick(mx, my)) return;
        if (activeWindow == Window.SETTINGS  && handleSettingsClick(mx, my))  return;

        if (!open) return;
        int menuW = screenW / 3;
        int btnX  = menuW / 2 - MENU_BTN_W / 2;
        if      (inside(mx, my, btnX, MENU_BTN_TRADE_Y, MENU_BTN_W, MENU_BTN_H)) activeWindow = Window.TRADE;
        else if (inside(mx, my, btnX, MENU_BTN_INV_Y,   MENU_BTN_W, MENU_BTN_H)) activeWindow = Window.INVENTORY;
        else if (inside(mx, my, btnX, MENU_BTN_SET_Y,   MENU_BTN_W, MENU_BTN_H)) activeWindow = Window.SETTINGS;
        else if (inside(mx, my, btnX, MENU_BTN_PLAY_Y,  MENU_BTN_W, MENU_BTN_H)) open = false;
    }

    public void mouseWheel(float amount) {
        if (activeWindow != Window.INVENTORY) return;
        inventoryScroll = PApplet.constrain(
                inventoryScroll + (amount > 0 ? 1 : -1), 0, maxPackScroll());
    }

    public boolean isOpen() { return open; }

    // ── Draw: mini button ─────────────────────────────────────────────────────

    private void drawMiniButton(PApplet p) {
        p.fill(C_MINI_FILL); p.stroke(C_BTN_STR); p.strokeWeight(1.5f);
        p.rect(MINI_X, MINI_Y, MINI_W, MINI_H, 6);
        p.strokeWeight(1);
        p.fill(C_TEXT); useFont(p, 15);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.text("≡  Menu", MINI_X + MINI_W / 2f, MINI_Y + MINI_H / 2f);
    }

    // ── Draw: main menu ───────────────────────────────────────────────────────

    private void drawMainMenu(PApplet p) {
        int mW = screenW / 3, mH = screenH;
        int btnX = mW / 2 - MENU_BTN_W / 2;

        // Panel
        p.fill(C_BG); p.stroke(C_WIN_STR); p.strokeWeight(1.5f);
        p.rect(0, 0, mW, mH);
        p.strokeWeight(1);

        // Accent bar at top
        p.fill(C_ACCENT); p.noStroke();
        p.rect(0, 0, mW, 58);

        // Title
        p.fill(255); useFont(p, 22);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.text("📋  Menu", mW / 2f, MENU_TITLE_Y);

        // Gumballs
        p.fill(C_GUMBALLS); useFont(p, 15);
        p.text("🟡  " + inventory.getGumballs() + " gumballs", mW / 2f, MENU_GUMBALLS_Y);

        drawButton(p, "🛒  Trade",     btnX, MENU_BTN_TRADE_Y, MENU_BTN_W, MENU_BTN_H);
        drawButton(p, "🎒  Inventory", btnX, MENU_BTN_INV_Y,   MENU_BTN_W, MENU_BTN_H);
        drawButton(p, "⚙  Settings",  btnX, MENU_BTN_SET_Y,   MENU_BTN_W, MENU_BTN_H);
        drawButton(p, "▶  Play",       btnX, MENU_BTN_PLAY_Y,  MENU_BTN_W, MENU_BTN_H);
    }

    // ── Draw: trade window ────────────────────────────────────────────────────

    private void drawTradeWindow(PApplet p) {
        int x = cx(TRADE_W), y = cy(TRADE_H);
        drawWindowShell(p, "🛒  Card Shop", x, y, TRADE_W, TRADE_H);

        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        useFont(p, 12);
        p.fill(C_COMMON);
        p.text("Common  40%",      x + TRADE_LEGEND_X, y + TRADE_COMMON_Y);
        p.fill(C_RARE_R, C_RARE_G, C_RARE_B);
        p.text("Rare  30%",        x + TRADE_LEGEND_X, y + TRADE_RARE_Y);
        p.fill(C_LEG_R, C_LEG_G, C_LEG_B);
        p.text("Legendary  9%",    x + TRADE_LEGEND_X, y + TRADE_LEGEND_Y);
        p.fill(C_CYBER_R, C_CYBER_G, C_CYBER_B);
        p.text("Cyber Special  1%",x + TRADE_LEGEND_X, y + TRADE_CYBER_Y);

        p.fill(C_TEXT_MED);
        useFont(p, 13);
        p.text("Gumballs: " + inventory.getGumballs(), x + TRADE_LEGEND_X, y + TRADE_GUMBALLS_Y);
        drawButton(p, "Draw  20 🟡", x + TRADE_LEGEND_X, y + TRADE_BTN_Y, TRADE_BTN_W, TRADE_BTN_H);

        p.fill(C_TEXT_SOFT); useFont(p, 12);
        p.text(tradeMessage, x + TRADE_LEGEND_X, y + TRADE_MESSAGE_Y);

        for (int i = 0; i < CardDefinition.ALL.length; i++) {
            int cx = x + TRADE_CARDS_X + (i % TRADE_CARDS_PER_ROW) * TRADE_CARD_COL_W;
            int cy = y + TRADE_CARDS_Y  + (i / TRADE_CARDS_PER_ROW) * TRADE_CARD_ROW_H;
            drawSmallCard(p, CardDefinition.ALL[i], cx, cy, TRADE_CARD_W, TRADE_CARD_H);
        }
    }

    // ── Draw: inventory window ────────────────────────────────────────────────

    private void drawInventoryWindow(PApplet p) {
        int x = cx(INVENTORY_W), y = cy(INVENTORY_H);
        drawWindowShell(p, "🎒  Inventory", x, y, INVENTORY_W, INVENTORY_H);

        // Section labels
        p.fill(C_ACCENT); useFont(p, 14);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.text("Battle Deck", x + INV_DECK_X, y + INV_SECTION_Y);
        p.text("Backpack",    x + INV_PACK_X, y + INV_SECTION_Y);

        // Separator line under labels
        p.stroke(C_ACCENT); p.strokeWeight(1.5f);
        p.line(x + INV_DECK_X, y + INV_SECTION_Y + 12,
                x + INV_DECK_X + 160, y + INV_SECTION_Y + 12);
        p.line(x + INV_PACK_X, y + INV_SECTION_Y + 12,
                x + INVENTORY_W - 50, y + INV_SECTION_Y + 12);
        p.strokeWeight(1);

        // Equipped deck
        CardDefinition[] equipped = inventory.getEquippedCards();
        for (int i = 0; i < equipped.length; i++) {
            int cx = x + INV_DECK_X + (i % 2) * INV_DECK_COL_W;
            int cy = y + INV_DECK_START_Y + (i / 2) * INV_DECK_ROW_H;
            drawSmallCard(p, equipped[i], cx, cy, INV_DECK_W, INV_DECK_H);
            if (i == inventory.getSelectedSlot()) {
                p.noFill();
                p.stroke(C_SEL_R, C_SEL_G, C_SEL_B);
                p.strokeWeight(2.5f);
                p.rect(cx - INV_SELECT_BORDER, cy - INV_SELECT_BORDER,
                        INV_DECK_W + INV_SELECT_BORDER * 2,
                        INV_DECK_H + INV_SELECT_BORDER * 2, CARD_RADIUS);
                p.strokeWeight(1);
            }
        }

        // Backpack
        List<CardDefinition> backpack = inventory.getBackpackCards();
        int visible = INV_PACK_COLS * INV_PACK_ROWS;
        for (int i = 0; i < visible; i++) {
            int idx = inventoryScroll * INV_PACK_COLS + i;
            if (idx >= backpack.size()) break;
            int cx = x + INV_PACK_X + (i % INV_PACK_COLS) * INV_PACK_COL_W;
            int cy = y + INV_PACK_START_Y + (i / INV_PACK_COLS) * INV_PACK_ROW_H;
            drawSmallCard(p, backpack.get(idx), cx, cy, INV_PACK_W, INV_PACK_H);
        }

        drawScrollBar(p, x + INVENTORY_W - INV_SCROLL_X_OFF, y + INV_SCROLL_PAD_TOP,
                INVENTORY_H - INV_SCROLL_PAD_BOT, maxPackScroll(), inventoryScroll);
    }

    // ── Draw: settings window ─────────────────────────────────────────────────

    private void drawSettingsWindow(PApplet p) {
        int x = cx(SETTINGS_W), y = cy(SETTINGS_H);
        drawWindowShell(p, "⚙  Settings", x, y, SETTINGS_W, SETTINGS_H);
        p.fill(C_TEXT_SOFT); useFont(p, 13);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.text("(Nothing to configure yet!)", x + 20, y + 100);
    }

    // ── Click handlers ────────────────────────────────────────────────────────

    private boolean handleTradeClick(int mx, int my) {
        int x = cx(TRADE_W), y = cy(TRADE_H);
        if (handleClose(mx, my, x, y, TRADE_W)) return true;
        if (inside(mx, my, x + TRADE_LEGEND_X, y + TRADE_BTN_Y, TRADE_BTN_W, TRADE_BTN_H)) {
            CardDefinition pulled = inventory.buyRandomCard();
            tradeMessage = pulled != null
                    ? "Got: " + pulled.name + " (" + pulled.rarity + ")  [" + pulled.subject + "]"
                    : "Not enough gumballs!";
            return true;
        }
        return false;
    }

    private boolean handleInventoryClick(int mx, int my) {
        int x = cx(INVENTORY_W), y = cy(INVENTORY_H);
        if (handleClose(mx, my, x, y, INVENTORY_W)) return true;

        for (int i = 0; i < inventory.getEquippedCards().length; i++) {
            int cx = x + INV_DECK_X + (i % 2) * INV_DECK_COL_W;
            int cy = y + INV_DECK_START_Y + (i / 2) * INV_DECK_ROW_H;
            if (inside(mx, my, cx, cy, INV_DECK_W, INV_DECK_H)) {
                inventory.selectSlot(i); return true;
            }
        }

        List<CardDefinition> backpack = inventory.getBackpackCards();
        for (int i = 0; i < INV_PACK_COLS * INV_PACK_ROWS; i++) {
            int idx = inventoryScroll * INV_PACK_COLS + i;
            if (idx >= backpack.size()) break;
            int cx = x + INV_PACK_X + (i % INV_PACK_COLS) * INV_PACK_COL_W;
            int cy = y + INV_PACK_START_Y + (i / INV_PACK_COLS) * INV_PACK_ROW_H;
            if (inside(mx, my, cx, cy, INV_PACK_W, INV_PACK_H)) {
                inventory.swapWithBackpack(idx); return true;
            }
        }
        return false;
    }

    private boolean handleSettingsClick(int mx, int my) {
        return handleClose(mx, my, cx(SETTINGS_W), cy(SETTINGS_H), SETTINGS_W);
    }

    private boolean handleClose(int mx, int my, int x, int y, int w) {
        if (inside(mx, my, x + w - WIN_CLOSE_MARGIN, y + WIN_CLOSE_PAD, WIN_CLOSE_SIZE, WIN_CLOSE_SIZE)) {
            activeWindow = Window.NONE; return true;
        }
        return false;
    }

    // ── Draw primitives ───────────────────────────────────────────────────────

    private void drawWindowShell(PApplet p, String title, int x, int y, int w, int h) {
        // Shadow
        p.fill(0, 0, 0, 30); p.noStroke();
        p.rect(x + 5, y + 5, w, h, 10);

        // Window body
        p.fill(C_BG); p.stroke(C_WIN_STR); p.strokeWeight(1.5f);
        p.rect(x, y, w, h, 8);
        p.strokeWeight(1);

        // Title bar
        p.fill(C_ACCENT); p.noStroke();
        p.rect(x, y, w, 46, 8, 8, 0, 0);

        // Title text
        p.fill(255); useFont(p, WIN_TITLE_SIZE);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.text(title, x + WIN_TITLE_X, y + WIN_TITLE_Y);

        // Close button
        p.fill(255, 80, 80); p.noStroke();
        p.ellipse(x + w - 22, y + 22, 18, 18);
        p.fill(255); useFont(p, 12);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.text("✕", x + w - 22, y + 22);
    }

    private void drawScrollBar(PApplet p, int x, int y, int h, int max, int cur) {
        p.fill(C_SCROLL_BG); p.stroke(140); p.rect(x, y, SCROLLBAR_W, h, SCROLLBAR_R);
        float th = max == 0 ? h : Math.max(SCROLLBAR_MIN, h / (max + 1f));
        float ty = max == 0 ? y : y + (h - th) * (cur / (float) max);
        p.fill(C_SCROLL_FG); p.noStroke();
        p.rect(x + 2, ty + 2, SCROLLBAR_W - 4, th - 4, SCROLLBAR_R);
    }

    private void drawButton(PApplet p, String text, int x, int y, int w, int h) {
        p.fill(C_BTN_FILL); p.stroke(C_BTN_STR); p.strokeWeight(1.5f);
        p.rect(x, y, w, h, 6);
        p.strokeWeight(1);
        p.fill(C_ACCENT); useFont(p, 15);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.text(text, x + w / 2f, y + h / 2f);
    }

    private void drawSmallCard(PApplet p, CardDefinition card, int x, int y, int w, int h) {
        if (card == null) {
            p.fill(225); p.stroke(C_CARD_STR); p.rect(x, y, w, h, CARD_RADIUS);
            return;
        }
        // White body
        p.fill(255, 253, 248); p.stroke(C_CARD_STR); p.strokeWeight(1);
        p.rect(x, y, w, h, CARD_RADIUS);

        // Rarity strip
        p.fill(card.strokeColor); p.noStroke();
        p.rect(x + 1, y + 1, w - 2, CARD_STRIP_H, CARD_RADIUS, CARD_RADIUS, 0, 0);

        // Name
        p.fill(C_TEXT); useFont(p, CARD_NAME_SIZE);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.text(card.name, x + w / 2f, y + CARD_STRIP_H + (h - CARD_STRIP_H) * CARD_NAME_POS);

        // Subject
        p.fill(C_TEXT_SOFT); useFont(p, CARD_SUBJ_SIZE);
        p.text(card.subject, x + w / 2f, y + CARD_STRIP_H + (h - CARD_STRIP_H) * CARD_SUBJ_POS);

        // Damage
        p.fill(C_TEXT); useFont(p, CARD_DMG_SIZE);
        p.text(card.damage + " dmg", x + w / 2f, y + CARD_STRIP_H + (h - CARD_STRIP_H) * CARD_DMG_POS);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void ensureFont(PApplet p) {
        if (font == null) font = p.createFont("Comic Sans MS", 14, true);
    }

    private void useFont(PApplet p, int size) {
        p.textFont(font); p.textSize(size);
    }

    private int cx(int w) { return (screenW - w) / 2; }
    private int cy(int h) { return (screenH - h) / 2; }

    private int maxPackScroll() {
        int rows = (int) Math.ceil(inventory.getBackpackCards().size() / (double) INV_PACK_COLS);
        return Math.max(0, rows - INV_PACK_ROWS);
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}