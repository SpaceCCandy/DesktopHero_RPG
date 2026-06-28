package ui;

import game.SoundManager;
import processing.core.PApplet;
import processing.core.PFont;
import rpg.CardDefinition;
import rpg.CardInventory;
import rpg.CheatSheetDefinition;

import java.util.List;

public class MenuScreen {

    private enum Window { NONE, TRADE, INVENTORY, SETTINGS }
    private enum TradeTab { CARDS, CHEAT_SHEETS }

    // ── Mini toggle ──────────────────────────────────────────────────────────
    private static final int MINI_X = 10, MINI_Y = 10, MINI_W = 95, MINI_H = 34;

    // ── Main menu ────────────────────────────────────────────────────────────
    private static final int MENU_TITLE_Y     = 44,  MENU_GUMBALLS_Y  = 82;
    private static final int MENU_BTN_W       = 200, MENU_BTN_H       = 48;
    private static final int MENU_BTN_TRADE_Y = 124, MENU_BTN_INV_Y   = 192;
    private static final int MENU_BTN_SET_Y   = 260, MENU_BTN_PLAY_Y  = 328;

    // ── Trade window ─────────────────────────────────────────────────────────
    // TRADE_CONTENT_Y_OFFSET shifts every piece of content below the tab bar
    // down uniformly, so the tab bar slots in above everything else without
    // needing to recompute each individual Y constant by hand.
    private static final int TRADE_CONTENT_Y_OFFSET = 34;
    private static final int TRADE_TAB_Y      = 50, TRADE_TAB_H = 30;
    private static final int TRADE_TAB_W      = 150, TRADE_TAB_GAP = 8;
    private static final int TRADE_TAB_X      = 24;

    private static final int TRADE_W = 580, TRADE_H = 400 + TRADE_CONTENT_Y_OFFSET;
    private static final int TRADE_LEGEND_X      = 24,  TRADE_COMMON_Y  = 66 + TRADE_CONTENT_Y_OFFSET;
    private static final int TRADE_RARE_Y        = 84 + TRADE_CONTENT_Y_OFFSET, TRADE_LEGEND_Y  = 102 + TRADE_CONTENT_Y_OFFSET;
    private static final int TRADE_CYBER_Y       = 120 + TRADE_CONTENT_Y_OFFSET, TRADE_GUMBALLS_Y = 158 + TRADE_CONTENT_Y_OFFSET;
    private static final int TRADE_BTN_Y         = 182 + TRADE_CONTENT_Y_OFFSET, TRADE_BTN_W     = 130, TRADE_BTN_H = 38;
    private static final int TRADE_MESSAGE_Y     = 260 + TRADE_CONTENT_Y_OFFSET;
    private static final int TRADE_CARDS_X       = 215, TRADE_CARDS_Y   = 58 + TRADE_CONTENT_Y_OFFSET;
    private static final int TRADE_CARD_COL_W    = 66,  TRADE_CARD_ROW_H = 116;
    private static final int TRADE_CARD_W        = 56,  TRADE_CARD_H     = 96;
    private static final int TRADE_CARDS_PER_ROW = 5;
    private static final int TRADE_CARD_ROWS_VISIBLE = 2; // visible rows before scrolling kicks in
    private static final int TRADE_CARDS_VIEW_H  = TRADE_CARD_ROWS_VISIBLE * TRADE_CARD_ROW_H;
    private static final int TRADE_SCROLL_X_OFF  = 30;

    // ── Cheat sheet shop tab ────────────────────────────────────────────────────
    // Reuses the same right-hand column region as the card grid (TRADE_CARDS_X),
    // just with bigger boxes since each sheet needs room for a name, buff
    // description, and gumball cost, and there are far fewer sheets than cards.
    private static final int SHOP_SHEET_COL_W    = 165, SHOP_SHEET_ROW_H = 118;
    private static final int SHOP_SHEET_W        = 150, SHOP_SHEET_H     = 100;
    private static final int SHOP_SHEETS_PER_ROW = 2;
    private static final int SHOP_SHEET_ROWS_VISIBLE = 2;
    private static final int SHOP_SHEETS_VIEW_H  = SHOP_SHEET_ROWS_VISIBLE * SHOP_SHEET_ROW_H;

