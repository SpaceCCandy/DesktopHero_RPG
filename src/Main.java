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
    private AssetManager assets;

    private Player player;
    private Camera camera;
    private GameMap gameMap;
    private MenuScreen menu;
    private BattleScreen battleScreen;
    private DialogOverlay dialogOverlay;
    private CardInventory cardInventory;
    private GameMap.Npc activeNpc;
    private GameMap.DoorEnemy activeDoorEnemy;
    private GameMap.StoryNpc activeStoryNpc;
    private StoryEvent activeStoryEvent;
    private boolean activeBattleIsStory;
    private int battleEndFrames;
    private int lastFrameMillis;
    private int storyProgression;
    private static final String STORY_SAVE_FILE = "data/story_progress.csv";

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

        player = new Player(
                this, 200, 300,
                assets.playerIdle,
                assets.playerWalk,   // ← jump second
                assets.playerJump
        );

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

        if(gameState == GameState.BATTLE) {
            battleScreen.draw(this);
            finishBattleAfterDelay();
            return;
        }

        if(gameState == GameState.DIALOG) {
            drawGameWorld(false);
            dialogOverlay.draw(this);
            return;
        }

        if(gameState != GameState.GAME) {
            return;
        }

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
            if (plat != null) {
                player.landOnPlatform(plat.surfaceY());
            }

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
        menu.draw(this);
    }

    @Override
    public void keyPressed() {

        if(gameState != GameState.GAME) {
            return;
        }

        if(key == 'e' || key == 'E') {
            interactIfNearNpc();
            return;
        }

        if(key == 'a' || key == 'A') {
            player.moveLeft = true;
        }

        if(key == 'd' || key == 'D') {
            player.moveRight = true;
        }

        if(key == 'w' || key == 'W') {
            player.jump();
        }
    }

    @Override
    public void keyReleased() {

        if(gameState != GameState.GAME) {
            return;
        }

        if(key == 'a' || key == 'A') {
            player.moveLeft = false;
        }

        if(key == 'd' || key == 'D') {
            player.moveRight = false;
        }
    }

    public void mousePressed() {
        if(gameState == GameState.BATTLE) {
            battleScreen.mousePressed(mouseX, mouseY);
            return;
        }

        if(gameState == GameState.DIALOG) {
            dialogOverlay.mousePressed(mouseX, mouseY, width, height);
            handleDialogOutcome();
            return;
        }

        menu.mousePressed(mouseX, mouseY);
    }

    public void mouseWheel(MouseEvent event) {
        menu.mouseWheel(event.getCount());
    }

    private void interactIfNearNpc() {
        GameMap.StoryNpc storyNpc = gameMap.findNearbyStoryNpc(player);
        if (storyNpc != null) {
            startStoryDialog(storyNpc);
            return;
        }

        if (gameMap.isNearMathTestDoor(player)) {
            startStoryDialog(1, "mathtest");
            return;
        }

        GameMap.DoorEnemy doorEnemy = gameMap.findNearbyDoorEnemy(player);
        if (doorEnemy != null) {
            startBattle(doorEnemy);
            return;
        }

        GameMap.Npc nearbyNpc = gameMap.findNearbyNpc(player);

        if (nearbyNpc == null) {
            return;
        }

        startBattle(nearbyNpc);
    }

    private void startBattle(GameMap.Npc npc) {
        activeNpc = npc;
        activeDoorEnemy = null;
        activeStoryNpc = null;
        activeStoryEvent = null;
        activeBattleIsStory = false;
        player.moveLeft = false;
        player.moveRight = false;
        battleScreen.reset(npc.getMaxHealth(), npc.getType(), assets.playerIdle, assets.getNpcSprite(npc.getSpriteIndex()));
        battleEndFrames = 0;
        gameState = GameState.BATTLE;
    }

    private void startBattle(GameMap.DoorEnemy doorEnemy) {
        activeDoorEnemy = doorEnemy;
        activeNpc = null;
        activeStoryNpc = null;
        activeStoryEvent = null;
        activeBattleIsStory = false;
        player.moveLeft = false;
        player.moveRight = false;
        battleScreen.reset(doorEnemy.getMaxHealth(), doorEnemy.getType(), assets.playerIdle, assets.getNpcSprite(doorEnemy.getSpriteIndex()));
        battleEndFrames = 0;
        gameState = GameState.BATTLE;
    }

    private void startStoryDialog(GameMap.StoryNpc npc) {
        activeStoryNpc = npc;
        activeNpc = null;
        activeDoorEnemy = null;
        activeStoryEvent = storyEventForStep(npc.progressionStep);
        activeBattleIsStory = false;
        player.moveLeft = false;
        player.moveRight = false;
        dialogOverlay.start(activeStoryEvent.lines, assets.getStorySprite(npc.spriteId));
        gameState = GameState.DIALOG;
    }

    private void startStoryDialog(int progressionStep, String spriteId) {
        activeStoryNpc = null;
        activeNpc = null;
        activeDoorEnemy = null;
        activeStoryEvent = storyEventForStep(progressionStep);
        activeBattleIsStory = false;
        player.moveLeft = false;
        player.moveRight = false;
        dialogOverlay.start(activeStoryEvent.lines, assets.getStorySprite(spriteId));
        gameState = GameState.DIALOG;
    }

    private void handleDialogOutcome() {
        DialogOverlay.Outcome outcome = dialogOverlay.getOutcome();
        if (outcome == DialogOverlay.Outcome.NONE) {
            return;
        }

        if (outcome == DialogOverlay.Outcome.START_FIGHT) {
            startStoryBattle();
            return;
        }

        if (outcome == DialogOverlay.Outcome.CLOSED) {
            completeStoryEvent();
        }
    }

    private void startStoryBattle() {
        if (activeStoryEvent == null || activeStoryNpc == null) {
            gameState = GameState.GAME;
            return;
        }

        activeBattleIsStory = true;
        battleEndFrames = 0;
        battleScreen.reset(activeStoryEvent.enemyHealth, activeStoryEvent.enemyType,
                assets.playerIdle, assets.getStorySprite(activeStoryEvent.spriteId), activeStoryEvent.weaknessSubjects);
        gameState = GameState.BATTLE;
    }

    private void finishBattleAfterDelay() {
        BattleScreen.Result result = battleScreen.getResult();

        if (result == BattleScreen.Result.NONE) {
            battleEndFrames = 0;
            return;
        }

        battleEndFrames++;

        if (battleEndFrames < 45) {
            return;
        }

        if (result == BattleScreen.Result.PLAYER_WON) {
            if (activeBattleIsStory) {
                completeStoryEvent();
                return;
            }

            if (activeDoorEnemy != null) {
                gameMap.markDefeated(activeDoorEnemy);
            } else {
                gameMap.markDefeated(activeNpc);
            }

            GameMap.EnemyType type = battleScreen.getDefeatedEnemyType();
            int reward = 10;
            if (type == GameMap.EnemyType.ACE)  reward = 15;
            if (type == GameMap.EnemyType.JOCK) reward = 20;
            cardInventory.addGumballs(reward);
            battleScreen.showGumballReward(reward); // NEW
        }

        if (result == BattleScreen.Result.PLAYER_LOST && activeBattleIsStory) {
            activeStoryEvent = null;
            activeStoryNpc = null;
            activeBattleIsStory = false;
        }

        activeNpc = null;
        activeDoorEnemy = null;
        lastFrameMillis = millis();
        gameState = GameState.GAME;
    }

    private void completeStoryEvent() {
        if (activeStoryEvent != null) {
            applyStoryReward(activeStoryEvent);
        }

        gameMap.markDefeated(activeStoryNpc);
        storyProgression++;
        saveStoryProgression();
        gameMap.setStoryProgression(storyProgression);
        activeStoryNpc = null;
        activeStoryEvent = null;
        activeBattleIsStory = false;
        activeNpc = null;
        activeDoorEnemy = null;
        lastFrameMillis = millis();
        gameState = GameState.GAME;
    }

    private void applyStoryReward(StoryEvent event) {
        if (event.gumballs > 0) {
            cardInventory.addGumballs(event.gumballs);
        }

        for (int i = 0; i < event.randomCardDrops; i++) {
            cardInventory.addCard(CardDefinition.randomCard(this));
        }

        for (String cardName : event.namedCardDrops) {
            cardInventory.addCard(CardDefinition.findByName(cardName));
        }

        for (String cheatSheetName : event.cheatSheetDrops) {
            cardInventory.addCheatSheet(cheatSheetName);
        }
    }

    private int loadStoryProgression() {
        String[] lines = loadStrings(STORY_SAVE_FILE);
        if (lines == null || lines.length == 0) {
            return 0;
        }

        return Math.max(0, parseInt(lines[0], 0));
    }

    private void saveStoryProgression() {
        saveStrings(STORY_SAVE_FILE, new String[]{String.valueOf(storyProgression)});
    }

    private StoryEvent storyEventForStep(int step) {
        switch (step) {
            case 0:
                return new StoryEvent("dexter", false, GameMap.EnemyType.GEEK, 80, 0, 0,
                        new String[]{"Studying", "Bonus answers", "ChatGPT", "Textbook"},
                        new DialogOverlay.Line[]{
                                line("Dexter", "Hey there! Haven't seen you around here before. You must be a freshman huh?", "Yeah, first day.", "Do I look that lost?"),
                                line("Dexter", "Are you ready for that math test today? Got your cards ready?", "What cards?", "Cards?"),
                                line("Dexter", "Your Knowledge Cards! Everyone here uses them. They boost your brain power during tests. It's kind of hard to explain.", "That sounds insane.", "Where do I get them?"),
                                line("Dexter", "Look, I don't have time. Bell's about to ring. Here, take some of my spares. You're gonna need them.", "Thanks man.", "This school is already weird."),
                                item("Dexter", "You received 4 Common Knowledge Cards. Tutorial unlocked. Dexter runs off.")
                        });
            case 1:
                return new StoryEvent("mathtest", true, GameMap.EnemyType.GEEK, 95, 0, 2,
                        new String[0],
                        new DialogOverlay.Line[]{
                                line("Math Test", "The first test lands on your desk. No choice but to fight it.", "Start exam", "I was not ready")
                        });
            case 2:
                return new StoryEvent("stacey", false, GameMap.EnemyType.ACE, 90, 0, 0,
                        new String[0],
                        new DialogOverlay.Line[]{
                                line("Stacey", "Ugh, watch where you're going freshman.", "You bumped into ME.", "My bad."),
                                line("Stacey", "Do you even have any cards? You look broke.", "I got enough.", "Why does everyone keep talking about cards?"),
                                item("Stacey", "Stacey walks away flipping a Legendary card between her fingers.")
                        });
            case 3:
                return new StoryEvent("rico", true, GameMap.EnemyType.GEEK, 120, 15, 1,
                        new String[0],
                        new DialogOverlay.Line[]{
                                line("Rico", "Yo, you're the freshman that passed Mr. Henderson's test right? Word travels fast around here.", "I guess that's me.", "Already?"),
                                line("Rico", "Look, I know where the cards come from. Not all of it, but enough. There's a guy who runs a supply. You want in?", "Obviously.", "What's the catch?"),
                                line("Rico", "No catch. Well... small catch. You gotta beat me first. Call it an entrance fee.", "Let's go.", "You serious right now?")
                        });
            case 4:
                return new StoryEvent("msPatel", false, GameMap.EnemyType.GEEK, 80, 0, 0,
                        new String[0], new String[]{"Extra Time"},
                        new DialogOverlay.Line[]{
                                line("Ms. Patel", "Oh, a new face! Welcome to Deskintop High. I hope you're settling in okay.", "It's... something.", "Why does everyone have cards?"),
                                line("Ms. Patel", "Cards? Oh I wouldn't worry about those. Just focus on your studies.", "That's actually helpful.", "Are you hiding something?"),
                                item("Ms. Patel", "You received 1 Cheat Sheet: Extra Time.")
                        });
            case 5:
                return new StoryEvent("jamie", true, GameMap.EnemyType.GEEK, 150, 20, 2,
                        new String[]{"Textbook"}, new String[]{"Process of Elimination"},
                        new DialogOverlay.Line[]{
                                line("Jamie", "You've been asking around haven't you. About where the cards come from.", "Maybe.", "Who's asking?"),
                                line("Jamie", "The Val doesn't like freshmen poking around. Walk away.", "Not a chance.", "Who's The Val?"),
                                line("Jamie", "Wrong answer.", "Fight", "Bring it")
                        });
            case 6:
                return new StoryEvent("dexter", false, GameMap.EnemyType.GEEK, 80, 0, 0,
                        new String[]{"Emotional Damage"},
                        new DialogOverlay.Line[]{
                                line("Dexter", "Hey, wait, how do you know about The Val already?", "You knew this whole time?", "Relax, I just beat up his guy."),
                                line("Dexter", "Jamie?! You beat Jamie?! Marcus is going to know who you are now.", "Good.", "Let him come."),
                                line("Dexter", "I used to work for him. He makes kids dependent on cheat sheets, then takes them away before finals.", "Then I'll make myself worth it.", "Sounds like a plan to me."),
                                item("Dexter", "You received 1 Legendary Card: Emotional Damage.")
                        });
            case 7:
                return new StoryEvent("stacey", true, GameMap.EnemyType.ACE, 140, 25, 1,
                        new String[0],
                        new DialogOverlay.Line[]{
                                line("Stacey", "So you're the freshman everyone's talking about. Marcus wanted me to deliver a message.", "What message?", "Let me guess: back off?"),
                                line("Stacey", "He said, and I quote: Impressive. But this is where it ends.", "At least you're honest.", "You're doing this for a card?")
                        });
            case 8:
                return new StoryEvent("announcement", false, GameMap.EnemyType.ACE, 80, 0, 0,
                        new String[0],
                        new DialogOverlay.Line[]{
                                line("Announcement", "Attention Deskintop High. Semester finals are in two weeks. I'd hate to see anyone underprepared.", "Who WAS that?", "Okay he's actually terrifying."),
                                item("Announcement", "Marcus is now visible on the map as a locked encounter. A new hallway opens up.")
                        });
            case 9:
                return new StoryEvent("val", true, GameMap.EnemyType.ACE, 220, 50, 2,
                        new String[]{"WannaCry"}, new String[]{"Extra Time", "Process of Elimination", "Curve"},
                        new DialogOverlay.Line[]{
                                line("The Val", "You made it. I'll be honest. I didn't think you would.", "They were both wrong.", "I had a good teacher. Sort of."),
                                line("The Val", "I don't actually need these cards. This was never about the grade.", "Then what was it about?", "Control?"),
                                line("The Val", "About proving that no one here deserves to win. Prove me wrong.", "Prove it.", "Let's end this.")
                        });
            default:
                return new StoryEvent("dexter", false, GameMap.EnemyType.GEEK, 80, 0, 0,
                        new String[0],
                        new DialogOverlay.Line[]{item("System", "Story complete. Valedictorian: TBD.")});
        }
    }

    private DialogOverlay.Line line(String speaker, String text, String firstResponse, String secondResponse) {
        return new DialogOverlay.Line(speaker, text, new String[]{firstResponse, secondResponse}, null, false);
    }

    private DialogOverlay.Line item(String speaker, String text) {
        return new DialogOverlay.Line(speaker, text, null, text, false);
    }

    private static class StoryEvent {
        final String spriteId;
        final boolean hasBattle;
        final GameMap.EnemyType enemyType;
        final int enemyHealth;
        final int gumballs;
        final int randomCardDrops;
        final String[] namedCardDrops;
        final String[] cheatSheetDrops;
        final String[] weaknessSubjects;
        final DialogOverlay.Line[] lines;

        StoryEvent(String spriteId, boolean hasBattle, GameMap.EnemyType enemyType, int enemyHealth,
                   int gumballs, int randomCardDrops, String[] namedCardDrops, DialogOverlay.Line[] lines) {
            this(spriteId, hasBattle, enemyType, enemyHealth, gumballs, randomCardDrops,
                    namedCardDrops, new String[0], lines);
        }

        StoryEvent(String spriteId, boolean hasBattle, GameMap.EnemyType enemyType, int enemyHealth,
                   int gumballs, int randomCardDrops, String[] namedCardDrops, String[] cheatSheetDrops,
                   DialogOverlay.Line[] lines) {
            this.spriteId = spriteId;
            this.hasBattle = hasBattle;
            this.enemyType = enemyType;
            this.enemyHealth = enemyHealth;
            this.gumballs = gumballs;
            this.randomCardDrops = randomCardDrops;
            this.namedCardDrops = namedCardDrops;
            this.cheatSheetDrops = cheatSheetDrops;
            this.weaknessSubjects = weaknessSubjectsFor(spriteId);
            this.lines = lines;

            if (hasBattle && lines.length > 0) {
                lines[lines.length - 1] = new DialogOverlay.Line(
                        lines[lines.length - 1].speaker,
                        lines[lines.length - 1].text,
                        lines[lines.length - 1].responses,
                        lines[lines.length - 1].itemText,
                        true);
            }
        }

        private String[] weaknessSubjectsFor(String spriteId) {
            if ("mathtest".equals(spriteId)) return new String[]{"Math"};
            if ("rico".equals(spriteId)) return new String[]{"Gym", "Science"};
            if ("jamie".equals(spriteId)) return new String[]{"Science", "English"};
            if ("val".equals(spriteId)) return new String[]{"Gym"};
            return new String[0];
        }
    }
}
