package ui;

import processing.core.PApplet;
import rpg.CardDefinition;
import rpg.CardInventory;
import rpg.CheatSheetDefinition;

import java.util.List;

public class MenuScreen {

    private enum Window {
        NONE,
        TRADE,
        INVENTORY,
        SETTINGS
    }

    // ── Mini toggle button ───────────────────────────────────────────────────
    private static final int MINI_X = 10;
    private static final int MINI_Y = 10;
    private static final int MINI_W = 95;
    private static final int MINI_H = 34;

    // ── Main menu ────────────────────────────────────────────────────────────
    private static final int MENU_TITLE_Y      = 44;
    private static final int MENU_GUMBALLS_Y   = 84;
    private static final int MENU_BTN_W        = 200;
    private static final int MENU_BTN_H        = 48;
    private static final int MENU_BTN_TRADE_Y  = 130;
    private static final int MENU_BTN_INV_Y    = 198;
    private static final int MENU_BTN_SET_Y    = 266;
    private static final int MENU_BTN_PLAY_Y   = 334;
    private static final int MENU_TITLE_SIZE   = 26;
    private static final int MENU_GUMBALL_SIZE = 17;

    // ── Trade window ─────────────────────────────────────────────────────────
    private static final int TRADE_W              = 560;
    private static final int TRADE_H              = 330;
    private static final int TRADE_LEGEND_X       = 24;
    private static final int TRADE_COMMON_Y       = 60;
    private static final int TRADE_RARE_Y         = 78;
    private static final int TRADE_LEGEND_Y       = 96;
    private static final int TRADE_CYBER_Y        = 114;
    private static final int TRADE_GUMBALLS_Y     = 154;
    private static final int TRADE_BTN_Y          = 178;
    private static final int TRADE_BTN_W          = 130;
    private static final int TRADE_BTN_H          = 38;
    private static final int TRADE_MESSAGE_Y      = 248;
    private static final int TRADE_CARDS_START_X  = 205;
    private static final int TRADE_CARDS_START_Y  = 58;
    private static final int TRADE_CARD_COL_W     = 66;
    private static final int TRADE_CARD_ROW_H     = 116;
    private static final int TRADE_CARD_W         = 56;
    private static final int TRADE_CARD_H         = 96;
    private static final int TRADE_CARDS_PER_ROW  = 5;

    // ── Inventory window ─────────────────────────────────────────────────────
    private static final int INVENTORY_W            = 700;
    private static final int INVENTORY_H            = 480;
    private static final int INV_SECTION_LABEL_Y    = 76;
    private static final int INV_DECK_X             = 32;
    private static final int INV_DECK_CARD_COL_W    = 92;
    private static final int INV_DECK_CARD_ROW_H    = 106;
    private static final int INV_DECK_CARD_START_Y  = 104;
    private static final int INV_DECK_CARD_W        = 74;
    private static final int INV_DECK_CARD_H        = 92;
    private static final int INV_SELECT_BORDER      = 4;
    private static final int INV_PACK_X             = 250;
    private static final int INV_PACK_CARD_COL_W    = 96;
    private static final int INV_PACK_CARD_ROW_H    = 98;
    private static final int INV_PACK_CARD_START_Y  = 104;
    private static final int INV_PACK_CARD_W        = 78;
    private static final int INV_PACK_CARD_H        = 82;
    private static final int INV_PACK_COLS          = 4;
    private static final int INV_PACK_VISIBLE_ROWS  = 2;
    private static final int INV_SCROLLBAR_OFFSET_X = 38;
    private static final int INV_SCROLLBAR_PAD_TOP  = 104;
    private static final int INV_SCROLLBAR_PAD_BOT  = 196;

    // ── Settings window ──────────────────────────────────────────────────────
    private static final int SETTINGS_W = 320;
    private static final int SETTINGS_H = 320;

    // ── Small card rendering ─────────────────────────────────────────────────
    private static final int   CARD_STRIP_H      = 8;
    private static final int   CARD_STRIP_RADIUS = 5;
    private static final float CARD_NAME_POS     = 0.30f;
    private static final float CARD_SUBJ_POS     = 0.58f;
    private static final float CARD_DMG_POS      = 0.82f;
    private static final int   CARD_NAME_SIZE    = 11;
    private static final int   CARD_SUBJ_SIZE    = 10;
    private static final int   CARD_DMG_SIZE     = 12;