    // ── Inventory window ─────────────────────────────────────────────────────
    // Layout, top to bottom inside the window:
    //   INV_SECTION_Y       -> "Battle Deck" / "Backpack" labels + divider
    //   INV_PACK_START_Y    -> backpack card grid (INV_PACK_ROWS rows, scrollable)
    //   INV_SHEET_LABEL_Y   -> "Cheat Sheets" label + divider (clear of the grid above)
    //   INV_SHEET_Y         -> cheat sheet boxes (also scrollable, single row visible)
    private static final int INVENTORY_W = 700, INVENTORY_H = 560;
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
    // Backpack grid occupies INV_PACK_START_Y .. INV_PACK_START_Y + INV_PACK_ROWS*INV_PACK_ROW_H
    // = 104 .. 398. Scrollbar spans exactly that range so it never collides with the cheat
    // sheets section below.
    private static final int INV_PACK_GRID_H     = INV_PACK_ROWS * INV_PACK_ROW_H;
    private static final int INV_SCROLL_PAD_TOP  = INV_PACK_START_Y;
    private static final int INV_SCROLL_PAD_BOT  = INVENTORY_H - (INV_PACK_START_Y + INV_PACK_GRID_H);

    // Cheat sheets sub-section sits below the backpack grid with clear spacing.
    private static final int INV_SHEET_LABEL_Y   = INV_PACK_START_Y + INV_PACK_GRID_H + 34; // 432
    private static final int INV_SHEET_DIVIDER_Y = INV_SHEET_LABEL_Y + 12;
    private static final int INV_SHEET_Y         = INV_SHEET_LABEL_Y + 20;
    private static final int INV_SHEET_X         = 32;
    private static final int INV_SHEET_W         = 110, INV_SHEET_H = 78;
    private static final int INV_SHEET_GAP       = 10;
    private static final int INV_SHEET_VISIBLE   = 5; // how many sheet boxes fit on one row
    private static final int INV_SHEET_SCROLL_X_OFF = 30;

    // ── Settings ─────────────────────────────────────────────────────────────
    private static final int SETTINGS_W = 320, SETTINGS_H = 320;
    private static final int SETTINGS_VOL_LABEL_Y = 96;
    private static final int SETTINGS_SLIDER_X    = 20,  SETTINGS_SLIDER_Y = 130;
    private static final int SETTINGS_SLIDER_W    = 280, SETTINGS_SLIDER_H = 14;
    private static final int SETTINGS_SLIDER_HIT_PAD = 10; // extra grab area above/below the bar
    private static final int SETTINGS_VOL_PCT_Y   = 160;

    // ── Locked-feature toast (bottom of screen) ─────────────────────────────
    private static final int LOCK_MSG_DURATION = 150; // frames (~2.5s at 60fps)
    private static final int LOCK_MSG_FADE     = 30;  // frames spent fading out at the end
    private static final int LOCK_MSG_H        = 44;
    private static final int LOCK_MSG_BOTTOM_MARGIN = 24;

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
    private SoundManager sound;
    private PFont font;
    private boolean open           = true;
    private Window  activeWindow   = Window.NONE;
    private TradeTab tradeTab      = TradeTab.CARDS;
    private String  tradeMessage   = "Spend gumballs to draw new cards!";
    private String  cheatShopMessage = "Spend gumballs to buy a cheat sheet!";
    private int     screenW        = 960, screenH = 580;
    private int     inventoryScroll;
    private int     cheatScroll;
    private int     tradeScroll;

