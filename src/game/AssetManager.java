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

    // Fightable NPC sprites
    public final PImage npcGeek;
    public final PImage npcAce;
    public final PImage npcJock;
    public final PImage npcDexter;
    public final PImage npcJamie;
    public final PImage npcMsPatel;
    public final PImage npcMathTest;
    public final PImage npcAnnouncement;
    public final PImage npcVal;
    public final PImage npcStacey;
    public final PImage npcRico;
    public final PImage npcBilly;

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

        fence       = loadSafe(app, "assets/backgrounds/fences.png");
        tree        = loadSafe(app, "assets/backgrounds/Trees.png");
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
        plant   = loadSafe(app, "assets/backgrounds/plant.png");
        windows = new PImage[]{
                loadSafe(app, "assets/backgrounds/window1.png"),
                loadSafe(app, "assets/backgrounds/window2.png")
        };
        board       = loadSafe(app, "assets/backgrounds/board.png");
        idleNpcs = new PImage[]{
                loadSafe(app, "assets/npc/idleNPC1.png"),
                loadSafe(app, "assets/npc/idleNPC2.png")
        };
        walkingNpc  = loadSafe(app, "assets/npc/walkingNPC1.png");

        // Fightable enemy sprites
        npcGeek = loadSafe(app, "assets/npc/NPC_Geek.png");
        npcAce  = loadSafe(app, "assets/npc/NPC_Ace.png");
        npcJock = loadSafe(app, "assets/npc/NPC_Jock.png");
        npcDexter = loadSafe(app, "assets/npc/NPC_Dexter.png");
        npcJamie = loadSafe(app, "assets/npc/NPC_Jamie.png");
        npcMsPatel = loadSafe(app, "assets/npc/NPC_MsPatel.png");
        npcMathTest = loadSafe(app, "assets/npc/NPC_Mathtest.png");
        npcAnnouncement = loadSafe(app, "assets/npc/NPC_Announcement.png");
        npcVal = loadSafe(app, "assets/npc/NPC_Val.png");
        npcStacey = loadSafe(app, "assets/npc/NPC_Stacey.png");
        npcRico = loadSafe(app, "assets/npc/NPC_Rico.png");
        npcBilly = loadSafe(app, "assets/Billy.png");
    }

    /** Returns the sprite image for the given enemy type. */
    public PImage spriteForType(GameMap.EnemyType type) {
        if (type == GameMap.EnemyType.GEEK) return npcGeek;
        if (type == GameMap.EnemyType.ACE)  return npcAce;
        return npcJock;
    }

    public PImage storySprite(String id) {
        if ("dexter".equals(id)) return npcDexter;
        if ("jamie".equals(id)) return npcJamie;
        if ("msPatel".equals(id)) return npcMsPatel;
        if ("mathtest".equals(id)) return npcMathTest;
        if ("announcement".equals(id)) return npcAnnouncement;
        if ("val".equals(id)) return npcVal;
        if ("stacey".equals(id)) return npcStacey;
        if ("rico".equals(id)) return npcRico;
        if ("jock".equals(id)) return npcJock;
        if ("billy".equals(id)) return npcBilly;
        return npcDexter;
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
