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
    public final PImage[] clouds;
    public final PImage[] lockers;
    public final PImage   plant;
    public final PImage[] windows;
    public final PImage   board;
    public final PImage[] idleNpcs;
    public final PImage   walkingNpc;
    public final PImage   dexter;
    public final PImage   jamie;
    public final PImage   msPatel;
    public final PImage   mathtest;
    public final PImage   announcement;
    public final PImage   val;
    public final PImage   stacey;
    public final PImage   rico;
    public final PImage   billy;

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
        clouds = new PImage[]{
                loadSafe(app, "assets/backgrounds/cloud1.png"),
                loadSafe(app, "assets/backgrounds/cloud2.png"),
                loadSafe(app, "assets/backgrounds/cloud3.png")
        };

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

        dexter = loadSafe(app, "assets/NPC_Dexter.png");
        jamie = loadSafe(app, "assets/NPC_Jamie.png");
        msPatel = loadSafe(app, "assets/NPC_MsPatel.png");
        mathtest = loadSafe(app, "assets/NPC_Mathtest.png");
        announcement = loadSafe(app, "assets/NPC_Announcement.png");
        val = loadSafe(app, "assets/NPC_Val.png");
        stacey = loadSafe(app, "assets/NPC_Stacey.png");
        rico = loadSafe(app, "assets/NPC_Rico.png");
        billy = loadSafe(app, "assets/Billy.png");
    }

    public PImage getNpcSprite(int index) {
        if (idleNpcs == null || idleNpcs.length == 0) {
            return null;
        }
        int safeIndex = Math.floorMod(index, idleNpcs.length);
        return idleNpcs[safeIndex];
    }

    public PImage getStorySprite(String id) {
        if ("dexter".equals(id)) return dexter;
        if ("jamie".equals(id)) return jamie;
        if ("msPatel".equals(id)) return msPatel;
        if ("mathtest".equals(id)) return mathtest;
        if ("announcement".equals(id)) return announcement;
        if ("val".equals(id)) return val;
        if ("stacey".equals(id)) return stacey;
        if ("rico".equals(id)) return rico;
        if ("billy".equals(id)) return billy;
        return null;
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