    private boolean tradingUnlocked;
    private String  lockedMessage;
    private int     lockedMessageFrames;
    private boolean draggingVolumeSlider;

    public MenuScreen(CardInventory inventory) {
        this.inventory = inventory;
    }

    /** Wires up the sound manager so the Settings window can control music volume. */
    public void setSound(SoundManager sound) { this.sound = sound; }

    /** Call once trading should be available (e.g. after the player talks to Rico). */
    public void setTradingUnlocked(boolean unlocked) { this.tradingUnlocked = unlocked; }

    // ── Public API ────────────────────────────────────────────────────────────

    public void draw(PApplet p) {
        ensureFont(p);
        screenW = p.width; screenH = p.height;

        drawMiniButton(p);
        if (open) drawMainMenu(p);

        if      (activeWindow == Window.TRADE)     drawTradeWindow(p);
        else if (activeWindow == Window.INVENTORY) drawInventoryWindow(p);
        else if (activeWindow == Window.SETTINGS)  drawSettingsWindow(p);

        if (lockedMessageFrames > 0) {
            drawLockedMessageToast(p);
            lockedMessageFrames--;
        }
    }

    public void mousePressed(int mx, int my) {
        if (inside(mx, my, MINI_X, MINI_Y, MINI_W, MINI_H)) { open = !open; return; }

        if (activeWindow == Window.TRADE     && handleTradeClick(mx, my))     return;
        if (activeWindow == Window.INVENTORY && handleInventoryClick(mx, my)) return;
        if (activeWindow == Window.SETTINGS  && handleSettingsClick(mx, my))  return;

        if (!open) return;
        int menuW = screenW / 3;
        int btnX  = menuW / 2 - MENU_BTN_W / 2;
        if (inside(mx, my, btnX, MENU_BTN_TRADE_Y, MENU_BTN_W, MENU_BTN_H)) {
            if (tradingUnlocked) activeWindow = Window.TRADE;
            else showLockedMessage("You have not unlocked this feature yet. Talk to Rico first!");
        }
        else if (inside(mx, my, btnX, MENU_BTN_INV_Y,   MENU_BTN_W, MENU_BTN_H)) activeWindow = Window.INVENTORY;
        else if (inside(mx, my, btnX, MENU_BTN_SET_Y,   MENU_BTN_W, MENU_BTN_H)) activeWindow = Window.SETTINGS;
        else if (inside(mx, my, btnX, MENU_BTN_PLAY_Y,  MENU_BTN_W, MENU_BTN_H)) open = false;
    }

    public void mouseDragged(int mx, int my) {
        if (draggingVolumeSlider) setVolumeFromMouseX(mx);
    }

    public void mouseReleased() {
        draggingVolumeSlider = false;
    }

    private void showLockedMessage(String text) {
        lockedMessage = text;
        lockedMessageFrames = LOCK_MSG_DURATION;
    }

    public void mouseWheel(float amount, int mx, int my) {
        int dir = amount > 0 ? 1 : -1;

        if (activeWindow == Window.INVENTORY) {
            int x = cx(INVENTORY_W), y = cy(INVENTORY_H);
            // Backpack grid zone
            if (inside(mx, my, x + INV_PACK_X, y + INV_PACK_START_Y,
                    INV_PACK_COLS * INV_PACK_COL_W, INV_PACK_GRID_H)) {
                inventoryScroll = PApplet.constrain(inventoryScroll + dir, 0, maxPackScroll());
                return;
            }
            // Cheat sheets row zone
            if (inside(mx, my, x + INV_SHEET_X, y + INV_SHEET_Y,
                    INVENTORY_W - INV_SHEET_X - 20, INV_SHEET_H)) {
                cheatScroll = PApplet.constrain(cheatScroll + dir, 0, maxCheatScroll());
                return;
            }
            // Default: scroll whichever has room, preferring the backpack
            if (maxPackScroll() > 0) inventoryScroll = PApplet.constrain(inventoryScroll + dir, 0, maxPackScroll());
            else if (maxCheatScroll() > 0) cheatScroll = PApplet.constrain(cheatScroll + dir, 0, maxCheatScroll());
            return;
        }

        if (activeWindow == Window.TRADE && tradeTab == TradeTab.CARDS) {
            tradeScroll = PApplet.constrain(tradeScroll + dir, 0, maxTradeScroll());
        }
    }

