import game.AssetManager;
import game.Camera;
import game.GameMap;
import game.Player;
import processing.core.PApplet;
import processing.event.MouseEvent;
import rpg.CardDefinition;
import rpg.CardInventory;
import systems.GameState;
import ui.BattleScreen;
import ui.DialogOverlay;
import ui.MenuScreen;

public class Main extends PApplet {
    private static final String STORY_SAVE_FILE = "data/story_progress.csv";

    private AssetManager assets;
    private Player player;
    private Camera camera;
    private GameMap gameMap;
    private MenuScreen menu;
    private BattleScreen battleScreen;
    private DialogOverlay dialogOverlay;
    private CardInventory cardInventory;

    private GameMap.FightNpc activeFightNpc;
    private GameMap.StoryNpc activeStoryNpc;
    private StoryEvent activeStoryEvent;
    private boolean activeIsStoryBattle;
    private boolean activeIsMathTest;

    private int battleEndFrames;
    private int lastFrameMillis;
    private int storyProgression;
    private GameState gameState;

    public static void main(String[] args) {
        PApplet.main("Main");
    }

    @Override
    public void settings() {
        size(960, 580);
    }

    @Override
    public void setup() {
        gameState = GameState.GAME;
        assets = new AssetManager(this);
        player = new Player(this, 200, 300, assets.playerIdle, assets.playerWalk, assets.playerJump);
        camera = new Camera();
        cardInventory = new CardInventory(this);
        gameMap = new GameMap(this, assets);
        menu = new MenuScreen(cardInventory);
        battleScreen = new BattleScreen(cardInventory);
        dialogOverlay = new DialogOverlay();
        storyProgression = loadStoryProgression();
        gameMap.setStoryProgression(storyProgression);
        lastFrameMillis = millis();
    }

    @Override
    public void draw() {
        if (gameState == GameState.BATTLE) {
            battleScreen.draw(this);
            finishBattleAfterDelay();
            return;
        }

        if (gameState == GameState.DIALOG) {
            drawGameWorld(false);
            dialogOverlay.draw(this);
            return;
        }

        if (gameState != GameState.GAME) return;
        drawGameWorld(true);
    }

    private void drawGameWorld(boolean updateWorld) {
        if (updateWorld) {
            int now = millis();
            float frameScale = (now - lastFrameMillis) / (1000f / 60f);
            lastFrameMillis = now;

            player.setSchoolZoom(gameMap.isSchoolX(player.getX() + player.getWidth() / 2f));
            player.update(frameScale);
            gameMap.blockBuildingCoveredJumps(player);

            GameMap.TerrainBlock plat = gameMap.getPlatformAt(player);
            if (plat != null) player.landOnPlatform(plat.surfaceY());
            camera.update(player);
        }

        pushMatrix();
        float worldZoom = 1f + (player.getScale() - 1f) * 0.35f;
        translate(width / 2f, 500);
        scale(worldZoom);
        translate(-width / 2f, -500);
        translate(-camera.getX(), 0);
        gameMap.renderBehindPlayer(camera.getX(), width, player);
        player.render();
        gameMap.renderInFrontOfPlayer(camera.getX(), width, player);
        popMatrix();

        gameMap.drawInteractionPrompt(player);
        drawQuestPanel();
        menu.draw(this);
    }

    @Override
    public void keyPressed() {
        if (gameState != GameState.GAME) return;

        if (key == 'e' || key == 'E') {
            interactIfNear();
            return;
        }
        if (key == 'a' || key == 'A') player.moveLeft = true;
        if (key == 'd' || key == 'D') player.moveRight = true;
        if (key == 'w' || key == 'W') player.jump();
    }

    @Override
    public void keyReleased() {
        if (gameState != GameState.GAME) return;
        if (key == 'a' || key == 'A') player.moveLeft = false;
        if (key == 'd' || key == 'D') player.moveRight = false;
    }

    @Override
    public void mousePressed() {
        if (gameState == GameState.BATTLE) {
            battleScreen.mousePressed(mouseX, mouseY);
            return;
        }
        if (gameState == GameState.DIALOG) {
            dialogOverlay.mousePressed(mouseX, mouseY, width, height);
            handleDialogOutcome();
            return;
        }
        menu.mousePressed(mouseX, mouseY);
    }

