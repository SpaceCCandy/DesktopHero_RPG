package game;

import processing.core.PApplet;
import processing.core.PImage;

public class AssetManager {

    public final PImage playerIdle;
    public final PImage playerJump;
    public final PImage[] playerWalk;

    public final PImage[] buildings;
    public final PImage   fence;
    public final PImage   tree;
    public final PImage[] lockers;
    public final PImage   plant;
    public final PImage[] windows;
    public final PImage   board;
    public final PImage[] idleNpcs;
    public final PImage   walkingNpc;

    public AssetManager(PApplet app) {
        playerIdle = app.loadImage("assets/PlayerIdle.png");
        playerJump = app.loadImage("assets/PlayerJump.png");
        playerWalk = new PImage[]{
                app.loadImage("assets/PlayerWalk1.png"),
                app.loadImage("assets/PlayerWalk2.png"),
                app.loadImage("assets/PlayerWalk3.png")
        };

        buildings = new PImage[]{
                app.loadImage("assets/buildings/building1.jpg"),
                app.loadImage("assets/buildings/building2.jpg"),
                app.loadImage("assets/buildings/building3.jpg"),
                app.loadImage("assets/buildings/building4.jpg")
        };

        fence = loadSafe(app, "assets/backgrounds/fences.png");
        tree  = loadSafe(app, "assets/backgrounds/Trees.png");

        lockers = new PImage[]{
                loadSafe(app, "assets/backgrounds/locker1.png"),
                loadSafe(app, "assets/backgrounds/locker2.png"),
                loadSafe(app, "assets/backgrounds/locker3.png")
        };
        plant = loadSafe(app, "assets/backgrounds/plant.png");
        windows = new PImage[]{
                loadSafe(app, "assets/backgrounds/window1.png"),
                loadSafe(app, "assets/backgrounds/window2.png")
        };
        board = loadSafe(app, "assets/backgrounds/board.png");
        idleNpcs = new PImage[]{
                loadSafe(app, "assets/npc/idleNPC1.png"),
                loadSafe(app, "assets/npc/idleNPC2.png")
        };
        walkingNpc = loadSafe(app, "assets/npc/walkingNPC1.png");
    }

    private PImage loadSafe(PApplet app, String path) {
        try {
            PImage img = app.loadImage(path);
            return (img != null && img.width > 0) ? img : null;
        } catch (Exception e) {
            return null;
        }
    }
}