    // ── Window chrome ────────────────────────────────────────────────────────
    private static final int WINDOW_TITLE_SIZE    = 22;
    private static final int WINDOW_TITLE_X       = 18;
    private static final int WINDOW_TITLE_Y       = 26;
    private static final int WINDOW_CLOSE_MARGIN  = 42;
    private static final int WINDOW_CLOSE_SIZE    = 28;
    private static final int WINDOW_CLOSE_PAD     = 11;
    private static final int WINDOW_CLOSE_STROKE  = 4;
    private static final int SCROLLBAR_W          = 14;
    private static final int SCROLLBAR_THUMB_MIN  = 36;
    private static final int SCROLLBAR_RADIUS     = 8;

    // ── Button ───────────────────────────────────────────────────────────────
    private static final int BTN_TEXT_SIZE = 16;
    private static final int BTN_RADIUS    = 4;

    // ── Colors ───────────────────────────────────────────────────────────────
    private static final int COLOR_BG           = 0xFFF8F8F8;
    private static final int COLOR_TEXT_DARK    = 20;
    private static final int COLOR_TEXT_MED     = 30;
    private static final int COLOR_TEXT_GREY    = 100;
    private static final int COLOR_GUMBALLS     = 0xFF197DCC;  // blue
    private static final int COLOR_COMMON_TEXT  = 60;
    private static final int COLOR_RARE_R       = 45;
    private static final int COLOR_RARE_G       = 190;
    private static final int COLOR_RARE_B       = 80;
    private static final int COLOR_LEG_R        = 35;
    private static final int COLOR_LEG_G        = 160;
    private static final int COLOR_LEG_B        = 220;
    private static final int COLOR_CYBER_R      = 120;
    private static final int COLOR_CYBER_G      = 70;
    private static final int COLOR_CYBER_B      = 230;
    private static final int COLOR_SELECTED_R   = 30;
    private static final int COLOR_SELECTED_G   = 120;
    private static final int COLOR_SELECTED_B   = 255;
    private static final int COLOR_CARD_STROKE  = 180;
    private static final int COLOR_SCROLLBAR_BG = 245;
    private static final int COLOR_SCROLLBAR_FG = 120;
    private static final int COLOR_BTN_FILL     = 225;
    private static final int COLOR_BTN_STROKE   = 70;
    private static final int COLOR_WIN_STROKE   = 75;
    private static final int COLOR_MINI_FILL    = 220;

    // ── State ────────────────────────────────────────────────────────────────
    private final CardInventory inventory;
    private boolean open = true;
    private Window activeWindow = Window.NONE;
    private String tradeMessage = "The official currency is gumballs.";
    private int screenW = 960;
    private int screenH = 580;
    private int inventoryScroll;