    public void mouseWheel(MouseEvent event) {
        menu.mouseWheel(event.getCount());
    }

    private void interactIfNear() {
        GameMap.StoryNpc storyNpc = gameMap.findNearbyStoryNpc(player);
        if (storyNpc != null) {
            startStoryDialog(storyNpc.getProgressionStep(), storyNpc);
            return;
        }

        if (gameMap.isNearMathTestDoor(player)) {
            startStoryDialog(1, null);
            return;
        }

        GameMap.FightNpc npc = gameMap.findNearbyFightNpc(player);
        if (npc != null) startBattle(npc);
    }

    private void startBattle(GameMap.FightNpc npc) {
        activeFightNpc = npc;
        activeStoryNpc = null;
        activeStoryEvent = null;
        activeIsStoryBattle = false;
        activeIsMathTest = false;
        player.moveLeft = false;
        player.moveRight = false;
        battleScreen.reset(npc.getMaxHealth(), npc.getType());
        battleEndFrames = 0;
        gameState = GameState.BATTLE;
    }

    private void startStoryDialog(int step, GameMap.StoryNpc npc) {
        activeStoryNpc = npc;
        activeFightNpc = null;
        activeStoryEvent = storyEvent(step);
        activeIsStoryBattle = false;
        activeIsMathTest = step == 1;
        player.moveLeft = false;
        player.moveRight = false;
        dialogOverlay.start(activeStoryEvent.lines, assets.storySprite(activeStoryEvent.spriteId));
        gameState = GameState.DIALOG;
    }

    private void handleDialogOutcome() {
        DialogOverlay.Outcome outcome = dialogOverlay.getOutcome();
        if (outcome == DialogOverlay.Outcome.NONE) return;
        if (outcome == DialogOverlay.Outcome.START_FIGHT) {
            startStoryBattle();
        } else {
            completeStoryEvent();
        }
    }

    private void startStoryBattle() {
        if (activeStoryEvent == null) {
            gameState = GameState.GAME;
            return;
        }
        activeIsStoryBattle = true;
        battleScreen.reset(activeStoryEvent.enemyHealth, activeStoryEvent.enemyType);
        battleScreen.setEnemyName(activeStoryEvent.battleName);
        battleEndFrames = 0;
        gameState = GameState.BATTLE;
    }

    private void finishBattleAfterDelay() {
        BattleScreen.Result result = battleScreen.getResult();
        if (result == BattleScreen.Result.NONE) {
            battleEndFrames = 0;
            return;
        }

        battleEndFrames++;
        if (battleEndFrames < 45) return;

        if (result == BattleScreen.Result.PLAYER_WON) {
            if (activeIsStoryBattle || activeIsMathTest) {
                completeStoryEvent();
                return;
            }

            if (activeFightNpc != null) {
                gameMap.markDefeated(activeFightNpc);
                int reward = 10;
                if (activeFightNpc.getType() == GameMap.EnemyType.ACE) reward = 15;
                if (activeFightNpc.getType() == GameMap.EnemyType.JOCK) reward = 20;
                cardInventory.addGumballs(reward);
                battleScreen.showGumballReward(reward);
            }
        }

        activeFightNpc = null;
        activeStoryNpc = null;
        activeStoryEvent = null;
        activeIsStoryBattle = false;
        activeIsMathTest = false;
        lastFrameMillis = millis();
        gameState = GameState.GAME;
    }

    private void completeStoryEvent() {
        if (activeStoryEvent != null) applyStoryReward(activeStoryEvent);
        gameMap.advanceStoryNpc();
        storyProgression++;
        saveStoryProgression();
        gameMap.setStoryProgression(storyProgression);
        activeFightNpc = null;
        activeStoryNpc = null;
        activeStoryEvent = null;
        activeIsStoryBattle = false;
        activeIsMathTest = false;
        lastFrameMillis = millis();
        gameState = GameState.GAME;
    }

