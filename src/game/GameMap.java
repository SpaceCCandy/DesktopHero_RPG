package game;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

import java.io.File;
import java.util.*;

public class GameMap {

    // ── World ────────────────────────────────────────────────────────────────
    private static final int    GROUND_Y           = 500;
    private static final int    CHUNK_W            = 900;
    private static final String SAVE_FILE          = "data/generated_map.csv";
    private static final String MAP_VERSION        = "9";
    private static final int    SCHOOL_START_CHUNK = 4;

    // ── Terrain ───────────────────────────────────────────────────────────────
    private static final int   TERR_MIN_W  = 180;
    private static final int   TERR_MAX_W  = 340;
    private static final int   TERR_MIN_H  = 55;
    private static final int   TERR_MAX_H  = 190;
    private static final float TERR_CHANCE = 0.55f;

    // ── Buildings ─────────────────────────────────────────────────────────────
    private static final int BLDG_MIN_W   = 85;
    private static final int BLDG_MAX_W   = 260;
    private static final int BLDG_MIN_H   = 150;
    private static final int BLDG_MAX_H   = 270;
    private static final int BLDG_GAP_MIN = 70;
    private static final int BLDG_GAP_MAX = 125;

    // ── Trees & fences ────────────────────────────────────────────────────────
    private static final int TREE_MIN_H  = 85;
    private static final int TREE_MAX_H  = 170;
    private static final int FENCE_H     = 42;
    private static final int IMAGE_GAP_MIN = 56;

    // ── School interior ───────────────────────────────────────────────────────
    private static final int   SCHOOL_CEILING_Y      = 0;
    private static final int   SCHOOL_FLOOR_Y        = GROUND_Y;
    private static final int   SCHOOL_LOCKER_H       = 190;
    private static final int   SCHOOL_PLANT_H        = 145;
    private static final int   SCHOOL_DOOR_H         = 195;
    private static final int   SCHOOL_WINDOW_H       = 155;
    private static final int   SCHOOL_NPC_H          = 160;
    private static final int   WALKING_NPC_TRAVEL    = 260;
    private static final int   SCHOOL_WINDOW_GAP     = 300;
    private static final float SCHOOL_ENTRY_FADE     = 120f;

    // ── Fightable NPCs ────────────────────────────────────────────────────────
    private static final int FIGHT_NPCS_PER_CHUNK = 3;
    private static final int FIGHT_NPC_H          = 140;

    // ── Enemy types ───────────────────────────────────────────────────────────
    public enum EnemyType { GEEK, ACE, JOCK }

    // ── State ─────────────────────────────────────────────────────────────────
    private final PApplet      app;
    private final AssetManager assets;
    private PFont              font;

    private final List<TerrainBlock> terrain    = new ArrayList<>();
    private final List<Building>     buildings  = new ArrayList<>();
    private final List<Prop>         props      = new ArrayList<>();
    private final List<SchoolNpc>    schoolNpcs = new ArrayList<>();
    private final List<FightNpc>     fightNpcs  = new ArrayList<>();
    private final List<StoryNpc>     storyNpcs  = new ArrayList<>();
    private final Set<Integer>       generatedChunks = new HashSet<>();
    private int storyProgression;

    // ── Visibility cache ──────────────────────────────────────────────────────
    private float cachedCamX = Float.MIN_VALUE;
    private int   cachedScrW = -1;
    private final List<TerrainBlock> visTerrain    = new ArrayList<>();
    private final List<Building>     visBuildings  = new ArrayList<>();
    private final List<Prop>         visProps      = new ArrayList<>();
    private final List<SchoolNpc>    visSchoolNpcs = new ArrayList<>();

    // ── Math-test door tracking ───────────────────────────────────────────────
    // Set when the first school chunk is generated; points to a specific door prop.
    private Prop mathTestDoor = null;