    /** Legacy entry point retained for callers that don't track mouse position. */
    public void mouseWheel(float amount) {
        mouseWheel(amount, -1, -1);
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

        drawButton(p, tradingUnlocked ? "🛒  Trade" : "🔒  Trade", btnX, MENU_BTN_TRADE_Y, MENU_BTN_W, MENU_BTN_H);
        drawButton(p, "🎒  Inventory", btnX, MENU_BTN_INV_Y,   MENU_BTN_W, MENU_BTN_H);
        drawButton(p, "⚙  Settings",  btnX, MENU_BTN_SET_Y,   MENU_BTN_W, MENU_BTN_H);
        drawButton(p, "▶  Play",       btnX, MENU_BTN_PLAY_Y,  MENU_BTN_W, MENU_BTN_H);
    }

    // ── Draw: trade window ────────────────────────────────────────────────────

    private void drawTradeWindow(PApplet p) {
        int x = cx(TRADE_W), y = cy(TRADE_H);
        drawWindowShell(p, "🛒  Card Shop", x, y, TRADE_W, TRADE_H);

        drawTradeTabs(p, x, y);

        if (tradeTab == TradeTab.CARDS) {
            drawCardsTab(p, x, y);
        } else {
            drawCheatSheetsTab(p, x, y);
        }
    }

    private void drawTradeTabs(PApplet p, int x, int y) {
        drawTabButton(p, "Knowledge Cards", x + TRADE_TAB_X, y + TRADE_TAB_Y,
                TRADE_TAB_W, TRADE_TAB_H, tradeTab == TradeTab.CARDS);
        drawTabButton(p, "Cheat Sheets", x + TRADE_TAB_X + TRADE_TAB_W + TRADE_TAB_GAP, y + TRADE_TAB_Y,
                TRADE_TAB_W, TRADE_TAB_H, tradeTab == TradeTab.CHEAT_SHEETS);
    }

    private void drawTabButton(PApplet p, String label, int x, int y, int w, int h, boolean active) {
        p.fill(active ? C_ACCENT : 0xFFE8E4F0);
        p.stroke(active ? C_ACCENT : 0xFFBBB0CC);
        p.strokeWeight(1.5f);
        p.rect(x, y, w, h, 6);
        p.strokeWeight(1);
        p.fill(active ? 255 : 90);
        useFont(p, 12);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.text(label, x + w / 2f, y + h / 2f);
    }

    private void drawCardsTab(PApplet p, int x, int y) {
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
            int row = i / TRADE_CARDS_PER_ROW;
            int visibleRow = row - tradeScroll;
            if (visibleRow < 0 || visibleRow >= TRADE_CARD_ROWS_VISIBLE) continue; // scrolled out of view
            int cx = x + TRADE_CARDS_X + (i % TRADE_CARDS_PER_ROW) * TRADE_CARD_COL_W;
            int cy = y + TRADE_CARDS_Y + visibleRow * TRADE_CARD_ROW_H;
            drawSmallCard(p, CardDefinition.ALL[i], cx, cy, TRADE_CARD_W, TRADE_CARD_H);
        }