    private void applyStoryReward(StoryEvent event) {
        if (event.gumballs > 0) cardInventory.addGumballs(event.gumballs);
        for (int i = 0; i < event.randomCards; i++) cardInventory.addCard(CardDefinition.randomCard(this));
        for (String cardName : event.namedCards) cardInventory.addCard(CardDefinition.findByName(cardName));
        for (int i = 0; i < event.slotCards.length; i++) {
            cardInventory.addCardToSlot(CardDefinition.findByName(event.slotCards[i]), i);
        }
    }

    private void drawQuestPanel() {
        StoryEvent event = storyEvent(storyProgression);
        int x = width - 245;
        int y = 18;
        fill(255, 252, 235, 235);
        stroke(70);
        rect(x, y, 220, 76, 6);
        fill(40);
        textAlign(LEFT, TOP);
        textSize(14);
        text("Active Task", x + 14, y + 10);
        textSize(12);
        text(event.questText, x + 14, y + 34, 192, 34);
    }

    private int loadStoryProgression() {
        String[] lines = loadStrings(STORY_SAVE_FILE);
        if (lines == null || lines.length == 0) return 0;
        return Math.max(0, parseInt(lines[0], 0));
    }

    private void saveStoryProgression() {
        saveStrings(STORY_SAVE_FILE, new String[]{String.valueOf(storyProgression)});
    }

    private DialogOverlay.Line line(String speaker, String text, String a, String b) {
        return new DialogOverlay.Line(speaker, text, new String[]{a, b}, null, false);
    }

    private DialogOverlay.Line item(String speaker, String text) {
        return new DialogOverlay.Line(speaker, text, null, text, false);
    }