    public GameMap(PApplet app, AssetManager assets) {
        this.app    = app;
        this.assets = assets;
        font = app.createFont("Comic Sans MS", 14, true);
        loadGeneratedMap();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void renderBehindPlayer(float camX, int scrW, Player player) {
        int first = Math.max(0, PApplet.floor(camX / CHUNK_W) - 1);
        int last  = PApplet.floor((camX + scrW) / CHUNK_W) + 1;
        for (int c = first; c <= last; c++) {
            generateChunkIfNeeded(c);
            ensureSchoolNpcsForChunk(c);
            ensureFightNpcsForChunk(c);
        }
        refreshCache(camX, scrW);

        drawSky();
        drawParallaxBackground(camX, scrW);
        drawSchoolBackground(camX, scrW);
        drawFences(camX, scrW);
        drawTerrain();
        drawBuildingsForLayer(false);
        if (!playerBehindGroundBuildings(player)) {
            drawBuildingsForLayer(true);
        }
        drawSchoolProps();
        drawMathTestArrow(camX, scrW);
        drawSchoolNpcs(camX, scrW);
        drawTrees(camX, scrW);
        drawGround(camX, scrW);
        drawFightNpcs(camX, scrW);
        drawStoryNpcs(camX, scrW);
        drawSchoolExteriorCover(camX, scrW, player);
    }

    public void renderInFrontOfPlayer(float camX, int scrW, Player player) {
        if (playerBehindGroundBuildings(player)) {
            drawBuildingsForLayer(true);
        }
    }

    public boolean isSchoolX(float x) {
        return x >= SCHOOL_START_CHUNK * CHUNK_W;
    }

    public float schoolEntryProgress(Player player) {
        if (player == null) return 0f;
        float start = SCHOOL_START_CHUNK * CHUNK_W;
        float cx = player.getX() + player.getWidth() / 2f;
        return PApplet.constrain((cx - start) / SCHOOL_ENTRY_FADE, 0f, 1f);
    }

    public TerrainBlock getPlatformAt(Player player) {
        return getPlatformAt(player.getX(), player.getY(),
                player.getWidth(), player.getHeight(), player.getPreviousY());
    }

    public TerrainBlock getPlatformAt(float px, float py, float pw, float ph) {
        return getPlatformAt(px, py, pw, ph, py);
    }

    private TerrainBlock getPlatformAt(float px, float py, float pw, float ph, float prevY) {
        for (TerrainBlock t : terrain) {
            boolean xOk = px + pw > t.x && px < t.x + t.w;
            boolean yOk = py + ph >= t.surfaceY() && py + ph <= t.surfaceY() + 14;
            if (xOk && yOk && canLand(px, pw, ph, prevY, t)) return t;
        }
        return null;
    }

    private boolean canLand(float px, float pw, float ph, float prevY, TerrainBlock t) {
        if (t.h <= 1) return true;
        boolean wasOn = Math.abs(prevY + ph - t.surfaceY()) <= 18;
        if (wasOn) return true;
        for (Building b : buildings) {
            if (Math.abs((b.y + b.h) - t.surfaceY()) < 2
                    && px + pw > b.x && px < b.x + b.w) return false;
        }
        return true;
    }

    public void blockBuildingCoveredJumps(Player player) { }

    /** Returns the nearest undefeated fightable NPC close enough to interact, or null. */
    public FightNpc findNearbyFightNpc(Player player) {
        float cx = player.getX() + player.getWidth() / 2f;
        float fy = player.getY() + player.getHeight();
        for (FightNpc npc : fightNpcs) {
            if (!npc.defeated
                    && Math.abs(cx - npc.x) < 85
                    && Math.abs(fy - npc.y) < 90)
                return npc;
        }
        return null;
    }

    public StoryNpc findNearbyStoryNpc(Player player) {
        float cx = player.getX() + player.getWidth() / 2f;
        float fy = player.getY() + player.getHeight();
        for (StoryNpc npc : storyNpcs) {
            if (!npc.defeated
                    && Math.abs(cx - npc.x) < 95
                    && Math.abs(fy - npc.y) < 110) {
                return npc;
            }
        }
        return null;
    }

    /** Returns true if the player is near the designated math-test door. */
    public boolean isNearMathTestDoor(Player player) {
        if (storyProgression != 1) return false;
        if (mathTestDoor == null) return false;
        float cx = player.getX() + player.getWidth() / 2f;
        float fy = player.getY() + player.getHeight();
        float doorCx = mathTestDoor.x + mathTestDoor.w / 2f;
        float doorFy = mathTestDoor.y + mathTestDoor.h;
        return Math.abs(cx - doorCx) < 90 && Math.abs(fy - doorFy) < 100;
    }

    public void drawInteractionPrompt(Player player) {
        StoryNpc storyNpc = findNearbyStoryNpc(player);
        boolean nearFight = findNearbyFightNpc(player) != null;
        boolean nearMath  = isNearMathTestDoor(player);
        if (storyNpc == null && !nearFight && !nearMath) return;

        app.textFont(font);
        app.fill(255, 245, 170);
        app.stroke(80);
        app.rect(320, 430, 320, 46, 6);
        app.fill(20);
        app.textAlign(PApplet.CENTER, PApplet.CENTER);
        app.textSize(16);
        if (nearMath) app.text("E to take math test", 480, 453);
        else if (storyNpc != null) app.text("E to talk", 480, 453);
        else app.text("E to fight", 480, 453);
    }

    public void markDefeated(FightNpc npc) {
        if (npc == null) return;
        npc.defeated = true;
        saveGeneratedMap();
    }

    public void setStoryProgression(int progression) {
        if (storyProgression == progression && !storyNpcs.isEmpty()) return;
        storyProgression = progression;
        storyNpcs.clear();
        spawnStoryNpcForProgression();
    }

    public void advanceStoryNpc() {
        for (StoryNpc npc : storyNpcs) {
            npc.defeated = true;
        }
        storyNpcs.clear();
    }

    public String activeStoryNpcName() {
        return storyNpcs.isEmpty() ? null : storyNpcs.get(0).name;
    }

    // ── Chunk generation ──────────────────────────────────────────────────────

    private void generateChunkIfNeeded(int chunk) {
        if (generatedChunks.contains(chunk)) return;
        generatedChunks.add(chunk);
        invalidateCache();

        Random rng    = new Random(4000L + chunk * 99173L);
        float  startX = chunk * CHUNK_W;

        if (chunk >= SCHOOL_START_CHUNK) {
            generateSchoolChunk(chunk, rng, startX);
            saveGeneratedMap();
            return;
        }

        // ── Step 1: elevated terrain blocks ──────────────────────────────────
        List<TerrainBlock> chunkTerrain = new ArrayList<>();
        if (rng.nextFloat() < TERR_CHANCE) {
            int count  = 1 + rng.nextInt(2);
            float cursor = startX + 80 + rng.nextInt(120);
            for (int i = 0; i < count; i++) {
                float tw = TERR_MIN_W + rng.nextInt(TERR_MAX_W - TERR_MIN_W);
                float th = TERR_MIN_H + rng.nextInt(TERR_MAX_H - TERR_MIN_H);
                if (cursor + tw > startX + CHUNK_W - 40) break;
                TerrainBlock block = new TerrainBlock(chunk, cursor, tw, th);
                addStairs(chunk, block, chunkTerrain, rng);
                chunkTerrain.add(block);
                terrain.add(block);
                cursor = cursor + tw + 120 + rng.nextInt(150);
            }
        }

        // ── Step 2: buildings ─────────────────────────────────────────────────
        List<float[]> surfaces = new ArrayList<>();
        surfaces.add(new float[]{ startX, startX + CHUNK_W, GROUND_Y });
        for (TerrainBlock t : chunkTerrain) {
            if (t.w >= TERR_MIN_W) surfaces.add(new float[]{ t.x, t.x + t.w, t.surfaceY() });
        }
        for (float[] surf : surfaces) {
            float cursor = surf[0] + rng.nextInt(30);
            while (cursor < surf[1] - BLDG_MIN_W) {
                float maxW = Math.min(BLDG_MAX_W, surf[1] - cursor);
                if (maxW < BLDG_MIN_W) break;
                int    imgIdx = rng.nextInt(assets.buildings.length);
                PImage img    = safeGetImage(imgIdx);
                float  ratio  = imageRatio(img, 0.75f);
                float  bh     = BLDG_MIN_H + rng.nextInt(BLDG_MAX_H - BLDG_MIN_H);
                float  bw     = bh * ratio;
                if (bw > maxW) { bw = maxW; bh = bw / ratio; }
                if (bw < BLDG_MIN_W || bh < BLDG_MIN_H * 0.8f) break;
                if (!buildingOverlaps(chunk, cursor, cursor + bw, surf[2]))
                    buildings.add(new Building(chunk, cursor, surf[2] - bh, bw, bh, imgIdx));
                cursor += bw + BLDG_GAP_MIN + rng.nextInt(BLDG_GAP_MAX - BLDG_GAP_MIN);
                if (rng.nextFloat() < 0.30f) break;
            }
        }

        // ── Step 3: trees and fences ──────────────────────────────────────────
        List<float[]> footprints = new ArrayList<>();
        for (Building b : buildings) {
            if (b.chunk == chunk) footprints.add(new float[]{ b.x, b.x + b.w });
        }
        placeFences(chunk, rng, startX, footprints);
        placeTrees(chunk, rng, startX, chunkTerrain, footprints);

        saveGeneratedMap();
    }

    private void addStairs(int chunk, TerrainBlock block,
                           List<TerrainBlock> chunkTerrain, Random rng) {
        int   steps     = Math.max(2, (int) Math.ceil(block.h / 35f));
        float stepW     = 55;
        boolean fromRight = rng.nextBoolean();
        for (int i = 1; i <= steps; i++) {
            float stepH = block.h * i / (steps + 1f);
            float stepX = fromRight
                    ? block.x + block.w + (steps - i) * stepW
                    : block.x - (steps - i + 1) * stepW;
            if (stepX < chunk * CHUNK_W + 20 || stepX + stepW > (chunk + 1) * CHUNK_W - 20) continue;
            TerrainBlock step = new TerrainBlock(chunk, stepX, stepW, stepH);
            chunkTerrain.add(step);
            terrain.add(step);
        }
    }

    private boolean buildingOverlaps(int chunk, float x1, float x2, float surfY) {
        for (Building b : buildings) {
            if (b.chunk != chunk) continue;
            if (Math.abs(b.y + b.h - surfY) < 2
                    && x2 > b.x - BLDG_GAP_MIN && x1 < b.x + b.w + BLDG_GAP_MIN) return true;
        }
        return false;
    }

    private void placeFences(int chunk, Random rng, float startX, List<float[]> fps) {
        int attempts = 3 + rng.nextInt(3);
        for (int i = 0; i < attempts; i++) {
            float fx  = startX + 40 + rng.nextInt((int)(CHUNK_W - 200));
            float fLen = 70 + rng.nextInt(150);
            if (!gapOverlaps(fx, fx + fLen, fps, 28)) {
                props.add(new Prop(chunk, PropType.FENCE, fx, GROUND_Y - FENCE_H, fLen, FENCE_H, 0));
                fps.add(new float[]{ fx, fx + fLen });
            }
        }
    }

    private void placeTrees(int chunk, Random rng, float startX,
                            List<TerrainBlock> chunkTerrain, List<float[]> fps) {
        int groundTrees = 3 + rng.nextInt(3);
        for (int i = 0; i < groundTrees; i++) {
            float th = TREE_MIN_H + rng.nextInt(TREE_MAX_H - TREE_MIN_H);
            float tw = th * 0.65f;
            float tx = startX + 30 + rng.nextInt((int)(CHUNK_W - tw - 60));
            if (!gapOverlaps(tx, tx + tw, fps, 36)) {
                props.add(new Prop(chunk, PropType.TREE, tx, GROUND_Y - th, tw, th, 0));
                fps.add(new float[]{ tx, tx + tw });
            }
        }
        for (TerrainBlock t : chunkTerrain) {
            if (rng.nextFloat() < 0.8f) {
                float th = TREE_MIN_H + rng.nextInt(TREE_MAX_H - TREE_MIN_H);
                float tw = Math.min(th * 0.65f, t.w - 20);
                if (tw < 30) continue;
                float tx = t.x + 10 + rng.nextInt(Math.max(1, (int)(t.w - tw - 20)));
                if (!gapOverlaps(tx, tx + tw, fps, 36)) {
                    props.add(new Prop(chunk, PropType.TREE, tx, t.surfaceY() - th, tw, th, 0));
                    fps.add(new float[]{ tx, tx + tw });
                }
            }
        }
    }

    private void generateSchoolChunk(int chunk, Random rng, float startX) {
        List<float[]> occupied = new ArrayList<>();
        List<float[]> windowsOnly = new ArrayList<>();

        // Windows along the top
        int windowCount = 2 + rng.nextInt(2);
        for (int i = 0; i < windowCount; i++) {
            int    imgIdx = assets.windows.length == 0 ? 0 : rng.nextInt(assets.windows.length);
            PImage img    = safeImage(assets.windows, imgIdx);
            float  h = SCHOOL_WINDOW_H;
            float  w = h * imageRatio(img, 0.75f);
            float  x = startX + 70 + i * (w + SCHOOL_WINDOW_GAP) + rng.nextInt(45);
            float  y = SCHOOL_CEILING_Y + 32;
            if (addSchoolProp(chunk, occupied, PropType.WINDOW, x, y, w, h, imgIdx, SCHOOL_WINDOW_GAP)) {
                windowsOnly.add(new float[]{ x, y, x + w, y + h });
            }
        }

        // Floor props
        float cursor = startX + 55 + rng.nextInt(50);
        while (cursor < startX + CHUNK_W - 80) {
            float choice = rng.nextFloat();
            PropType type;
            PImage img  = null;
            float h;
            int imgIdx  = 0;

            if (choice < 0.40f) {
                type   = PropType.LOCKER;
                imgIdx = rng.nextInt(Math.max(1, assets.lockers.length));
                img    = safeImage(assets.lockers, imgIdx);
                h      = SCHOOL_LOCKER_H;
            } else if (choice < 0.62f) {
                type = PropType.PLANT;
                img  = assets.plant;
                h    = SCHOOL_PLANT_H;
            } else if (choice < 0.80f) {
                type = PropType.DOOR;
                h    = SCHOOL_DOOR_H;
            } else {
                type   = PropType.BOARD;
                img    = assets.board;
                h      = 82 + rng.nextInt(45);
            }

            float w = (type == PropType.DOOR) ? 100
                    : h * imageRatio(img, type == PropType.BOARD ? 1.35f : 0.75f);
            float y = (type == PropType.BOARD)
                    ? SCHOOL_CEILING_Y + 145 + rng.nextInt(45)
                    : SCHOOL_FLOOR_Y - h;

            if (addSchoolProp(chunk, occupied, type, cursor, y, w, h, imgIdx, IMAGE_GAP_MIN)) {
                // Tag the very first door in the very first school chunk as the math-test door
                if (type == PropType.DOOR && mathTestDoor == null
                        && chunk == SCHOOL_START_CHUNK) {
                    mathTestDoor = props.get(props.size() - 1);
                }
                cursor += w + 55 + rng.nextInt(80);
            } else {
                cursor += 65;
            }
        }
    }

    private boolean addSchoolProp(int chunk, List<float[]> occupied, PropType type,
                                  float x, float y, float w, float h,
                                  int imgIdx, float gap) {
        if (x < chunk * CHUNK_W + 20 || x + w > (chunk + 1) * CHUNK_W - 20) return false;
        if (rectOverlaps(x, y, w, h, occupied, gap)) return false;
        props.add(new Prop(chunk, type, x, y, w, h, imgIdx));
        occupied.add(new float[]{ x, y, x + w, y + h });
        return true;
    }

    private boolean rectOverlaps(float x, float y, float w, float h,
                                 List<float[]> rects, float gap) {
        float x2 = x + w, y2 = y + h;
        for (float[] r : rects) {
            if (x2 > r[0] - gap && x < r[2] + gap && y2 > r[1] - gap && y < r[3] + gap) return true;
        }
        return false;
    }

    // ── NPC spawning ──────────────────────────────────────────────────────────

    /** Spawn decorative school NPCs (non-fightable) — only in school. */
    private void ensureSchoolNpcsForChunk(int chunk) {
        if (chunk < SCHOOL_START_CHUNK) return;
        for (SchoolNpc n : schoolNpcs) if (n.chunk == chunk) return;

        Random rng = new Random(13000L + chunk * 34871L);
        List<float[]> occupied = new ArrayList<>();

        int staticCount = 2 + rng.nextInt(3);
        for (int i = 0; i < staticCount; i++) {
            int   imgIdx = rng.nextInt(Math.max(1, assets.idleNpcs.length));
            float npcW   = schoolNpcW(safeImage(assets.idleNpcs, imgIdx));
            float x      = openSchoolNpcX(chunk, rng, occupied, npcW, false);
            occupied.add(new float[]{ x, x + npcW });
            schoolNpcs.add(new SchoolNpc(chunk, x, SCHOOL_FLOOR_Y, imgIdx, false, rng.nextInt(WALKING_NPC_TRAVEL)));
        }

        float walkW = schoolNpcW(assets.walkingNpc);
        float walkX = openSchoolNpcX(chunk, rng, occupied, walkW + WALKING_NPC_TRAVEL, true);
        occupied.add(new float[]{ walkX, walkX + walkW + WALKING_NPC_TRAVEL });
        schoolNpcs.add(new SchoolNpc(chunk, walkX, SCHOOL_FLOOR_Y, 0, true, rng.nextInt(WALKING_NPC_TRAVEL)));
    }

    /** Spawn fightable typed NPCs — only in school chunks. */
    private void ensureFightNpcsForChunk(int chunk) {
        if (chunk < SCHOOL_START_CHUNK) return;
        int count = 0;
        for (FightNpc n : fightNpcs) if (n.chunk == chunk) count++;
        if (count >= FIGHT_NPCS_PER_CHUNK) return;

        Random      rng   = new Random(9000L + chunk * 55147L);
        EnemyType[] types = EnemyType.values();

        while (count < FIGHT_NPCS_PER_CHUNK) {
            float     nx = chunk * CHUNK_W + 160 + count * 220 + rng.nextInt(80);
            int       hp = 70 + rng.nextInt(81);
            EnemyType t  = types[rng.nextInt(types.length)];
            fightNpcs.add(new FightNpc(chunk, nx, GROUND_Y, false, hp, t));
            count++;
        }
    }

    private void spawnStoryNpcForProgression() {
        if (storyProgression == 1 || storyProgression >= 20) return;

        String[] ids = {
                "dexter", "mathtest", "stacey", "dexter", "jock",
                "msPatel", "rico", "rico", "mathtest", "stacey",
                "jamie", "msPatel", "dexter", "mathtest", "rico",
                "stacey", "dexter", "announcement", "rico", "val"
        };
        String[] names = {
                "Dexter", "Math Test", "Stacey", "Dexter", "Hallway Jock",
                "Ms. Patel", "Rico", "Rico", "Science Test", "Stacey",
                "Jamie", "Ms. Patel", "Dexter", "English Test", "Rico",
                "Stacey", "Dexter", "Announcement", "Rico + Dexter", "The Val"
        };

        int step = Math.max(0, Math.min(storyProgression, ids.length - 1));
        Random rng = new Random(88000L + step * 3911L);
        int chunk = SCHOOL_START_CHUNK + step + rng.nextInt(2);
        float x = chunk * CHUNK_W + 170 + rng.nextInt(CHUNK_W - 340);
        storyNpcs.add(new StoryNpc(chunk, x, GROUND_Y, ids[step], names[step], step, false));
    }

    private float schoolNpcW(PImage img) {
        return SCHOOL_NPC_H * imageRatio(img, 0.75f);
    }

    private float openSchoolNpcX(int chunk, Random rng, List<float[]> occupied,
                                 float width, boolean walking) {
        float minX = chunk * CHUNK_W + 85;
        float maxX = (chunk + 1) * CHUNK_W - width - 85;
        float gap  = walking ? 30 : 45;
        for (int attempt = 0; attempt < 30; attempt++) {
            float x = minX + rng.nextFloat() * Math.max(1, maxX - minX);
            if (!gapOverlaps(x, x + width, occupied, gap)) return x;
        }
        float x = minX;
        while (x <= maxX) {
            if (!gapOverlaps(x, x + width, occupied, gap)) return x;
            x += width + gap;
        }
        return minX;
    }

    // ── Visibility cache ──────────────────────────────────────────────────────

    private void refreshCache(float camX, int scrW) {
        if (Math.abs(camX - cachedCamX) < 4f && scrW == cachedScrW
                && !visTerrain.isEmpty()) return;
        cachedCamX = camX;
        cachedScrW = scrW;

        visTerrain.clear();
        for (TerrainBlock t : terrain)
            if (visible(t.x, t.w, camX, scrW)) visTerrain.add(t);

        visBuildings.clear();
        for (Building b : buildings)
            if (visible(b.x, b.w, camX, scrW)) visBuildings.add(b);

        visProps.clear();
        for (Prop p : props)
            if (visible(p.x, p.w, camX, scrW)) visProps.add(p);

        visSchoolNpcs.clear();
        for (SchoolNpc n : schoolNpcs) {
            float span = n.walking ? WALKING_NPC_TRAVEL + 80 : 80;
            if (visible(n.x, span, camX, scrW)) visSchoolNpcs.add(n);
        }
    }

    private void invalidateCache() { cachedCamX = Float.MIN_VALUE; }

    // ── Draw ──────────────────────────────────────────────────────────────────

    private void useFont(float size) {
        app.textFont(font);
        app.textSize(size);
    }

    private void drawSky() { app.background(210, 230, 255); }

    private void drawParallaxBackground(float camX, int scrW) {
        int first = Math.max(0, PApplet.floor((camX * 0.35f) / CHUNK_W) - 1);
        int last = PApplet.floor(((camX + scrW) * 0.35f) / CHUNK_W) + 2;

        for (int c = first; c <= last; c++) {
            Random rng = new Random(52000L + c * 7411L);
            float baseX = c * CHUNK_W;

            for (int i = 0; i < 3; i++) {
                int idx = rng.nextInt(Math.max(1, assets.clouds.length));
                PImage img = safeImage(assets.clouds, idx);
                float w = 110 + rng.nextInt(70);
                float h = w / imageRatio(img, 2.2f);
                float x = baseX + 80 + i * 260 + rng.nextInt(90) + camX * 0.55f;
                float y = 35 + rng.nextInt(115);
                if (!visible(x, w, camX, scrW)) continue;
                if (img != null) {
                    app.tint(255, 205);
                    app.image(img, x, y, w, h);
                    app.noTint();
                } else {
                    app.noStroke();
                    app.fill(255, 255, 255, 180);
                    app.ellipse(x + w * 0.4f, y + h * 0.5f, w * 0.75f, h);
                    app.ellipse(x + w * 0.7f, y + h * 0.55f, w * 0.65f, h * 0.85f);
                }
            }

            for (int i = 0; i < 2; i++) {
                int idx = rng.nextInt(Math.max(1, assets.buildings.length));
                PImage img = safeGetImage(idx);
                float w = 140 + rng.nextInt(80);
                float h = 190 + rng.nextInt(90);
                float x = baseX + 100 + i * 360 + rng.nextInt(80) + camX * 0.32f;
                float y = GROUND_Y - h;
                if (!visible(x, w, camX, scrW)) continue;
                if (img != null) {
                    app.tint(255, 135);
                    app.image(img, x, y, w, h);
                    app.noTint();
                } else {
                    app.noStroke();
                    app.fill(205, 218, 230, 150);
                    app.rect(x, y, w, h, 4);
                }
            }
        }
    }

    private void drawGround(float camX, int scrW) {
        float schoolX = SCHOOL_START_CHUNK * CHUNK_W;
        if (camX >= schoolX) return;
        float drawX = camX - 20;
        float drawW = Math.min(scrW + 40, schoolX - drawX);
        app.fill(100, 180, 255);
        app.stroke(60, 140, 220);
        app.strokeWeight(2);
        app.rect(drawX, GROUND_Y, drawW, 120);
        app.strokeWeight(1);
    }

    private void drawTerrain() {
        for (TerrainBlock t : visTerrain) {
            app.fill(100, 180, 255);
            app.stroke(60, 140, 220);
            app.strokeWeight(2);
            app.rect(t.x, t.surfaceY(), t.w, t.h);
            app.strokeWeight(1);
        }
    }

    private void drawBuildingsForLayer(boolean groundLayer) {
        for (Building b : visBuildings) {
            boolean onGround = Math.abs((b.y + b.h) - GROUND_Y) < 2;
            if (onGround != groundLayer) continue;
            PImage img   = safeGetImage(b.imageIndex);
            float  baseY = b.y + b.h;
            if (img != null) {
                float drawW = b.w, drawH = b.w / imageRatio(img, 1f);
                if (drawH > b.h) { drawH = b.h; drawW = drawH * imageRatio(img, 1f); }
                app.image(img, b.x + b.w / 2f - drawW / 2f, baseY - drawH, drawW, drawH);
            } else {
                app.fill(235, 230, 220); app.stroke(80);
                app.rect(b.x, b.y, b.w, b.h, 3);
                app.fill(200, 220, 240); app.noStroke();
                float ww = b.w * 0.25f, wh = b.h * 0.18f;
                app.rect(b.x + b.w * 0.15f, b.y + b.h * 0.15f, ww, wh, 2);
                app.rect(b.x + b.w * 0.55f, b.y + b.h * 0.15f, ww, wh, 2);
            }
        }
    }

    private void drawSchoolBackground(float camX, int scrW) {
        int first = Math.max(SCHOOL_START_CHUNK, PApplet.floor(camX / CHUNK_W) - 1);
        int last  = PApplet.floor((camX + scrW) / CHUNK_W) + 1;
        for (int c = first; c <= last; c++) {
            float x = c * CHUNK_W;
            app.noStroke();
            app.fill(236, 232, 218);
            app.rect(x, SCHOOL_CEILING_Y, CHUNK_W, SCHOOL_FLOOR_Y);
            app.fill(198, 190, 176);
            app.rect(x, SCHOOL_FLOOR_Y, CHUNK_W, 120);
            app.stroke(150, 140, 128);
            app.line(x, SCHOOL_CEILING_Y, x + CHUNK_W, SCHOOL_CEILING_Y);
            app.line(x, SCHOOL_FLOOR_Y, x + CHUNK_W, SCHOOL_FLOOR_Y);
            app.stroke(210, 205, 194);
            for (float tx = x; tx < x + CHUNK_W; tx += 70)
                app.line(tx, SCHOOL_FLOOR_Y, tx + 50, SCHOOL_FLOOR_Y + 120);
            app.strokeWeight(1);
        }
    }

    private void drawSchoolExteriorCover(float camX, int scrW, Player player) {
        float progress = schoolEntryProgress(player);
        float alpha    = 255f * (1f - progress);
        if (alpha <= 1f) return;

        int first = Math.max(SCHOOL_START_CHUNK, PApplet.floor(camX / CHUNK_W) - 1);
        int last  = PApplet.floor((camX + scrW) / CHUNK_W) + 1;
        app.pushStyle();
        for (int c = first; c <= last; c++) {
            float x = c * CHUNK_W;
            app.noStroke();
            app.fill(230, 225, 207, alpha);
            app.rect(x, 0, CHUNK_W, SCHOOL_FLOOR_Y);
            app.fill(202, 196, 176, alpha);
            app.rect(x, SCHOOL_FLOOR_Y, CHUNK_W, 120);
            app.stroke(180, 170, 150, alpha);
            app.strokeWeight(2);
            app.line(x, SCHOOL_FLOOR_Y, x + CHUNK_W, SCHOOL_FLOOR_Y);
            drawExtWindow(x + 105, 48, alpha);
            drawExtWindow(x + 385, 48, alpha);
            drawExtWindow(x + 665, 48, alpha);
            drawExtWindow(x + 120, 263, alpha);
            drawExtWindow(x + 410, 263, alpha);
            app.noStroke();
            app.fill(115, 82, 54, alpha);
            app.rect(x + CHUNK_W - 150, SCHOOL_FLOOR_Y - 190, 95, 190, 3);
            app.fill(225, 190, 72, alpha);
            app.ellipse(x + CHUNK_W - 72, SCHOOL_FLOOR_Y - 95, 7, 7);
        }
        app.popStyle();
    }

    private void drawExtWindow(float x, float y, float alpha) {
        app.fill(230, 238, 246, alpha);
        app.stroke(105, 105, 100, alpha);
        app.strokeWeight(3);
        app.rect(x, y, 120, 90, 3);
        app.strokeWeight(1);
        app.line(x + 60, y + 4, x + 60, y + 86);
        app.line(x + 4, y + 45, x + 116, y + 45);
        app.fill(170, 185, 198, alpha * 0.4f);
        app.noStroke();
        app.rect(x + 4, y + 4, 112, 82, 3);
    }

    private void drawSchoolProps() {
        for (Prop prop : visProps) {
            if (!isSchoolProp(prop.type)) continue;
            PImage img = imageForSchoolProp(prop);
            if (img != null) {
                drawImagePreserved(img, prop.x, prop.y, prop.w, prop.h);
            } else {
                drawFallbackSchoolProp(prop);
            }
        }
    }

    /** Draw a pulsing arrow above the designated math-test door. */
    private void drawMathTestArrow(float camX, int scrW) {
        if (mathTestDoor == null) return;
        if (!visible(mathTestDoor.x, mathTestDoor.w, camX, scrW)) return;

        float cx     = mathTestDoor.x + mathTestDoor.w / 2f;
        float topY   = mathTestDoor.y - 14;
        float pulse  = 0.5f + 0.5f * PApplet.sin(app.frameCount * 0.12f);

        // Pulsing red downward arrow
        app.noStroke();
        app.fill(255, 60 + (int)(120 * pulse), 60, 220);
        float arrowTip = topY - pulse * 8;
        app.triangle(cx - 12, arrowTip - 18, cx + 12, arrowTip - 18, cx, arrowTip);

        // Small label above arrow
        useFont(11);
        app.fill(30);
        app.textAlign(PApplet.CENTER, PApplet.BOTTOM);
        app.text("Math Test", cx, arrowTip - 22);
    }

    private void drawSchoolNpcs(float camX, int scrW) {
        for (SchoolNpc npc : visSchoolNpcs) {
            float  x     = npc.x;
            boolean right = true;
            PImage img;

            if (npc.walking) {
                float t = ((app.millis() / 16f) + npc.phase) % (WALKING_NPC_TRAVEL * 2);
                if (t > WALKING_NPC_TRAVEL) { x += WALKING_NPC_TRAVEL * 2 - t; right = false; }
                else                        { x += t; }
                img = assets.walkingNpc;
            } else {
                img = safeImage(assets.idleNpcs, npc.imageIndex);
            }

            float h = SCHOOL_NPC_H, w = h * imageRatio(img, 0.75f);
            if (!visible(x, w, camX, scrW)) continue;

            if (img != null) {
                if (right) {
                    app.image(img, x, SCHOOL_FLOOR_Y - h, w, h);
                } else {
                    app.pushMatrix();
                    app.translate(x + w, SCHOOL_FLOOR_Y - h);
                    app.scale(-1, 1);
                    app.image(img, 0, 0, w, h);
                    app.popMatrix();
                }
            } else {
                app.fill(120, 80, 150); app.noStroke();
                app.rect(x, SCHOOL_FLOOR_Y - h, w, h, 4);
            }
        }
    }

    /** Draw typed fightable NPCs using their specific sprites. */
    private void drawFightNpcs(float camX, int scrW) {
        for (FightNpc npc : fightNpcs) {
            if (npc.defeated || !visible(npc.x - 40, 80, camX, scrW)) continue;

            PImage img = assets.spriteForType(npc.type);
            float  h   = FIGHT_NPC_H;
            float  w   = (img != null) ? h * imageRatio(img, 0.75f) : 50;
            float  drawX = npc.x - w / 2f;
            float  drawY = npc.y - h;

            if (img != null) {
                app.image(img, drawX, drawY, w, h);
            } else {
                // Fallback coloured rectangle
                if      (npc.type == EnemyType.GEEK) app.fill(255, 230,  50);
                else if (npc.type == EnemyType.ACE)  app.fill( 80, 200, 100);
                else                                  app.fill(255, 140,  40);
                app.stroke(60); app.rect(drawX, drawY, w, h, 5);
            }

            // Name + HP label
            useFont(12);
            app.fill(30);
            app.textAlign(PApplet.CENTER, PApplet.BOTTOM);
            String label = npc.type.name().charAt(0)
                    + npc.type.name().substring(1).toLowerCase();
            app.text(label + "  " + npc.maxHealth + "HP", npc.x, drawY - 4);
        }
    }

    private void drawStoryNpcs(float camX, int scrW) {
        for (StoryNpc npc : storyNpcs) {
            if (npc.defeated || !visible(npc.x - 55, 110, camX, scrW)) continue;

            PImage img = assets.storySprite(npc.spriteId);
            float h = 135;
            float w = img != null ? h * imageRatio(img, 0.75f) : 75;
            float drawX = npc.x - w / 2f;
            float drawY = npc.y - h;
            if (img != null) {
                app.image(img, drawX, drawY, w, h);
            } else {
                app.fill(120, 90, 180);
                app.stroke(60);
                app.rect(drawX, drawY, w, h, 5);
            }

            useFont(12);
            app.fill(30);
            app.textAlign(PApplet.CENTER, PApplet.BOTTOM);
            app.text(npc.name, npc.x, drawY - 4);
        }
    }

    private void drawFences(float camX, int scrW) {
        for (Prop p : visProps) {
            if (p.type != PropType.FENCE) continue;
            PImage img = assets.fence;
            if (img != null) drawCropped(img, p.x, p.y + p.h - FENCE_H, p.w, FENCE_H);
            else { app.fill(180); app.stroke(80); app.rect(p.x, p.y, p.w, p.h); }
        }
    }

    private void drawTrees(float camX, int scrW) {
        for (Prop p : visProps) {
            if (p.type != PropType.TREE) continue;
            PImage img = assets.tree;
            if (img != null) {
                float dh = p.h, dw = dh * img.width / (float) img.height;
                app.image(img, p.x + p.w / 2f - dw / 2f, p.y, dw, dh);
            } else {
                app.fill(60, 120, 40); app.noStroke();
                app.ellipse(p.x + p.w / 2f, p.y + p.h * 0.38f, p.w, p.h * 0.65f);
                app.fill(80, 50, 20);
                app.rect(p.x + p.w * 0.42f, p.y + p.h * 0.6f, p.w * 0.16f, p.h * 0.4f);
            }
        }
    }

    // ── School prop drawing helpers ───────────────────────────────────────────

    private boolean isSchoolProp(PropType t) {
        return t == PropType.LOCKER || t == PropType.PLANT
                || t == PropType.WINDOW || t == PropType.BOARD || t == PropType.DOOR;
    }

    private PImage imageForSchoolProp(Prop p) {
        if (p.type == PropType.LOCKER) return safeImage(assets.lockers, p.imageIndex);
        if (p.type == PropType.PLANT)  return assets.plant;
        if (p.type == PropType.WINDOW) return safeImage(assets.windows, p.imageIndex);
        if (p.type == PropType.BOARD)  return assets.board;
        return null; // DOOR uses fallback
    }

    private void drawFallbackSchoolProp(Prop p) {
        if (p.type == PropType.DOOR) {
            boolean isMath = (p == mathTestDoor);
            app.fill(isMath ? 0xFF7A3C8C : 0xFF785A42);  // purple for math door, brown otherwise
            app.stroke(isMath ? 0xFF4A1C6C : 0xFF46382A);
            app.rect(p.x, p.y, p.w, p.h, 3);
            app.fill(230, 198, 80); app.noStroke();
            app.ellipse(p.x + p.w * 0.82f, p.y + p.h * 0.52f, 7, 7);
            return;
        }
        app.fill(190); app.stroke(90); app.rect(p.x, p.y, p.w, p.h, 3);
    }

    private void drawImagePreserved(PImage img, float x, float y, float maxW, float maxH) {
        float ratio = imageRatio(img, 1f);
        float dw = maxW, dh = dw / ratio;
        if (dh > maxH) { dh = maxH; dw = dh * ratio; }
        app.image(img, x + maxW / 2f - dw / 2f, y + maxH - dh, dw, dh);
    }

    private void drawCropped(PImage img, float x, float y, float w, float h) {
        float tr = w / h, sr = imageRatio(img, tr);
        float sx1 = 0, sy1 = 0, sx2 = img.width, sy2 = img.height;
        if (sr > tr) { float cw = img.height * tr; sx1 = (img.width - cw) / 2f; sx2 = sx1 + cw; }
        else if (sr < tr) { float ch = img.width / tr; sy1 = (img.height - ch) / 2f; sy2 = sy1 + ch; }
        app.image(img, x, y, w, h, (int)sx1, (int)sy1, (int)sx2, (int)sy2);
    }

    // ── Save / Load ───────────────────────────────────────────────────────────

    private void saveGeneratedMap() {
        List<String> lines = new ArrayList<>();
        File file = new File(app.sketchPath(SAVE_FILE));
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        lines.add("V," + MAP_VERSION);
        for (int c : generatedChunks) lines.add("C," + c);
        for (TerrainBlock t : terrain)
            lines.add("T," + t.chunk + "," + t.x + "," + t.w + "," + t.h);
        for (Building b : buildings)
            lines.add("B," + b.chunk + "," + b.x + "," + b.y + "," + b.w + "," + b.h + "," + b.imageIndex);
        for (Prop p : props)
            lines.add("P," + p.chunk + "," + p.type.name() + "," + p.x + "," + p.y + "," + p.w + "," + p.h + "," + p.imageIndex);
        for (FightNpc n : fightNpcs)
            lines.add("N," + n.chunk + "," + n.x + "," + n.y + "," + n.defeated + "," + n.maxHealth + "," + n.type.name());

        app.saveStrings(file.getAbsolutePath(), lines.toArray(new String[0]));
    }

    private void loadGeneratedMap() {
        File file = new File(app.sketchPath(SAVE_FILE));
        if (!file.exists()) return;
        String[] lines = app.loadStrings(file.getAbsolutePath());
        if (lines == null || lines.length == 0) return;
        if (!("V," + MAP_VERSION).equals(lines[0])) return;

        for (String line : lines) {
            String[] p = PApplet.split(line, ',');
            if (p.length == 0) continue;
            try {
                switch (p[0]) {
                    case "C": generatedChunks.add(PApplet.parseInt(p[1])); break;
                    case "T": terrain.add(new TerrainBlock(PApplet.parseInt(p[1]),
                            PApplet.parseFloat(p[2]), PApplet.parseFloat(p[3]), PApplet.parseFloat(p[4]))); break;
                    case "B": buildings.add(new Building(PApplet.parseInt(p[1]),
                            PApplet.parseFloat(p[2]), PApplet.parseFloat(p[3]),
                            PApplet.parseFloat(p[4]), PApplet.parseFloat(p[5]),
                            p.length >= 7 ? PApplet.parseInt(p[6]) : 0)); break;
                    case "P": {
                        Prop prop = new Prop(PApplet.parseInt(p[1]), PropType.valueOf(p[2]),
                                PApplet.parseFloat(p[3]), PApplet.parseFloat(p[4]),
                                PApplet.parseFloat(p[5]), PApplet.parseFloat(p[6]),
                                p.length >= 8 ? PApplet.parseInt(p[7]) : 0);
                        props.add(prop);
                        // Re-link math test door: first DOOR prop in the first school chunk
                        if (prop.type == PropType.DOOR && mathTestDoor == null
                                && prop.chunk == SCHOOL_START_CHUNK) {
                            mathTestDoor = prop;
                        }
                        break;
                    }
                    case "N": {
                        EnemyType t = p.length >= 7
                                ? EnemyType.valueOf(p[6])
                                : EnemyType.values()[(int)(Math.random() * 3)];
                        fightNpcs.add(new FightNpc(PApplet.parseInt(p[1]),
                                PApplet.parseFloat(p[2]), PApplet.parseFloat(p[3]),
                                PApplet.parseBoolean(p[4]), PApplet.parseInt(p[5]), t));
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean visible(float x, float w, float camX, int scrW) {
        return x + w >= camX - 100 && x <= camX + scrW + 100;
    }

    private boolean playerBehindGroundBuildings(Player player) {
        return player != null && player.isOnPlatformLayer();
    }

    private boolean gapOverlaps(float x1, float x2, List<float[]> fps, float gap) {
        for (float[] fp : fps) {
            if (x2 > fp[0] - gap && x1 < fp[1] + gap) return true;
        }
        return false;
    }

    private PImage safeGetImage(int idx) {
        if (assets.buildings == null || idx >= assets.buildings.length) return null;
        PImage img = assets.buildings[idx];
        return (img != null && img.width > 0) ? img : null;
    }

    private PImage safeImage(PImage[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length) return null;
        PImage img = arr[idx];
        return (img != null && img.width > 0 && img.height > 0) ? img : null;
    }

    private float imageRatio(PImage img, float fallback) {
        if (img == null || img.height <= 0) return fallback;
        return img.width / (float) img.height;
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    private enum PropType { TREE, FENCE, LOCKER, PLANT, WINDOW, BOARD, DOOR }

    public static class TerrainBlock {
        final int chunk; final float x, w, h; public float y;
        TerrainBlock(int chunk, float x, float w, float h) {
            this.chunk = chunk; this.x = x; this.w = w; this.h = h;
        }
        public float surfaceY() { return GROUND_Y - h; }
    }

    private static class Building {
        final int chunk; final float x, y, w, h; final int imageIndex;
        Building(int chunk, float x, float y, float w, float h, int imageIndex) {
            this.chunk = chunk; this.x = x; this.y = y; this.w = w; this.h = h; this.imageIndex = imageIndex;
        }
    }

    private static class Prop {
        final int chunk; final PropType type; final float x, y, w, h; final int imageIndex;
        Prop(int c, PropType t, float x, float y, float w, float h, int i) {
            chunk = c; type = t; this.x = x; this.y = y; this.w = w; this.h = h; imageIndex = i;
        }
    }

    private static class SchoolNpc {
        final int chunk; final float x, y; final int imageIndex, phase; final boolean walking;
        SchoolNpc(int c, float x, float y, int img, boolean walk, int phase) {
            chunk = c; this.x = x; this.y = y; imageIndex = img; walking = walk; this.phase = phase;
        }
    }

    /** A fightable NPC with a typed sprite and HP. */
    public static class FightNpc {
        final int chunk; final float x, y; final int maxHealth; final EnemyType type;
        boolean defeated;
        FightNpc(int chunk, float x, float y, boolean defeated, int maxHealth, EnemyType type) {
            this.chunk = chunk; this.x = x; this.y = y;
            this.defeated = defeated; this.maxHealth = maxHealth; this.type = type;
        }
        public int       getMaxHealth() { return maxHealth; }
        public EnemyType getType()      { return type; }
    }

    public static class StoryNpc {
        final int chunk;
        final float x, y;
        final String spriteId;
        final String name;
        final int progressionStep;
        boolean defeated;

        StoryNpc(int chunk, float x, float y, String spriteId, String name, int progressionStep, boolean defeated) {
            this.chunk = chunk;
            this.x = x;
            this.y = y;
            this.spriteId = spriteId;
            this.name = name;
            this.progressionStep = progressionStep;
            this.defeated = defeated;
        }

        public String getSpriteId() { return spriteId; }
        public String getName() { return name; }
        public int getProgressionStep() { return progressionStep; }
    }
}
