import game.AssetManager;
import game.Camera;
import game.GameMap;
import game.Player;
import processing.core.PApplet;
import processing.event.MouseEvent;
import rpg.CardInventory;
import systems.GameState;
import ui.BattleScreen;
import ui.MenuScreen;

public class Main extends PApplet {
    private AssetManager assets;

    private Player player;
    private Camera camera;
    private GameMap gameMap;
    private MenuScreen menu;
    private BattleScreen battleScreen;
    private CardInventory cardInventory;
    private GameMap.Npc activeNpc;
    private int battleEndFrames;

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
    }

    @Override
    public void draw() {

        if(gameState == GameState.BATTLE) {
            battleScreen.draw(this);
            finishBattleAfterDelay();
            return;
        }

        if(gameState != GameState.GAME) {
            return;
        }

        player.setSchoolZoom(gameMap.isSchoolX(player.getX()));
        player.update();
        gameMap.blockBuildingCoveredJumps(player);

        GameMap.TerrainBlock plat = gameMap.getPlatformAt(player.getX(), player.getY(), player.getWidth(), player.getHeight());
        if (plat != null) {
            player.landOnPlatform(plat.surfaceY());
        }

        camera.update(player);

        pushMatrix();

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
            startBattleIfNearNpc();
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

        menu.mousePressed(mouseX, mouseY);
    }

    public void mouseWheel(MouseEvent event) {
        menu.mouseWheel(event.getCount());
    }

    private void startBattleIfNearNpc() {
        GameMap.Npc nearbyNpc = gameMap.findNearbyNpc(player);

        if (nearbyNpc == null) {
            return;
        }

        startBattle(nearbyNpc);
    }

    private void startBattle(GameMap.Npc npc) {
        activeNpc = npc;
        player.moveLeft = false;
        player.moveRight = false;
        battleScreen.reset(npc.getMaxHealth(), npc.getType());  // updated
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

        if (battleEndFrames < 45) {
            return;
        }

        if (result == BattleScreen.Result.PLAYER_WON) {
            gameMap.markDefeated(activeNpc);

            GameMap.EnemyType type = battleScreen.getDefeatedEnemyType();
            int reward = 10;
            if (type == GameMap.EnemyType.ACE)  reward = 15;
            if (type == GameMap.EnemyType.JOCK) reward = 20;
            cardInventory.addGumballs(reward);
            battleScreen.showGumballReward(reward); // NEW
        }

        activeNpc = null;
        gameState = GameState.GAME;
    }
}