    private StoryEvent storyEvent(int step) {
        switch (step) {
            case 0:
                return story("dexter", "Find Dexter in the school.", false, GameMap.EnemyType.GEEK, 80, "Dexter", 0, 0,
                        new String[0], new String[]{"Studying", "Bonus answers", "ChatGPT", "Textbook"},
                        line("Dexter", "Hey there! Haven't seen you around here before. You must be a freshman huh?", "Yeah, first day.", "Do I look that lost?"),
                        line("Dexter", "Are you ready for that math test today? Got your cards ready?", "What cards?", "Cards?"),
                        line("Dexter", "Your Knowledge Cards! Everyone here uses them. Here, take some of my spares.", "Thanks man.", "This school is already weird."),
                        item("Dexter", "You received 4 Common Knowledge Cards. Tutorial unlocked."));
            case 1:
                return story("mathtest", "Find the purple classroom door and take the math test.", true, GameMap.EnemyType.GEEK, 130, "Math Test", 0, 2,
                        new String[0], new String[0],
                        line("Math Test", "The first test lands on your desk. No choice but to fight it.", "Start exam", "I was not ready"));
            case 2:
                return story("stacey", "Find Stacey in the school hallway.", false, GameMap.EnemyType.ACE, 90, "Stacey", 0, 0,
                        new String[0], new String[0],
                        line("Stacey", "Ugh, watch where you're going freshman.", "You bumped into ME.", "My bad."),
                        line("Stacey", "Do you even have any cards? You look broke.", "I got enough.", "Why does everyone keep talking about cards?"),
                        item("Stacey", "Stacey walks away flipping a Legendary card between her fingers."));
            case 3:
                return story("dexter", "Find Dexter for a quick check-in.", false, GameMap.EnemyType.GEEK, 80, "Dexter", 0, 0,
                        new String[0], new String[0],
                        line("Dexter", "Yo, you actually passed?! I didn't think you'd make it through day one honestly.", "Gee, thanks.", "Neither did I."),
                        line("Dexter", "Keep your head down. Don't go asking where cards come from.", "Too late.", "Noted."));
            case 4:
                return story("jock", "A hallway Jock is blocking the path.", true, GameMap.EnemyType.JOCK, 115, "Hallway Jock", 10, 0,
                        new String[0], new String[0],
                        line("Hallway Jock", "Aye freshman. You got a hall pass?", "It's lunch.", "Since when do Jocks care?"),
                        line("Hallway Jock", "Since I say so. You wanna get through? Beat me.", "Let's go.", "Fine."));
            case 5:
                return story("msPatel", "Find Ms. Patel.", false, GameMap.EnemyType.GEEK, 80, "Ms. Patel", 0, 0,
                        new String[0], new String[0],
                        line("Ms. Patel", "Oh, a new face! Welcome to Deskintop High.", "It's... something.", "Why does everyone have cards?"),
                        item("Ms. Patel", "You received 1 Cheat Sheet: Extra Time."));
            case 6:
                return story("rico", "Find Rico and ask about the card supply.", true, GameMap.EnemyType.GEEK, 135, "Rico", 15, 1,
                        new String[0], new String[0],
                        line("Rico", "Word travels fast. You want to know where cards come from?", "Obviously.", "What's the catch?"),
                        line("Rico", "Small catch. You gotta beat me first.", "Let's go.", "You serious?"));
            case 7:
                return story("rico", "Find Rico by the Lost & Found.", false, GameMap.EnemyType.GEEK, 80, "Rico", 0, 0,
                        new String[0], new String[0],
                        line("Rico", "See that machine? Drop gumballs in, pull a card out. Nobody knows who stocks it.", "Who's The Val?", "How many gumballs?"),
                        item("Rico", "Lost & Found machine explained."));
            case 8:
                return story("mathtest", "Find the Science Test encounter.", true, GameMap.EnemyType.GEEK, 145, "Science Test", 10, 1,
                        new String[0], new String[0],
                        line("Science Test", "Another exam. This one smells like lab chemicals and panic.", "Begin.", "Here we go."));
            case 9:
                return story("stacey", "Find Stacey again.", false, GameMap.EnemyType.ACE, 80, "Stacey", 0, 0,
                        new String[0], new String[0],
                        line("Stacey", "You're still here. Huh.", "Surprised?", "Miss me?"),
                        item("Stacey", "She warns you: this school chews people up."));
            case 10:
                return story("jamie", "Find Jamie, Marcus's lieutenant.", true, GameMap.EnemyType.GEEK, 160, "Jamie", 20, 2,
                        new String[]{"Textbook"}, new String[0],
                        line("Jamie", "You've been asking around about where the cards come from.", "Maybe.", "Who's asking?"),
                        line("Jamie", "The Val doesn't like freshmen poking around. Walk away.", "Not a chance.", "Who's The Val?"),
                        line("Jamie", "Wrong answer.", "Fight", "Bring it."));
            case 11:
                return story("msPatel", "Find Ms. Patel after class.", false, GameMap.EnemyType.GEEK, 80, "Ms. Patel", 0, 0,
                        new String[]{"Textbook"}, new String[0],
                        line("Ms. Patel", "Be careful. Whoever is at the top isn't someone to take lightly.", "Do you know who it is?", "Teachers know?"),
                        item("Ms. Patel", "You received 1 Rare Card: Textbook."));
            case 12:
                return story("dexter", "Find Dexter. He looks nervous.", false, GameMap.EnemyType.GEEK, 80, "Dexter", 0, 0,
                        new String[]{"Emotional Damage"}, new String[0],
                        line("Dexter", "How do you know about The Val already?", "I beat Jamie.", "Jamie told me everything."),
                        line("Dexter", "I used to work for him. He gets kids dependent, then pulls help before finals.", "Then I'll make myself worth it.", "Sounds like a plan."),
                        item("Dexter", "You received 1 Legendary Card: Emotional Damage."));
            case 13:
                return story("mathtest", "Find the English Test.", true, GameMap.EnemyType.ACE, 155, "English Test", 15, 1,
                        new String[0], new String[0],
                        line("English Test", "The third exam starts. Something about this one feels orchestrated.", "Begin.", "No shortcuts."));
            case 14:
                return story("rico", "Find Rico to put the pieces together.", false, GameMap.EnemyType.GEEK, 80, "Rico", 0, 0,
                        new String[0], new String[0],
                        line("Rico", "Marcus plants cheat sheets, gets people hooked, then pulls the rug.", "That's calculated.", "So what do we do?"),
                        item("Rico", "You received 1 Cheat Sheet: Bonus Answers."));
            case 15:
                return story("stacey", "Find Stacey. Marcus sent her.", true, GameMap.EnemyType.ACE, 170, "Stacey", 25, 1,
                        new String[0], new String[0],
                        line("Stacey", "Marcus wanted me to deliver a message.", "Let me guess - back off?", "What message?"),
                        line("Stacey", "Impressive. But this is where it ends.", "At least you're honest.", "For a card?"));
            case 16:
                return story("dexter", "Find Dexter before the final.", false, GameMap.EnemyType.GEEK, 80, "Dexter", 0, 0,
                        new String[]{"Punching Ghost"}, new String[0],
                        line("Dexter", "This is it huh. You're actually going after Marcus.", "Yeah.", "Someone has to."),
                        item("Dexter", "You received 1 Legendary Card: Punching Ghost."));
            case 17:
                return story("announcement", "Listen to the announcement.", false, GameMap.EnemyType.ACE, 80, "Announcement", 0, 0,
                        new String[0], new String[0],
                        line("Announcement", "Attention Deskintop High. Finals are in one week. I'd hate to see anyone underprepared.", "Who WAS that?", "Actually terrifying."),
                        item("Announcement", "Marcus is now visible. The senior hallway opens."));
            case 18:
                return story("rico", "Find Rico and Dexter before the final.", false, GameMap.EnemyType.GEEK, 80, "Rico + Dexter", 0, 0,
                        new String[0], new String[0],
                        line("Rico", "We're not coming with you. But whatever happens in there, you earned it.", "You guys are the worst.", "...Thanks."),
                        item("Dexter", "You received Extra Time and Process of Elimination."));
            case 19:
                return story("val", "Find Marcus Reid, The Val.", true, GameMap.EnemyType.ACE, 240, "The Val", 0, 2,
                        new String[]{"WannaCry"}, new String[0],
                        line("The Val", "You made it. I'll be honest - I didn't think you would.", "They were both wrong.", "I had help."),
                        line("The Val", "This was never about the grade. It was about proving no one deserves to win.", "That's different.", "I earned this."),
                        line("The Val", "Prove it.", "Fight", "Let's end this."));
            default:
                return story("announcement", "Story complete. Valedictorian: TBD.", false, GameMap.EnemyType.GEEK, 80, "Story Complete", 0, 0,
                        new String[0], new String[0],
                        item("System", "Full game complete. Valedictorian: TBD."));
        }
    }