    public MenuScreen(CardInventory inventory) {
        this.inventory = inventory;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public void draw(PApplet p) {
        screenW = p.width;
        screenH = p.height;

        drawMiniButton(p);

        if (open) drawMainMenu(p);

        if      (activeWindow == Window.TRADE)     drawTradeWindow(p);
        else if (activeWindow == Window.INVENTORY) drawInventoryWindow(p);
        else if (activeWindow == Window.SETTINGS)  drawSettingsWindow(p);
    }

    public void mousePressed(int mx, int my) {
        if (inside(mx, my, MINI_X, MINI_Y, MINI_W, MINI_H)) {
            open = !open;
            return;
        }

        if (activeWindow == Window.TRADE     && handleTradeClick(mx, my))     return;
        if (activeWindow == Window.INVENTORY && handleInventoryClick(mx, my)) return;
        if (activeWindow == Window.SETTINGS  && handleSettingsClick(mx, my))  return;

        if (!open) return;

        int menuW = screenW / 3;
        int btnX = menuW / 2 - MENU_BTN_W / 2;

        if      (inside(mx, my, btnX, MENU_BTN_TRADE_Y, MENU_BTN_W, MENU_BTN_H)) activeWindow = Window.TRADE;
        else if (inside(mx, my, btnX, MENU_BTN_INV_Y,   MENU_BTN_W, MENU_BTN_H)) activeWindow = Window.INVENTORY;
        else if (inside(mx, my, btnX, MENU_BTN_SET_Y,   MENU_BTN_W, MENU_BTN_H)) activeWindow = Window.SETTINGS;
        else if (inside(mx, my, btnX, MENU_BTN_PLAY_Y,  MENU_BTN_W, MENU_BTN_H)) open = false;
    }

    public void mouseWheel(float amount) {
        if (activeWindow != Window.INVENTORY) return;

        int maxScroll = maxInventoryScroll();
        inventoryScroll += amount > 0 ? 1 : -1;
        inventoryScroll = Math.max(0, Math.min(maxScroll, inventoryScroll));
    }

    public boolean isOpen() {
        return open;
    }

    // ── Draw: top-level panels ───────────────────────────────────────────────

    private void drawMiniButton(PApplet p) {
        p.fill(COLOR_MINI_FILL);
        p.stroke(0);
        p.rect(MINI_X, MINI_Y, MINI_W, MINI_H, BTN_RADIUS);

        p.fill(0);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(BTN_TEXT_SIZE);
        p.text("Menu", MINI_X + MINI_W / 2f, MINI_Y + MINI_H / 2f);
    }

    private void drawMainMenu(PApplet p) {
        int mW = screenW / 3;
        int mH = screenH;
        int btnX = mW / 2 - MENU_BTN_W / 2;

        p.fill(COLOR_BG);
        p.stroke(45);
        p.rect(0, 0, mW, mH);

        p.fill(COLOR_TEXT_DARK);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(MENU_TITLE_SIZE);
        p.text("Menu", mW / 2f, MENU_TITLE_Y);

        p.fill(COLOR_GUMBALLS);
        p.textSize(MENU_GUMBALL_SIZE);
        p.text(inventory.getGumballs() + " gumballs", mW / 2f, MENU_GUMBALLS_Y);

        drawButton(p, "Trade",     btnX, MENU_BTN_TRADE_Y, MENU_BTN_W, MENU_BTN_H);
        drawButton(p, "Inventory", btnX, MENU_BTN_INV_Y,   MENU_BTN_W, MENU_BTN_H);
        drawButton(p, "Settings",  btnX, MENU_BTN_SET_Y,   MENU_BTN_W, MENU_BTN_H);
        drawButton(p, "Play",      btnX, MENU_BTN_PLAY_Y,  MENU_BTN_W, MENU_BTN_H);
    }

    // ── Draw: windows ────────────────────────────────────────────────────────

    private void drawTradeWindow(PApplet p) {
        int x = centeredX(TRADE_W);
        int y = centeredY(TRADE_H);

        drawWindowShell(p, "Trade", x, y, TRADE_W, TRADE_H);

        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.textSize(12);

        p.fill(COLOR_COMMON_TEXT);
        p.text("Common 40%",      x + TRADE_LEGEND_X, y + TRADE_COMMON_Y);
        p.fill(COLOR_RARE_R,  COLOR_RARE_G,  COLOR_RARE_B);
        p.text("Rare 30%",        x + TRADE_LEGEND_X, y + TRADE_RARE_Y);
        p.fill(COLOR_LEG_R,   COLOR_LEG_G,   COLOR_LEG_B);
        p.text("Legendary 9%",    x + TRADE_LEGEND_X, y + TRADE_LEGEND_Y);
        p.fill(COLOR_CYBER_R, COLOR_CYBER_G, COLOR_CYBER_B);
        p.text("Cyber Special 1%",x + TRADE_LEGEND_X, y + TRADE_CYBER_Y);

        p.fill(COLOR_TEXT_MED);
        p.text("Gumballs: " + inventory.getGumballs(), x + TRADE_LEGEND_X, y + TRADE_GUMBALLS_Y);
        drawButton(p, "Draw 20", x + TRADE_LEGEND_X, y + TRADE_BTN_Y, TRADE_BTN_W, TRADE_BTN_H);
        p.text(tradeMessage, x + TRADE_LEGEND_X, y + TRADE_MESSAGE_Y);

        for (int i = 0; i < CardDefinition.ALL.length; i++) {
            int cardX = x + TRADE_CARDS_START_X + (i % TRADE_CARDS_PER_ROW) * TRADE_CARD_COL_W;
            int cardY = y + TRADE_CARDS_START_Y  + (i / TRADE_CARDS_PER_ROW) * TRADE_CARD_ROW_H;
            drawSmallCard(p, CardDefinition.ALL[i], cardX, cardY, TRADE_CARD_W, TRADE_CARD_H);
        }
    }

    private void drawInventoryWindow(PApplet p) {
        int x = centeredX(INVENTORY_W);
        int y = centeredY(INVENTORY_H);

        drawWindowShell(p, "Inventory", x, y, INVENTORY_W, INVENTORY_H);

        // Section labels
        p.fill(COLOR_TEXT_MED);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.textSize(15);
        p.text("Battle Deck", x + INV_DECK_X,  y + INV_SECTION_LABEL_Y);
        p.text("Backpack",    x + INV_PACK_X,  y + INV_SECTION_LABEL_Y);

        // Equipped deck
        CardDefinition[] equipped = inventory.getEquippedCards();
        for (int i = 0; i < equipped.length; i++) {
            int cardX = x + INV_DECK_X + (i % 2) * INV_DECK_CARD_COL_W;
            int cardY = y + INV_DECK_CARD_START_Y + (i / 2) * INV_DECK_CARD_ROW_H;
            drawSmallCard(p, equipped[i], cardX, cardY, INV_DECK_CARD_W, INV_DECK_CARD_H);

            if (i == inventory.getSelectedSlot()) {
                p.noFill();
                p.stroke(COLOR_SELECTED_R, COLOR_SELECTED_G, COLOR_SELECTED_B);
                p.rect(cardX - INV_SELECT_BORDER, cardY - INV_SELECT_BORDER,
                        INV_DECK_CARD_W + INV_SELECT_BORDER * 2,
                        INV_DECK_CARD_H + INV_SELECT_BORDER * 2, BTN_RADIUS);
            }
        }

        // Backpack
        List<CardDefinition> backpack = inventory.getBackpackCards();
        int visibleSlots = INV_PACK_COLS * INV_PACK_VISIBLE_ROWS;

        for (int i = 0; i < visibleSlots; i++) {
            int cardIndex = inventoryScroll * INV_PACK_COLS + i;
            if (cardIndex >= backpack.size()) break;

            int cardX = x + INV_PACK_X + (i % INV_PACK_COLS) * INV_PACK_CARD_COL_W;
            int cardY = y + INV_PACK_CARD_START_Y + (i / INV_PACK_COLS) * INV_PACK_CARD_ROW_H;
            drawSmallCard(p, backpack.get(cardIndex), cardX, cardY, INV_PACK_CARD_W, INV_PACK_CARD_H);
        }

        drawScrollBar(p,
                x + INVENTORY_W - INV_SCROLLBAR_OFFSET_X,
                y + INV_SCROLLBAR_PAD_TOP,
                INVENTORY_H - INV_SCROLLBAR_PAD_BOT,
                maxInventoryScroll(), inventoryScroll);

        drawCheatSheets(p, x + INV_PACK_X, y + 310);
    }

    private void drawSettingsWindow(PApplet p) {
        int x = centeredX(SETTINGS_W);
        int y = centeredY(SETTINGS_H);

        drawWindowShell(p, "Settings", x, y, SETTINGS_W, SETTINGS_H);
        drawScrollBar(p, x + SETTINGS_W - 32, y + 58, SETTINGS_H - 84, 1, 0);
    }

    // ── Click handlers ───────────────────────────────────────────────────────

    private boolean handleTradeClick(int mx, int my) {
        int x = centeredX(TRADE_W);
        int y = centeredY(TRADE_H);

        if (handleClose(mx, my, x, y, TRADE_W)) return true;

        if (inside(mx, my, x + TRADE_LEGEND_X, y + TRADE_BTN_Y, TRADE_BTN_W, TRADE_BTN_H)) {
            CardDefinition pulled = inventory.buyRandomCard();
            if (pulled != null) {
                tradeMessage = "You got " + pulled.name + "! (" + pulled.rarity + ") [" + pulled.subject + "]";
            } else {
                tradeMessage = "Not enough gumballs.";
            }
            return true;
        }

        return false;
    }

    private boolean handleInventoryClick(int mx, int my) {
        int x = centeredX(INVENTORY_W);
        int y = centeredY(INVENTORY_H);

        if (handleClose(mx, my, x, y, INVENTORY_W)) return true;

        for (int i = 0; i < inventory.getEquippedCards().length; i++) {
            int cardX = x + INV_DECK_X + (i % 2) * INV_DECK_CARD_COL_W;
            int cardY = y + INV_DECK_CARD_START_Y + (i / 2) * INV_DECK_CARD_ROW_H;
            if (inside(mx, my, cardX, cardY, INV_DECK_CARD_W, INV_DECK_CARD_H)) {
                inventory.selectSlot(i);
                return true;
            }
        }

        List<CardDefinition> backpack = inventory.getBackpackCards();
        int visibleSlots = INV_PACK_COLS * INV_PACK_VISIBLE_ROWS;

        for (int i = 0; i < visibleSlots; i++) {
            int cardIndex = inventoryScroll * INV_PACK_COLS + i;
            if (cardIndex >= backpack.size()) break;

            int cardX = x + INV_PACK_X + (i % INV_PACK_COLS) * INV_PACK_CARD_COL_W;
            int cardY = y + INV_PACK_CARD_START_Y + (i / INV_PACK_COLS) * INV_PACK_CARD_ROW_H;
            if (inside(mx, my, cardX, cardY, INV_PACK_CARD_W, INV_PACK_CARD_H)) {
                inventory.swapWithBackpack(cardIndex);
                return true;
            }
        }

        return false;
    }

    private boolean handleSettingsClick(int mx, int my) {
        int x = centeredX(SETTINGS_W);
        int y = centeredY(SETTINGS_H);
        return handleClose(mx, my, x, y, SETTINGS_W);
    }

    private boolean handleClose(int mx, int my, int x, int y, int w) {
        if (inside(mx, my, x + w - WINDOW_CLOSE_MARGIN, y + WINDOW_CLOSE_PAD, WINDOW_CLOSE_SIZE, WINDOW_CLOSE_SIZE)) {
            activeWindow = Window.NONE;
            return true;
        }
        return false;
    }

    // ── Draw: primitives ─────────────────────────────────────────────────────

    private void drawWindowShell(PApplet p, String title, int x, int y, int w, int h) {
        p.fill(255);
        p.stroke(COLOR_WIN_STROKE);
        p.rect(x, y, w, h, 6);

        p.fill(COLOR_TEXT_DARK);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.textSize(WINDOW_TITLE_SIZE);
        p.text(title, x + WINDOW_TITLE_X, y + WINDOW_TITLE_Y);

        p.stroke(COLOR_TEXT_DARK);
        p.strokeWeight(WINDOW_CLOSE_STROKE);
        p.line(x + w - 36, y + 17, x + w - 14, y + 39);
        p.line(x + w - 14, y + 17, x + w - 36, y + 39);
        p.strokeWeight(1);
    }

    private void drawScrollBar(PApplet p, int x, int y, int h, int maxScroll, int currentScroll) {
        p.fill(COLOR_SCROLLBAR_BG);
        p.stroke(90);
        p.rect(x, y, SCROLLBAR_W, h, SCROLLBAR_RADIUS);

        float thumbH = maxScroll == 0 ? h : Math.max(SCROLLBAR_THUMB_MIN, h / (maxScroll + 1f));
        float travel = h - thumbH;
        float thumbY = maxScroll == 0 ? y : y + travel * (currentScroll / (float) maxScroll);

        p.fill(COLOR_SCROLLBAR_FG);
        p.rect(x + 2, thumbY + 2, SCROLLBAR_W - 4, thumbH - 4, SCROLLBAR_RADIUS);
    }

    private void drawButton(PApplet p, String text, int x, int y, int w, int h) {
        p.fill(COLOR_BTN_FILL);
        p.stroke(COLOR_BTN_STROKE);
        p.rect(x, y, w, h, BTN_RADIUS);

        p.fill(0);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(BTN_TEXT_SIZE);
        p.text(text, x + w / 2f, y + h / 2f);
    }

    private void drawSmallCard(PApplet p, CardDefinition card, int x, int y, int w, int h) {
        if (card == null) {
            p.fill(225);
            p.stroke(150);
            p.rect(x, y, w, h, CARD_STRIP_RADIUS);
            return;
        }

        // White body
        p.fill(255);
        p.stroke(COLOR_CARD_STROKE);
        p.strokeWeight(1);
        p.rect(x, y, w, h, CARD_STRIP_RADIUS);

        // Colored rarity strip on top
        p.fill(card.strokeColor);
        p.noStroke();
        p.rect(x + 1, y + 1, w - 2, CARD_STRIP_H, CARD_STRIP_RADIUS, CARD_STRIP_RADIUS, 0, 0);

        // Name
        p.fill(COLOR_TEXT_DARK);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(CARD_NAME_SIZE);
        p.text(card.name, x + w / 2f, y + CARD_STRIP_H + (h - CARD_STRIP_H) * CARD_NAME_POS);

        // Subject
        p.fill(COLOR_TEXT_GREY);
        p.textSize(CARD_SUBJ_SIZE);
        p.text(card.subject, x + w / 2f, y + CARD_STRIP_H + (h - CARD_STRIP_H) * CARD_SUBJ_POS);

        // Damage
        p.fill(COLOR_TEXT_DARK);
        p.textSize(CARD_DMG_SIZE);
        p.text(card.damage + " dmg", x + w / 2f, y + CARD_STRIP_H + (h - CARD_STRIP_H) * CARD_DMG_POS);
    }

    private void drawCheatSheets(PApplet p, int x, int y) {
        p.fill(COLOR_TEXT_MED);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.textSize(15);
        p.text("Cheat Sheets", x, y);

        List<CheatSheetDefinition> sheets = inventory.getCheatSheets();
        if (sheets.isEmpty()) {
            p.fill(COLOR_TEXT_GREY);
            p.textSize(12);
            p.text("None", x, y + 24);
            return;
        }

        // Card dimensions matching the backpack cards
        int cw = INV_PACK_CARD_W;
        int ch = INV_PACK_CARD_H;
        int colW = INV_PACK_CARD_COL_W;
        int rowH = INV_PACK_CARD_ROW_H;

        for (int i = 0; i < sheets.size(); i++) {
            int cardX = x + (i % INV_PACK_COLS) * colW;
            int cardY = y + 20 + (i / INV_PACK_COLS) * rowH;
            drawSmallCheatSheet(p, sheets.get(i), cardX, cardY, cw, ch);
        }
    }

    private void drawSmallCheatSheet(PApplet p, CheatSheetDefinition sheet, int x, int y, int w, int h) {
        // White body with colored top strip — same style as knowledge cards
        p.fill(255);
        p.stroke(COLOR_CARD_STROKE);
        p.strokeWeight(1);
        p.rect(x, y, w, h, CARD_STRIP_RADIUS);

        // Colored accent strip at top using the sheet's fill color
        p.fill(sheet.fillColor);
        p.noStroke();
        p.rect(x + 1, y + 1, w - 2, CARD_STRIP_H, CARD_STRIP_RADIUS, CARD_STRIP_RADIUS, 0, 0);

        // Name (where card name sits)
        p.fill(COLOR_TEXT_DARK);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(CARD_NAME_SIZE);
        p.text(sheet.name, x + w / 2f, y + CARD_STRIP_H + (h - CARD_STRIP_H) * CARD_NAME_POS);

        // Thin divider line
        p.stroke(215);
        p.line(x + 6, y + h * 0.52f, x + w - 6, y + h * 0.52f);

        // Description (where damage/subject sits)
        p.fill(COLOR_TEXT_GREY);
        p.textSize(9);
        p.text(sheet.description, x + 5, y + CARD_STRIP_H + (h - CARD_STRIP_H) * 0.76f, w - 10, h * 0.30f);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private int centeredX(int width)  { return (screenW - width)  / 2; }
    private int centeredY(int height) { return (screenH - height) / 2; }

    private int maxInventoryScroll() {
        int rows = (int) Math.ceil(inventory.getBackpackCards().size() / (double) INV_PACK_COLS);
        return Math.max(0, rows - INV_PACK_VISIBLE_ROWS);
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}