        drawScrollBar(p, x + TRADE_W - TRADE_SCROLL_X_OFF, y + TRADE_CARDS_Y,
                TRADE_CARDS_VIEW_H, maxTradeScroll(), tradeScroll);
    }

    private void drawCheatSheetsTab(PApplet p, int x, int y) {
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        useFont(p, 12);
        p.fill(C_TEXT_MED);
        p.text("Buy a cheat sheet to use in battle.", x + TRADE_LEGEND_X, y + TRADE_COMMON_Y);
        p.text("The selection rotates — refreshes in:", x + TRADE_LEGEND_X, y + TRADE_RARE_Y);
        p.fill(C_ACCENT);
        p.text(formatShopCountdown(inventory.getShopRefreshMillisRemaining()), x + TRADE_LEGEND_X, y + TRADE_LEGEND_Y);

        p.fill(C_TEXT_MED);
        useFont(p, 13);
        p.text("Gumballs: " + inventory.getGumballs(), x + TRADE_LEGEND_X, y + TRADE_GUMBALLS_Y);

        p.fill(C_TEXT_SOFT); useFont(p, 12);
        p.text(cheatShopMessage, x + TRADE_LEGEND_X, y + TRADE_MESSAGE_Y, 165, 60);

        List<CheatSheetDefinition> sheets = inventory.getShopOffers();
        for (int i = 0; i < sheets.size(); i++) {
            int row = i / SHOP_SHEETS_PER_ROW;
            int sx = x + TRADE_CARDS_X + (i % SHOP_SHEETS_PER_ROW) * SHOP_SHEET_COL_W;
            int sy = y + TRADE_CARDS_Y + row * SHOP_SHEET_ROW_H;
            drawShopSheet(p, sheets.get(i), sx, sy);
        }
        // No scrollbar needed — exactly SHOP_SLOT_COUNT (4) sheets are shown
        // at once, which fits the visible grid (2x2) with no overflow.
    }

    /** Formats milliseconds remaining as "MMm SSs" for the shop refresh countdown. */
    private String formatShopCountdown(long millisRemaining) {
        long totalSeconds = millisRemaining / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%dm %02ds", minutes, seconds);
    }

    private void drawShopSheet(PApplet p, CheatSheetDefinition sheet, int x, int y) {
        boolean canAfford = inventory.getGumballs() >= sheet.cost;

        p.fill(sheet.fillColor); p.stroke(canAfford ? 130 : 190); p.strokeWeight(1.5f);
        p.rect(x, y, SHOP_SHEET_W, SHOP_SHEET_H, 6);
        p.strokeWeight(1);

        p.fill(25); useFont(p, 12);
        p.textAlign(PApplet.CENTER, PApplet.TOP);
        p.text(sheet.name, x + SHOP_SHEET_W / 2f, y + 8, SHOP_SHEET_W - 10, 30);

        p.fill(90); useFont(p, 10);
        p.text(sheet.description, x + SHOP_SHEET_W / 2f, y + 38, SHOP_SHEET_W - 12, 32);

        p.fill(canAfford ? 0xFF5A4800 : 0xFFAA8800); useFont(p, 11);
        p.textAlign(PApplet.CENTER, PApplet.BOTTOM);
        p.text((canAfford ? "Buy " : "Need ") + sheet.cost + " 🟡", x + SHOP_SHEET_W / 2f, y + SHOP_SHEET_H - 6);
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
                INV_PACK_GRID_H, maxPackScroll(), inventoryScroll);

        // ── Cheat Sheets sub-section (sits below the backpack grid, never overlaps it) ──
        p.fill(C_ACCENT); useFont(p, 14);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.text("Cheat Sheets", x + INV_SHEET_X, y + INV_SHEET_LABEL_Y);
        p.stroke(C_ACCENT); p.strokeWeight(1.5f);
        p.line(x + INV_SHEET_X, y + INV_SHEET_DIVIDER_Y,
                x + INVENTORY_W - 50, y + INV_SHEET_DIVIDER_Y);
        p.strokeWeight(1);

        List<CheatSheetDefinition> sheets = inventory.getCheatSheets();
        for (int i = 0; i < INV_SHEET_VISIBLE; i++) {
            int idx = cheatScroll + i;
            if (idx >= sheets.size()) break;
            CheatSheetDefinition sheet = sheets.get(idx);
            int sx = x + INV_SHEET_X + i * (INV_SHEET_W + INV_SHEET_GAP);
            int sy = y + INV_SHEET_Y;
            p.fill(sheet.fillColor); p.stroke(160); p.strokeWeight(1);
            p.rect(sx, sy, INV_SHEET_W, INV_SHEET_H, 5);
            p.fill(C_TEXT); useFont(p, 11);
            p.textAlign(PApplet.CENTER, PApplet.CENTER);
            p.text(sheet.name, sx + INV_SHEET_W / 2f, sy + INV_SHEET_H / 2f - 10);
            p.fill(C_TEXT_SOFT); useFont(p, 9);
            p.text(sheet.description, sx + INV_SHEET_W / 2f, sy + INV_SHEET_H / 2f + 12,
                    INV_SHEET_W - 8, 28);
        }
        if (sheets.isEmpty()) {
            p.fill(C_TEXT_SOFT); useFont(p, 12);
            p.textAlign(PApplet.LEFT, PApplet.CENTER);
            p.text("None yet — find them in the story!", x + INV_SHEET_X, y + INV_SHEET_Y + INV_SHEET_H / 2f);
        } else if (sheets.size() > INV_SHEET_VISIBLE) {
            // Small horizontal scrollbar under the cheat sheet row when there's overflow
            int barY = y + INV_SHEET_Y + INV_SHEET_H + 8;
            int barW = INV_SHEET_VISIBLE * (INV_SHEET_W + INV_SHEET_GAP) - INV_SHEET_GAP;
            drawHScrollBar(p, x + INV_SHEET_X, barY, barW, maxCheatScroll(), cheatScroll);
        }
    }

    // ── Draw: settings window ─────────────────────────────────────────────────

    private void drawSettingsWindow(PApplet p) {
        int x = cx(SETTINGS_W), y = cy(SETTINGS_H);
        drawWindowShell(p, "⚙  Settings", x, y, SETTINGS_W, SETTINGS_H);

        p.fill(C_TEXT); useFont(p, 14);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.text("Music Volume", x + 20, y + SETTINGS_VOL_LABEL_Y);

        boolean soundOn = sound != null && sound.isAvailable();
        float volume = soundOn ? sound.getMusicVolume() : 0f;

        // Track
        p.fill(225); p.stroke(150); p.strokeWeight(1.5f);
        p.rect(x + SETTINGS_SLIDER_X, y + SETTINGS_SLIDER_Y, SETTINGS_SLIDER_W, SETTINGS_SLIDER_H, 7);
        p.strokeWeight(1);

        // Filled portion
        float fillW = SETTINGS_SLIDER_W * volume;
        p.fill(C_ACCENT2); p.noStroke();
        p.rect(x + SETTINGS_SLIDER_X, y + SETTINGS_SLIDER_Y, fillW, SETTINGS_SLIDER_H, 7);

        // Knob
        float knobX = x + SETTINGS_SLIDER_X + fillW;
        float knobY = y + SETTINGS_SLIDER_Y + SETTINGS_SLIDER_H / 2f;
        p.fill(255); p.stroke(C_ACCENT); p.strokeWeight(2);
        p.ellipse(knobX, knobY, 16, 16);
        p.strokeWeight(1);

        p.fill(C_TEXT_SOFT); useFont(p, 12);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.text(Math.round(volume * 100) + "%", x + 20, y + SETTINGS_VOL_PCT_Y);

        if (!soundOn) {
            p.fill(C_TEXT_SOFT); useFont(p, 11);
            p.text("(Sound library not installed — see SoundManager.java)", x + 20, y + SETTINGS_VOL_PCT_Y + 24);
        }
    }

    // ── Click handlers ────────────────────────────────────────────────────────

    private boolean handleTradeClick(int mx, int my) {
        int x = cx(TRADE_W), y = cy(TRADE_H);
        if (handleClose(mx, my, x, y, TRADE_W)) return true;

        // Tab buttons
        if (inside(mx, my, x + TRADE_TAB_X, y + TRADE_TAB_Y, TRADE_TAB_W, TRADE_TAB_H)) {
            tradeTab = TradeTab.CARDS;
            return true;
        }
        if (inside(mx, my, x + TRADE_TAB_X + TRADE_TAB_W + TRADE_TAB_GAP, y + TRADE_TAB_Y,
                TRADE_TAB_W, TRADE_TAB_H)) {
            tradeTab = TradeTab.CHEAT_SHEETS;
            return true;
        }

        if (tradeTab == TradeTab.CARDS) {
            if (inside(mx, my, x + TRADE_LEGEND_X, y + TRADE_BTN_Y, TRADE_BTN_W, TRADE_BTN_H)) {
                CardDefinition pulled = inventory.buyRandomCard();
                tradeMessage = pulled != null
                        ? "Got: " + pulled.name + " (" + pulled.rarity + ")  [" + pulled.subject + "]"
                        : "Not enough gumballs!";
                return true;
            }
        } else {
            List<CheatSheetDefinition> sheets = inventory.getShopOffers();
            for (int i = 0; i < sheets.size(); i++) {
                int row = i / SHOP_SHEETS_PER_ROW;
                int sx = x + TRADE_CARDS_X + (i % SHOP_SHEETS_PER_ROW) * SHOP_SHEET_COL_W;
                int sy = y + TRADE_CARDS_Y + row * SHOP_SHEET_ROW_H;
                if (inside(mx, my, sx, sy, SHOP_SHEET_W, SHOP_SHEET_H)) {
                    CheatSheetDefinition sheet = sheets.get(i);
                    boolean bought = inventory.buyCheatSheet(sheet);
                    cheatShopMessage = bought
                            ? "Bought: " + sheet.name + "!"
                            : "Not enough gumballs! Need " + sheet.cost + ".";
                    return true;
                }
            }
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
        int x = cx(SETTINGS_W), y = cy(SETTINGS_H);
        if (handleClose(mx, my, x, y, SETTINGS_W)) return true;

        if (sound != null && sound.isAvailable() && inside(mx, my,
                x + SETTINGS_SLIDER_X, y + SETTINGS_SLIDER_Y - SETTINGS_SLIDER_HIT_PAD,
                SETTINGS_SLIDER_W, SETTINGS_SLIDER_H + SETTINGS_SLIDER_HIT_PAD * 2)) {
            draggingVolumeSlider = true;
            setVolumeFromMouseX(mx);
            return true;
        }
        return false;
    }

    private void setVolumeFromMouseX(int mx) {
        if (sound == null || !sound.isAvailable()) return;
        int x = cx(SETTINGS_W);
        float fraction = (mx - (x + SETTINGS_SLIDER_X)) / (float) SETTINGS_SLIDER_W;
        sound.setMusicVolume(PApplet.constrain(fraction, 0f, 1f));
    }

    private boolean handleClose(int mx, int my, int x, int y, int w) {
        if (inside(mx, my, x + w - WIN_CLOSE_MARGIN, y + WIN_CLOSE_PAD, WIN_CLOSE_SIZE, WIN_CLOSE_SIZE)) {
            activeWindow = Window.NONE; return true;
        }
        return false;
    }

    // ── Draw primitives ───────────────────────────────────────────────────────

    private void drawLockedMessageToast(PApplet p) {
        int alpha = lockedMessageFrames < LOCK_MSG_FADE
                ? (int) (255 * (lockedMessageFrames / (float) LOCK_MSG_FADE))
                : 255;

        useFont(p, 13);
        float textW = p.textWidth(lockedMessage);
        float boxW = textW + 40;
        float x = (screenW - boxW) / 2f;
        float y = screenH - LOCK_MSG_H - LOCK_MSG_BOTTOM_MARGIN;

        p.fill(40, 30, 50, alpha); p.noStroke();
        p.rect(x, y, boxW, LOCK_MSG_H, 8);
        p.fill(255, 220, 90, alpha);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.text(lockedMessage, x + boxW / 2f, y + LOCK_MSG_H / 2f);
    }

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

    /** Horizontal counterpart to drawScrollBar, used under the cheat sheet row. */
    private void drawHScrollBar(PApplet p, int x, int y, int w, int max, int cur) {
        int barH = 8;
        p.fill(C_SCROLL_BG); p.stroke(140); p.rect(x, y, w, barH, 4);
        float tw = max == 0 ? w : Math.max(24, w / (max + 1f));
        float tx = max == 0 ? x : x + (w - tw) * (cur / (float) max);
        p.fill(C_SCROLL_FG); p.noStroke();
        p.rect(tx + 1, y + 1, tw - 2, barH - 2, 4);
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

        // Name — auto-fit so the full title always stays inside the card,
        // shrinking the font and/or wrapping onto a second line as needed
        // instead of letting long names like "Shakespearean Insult" overflow.
        p.fill(C_TEXT);
        float nameAreaW = w - 8;
        float nameAreaH = (h - CARD_STRIP_H) * (CARD_SUBJ_POS - CARD_NAME_POS) + 4;
        float nameCenterY = y + CARD_STRIP_H + (h - CARD_STRIP_H) * CARD_NAME_POS;
        drawFittedText(p, card.name, x + w / 2f, nameCenterY, nameAreaW, nameAreaH, CARD_NAME_SIZE);

        // Subject (wrapped to the card width so longer names like "Computer Science" don't overflow)
        p.fill(C_TEXT_SOFT); useFont(p, CARD_SUBJ_SIZE);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.text(card.subject, x + w / 2f, y + CARD_STRIP_H + (h - CARD_STRIP_H) * CARD_SUBJ_POS, w - 6, 22);

        // Damage
        p.fill(C_TEXT); useFont(p, CARD_DMG_SIZE);
        p.text(card.damage + " dmg", x + w / 2f, y + CARD_STRIP_H + (h - CARD_STRIP_H) * CARD_DMG_POS);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /**
     * Draws text centered in a box, shrinking the font size step by step
     * until the text fits within maxWidth on one line, or — if even the
     * smallest allowed size still doesn't fit on one line — wraps it onto
     * multiple lines within the given box instead of letting it spill past
     * the card's edges.
     */
    private void drawFittedText(PApplet p, String text, float cx, float cy,
                                float maxWidth, float maxHeight, int startSize) {
        int minSize = 8;
        p.textAlign(PApplet.CENTER, PApplet.CENTER);

        int size = startSize;
        useFont(p, size);
        while (size > minSize && p.textWidth(text) > maxWidth) {
            size--;
            useFont(p, size);
        }

        if (p.textWidth(text) <= maxWidth) {
            // Fits on one line at this size — draw it plainly, no wrap box
            // needed (a wrap box at this size could still vertically center
            // oddly for short text, so single-line stays unboxed).
            p.text(text, cx, cy);
        } else {
            // Still too wide even at the smallest size — wrap onto multiple
            // lines within the available box rather than overflowing.
            p.text(text, cx, cy, maxWidth, maxHeight);
        }
    }

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

    private int maxTradeScroll() {
        int rows = (int) Math.ceil(CardDefinition.ALL.length / (double) TRADE_CARDS_PER_ROW);
        return Math.max(0, rows - TRADE_CARD_ROWS_VISIBLE);
    }

    private int maxCheatScroll() {
        int count = inventory.getCheatSheets().size();
        return Math.max(0, count - INV_SHEET_VISIBLE);
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}