    private StoryEvent story(String spriteId, String questText, boolean battle, GameMap.EnemyType type,
                             int hp, String battleName, int gumballs, int randomCards,
                             String[] namedCards, String[] slotCards, DialogOverlay.Line... lines) {
        if (battle && lines.length > 0) {
            DialogOverlay.Line last = lines[lines.length - 1];
            lines[lines.length - 1] = new DialogOverlay.Line(last.speaker, last.text, last.responses, last.itemText, true);
        }
        return new StoryEvent(spriteId, questText, type, hp, battleName, gumballs, randomCards, namedCards, slotCards, lines);
    }

    private static class StoryEvent {
        final String spriteId;
        final String questText;
        final GameMap.EnemyType enemyType;
        final int enemyHealth;
        final String battleName;
        final int gumballs;
        final int randomCards;
        final String[] namedCards;
        final String[] slotCards;
        final DialogOverlay.Line[] lines;

        StoryEvent(String spriteId, String questText, GameMap.EnemyType enemyType, int enemyHealth,
                   String battleName, int gumballs, int randomCards, String[] namedCards,
                   String[] slotCards, DialogOverlay.Line[] lines) {
            this.spriteId = spriteId;
            this.questText = questText;
            this.enemyType = enemyType;
            this.enemyHealth = enemyHealth;
            this.battleName = battleName;
            this.gumballs = gumballs;
            this.randomCards = randomCards;
            this.namedCards = namedCards;
            this.slotCards = slotCards;
            this.lines = lines;
        }
    }
}
