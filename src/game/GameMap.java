package game;

import processing.core.PApplet;
import processing.core.PImage;

import java.io.File;
import java.util.*;

/*
 * Terrain generation follows 3 steps:
 *
 * Step 1 — Elevated terrain blocks (blue) connected to ground. No floating.
 *           Player can walk and jump onto them.
 *
 * Step 2 — Buildings sit on top of terrain (ground or elevated block).
 *           Buildings are purely visual — only terrain blocks affect movement.
 *           Building width must fit within the block beneath it.
 *
 * Step 3 — Trees (never behind buildings, can go on platforms) and
 *           fences (can go in gaps, including behind buildings) are placed last.
 */

public class GameMap {
    // ── World ────────────────────────────────────────────────────────────────
    private static final int   GROUND_Y      = 500;   // y of the flat ground surface
    private static final int   CHUNK_W       = 900;
    private static final int   NPCS_PER_CHUNK = 3;
    private static final String SAVE_FILE    = "data/generated_map.csv";
    private static final String MAP_VERSION  = "8";
    private static final int   SCHOOL_START_CHUNK = 3;

    // ── Elevated terrain blocks ───────────────────────────────────────────────
    private static final int   TERR_MIN_W    = 155;   // min width of a raised block
    private static final int   TERR_MAX_W    = 315;
    private static final int   TERR_MIN_H    = 55;    // how much it rises above ground
    private static final int   TERR_MAX_H    = 190;
    private static final float TERR_CHANCE   = 0.90f; // probability per chunk

    // ── Buildings ────────────────────────────────────────────────────────────
    private static final int   BLDG_MIN_W    = 85;
    private static final int   BLDG_MAX_W    = 260;
    private static final int   BLDG_MIN_H    = 180;
    private static final int   BLDG_MAX_H    = 320;
    private static final int   BLDG_GAP_MIN  = 24;    // min gap between buildings
    private static final int   BLDG_GAP_MAX  = 62;
    private static final int   BLDG_CLEARANCE = 12;

    // ── Trees ────────────────────────────────────────────────────────────────
    private static final int   TREE_MIN_H    = 140;
    private static final int   TREE_MAX_H    = 300;

    // ── Fences ───────────────────────────────────────────────────────────────
    private static final int   FENCE_H       = 42;
    private static final int   FENCE_SEG_W   = 80;    // width of one fence tile
    private static final int   IMAGE_GAP_MIN = 56;

    private static final int   SCHOOL_CEILING_Y = 0;
    private static final int   SCHOOL_FLOOR_Y = GROUND_Y;
    private static final int   SCHOOL_LOCKER_H = 190;
    private static final int   SCHOOL_PLANT_H = 125;
    private static final int   SCHOOL_DOOR_H = 195;
    private static final int   SCHOOL_WINDOW_H = 155;
    private static final int   SCHOOL_NPC_H = 160;
    private static final int   WALKING_NPC_TRAVEL = 460;
    private static final int   SCHOOL_WINDOW_GAP = 300;
    private static final float SCHOOL_ENTRY_FADE_DISTANCE = 120f;
    private static final float BACKGROUND_PARALLAX = 0.42f;
    private static final int   STAIR_STEP_W = 105;
    private static final int   MAP_SAVE_DEBOUNCE_MS = 900;

    // ── Enemy types ──────────────────────────────────────────────────────────
    public enum EnemyType { GEEK, ACE, JOCK }

    // ── State ────────────────────────────────────────────────────────────────
    private final PApplet      app;
    private final AssetManager assets;

    private final List<TerrainBlock> terrain   = new ArrayList<>();
    private final List<Building>     buildings = new ArrayList<>();
    private final List<Prop>         props     = new ArrayList<>();
    private final List<Npc>          npcs      = new ArrayList<>();
    private final List<SchoolNpc>    schoolNpcs = new ArrayList<>();
    private final List<StoryNpc>     storyNpcs = new ArrayList<>();
    private final Set<Integer>       generatedChunks = new HashSet<>();
    private final Map<Integer, BackgroundChunk> backgroundChunks = new HashMap<>();
    private long worldSeed = System.currentTimeMillis();
    private boolean mapSavePending;
    private int lastMapSaveMillis;
    private int storyProgression;

    // ── Visibility cache (perf) ───────────────────────────────────────────────
    private float   cachedCameraX = Float.MIN_VALUE;
    private int     cachedScreenW = -1;
    private final List<TerrainBlock> visibleTerrain   = new ArrayList<>();
    private final List<Building>     visibleBuildings = new ArrayList<>();
    private final List<Prop>         visibleProps     = new ArrayList<>();
    private final List<SchoolNpc>    visibleSchoolNpcs = new ArrayList<>();

    public GameMap(PApplet app, AssetManager assets) {
        this.app    = app;
        this.assets = assets;
        loadGeneratedMap();
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public void render(float cameraX, int screenW) {
        renderBehindPlayer(cameraX, screenW, null);
        renderInFrontOfPlayer(cameraX, screenW, null);
    }

    public void renderBehindPlayer(float cameraX, int screenW, Player player) {
        int first = Math.max(0, PApplet.floor(cameraX / CHUNK_W) - 1);
        int last  = PApplet.floor((cameraX + screenW) / CHUNK_W) + 1;

        for (int c = first; c <= last; c++) {
            generateChunkIfNeeded(c);
            ensureNpcsForChunk(c);
            ensureSchoolNpcsForChunk(c);
        }
        ensureStoryNpcForProgress();
        refreshVisibilityCache(cameraX, screenW);

        // Layer 1 — sky, parallax, clouds
        drawSky();
        drawParallaxBackground(cameraX, screenW);
        drawSchoolBackground(cameraX, screenW);

        // Layer 2 — platforms and buildings ON platforms
        drawTerrain(cameraX, screenW);
        drawBuildingsForLayer(cameraX, screenW, false); // platform buildings

        // Player slots in here when behind ground buildings (between layer 2 and 3)

        // Layer 3 — ground buildings (in front of platforms and layer 2)
        if (!shouldDrawPlayerBehindGroundBuildings(player)) {
            drawBuildingsForLayer(cameraX, screenW, true);
        }
        drawSchoolProps(cameraX, screenW);
        drawMathTestMarker(cameraX, screenW);
        drawSchoolNpcs(cameraX, screenW);

        // Layer 4 — trees and fences, always in front of everything above
        drawGround(cameraX, screenW);
        drawTrees(cameraX, screenW);
        drawFences(cameraX, screenW);
        drawNpcs(cameraX, screenW, player);
        drawStoryNpcs(cameraX, screenW, player);
        drawSchoolExteriorCover(cameraX, screenW, player);
        flushPendingMapSave();
    }

    public void renderInFrontOfPlayer(float cameraX, int screenW, Player player) {
        if (shouldDrawPlayerBehindGroundBuildings(player)) {
            // Layer 3 drawn after player — ground buildings, trees, fences all in front
            drawBuildingsForLayer(cameraX, screenW, true);
            drawSchoolProps(cameraX, screenW);
            drawMathTestMarker(cameraX, screenW);
            drawSchoolNpcs(cameraX, screenW);
            drawGround(cameraX, screenW);
            drawTrees(cameraX, screenW);
            drawFences(cameraX, screenW);
            drawNpcs(cameraX, screenW, player);
            drawStoryNpcs(cameraX, screenW, player);
            drawSchoolExteriorCover(cameraX, screenW, player);
        }
    }

    public boolean isSchoolX(float x) {
        return x >= SCHOOL_START_CHUNK * CHUNK_W;
    }

    public float schoolEntryProgress(Player player) {
        if (player == null) return 0f;
        float schoolStartX = SCHOOL_START_CHUNK * CHUNK_W;
        float playerCenterX = player.getX() + player.getWidth() / 2f;
        return PApplet.constrain((playerCenterX - schoolStartX) / SCHOOL_ENTRY_FADE_DISTANCE, 0f, 1f);
    }

    private boolean shouldDrawPlayerBehindGroundBuildings(Player player) {
        if (player == null) return false;
        return player.isOnPlatformLayer();
    }

    /** Returns the terrain block the player is landing on, or null. */
    public TerrainBlock getPlatformAt(Player player) {
        return getPlatformAt(player.getX(), player.getY(), player.getWidth(), player.getHeight(), player.getPreviousY());
    }

    public TerrainBlock getPlatformAt(float px, float py, float pw, float ph) {
        return getPlatformAt(px, py, pw, ph, py);
    }

    private TerrainBlock getPlatformAt(float px, float py, float pw, float ph, float previousY) {
        for (TerrainBlock t : terrain) {
            boolean xOverlap = px + pw > t.x && px < t.x + t.w;
            boolean landing  = py + ph >= t.surfaceY() && py + ph <= t.surfaceY() + 14;
            if (xOverlap && landing && canLandOnPlatformSpan(px, pw, ph, previousY, t)) return t;
        }
        return null;
    }

    private boolean canLandOnPlatformSpan(float px, float pw, float ph, float previousY, TerrainBlock platform) {
        if (platform.h <= 1) {
            return true;
        }

        float previousFootY = previousY + ph;
        boolean wasAlreadyOnPlatform = Math.abs(previousFootY - platform.surfaceY()) <= 18;
        if (wasAlreadyOnPlatform) {
            return true;
        }

        return !isPlatformSpanCoveredByBuilding(px, pw, platform);
    }

    private boolean isPlatformSpanCoveredByBuilding(float px, float pw, TerrainBlock platform) {
        for (Building b : buildings) {
            boolean sameSurface = Math.abs((b.y + b.h) - platform.surfaceY()) < 2;
            boolean xOverlap = px + pw > b.x && px < b.x + b.w;
            if (sameSurface && xOverlap) {
                return true;
            }
        }
        return false;
    }

    public void blockBuildingCoveredJumps(Player player) {
        // Buildings are visual facades. Jumping in front of them is allowed;
        // covered platform spans are blocked only by getPlatformAt().
    }

    public Npc findNearbyNpc(Player player) {
        float cx = player.getX() + player.getWidth() / 2f;
        float fy = player.getY() + player.getHeight();
        for (Npc npc : npcs) {
            if (!npc.defeated
                    && npc.interactable
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

    public DoorEnemy findNearbyDoorEnemy(Player player) {
        float cx = player.getX() + player.getWidth() / 2f;
        float fy = player.getY() + player.getHeight();
        for (Prop prop : props) {
            if (prop.type != PropType.DOOR || !prop.enemyDoor || prop.defeated) {
                continue;
            }
            float doorCenter = prop.x + prop.w / 2f;
            if (Math.abs(cx - doorCenter) < 90 && Math.abs(fy - (prop.y + prop.h)) < 100) {
                return new DoorEnemy(prop);
            }
        }
        return null;
    }

    public boolean isNearMathTestDoor(Player player) {
        if (storyProgression != 1) {
            return false;
        }

        // Math test triggers at a fixed location just inside the school entrance
        float mathTestX = SCHOOL_START_CHUNK * CHUNK_W + 200f;
        float cx = player.getX() + player.getWidth() / 2f;
        float fy = player.getY() + player.getHeight();

        // Within 120px horizontally and standing on the ground
        return Math.abs(cx - mathTestX) < 120 && Math.abs(fy - GROUND_Y) < 30;
    }

    public void drawInteractionPrompt(Player player) {
        StoryNpc storyNpc = findNearbyStoryNpc(player);
        boolean mathTestDoor = isNearMathTestDoor(player);
        DoorEnemy doorEnemy = findNearbyDoorEnemy(player);
        Npc fightNpc = findNearbyNpc(player);
        if (storyNpc == null && !mathTestDoor && doorEnemy == null && fightNpc == null) return;
        app.fill(255, 245, 170);
        app.stroke(80);
        app.rect(320, 430, 320, 46, 6);
        app.fill(20);
        app.textAlign(PApplet.CENTER, PApplet.CENTER);
        app.textSize(16);
        String prompt = "E to fight";
        if (storyNpc != null) prompt = "E to talk";
        if (doorEnemy != null) prompt = "E to fight";
        if (mathTestDoor) prompt = "E to take math test";
        app.text(prompt, 480, 453);
    }

    public void markDefeated(Npc npc) {
        if (npc == null) return;
        npc.defeated = true;
        saveGeneratedMap();
    }

    public void markDefeated(StoryNpc npc) {
        if (npc == null) return;
        npc.defeated = true;
    }

    public void markDefeated(DoorEnemy doorEnemy) {
        if (doorEnemy == null) return;
        doorEnemy.prop.defeated = true;
        saveGeneratedMap();
    }

    public void setStoryProgression(int storyProgression) {
        if (this.storyProgression == storyProgression && !storyNpcs.isEmpty()) {
            return;
        }

        this.storyProgression = storyProgression;
        storyNpcs.clear();
        ensureStoryNpcForProgress();
    }

    // ── Step 1: Terrain block generation ─────────────────────────────────────

    private void generateChunkIfNeeded(int chunk) {
        if (generatedChunks.contains(chunk)) return;
        generatedChunks.add(chunk);

        Random rng    = new Random(worldSeed + chunk * 99173L);
        float  startX = chunk * CHUNK_W;

        if (chunk >= SCHOOL_START_CHUNK) {
            generateSchoolChunk(chunk, rng, startX);
            requestMapSave();
            cachedCameraX = Float.MIN_VALUE;
            return;
        }

        // ── Step 1: place 0-2 elevated terrain blocks ─────────────────────
        List<TerrainBlock> chunkTerrain = new ArrayList<>();

        if (rng.nextFloat() < TERR_CHANCE) {
            int count = 1 + rng.nextInt(2); // 1 or 2 blocks per chunk
            float cursor = startX + 80 + rng.nextInt(120);

            for (int i = 0; i < count; i++) {
                float tw = TERR_MIN_W + rng.nextInt(TERR_MAX_W - TERR_MIN_W);
                float th = TERR_MIN_H + rng.nextInt(TERR_MAX_H - TERR_MIN_H);
                float tx = cursor;

                // Don't let block run past chunk boundary
                if (tx + tw > startX + CHUNK_W - 40) break;

                TerrainBlock block = new TerrainBlock(chunk, tx, tw, th);
                addStairsForBlock(chunk, block, chunkTerrain, rng);
                chunkTerrain.add(block);
                terrain.add(block);

                // gap before next block
                cursor = tx + tw + 120 + rng.nextInt(150);
            }
        }

        // ── Step 2: place buildings on terrain surfaces and ground ─────────
        // Build a list of "surfaces" = (startX, endX, surfaceY)
        // Ground surface spans the whole chunk
        List<float[]> surfaces = new ArrayList<>();
        surfaces.add(new float[]{ startX, startX + CHUNK_W, GROUND_Y });

        for (TerrainBlock t : chunkTerrain) {
            if (t.w >= TERR_MIN_W) {
                surfaces.add(new float[]{ t.x, t.x + t.w, t.surfaceY() });
            }
        }

        for (float[] surf : surfaces) {
            float surfStart  = surf[0];
            float surfEnd    = surf[1];
            float surfY      = surf[2];
            float cursor     = surfStart + rng.nextInt(30);

            while (cursor < surfEnd - BLDG_MIN_W) {
                float maxW = Math.min(BLDG_MAX_W, surfEnd - cursor);
                if (maxW < BLDG_MIN_W) break;

                int   imgIdx = rng.nextInt(assets.buildings.length);
                PImage img = safeGetImage(imgIdx);
                float ratio = imageRatio(img, 0.75f);
                float bh = BLDG_MIN_H + rng.nextInt(BLDG_MAX_H - BLDG_MIN_H);
                float bw = bh * ratio;
                if (bw > maxW) {
                    bw = maxW;
                    bh = bw / ratio;
                }
                if (bw < BLDG_MIN_W || bh < BLDG_MIN_H * 0.8f) {
                    break;
                }

                if (!buildingOverlaps(chunk, cursor, cursor + bw, surfY)) {
                    buildings.add(new Building(chunk, cursor, surfY - bh, bw, bh, imgIdx));
                }

                float gap = BLDG_GAP_MIN + rng.nextInt(BLDG_GAP_MAX - BLDG_GAP_MIN);
                cursor += bw + gap;

                // Sometimes leave a small break, but keep foreground chunks dense.
                if (rng.nextFloat() < 0.10f) break;
            }
        }

        // ── Step 3: trees and fences ──────────────────────────────────────
        // Collect building footprints for this chunk to avoid tree overlap
        List<float[]> bldgFootprints = new ArrayList<>();
        for (Building b : buildings) {
            if (b.chunk == chunk) bldgFootprints.add(new float[]{ b.x, b.x + b.w });
        }

        // Fences still avoid buildings
        List<float[]> fenceFootprints = new ArrayList<>();
        placeFences(chunk, rng, startX, fenceFootprints);

        // Trees only avoid each other and fences — NOT buildings (trees render in front)
        List<float[]> treeFootprints = new ArrayList<>(fenceFootprints);
        placeTrees(chunk, rng, startX, chunkTerrain, treeFootprints);

        requestMapSave();
        cachedCameraX = Float.MIN_VALUE; // invalidate visibility cache
    }

    private void addStairsForBlock(int chunk, TerrainBlock block, List<TerrainBlock> chunkTerrain, Random rng) {
        int steps = Math.max(2, (int) Math.ceil(block.h / 55f));
        float stepW = STAIR_STEP_W;
        boolean fromRight = rng.nextBoolean();

        for (int i = 1; i <= steps; i++) {
            float stepH = block.h * i / (steps + 1f);
            float stepX = fromRight
                    ? block.x + block.w + (steps - i) * stepW
                    : block.x - (steps - i + 1) * stepW;

            if (stepX < chunk * CHUNK_W + 20 || stepX + stepW > (chunk + 1) * CHUNK_W - 20) {
                continue;
            }

            TerrainBlock step = new TerrainBlock(chunk, stepX, stepW, stepH);
            chunkTerrain.add(step);
            terrain.add(step);
        }
    }

    private boolean buildingOverlaps(int chunk, float x1, float x2, float surfaceY) {
        for (Building building : buildings) {
            if (building.chunk != chunk) {
                continue;
            }

            float buildingBase = building.y + building.h;
            boolean sameSurface = Math.abs(buildingBase - surfaceY) < 2;
            boolean overlaps = x2 > building.x - BLDG_CLEARANCE && x1 < building.x + building.w + BLDG_CLEARANCE;

            if (sameSurface && overlaps) {
                return true;
            }
        }

        return false;
    }

    private void placeFences(int chunk, Random rng, float startX, List<float[]> imageFootprints) {
        // Try placing 1-2 fence segments in ground-level gaps
        int attempts = 3 + rng.nextInt(3);
        for (int i = 0; i < attempts; i++) {
            float fx    = startX + 40 + rng.nextInt((int)(CHUNK_W - 200));
            float fLen  = 70 + rng.nextInt(150);
            float fy    = GROUND_Y - FENCE_H;
            // Fences can go anywhere on ground — they're short enough not to matter visually
            if (!overlapsAnyWithGap(fx, fx + fLen, imageFootprints, 28)) {
                props.add(new Prop(chunk, PropType.FENCE, fx, fy, fLen, FENCE_H));
                imageFootprints.add(new float[]{fx, fx + fLen});
            }
        }
    }

    private void placeTrees(int chunk, Random rng, float startX,
                            List<TerrainBlock> chunkTerrain, List<float[]> imageFootprints) {
        int groundTrees = 6 + rng.nextInt(3); // bump attempts to 6–8
        int placed = 0;

        for (int i = 0; i < groundTrees; i++) {
            float th = TREE_MIN_H + rng.nextInt(TREE_MAX_H - TREE_MIN_H);
            float tw = th * 0.65f;

            // Try random position first, then fall back to a sweep
            float tx = -1;
            for (int attempt = 0; attempt < 8; attempt++) {
                float candidate = startX + 30 + rng.nextInt((int)(CHUNK_W - tw - 60));
                if (!overlapsAnyWithGap(candidate, candidate + tw, imageFootprints, 18)) { // was 36
                    tx = candidate;
                    break;
                }
            }

            if (tx < 0) continue; // no room found

            props.add(new Prop(chunk, PropType.TREE, tx, GROUND_Y - th, tw, th));
            imageFootprints.add(new float[]{tx, tx + tw});
            placed++;
        }

        // Guarantee at least 2 trees per chunk by forcing placement in sparse zones
        if (placed < 2) {
            for (float zoneStart = startX; zoneStart < startX + CHUNK_W - 100 && placed < 2; zoneStart += CHUNK_W / 3f) {
                float th = TREE_MIN_H + rng.nextInt(TREE_MAX_H - TREE_MIN_H);
                float tw = th * 0.65f;
                float tx = zoneStart + 20 + rng.nextInt((int)(CHUNK_W / 3f - tw - 40));
                if (!overlapsAnyWithGap(tx, tx + tw, imageFootprints, 10)) {
                    props.add(new Prop(chunk, PropType.TREE, tx, GROUND_Y - th, tw, th));
                    imageFootprints.add(new float[]{tx, tx + tw});
                    placed++;
                }
            }
        }

        // Trees on terrain blocks (unchanged)
        for (TerrainBlock t : chunkTerrain) {
            if (rng.nextFloat() < 0.9f) {
                float th = TREE_MIN_H + rng.nextInt(TREE_MAX_H - TREE_MIN_H);
                float tw = Math.min(th * 0.65f, t.w - 20);
                if (tw < 30) continue;
                float tx = t.x + 10 + rng.nextInt(Math.max(1, (int)(t.w - tw - 20)));
                if (!overlapsAnyWithGap(tx, tx + tw, imageFootprints, 36)) {
                    props.add(new Prop(chunk, PropType.TREE, tx, t.surfaceY() - th, tw, th));
                    imageFootprints.add(new float[]{tx, tx + tw});
                }
            }
        }
    }

    /** Returns true if [x1,x2] overlaps any of the footprint ranges. */
    private boolean overlapsAny(float x1, float x2, List<float[]> footprints) {
        for (float[] fp : footprints) {
            if (x2 > fp[0] + 10 && x1 < fp[1] - 10) return true;
        }
        return false;
    }

    private boolean overlapsAnyWithGap(float x1, float x2, List<float[]> footprints, float gap) {
        for (float[] fp : footprints) {
            if (x2 > fp[0] - gap && x1 < fp[1] + gap) return true;
        }
        return false;
    }

    private void generateSchoolChunk(int chunk, Random rng, float startX) {
        List<float[]> occupied = new ArrayList<>();
        List<float[]> windowsOnly = new ArrayList<>();

        int windows = 2 + rng.nextInt(2);
        for (int i = 0; i < windows; i++) {
            int imageIndex = assets.windows.length == 0 ? 0 : rng.nextInt(assets.windows.length);
            PImage img = safeImage(assets.windows, imageIndex);
            float h = SCHOOL_WINDOW_H;
            float w = h * imageRatio(img, 0.75f);
            float x = startX + 70 + i * (w + SCHOOL_WINDOW_GAP) + rng.nextInt(45);
            float y = SCHOOL_CEILING_Y + 32;
            if (addSchoolPropIfOpen(chunk, occupied, PropType.WINDOW, x, y, w, h, imageIndex, SCHOOL_WINDOW_GAP)) {
                windowsOnly.add(new float[]{x, y, x + w, y + h});
            } else {
                for (int attempt = 0; attempt < 5; attempt++) {
                    x = startX + 60 + rng.nextInt((int) (CHUNK_W - w - 120));
                    if (!rectOverlapsAny(x, y, w, h, windowsOnly, SCHOOL_WINDOW_GAP)
                            && addSchoolPropIfOpen(chunk, occupied, PropType.WINDOW, x, y, w, h, imageIndex, IMAGE_GAP_MIN)) {
                        windowsOnly.add(new float[]{x, y, x + w, y + h});
                        break;
                    }
                }
            }
        }

        // First pass — main props along the floor
        float cursor = startX + 55 + rng.nextInt(50);
        while (cursor < startX + CHUNK_W - 80) {
            float choice = rng.nextFloat();
            PropType type;
            float h;
            int imageIndex = 0;
            PImage img = null;

            if (choice < 0.45f) {
                type = PropType.LOCKER;
                imageIndex = rng.nextInt(Math.max(1, assets.lockers.length));
                img = safeImage(assets.lockers, imageIndex);
                h = SCHOOL_LOCKER_H;
            } else if (choice < 0.68f) {
                type = PropType.PLANT;
                img = assets.plant;
                h = SCHOOL_PLANT_H;
            } else if (choice < 0.75f) {
                type = PropType.DOOR;
                h = SCHOOL_DOOR_H;
            } else {
                type = PropType.BOARD;
                img = assets.board;
                h = 120 + rng.nextInt(67);
            }

            float w = type == PropType.DOOR ? 100 : h * imageRatio(img, type == PropType.BOARD ? 1.35f : 0.75f);
            float y = (type == PropType.BOARD)
                    ? SCHOOL_FLOOR_Y - h - 170
                    : SCHOOL_FLOOR_Y - h;

            if (addSchoolPropIfOpen(chunk, occupied, type, cursor, y, w, h, imageIndex, IMAGE_GAP_MIN)) {
                cursor += w + 55 + rng.nextInt(80);
            } else {
                cursor += 65;
            }
        }

        // Second pass — fill remaining gaps
        float fillCursor = startX + 40;
        while (fillCursor < startX + CHUNK_W - 60) {
            if (rectOverlapsAny(fillCursor, SCHOOL_FLOOR_Y - SCHOOL_LOCKER_H, 60, SCHOOL_LOCKER_H, occupied, IMAGE_GAP_MIN)) {
                fillCursor += 20;
                continue;
            }

            float fillChoice = rng.nextFloat();
            PropType fillType;
            float fillH;
            int fillImageIndex = 0;
            PImage fillImg = null;

            if (fillChoice < 0.35f) {
                fillType = PropType.LOCKER;
                fillImageIndex = rng.nextInt(Math.max(1, assets.lockers.length));
                fillImg = safeImage(assets.lockers, fillImageIndex);
                fillH = SCHOOL_LOCKER_H;
            } else if (fillChoice < 0.60f) {
                fillType = PropType.PLANT;
                fillImg = assets.plant;
                fillH = SCHOOL_PLANT_H;
            } else if (fillChoice < 0.80f) {
                fillType = PropType.DOOR;
                fillH = SCHOOL_DOOR_H;
            } else {
                fillType = PropType.BOARD;
                fillImg = assets.board;
                fillH = 120 + rng.nextInt(67);
            }

            float fillW = fillType == PropType.DOOR ? 100 : fillH * imageRatio(fillImg, fillType == PropType.BOARD ? 1.35f : 0.75f);
            float fillY = fillType == PropType.BOARD
                    ? SCHOOL_FLOOR_Y - fillH - 170
                    : SCHOOL_FLOOR_Y - fillH;

            if (addSchoolPropIfOpen(chunk, occupied, fillType, fillCursor, fillY, fillW, fillH, fillImageIndex, IMAGE_GAP_MIN)) {
                fillCursor += fillW + 30 + rng.nextInt(40);
            } else {
                fillCursor += 25;
            }
        }
    }

    private boolean addSchoolPropIfOpen(int chunk, List<float[]> occupied, PropType type,
                                        float x, float y, float w, float h, int imageIndex, float gap) {
        if (x < chunk * CHUNK_W + 20 || x + w > (chunk + 1) * CHUNK_W - 20) {
            return false;
        }
        if (rectOverlapsAny(x, y, w, h, occupied, gap)) {
            return false;
        }
        Prop prop = new Prop(chunk, type, x, y, w, h, imageIndex);
        if (type == PropType.DOOR) {
            int hash = Math.abs((int) (worldSeed + chunk * 43L + x * 11f));
            prop.enemyDoor = hash % 100 < 36;
            prop.maxHealth = 95 + hash % 61;
            prop.enemyType = EnemyType.values()[hash % EnemyType.values().length];
            prop.spriteIndex = hash % Math.max(1, assets.idleNpcs.length);
        }
        props.add(prop);
        occupied.add(new float[]{x, y, x + w, y + h});
        return true;
    }

    private boolean rectOverlapsAny(float x, float y, float w, float h, List<float[]> rects, float gap) {
        float x2 = x + w;
        float y2 = y + h;
        for (float[] r : rects) {
            if (x2 > r[0] - gap && x < r[2] + gap && y2 > r[1] - gap && y < r[3] + gap) {
                return true;
            }
        }
        return false;
    }

    // ── NPC spawning ─────────────────────────────────────────────────────────

    private void ensureNpcsForChunk(int chunk) {
        if (NPCS_PER_CHUNK <= 0 || chunk < SCHOOL_START_CHUNK) {
            return;
        }

        int count = 0;
        for (Npc n : npcs) if (n.chunk == chunk) count++;

        Random rng = new Random(worldSeed + chunk * 55147L);
        EnemyType[] types = EnemyType.values();

        while (count < NPCS_PER_CHUNK) {
            float nx   = chunk * CHUNK_W + 140 + count * 210 + rng.nextInt(80);
            int   hp   = 70 + rng.nextInt(81);
            EnemyType t = types[rng.nextInt(types.length)];
            int spriteIndex = rng.nextInt(Math.max(1, assets.idleNpcs.length));
            boolean interactable = rng.nextFloat() < 0.62f;
            npcs.add(new Npc(chunk, nx, GROUND_Y, false, hp, t, spriteIndex, interactable));
            count++;
        }

        requestMapSave();
    }

    private void ensureStoryNpcForProgress() {
        if (!storyNpcs.isEmpty()) {
            return;
        }

        StoryNpc npc = storyNpcForProgression(storyProgression);
        if (npc != null) {
            storyNpcs.add(npc);
        }
    }

    private StoryNpc storyNpcForProgression(int step) {
        switch (step) {
            case 0:
                return new StoryNpc("dexter", "Dexter", 0, 620, GROUND_Y, false);
            case 2:
                return new StoryNpc("stacey", "Stacey", 2, 5400, GROUND_Y, false);
            case 3:
                return new StoryNpc("rico", "Rico", 3, 8100, GROUND_Y, false);
            case 4:
                return new StoryNpc("msPatel", "Ms. Patel", 4, 10800, GROUND_Y, false);
            case 5:
                return new StoryNpc("jamie", "Jamie", 5, 13500, GROUND_Y, false);
            case 6:
                return new StoryNpc("dexter", "Dexter", 6, 16200, GROUND_Y, false);
            case 7:
                return new StoryNpc("stacey", "Stacey", 7, 18900, GROUND_Y, false);
            case 8:
                return new StoryNpc("announcement", "Announcement", 8, 21600, GROUND_Y, false);
            case 9:
                return new StoryNpc("val", "The Val", 9, 26100, GROUND_Y, false);
            default:
                return null;
        }
    }

    private void ensureSchoolNpcsForChunk(int chunk) {
        if (chunk < SCHOOL_START_CHUNK) {
            return;
        }

        int count = 0;
        for (SchoolNpc npc : schoolNpcs) {
            if (npc.chunk == chunk) count++;
        }
        if (count > 0) {
            return;
        }

        Random rng = new Random(worldSeed + chunk * 34871L);
        List<float[]> occupied = new ArrayList<>();
        int staticCount = 2 + rng.nextInt(3);
        for (int i = 0; i < staticCount; i++) {
            float npcW = schoolNpcWidth(safeImage(assets.idleNpcs, 0));
            float x = findOpenSchoolNpcX(chunk, rng, occupied, npcW, false);
            int imageIndex = rng.nextInt(Math.max(1, assets.idleNpcs.length));
            npcW = schoolNpcWidth(safeImage(assets.idleNpcs, imageIndex));
            x = findOpenSchoolNpcX(chunk, rng, occupied, npcW, false);
            occupied.add(new float[]{x, x + npcW});
            schoolNpcs.add(new SchoolNpc(chunk, x, SCHOOL_FLOOR_Y, imageIndex, false, rng.nextInt(WALKING_NPC_TRAVEL)));
        }

        float walkW = schoolNpcWidth(assets.walkingNpc);
        float walkX = findOpenSchoolNpcX(chunk, rng, occupied, walkW + WALKING_NPC_TRAVEL, true);
        occupied.add(new float[]{walkX, walkX + walkW + WALKING_NPC_TRAVEL});
        schoolNpcs.add(new SchoolNpc(chunk, walkX, SCHOOL_FLOOR_Y, 0, true, rng.nextInt(WALKING_NPC_TRAVEL)));
    }

    private float findOpenSchoolNpcX(int chunk, Random rng, List<float[]> occupied, float width, boolean walking) {
        float minX = chunk * CHUNK_W + 85;
        float maxX = (chunk + 1) * CHUNK_W - width - 85;
        float gap = walking ? 30 : 45;

        for (int attempt = 0; attempt < 30; attempt++) {
            float x = minX + rng.nextFloat() * Math.max(1, maxX - minX);
            if (!overlapsAnyWithGap(x, x + width, occupied, gap)) {
                return x;
            }
        }

        float x = minX;
        while (x <= maxX) {
            if (!overlapsAnyWithGap(x, x + width, occupied, gap)) {
                return x;
            }
            x += width + gap;
        }

        return minX;
    }

    private float schoolNpcWidth(PImage img) {
        return SCHOOL_NPC_H * imageRatio(img, 0.75f);
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    private void refreshVisibilityCache(float cameraX, int screenW) {
        if (Math.abs(cameraX - cachedCameraX) < 4f && screenW == cachedScreenW &&
                visibleTerrain.size() + visibleBuildings.size() + visibleProps.size() > 0) {
            return; // cache still valid
        }
        cachedCameraX = cameraX;
        cachedScreenW = screenW;

        visibleTerrain.clear();
        for (TerrainBlock t : terrain) {
            if (isVisible(t.x, t.w, cameraX, screenW)) visibleTerrain.add(t);
        }
        visibleBuildings.clear();
        for (Building b : buildings) {
            if (isVisible(b.x, b.w, cameraX, screenW)) visibleBuildings.add(b);
        }
        visibleProps.clear();
        for (Prop p : props) {
            if (isVisible(p.x, p.w, cameraX, screenW)) visibleProps.add(p);
        }
        visibleSchoolNpcs.clear();
        for (SchoolNpc n : schoolNpcs) {
            float npcSpan = n.walking ? WALKING_NPC_TRAVEL + 80 : 80;
            if (isVisible(n.x, npcSpan, cameraX, screenW)) {
                visibleSchoolNpcs.add(n);
            }
        }
    }

    private void drawSky() {
        app.background(210, 230, 255);
    }

    private void drawParallaxBackground(float cameraX, int screenW) {
        float parallaxCamera = cameraX * BACKGROUND_PARALLAX;
        int first = Math.max(0, PApplet.floor(parallaxCamera / CHUNK_W) - 1);
        int last = PApplet.floor((parallaxCamera + screenW) / CHUNK_W) + 1;

        app.pushStyle();
        for (int chunk = first; chunk <= last; chunk++) {
            BackgroundChunk background = backgroundForChunk(chunk);

            for (BackgroundSprite sprite : background.clouds) {
                PImage img = safeImage(assets.clouds, sprite.imageIndex);
                float drawX = parallaxX(sprite.x, cameraX);
                if (!isVisible(drawX, sprite.w, cameraX, screenW)) continue;
                if (img != null) {
                    app.tint(255, 190);
                    app.image(img, drawX, sprite.y, sprite.w, sprite.h);
                    app.noTint();
                } else {
                    app.noStroke();
                    app.fill(255, 255, 255, 180);
                    app.ellipse(drawX + sprite.w * 0.35f, sprite.y + sprite.h * 0.55f, sprite.w * 0.7f, sprite.h * 0.75f);
                    app.ellipse(drawX + sprite.w * 0.65f, sprite.y + sprite.h * 0.5f, sprite.w * 0.75f, sprite.h * 0.65f);
                }
            }

            for (BackgroundSprite sprite : background.buildings) {
                PImage img = safeGetImage(sprite.imageIndex);
                float drawX = parallaxX(sprite.x, cameraX);
                if (!isVisible(drawX, sprite.w, cameraX, screenW)) continue;
                if (img != null) {
                    app.image(img, drawX, sprite.y, sprite.w, sprite.h);
                    app.noStroke();
                    app.fill(210, 230, 255, 105);
                    app.rect(drawX, sprite.y, sprite.w, sprite.h);
                } else {
                    app.noStroke();
                    app.fill(232, 238, 244);
                    app.rect(drawX, sprite.y, sprite.w, sprite.h, 3);
                }
            }
        }
        app.noTint();
        app.popStyle();
    }

    private float parallaxX(float sourceX, float cameraX) {
        return sourceX + cameraX * (1f - BACKGROUND_PARALLAX);
    }

    private BackgroundChunk backgroundForChunk(int chunk) {
        BackgroundChunk cached = backgroundChunks.get(chunk);
        if (cached != null) {
            return cached;
        }

        Random rng = new Random(worldSeed + 21000L + chunk * 61843L);
        float chunkX = chunk * CHUNK_W;
        BackgroundChunk background = new BackgroundChunk();

        int clouds = 2 + rng.nextInt(2);
        for (int i = 0; i < clouds; i++) {
            int imageIndex = rng.nextInt(Math.max(1, assets.clouds.length));
            PImage img = safeImage(assets.clouds, imageIndex);
            float w = 105 + rng.nextInt(36);
            float h = w / imageRatio(img, 2.2f);
            float x = chunkX + 40 + rng.nextInt((int) Math.max(1, CHUNK_W - w - 80));
            float y = 35 + rng.nextInt(115);
            background.clouds.add(new BackgroundSprite(x, y, w, h, imageIndex));
        }

        int distantBuildings = 2 + rng.nextInt(2);
        for (int i = 0; i < distantBuildings; i++) {
            int imageIndex = rng.nextInt(Math.max(1, assets.buildings.length));
            float w = 115 + rng.nextInt(51);
            float h = 185 + rng.nextInt(95);
            float x = chunkX + 60 + i * 300 + rng.nextInt(85);
            float y = GROUND_Y - h;
            background.buildings.add(new BackgroundSprite(x, y, w, h, imageIndex));
        }

        backgroundChunks.put(chunk, background);
        return background;
    }

    private void drawGround(float cameraX, int screenW) {
        float schoolStartX = SCHOOL_START_CHUNK * CHUNK_W;
        if (cameraX >= schoolStartX) {
            return;
        }

        float drawX = cameraX - 20;
        float drawW = Math.min(screenW + 40, schoolStartX - drawX);
        // Flat blue ground strip
        app.fill(100, 180, 255);
        app.noStroke();
        app.rect(drawX, GROUND_Y, drawW, 120);
        app.stroke(60, 140, 220);
        app.strokeWeight(2);
        app.line(drawX, GROUND_Y, drawX + drawW, GROUND_Y);
        app.strokeWeight(1);
    }

    private void drawTerrain(float cameraX, int screenW) {
        // Draw terrain blocks first (blue raised sections)
        for (TerrainBlock t : visibleTerrain) {
            app.fill(100, 180, 255);
            app.noStroke();
            app.rect(t.x, t.surfaceY(), t.w, t.h);
        }
    }

    private void drawBuildingsForLayer(float cameraX, int screenW, boolean groundLayer) {
        for (Building b : visibleBuildings) {

            boolean buildingOnGround = Math.abs((b.y + b.h) - GROUND_Y) < 2;
            if (buildingOnGround != groundLayer) continue;

            PImage img = safeGetImage(b.imageIndex);
            float baseY = b.y + b.h;
            float slotH = b.h;
            float slotW = b.w;
            float slotX = b.x + b.w / 2f - slotW / 2f;
            float slotY = baseY - slotH;

            if (img != null) {
                float drawW = slotW;
                float drawH = drawW / imageRatio(img, 1f);
                if (drawH > slotH) {
                    drawH = slotH;
                    drawW = drawH * imageRatio(img, 1f);
                }
                float drawX = b.x + b.w / 2f - drawW / 2f;
                app.image(img, drawX, baseY - drawH, drawW, drawH);
            } else {
                // Fallback drawn building
                app.fill(235, 230, 220);
                app.stroke(80);
                app.rect(slotX, slotY, slotW, slotH, 3);
                // Windows
                app.fill(200, 220, 240);
                app.noStroke();
                float winW = slotW * 0.25f;
                float winH = slotH * 0.18f;
                app.rect(slotX + slotW * 0.15f, slotY + slotH * 0.15f, winW, winH, 2);
                app.rect(slotX + slotW * 0.55f, slotY + slotH * 0.15f, winW, winH, 2);
                if (slotH > 120) {
                    app.rect(slotX + slotW * 0.15f, slotY + slotH * 0.45f, winW, winH, 2);
                    app.rect(slotX + slotW * 0.55f, slotY + slotH * 0.45f, winW, winH, 2);
                }
            }
        }
    }

    private void drawSchoolBackground(float cameraX, int screenW) {
        int first = Math.max(SCHOOL_START_CHUNK, PApplet.floor(cameraX / CHUNK_W) - 1);
        int last = PApplet.floor((cameraX + screenW) / CHUNK_W) + 1;

        for (int chunk = first; chunk <= last; chunk++) {
            float x = chunk * CHUNK_W;
            app.noStroke();
            app.fill(236, 232, 218);
            app.rect(x, SCHOOL_CEILING_Y, CHUNK_W, SCHOOL_FLOOR_Y - SCHOOL_CEILING_Y);
            app.fill(198, 190, 176);
            app.rect(x, SCHOOL_FLOOR_Y, CHUNK_W, 120);
            app.stroke(150, 140, 128);
            app.line(x, SCHOOL_CEILING_Y, x + CHUNK_W, SCHOOL_CEILING_Y);
            app.line(x, SCHOOL_FLOOR_Y, x + CHUNK_W, SCHOOL_FLOOR_Y);
            app.stroke(210, 205, 194);
            for (float tileX = x; tileX < x + CHUNK_W; tileX += 70) {
                app.line(tileX, SCHOOL_FLOOR_Y, tileX + 50, SCHOOL_FLOOR_Y + 120);
            }
            app.strokeWeight(1);
        }
    }

    private void drawSchoolExteriorCover(float cameraX, int screenW, Player player) {
        float progress = schoolEntryProgress(player);
        float alpha = 255f * (1f - progress);
        if (alpha <= 1f) {
            return;
        }

        int first = Math.max(SCHOOL_START_CHUNK, PApplet.floor(cameraX / CHUNK_W) - 1);
        int last = PApplet.floor((cameraX + screenW) / CHUNK_W) + 1;

        app.pushStyle();
        for (int chunk = first; chunk <= last; chunk++) {
            float x = chunk * CHUNK_W;

            app.noStroke();
            app.fill(230, 225, 207, alpha);
            app.rect(x, 0, CHUNK_W, SCHOOL_FLOOR_Y);

            app.fill(202, 196, 176, alpha);
            app.rect(x, SCHOOL_FLOOR_Y, CHUNK_W, 120);

            app.stroke(180, 170, 150, alpha);
            app.strokeWeight(2);
            app.line(x, SCHOOL_FLOOR_Y, x + CHUNK_W, SCHOOL_FLOOR_Y);

            drawExteriorWindow(x + 105, 48, alpha);
            drawExteriorWindow(x + 385, 48, alpha);
            drawExteriorWindow(x + 665, 48, alpha);
            drawExteriorWindow(x + 120, 263, alpha);
            drawExteriorWindow(x + 410, 263, alpha);

            app.noStroke();
            app.fill(115, 82, 54, alpha);
            app.rect(x + CHUNK_W - 150, SCHOOL_FLOOR_Y - 190, 95, 190, 3);
            app.fill(225, 190, 72, alpha);
            app.ellipse(x + CHUNK_W - 72, SCHOOL_FLOOR_Y - 95, 7, 7);
        }
        app.popStyle();
    }

    private void drawExteriorWindow(float x, float y, float alpha) {
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

    private void drawMathTestMarker(float cameraX, int screenW) {
        if (storyProgression != 1) return;

        float mathTestX = SCHOOL_START_CHUNK * CHUNK_W + 200f;
        if (!isVisible(mathTestX - 60, 120, cameraX, screenW)) return;

        float markerX = mathTestX;
        float markerY = GROUND_Y;

        // Draw a desk/paper icon on the floor
        app.noStroke();
        app.fill(255, 240, 160);
        app.rect(markerX - 28, markerY - 48, 56, 40, 4);
        app.fill(80);
        app.textAlign(PApplet.CENTER, PApplet.CENTER);
        app.textSize(11);
        app.text("MATH TEST", markerX, markerY - 28);

        // Pulsing arrow pointing down at the spot
        float pulse = 0.5f + 0.5f * PApplet.sin(app.frameCount * 0.12f);
        app.fill(255, 80 + (int)(120 * pulse), 80);
        app.noStroke();
        float arrowY = markerY - 60 - pulse * 8;
        app.triangle(markerX - 10, arrowY, markerX + 10, arrowY, markerX, arrowY + 14);
    }

    private void drawSchoolProps(float cameraX, int screenW) {
        for (Prop prop : visibleProps) {
            if (!isSchoolProp(prop.type)) continue;

            PImage img = imageForSchoolProp(prop);
            if (img != null) {
                drawImagePreserved(img, prop.x, prop.y, prop.w, prop.h);
            } else {
                drawFallbackSchoolProp(prop);
            }
        }
    }

    private void drawSchoolNpcs(float cameraX, int screenW) {
        for (SchoolNpc npc : visibleSchoolNpcs) {
            float x = npc.x;
            boolean facingRight = true;
            PImage img;

            if (npc.walking) {
                float t = ((app.millis() / 16f) + npc.phase) % (WALKING_NPC_TRAVEL * 2);
                if (t > WALKING_NPC_TRAVEL) {
                    x += WALKING_NPC_TRAVEL * 2 - t;
                    facingRight = false;
                } else {
                    x += t;
                }
                img = assets.walkingNpc;
            } else {
                img = safeImage(assets.idleNpcs, npc.imageIndex);
            }

            float h = SCHOOL_NPC_H;
            float w = h * imageRatio(img, 0.75f);
            if (!isVisible(x, w, cameraX, screenW)) continue;

            if (img != null) {
                if (facingRight) {
                    app.image(img, x, SCHOOL_FLOOR_Y - h, w, h);
                } else {
                    app.pushMatrix();
                    app.translate(x + w, SCHOOL_FLOOR_Y - h);
                    app.scale(-1, 1);
                    app.image(img, 0, 0, w, h);
                    app.popMatrix();
                }
            } else {
                app.fill(120, 80, 150);
                app.noStroke();
                app.rect(x, SCHOOL_FLOOR_Y - h, w, h, 4);
            }
        }
    }

    private void drawFences(float cameraX, int screenW) {
        for (Prop prop : visibleProps) {
            if (prop.type != PropType.FENCE) continue;

            PImage img = assets.fence;
            if (img != null) {
                drawCroppedToFit(img, prop.x, prop.y + prop.h - FENCE_H, prop.w, FENCE_H);
            } else {
                app.fill(180); app.stroke(80);
                app.rect(prop.x, prop.y, prop.w, prop.h);
            }
        }
    }

    private void drawTrees(float cameraX, int screenW) {
        for (Prop prop : visibleProps) {
            if (prop.type != PropType.TREE) continue;

            PImage img = assets.tree;
            if (img != null) {
                // Preserve aspect ratio, anchor to bottom
                float drawH = prop.h;
                float drawW = drawH * img.width / (float) img.height;
                float drawX = prop.x + prop.w / 2f - drawW / 2f;
                app.image(img, drawX, prop.y, drawW, drawH);
            } else {
                // Fallback tree
                app.fill(60, 120, 40); app.noStroke();
                app.ellipse(prop.x + prop.w / 2f, prop.y + prop.h * 0.38f, prop.w, prop.h * 0.65f);
                app.fill(80, 50, 20);
                app.rect(prop.x + prop.w * 0.42f, prop.y + prop.h * 0.6f, prop.w * 0.16f, prop.h * 0.4f);
            }
        }
    }

    private void drawNpcs(float cameraX, int screenW, Player player) {
        float entryProgress = schoolEntryProgress(player);
        for (Npc npc : npcs) {
            if (npc.defeated || !isVisible(npc.x - 45, 90, cameraX, screenW)) continue;
            // If the school cover is still more than half visible, skip NPCs inside school
            if (entryProgress < 0.5f && isSchoolX(npc.x)) continue;

            PImage img = assets.getNpcSprite(npc.spriteIndex);
            float h = 115;
            float w = h * imageRatio(img, 0.75f);
            if (img != null) {
                app.image(img, npc.x - w / 2f, npc.y - h, w, h);
            } else {
                if      (npc.type == EnemyType.GEEK) { app.fill(255, 230,  50); app.stroke(180, 140,   0); }
                else if (npc.type == EnemyType.ACE)  { app.fill( 80, 200, 100); app.stroke( 30, 120,  50); }
                else                                  { app.fill(255, 140,  40); app.stroke(160,  80,   0); }
                app.rect(npc.x - 25, npc.y - 58, 50, 58, 5);
            }

            if (npc.interactable) {
                app.fill(40);
                app.textAlign(PApplet.CENTER, PApplet.BOTTOM);
                app.textSize(13);
                app.text("E to fight", npc.x, npc.y - h - 8);
            }
        }
    }

    private void drawStoryNpcs(float cameraX, int screenW, Player player) {
        float entryProgress = schoolEntryProgress(player);
        for (StoryNpc npc : storyNpcs) {
            if (npc.defeated || !isVisible(npc.x - 55, 110, cameraX, screenW)) continue;
            if (entryProgress < 0.5f && isSchoolX(npc.x)) continue;

            PImage img = assets.getStorySprite(npc.spriteId);
            float h = 125;
            float w = h * imageRatio(img, 0.75f);
            if (img != null) {
                app.image(img, npc.x - w / 2f, npc.y - h, w, h);
            } else {
                app.fill(120, 90, 190);
                app.stroke(60);
                app.rect(npc.x - 30, npc.y - 70, 60, 70, 5);
            }

            app.fill(30);
            app.textAlign(PApplet.CENTER, PApplet.BOTTOM);
            app.textSize(14);
            app.text(npc.name, npc.x, npc.y - h - 24);
            app.textSize(12);
            app.text("E to talk", npc.x, npc.y - h - 8);
        }
    }

    private boolean isVisible(float x, float w, float cameraX, int screenW) {
        return x + w >= cameraX - 100 && x <= cameraX + screenW + 100;
    }

    private PImage safeGetImage(int idx) {
        if (assets.buildings == null || idx >= assets.buildings.length) return null;
        PImage img = assets.buildings[idx];
        return (img != null && img.width > 0) ? img : null;
    }

    private PImage safeImage(PImage[] images, int idx) {
        if (images == null || idx < 0 || idx >= images.length) return null;
        PImage img = images[idx];
        return (img != null && img.width > 0 && img.height > 0) ? img : null;
    }

    private float imageRatio(PImage img, float fallback) {
        if (img == null || img.height <= 0) return fallback;
        return img.width / (float) img.height;
    }

    private boolean isSchoolProp(PropType type) {
        return type == PropType.LOCKER
                || type == PropType.PLANT
                || type == PropType.WINDOW
                || type == PropType.BOARD
                || type == PropType.DOOR;
    }

    private PImage imageForSchoolProp(Prop prop) {
        if (prop.type == PropType.LOCKER) return safeImage(assets.lockers, prop.imageIndex);
        if (prop.type == PropType.PLANT) return assets.plant;
        if (prop.type == PropType.WINDOW) return safeImage(assets.windows, prop.imageIndex);
        if (prop.type == PropType.BOARD) return assets.board;
        return null;
    }

    private void drawImagePreserved(PImage img, float x, float y, float maxW, float maxH) {
        float ratio = imageRatio(img, 1f);
        float drawW = maxW;
        float drawH = drawW / ratio;
        if (drawH > maxH) {
            drawH = maxH;
            drawW = drawH * ratio;
        }
        app.image(img, x + maxW / 2f - drawW / 2f, y + maxH - drawH, drawW, drawH);
    }

    private void drawCroppedToFit(PImage img, float x, float y, float w, float h) {
        float targetRatio = w / h;
        float sourceRatio = imageRatio(img, targetRatio);
        float sx1 = 0;
        float sy1 = 0;
        float sx2 = img.width;
        float sy2 = img.height;

        if (sourceRatio > targetRatio) {
            float cropW = img.height * targetRatio;
            sx1 = (img.width - cropW) / 2f;
            sx2 = sx1 + cropW;
        } else if (sourceRatio < targetRatio) {
            float cropH = img.width / targetRatio;
            sy1 = (img.height - cropH) / 2f;
            sy2 = sy1 + cropH;
        }

        app.image(img, x, y, w, h, (int) sx1, (int) sy1, (int) sx2, (int) sy2);
    }

    private void drawFallbackSchoolProp(Prop prop) {
        if (prop.type == PropType.DOOR) {
            if (prop.defeated) {
                // Defeated enemy door — faded, open-looking
                app.fill(160, 130, 100);
                app.stroke(90, 70, 50);
            } else if (prop.enemyDoor) {
                // Enemy door — red-tinted warning
                app.fill(145, 70, 60);
                app.stroke(90, 35, 28);
            } else {
                // Normal door
                app.fill(120, 92, 66);
                app.stroke(70, 52, 38);
            }
            app.rect(prop.x, prop.y, prop.w, prop.h, 3);
            // Door knob
            app.fill(230, 198, 80);
            app.noStroke();
            app.ellipse(prop.x + prop.w * 0.82f, prop.y + prop.h * 0.52f, 7, 7);
            // Enemy door warning symbol
            if (prop.enemyDoor && !prop.defeated) {
                app.fill(255, 80, 60);
                app.textAlign(PApplet.CENTER, PApplet.CENTER);
                app.textSize(18);
                app.text("!", prop.x + prop.w * 0.38f, prop.y + prop.h * 0.38f);
            }
            return;
        }

        app.fill(190);
        app.stroke(90);
        app.rect(prop.x, prop.y, prop.w, prop.h, 3);
    }

    // ── Save / Load ──────────────────────────────────────────────────────────

    private void saveGeneratedMap() {
        mapSavePending = false;
        lastMapSaveMillis = app.millis();

        List<String> lines = new ArrayList<>();
        File file = new File(app.sketchPath(SAVE_FILE));
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        lines.add("V," + MAP_VERSION);
        lines.add("S," + worldSeed);
        for (int c : generatedChunks) lines.add("C," + c);
        for (TerrainBlock t : terrain) lines.add("T," + t.chunk+","+t.x+","+t.w+","+t.h);
        for (Building b : buildings)   lines.add("B," + b.chunk+","+b.x+","+b.y+","+b.w+","+b.h+","+b.imageIndex);
        for (Prop p : props)           lines.add("P," + p.chunk+","+p.type.name()+","+p.x+","+p.y+","+p.w+","+p.h+","+p.imageIndex+","+p.enemyDoor+","+p.defeated+","+p.maxHealth+","+p.enemyType.name()+","+p.spriteIndex);
        for (Npc n : npcs)             lines.add("N," + n.chunk+","+n.x+","+n.y+","+n.defeated+","+n.maxHealth+","+n.type.name()+","+n.spriteIndex+","+n.interactable);

        app.saveStrings(file.getAbsolutePath(), lines.toArray(new String[0]));
    }

    private void requestMapSave() {
        if (!mapSavePending) {
            lastMapSaveMillis = app.millis();
        }
        mapSavePending = true;
    }

    private void flushPendingMapSave() {
        if (!mapSavePending) {
            return;
        }

        if (app.millis() - lastMapSaveMillis >= MAP_SAVE_DEBOUNCE_MS) {
            saveGeneratedMap();
        }
    }

    private void loadGeneratedMap() {
        File file = new File(app.sketchPath(SAVE_FILE));
        if (!file.exists()) return;
        String[] lines = app.loadStrings(file.getAbsolutePath());
        if (lines == null) return;
        if (lines.length == 0 || !("V," + MAP_VERSION).equals(lines[0])) return;

        for (String line : lines) {
            String[] p = PApplet.split(line, ',');
            if (p.length == 0) continue;
            try {
                switch (p[0]) {
                    case "S":
                        worldSeed = Long.parseLong(p[1]);  // NEW — restore the seed
                        break;
                    case "C":
                        generatedChunks.add(PApplet.parseInt(p[1]));
                        break;
                    case "T":
                        terrain.add(new TerrainBlock(PApplet.parseInt(p[1]),
                                PApplet.parseFloat(p[2]), PApplet.parseFloat(p[3]), PApplet.parseFloat(p[4])));
                        break;
                    case "B":
                        buildings.add(new Building(PApplet.parseInt(p[1]),
                                PApplet.parseFloat(p[2]), PApplet.parseFloat(p[3]),
                                PApplet.parseFloat(p[4]), PApplet.parseFloat(p[5]),
                                p.length >= 7 ? PApplet.parseInt(p[6]) : 0));
                        break;
                    case "P":
                        Prop prop = new Prop(PApplet.parseInt(p[1]), PropType.valueOf(p[2]),
                                PApplet.parseFloat(p[3]), PApplet.parseFloat(p[4]),
                                PApplet.parseFloat(p[5]), PApplet.parseFloat(p[6]),
                                p.length >= 8 ? PApplet.parseInt(p[7]) : 0);
                        if (p.length >= 13) {
                            prop.enemyDoor = PApplet.parseBoolean(p[8]);
                            prop.defeated = PApplet.parseBoolean(p[9]);
                            prop.maxHealth = PApplet.parseInt(p[10]);
                            prop.enemyType = EnemyType.valueOf(p[11]);
                            prop.spriteIndex = PApplet.parseInt(p[12]);
                        }
                        props.add(prop);
                        break;
                    case "N":
                        EnemyType type = p.length >= 7
                                ? EnemyType.valueOf(p[6])
                                : EnemyType.values()[(int)(Math.random() * 3)];
                        int spriteIndex = p.length >= 8 ? PApplet.parseInt(p[7]) : 0;
                        boolean interactable = p.length >= 9 ? PApplet.parseBoolean(p[8]) : true;
                        npcs.add(new Npc(PApplet.parseInt(p[1]),
                                PApplet.parseFloat(p[2]), PApplet.parseFloat(p[3]),
                                PApplet.parseBoolean(p[4]), PApplet.parseInt(p[5]), type,
                                spriteIndex, interactable));
                        break;
                }
            } catch (Exception ignored) {}
        }
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    private enum PropType { TREE, FENCE, LOCKER, PLANT, WINDOW, BOARD, DOOR }

    /** A raised terrain block connected to the ground — like a stepped sidewalk. */
    public static class TerrainBlock {
        final int   chunk;
        final float x, w, h;
        public float y;

        TerrainBlock(int chunk, float x, float w, float h) {
            this.chunk = chunk;
            this.x     = x;
            this.w     = w;
            this.h     = h;
        }

        /** The y coordinate of the top walkable surface. */
        public float surfaceY() { return GROUND_Y - h; }
    }

    private static class Building {
        final int   chunk;
        public final float x, y, w, h;
        final int   imageIndex;

        Building(int chunk, float x, float y, float w, float h, int imageIndex) {
            this.chunk      = chunk;
            this.x          = x;
            this.y          = y;
            this.w          = w;
            this.h          = h;
            this.imageIndex = imageIndex;
        }
    }

    private static class Prop {
        final int      chunk;
        final PropType type;
        final float    x, y, w, h;
        final int      imageIndex;
        boolean        enemyDoor;
        boolean        defeated;
        int            maxHealth = 100;
        EnemyType      enemyType = EnemyType.GEEK;
        int            spriteIndex;

        Prop(int chunk, PropType type, float x, float y, float w, float h) {
            this(chunk, type, x, y, w, h, 0);
        }

        Prop(int chunk, PropType type, float x, float y, float w, float h, int imageIndex) {
            this.chunk = chunk;
            this.type  = type;
            this.x     = x;
            this.y     = y;
            this.w     = w;
            this.h     = h;
            this.imageIndex = imageIndex;
        }
    }

    public static class DoorEnemy {
        private final Prop prop;

        DoorEnemy(Prop prop) {
            this.prop = prop;
        }

        public int getMaxHealth() { return prop.maxHealth; }
        public EnemyType getType() { return prop.enemyType; }
        public int getSpriteIndex() { return prop.spriteIndex; }
    }

    private static class SchoolNpc {
        final int chunk;
        final float x, y;
        final int imageIndex;
        final boolean walking;
        final int phase;

        SchoolNpc(int chunk, float x, float y, int imageIndex, boolean walking, int phase) {
            this.chunk = chunk;
            this.x = x;
            this.y = y;
            this.imageIndex = imageIndex;
            this.walking = walking;
            this.phase = phase;
        }
    }

    private static class BackgroundChunk {
        final List<BackgroundSprite> clouds = new ArrayList<>();
        final List<BackgroundSprite> buildings = new ArrayList<>();
    }

    private static class BackgroundSprite {
        final float x, y, w, h;
        final int imageIndex;

        BackgroundSprite(float x, float y, float w, float h, int imageIndex) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.imageIndex = imageIndex;
        }
    }

    public static class Npc {
        final int       chunk;
        final float     x, y;
        final int       maxHealth;
        final EnemyType type;
        final int       spriteIndex;
        final boolean   interactable;
        boolean         defeated;

        Npc(int chunk, float x, float y, boolean defeated, int maxHealth, EnemyType type,
            int spriteIndex, boolean interactable) {
            this.chunk     = chunk;
            this.x         = x;
            this.y         = y;
            this.defeated  = defeated;
            this.maxHealth = maxHealth;
            this.type      = type;
            this.spriteIndex = spriteIndex;
            this.interactable = interactable;
        }

        public int       getMaxHealth() { return maxHealth; }
        public EnemyType getType()      { return type; }
        public int       getSpriteIndex() { return spriteIndex; }
        public boolean   isInteractable() { return interactable; }
    }

    public static class StoryNpc {
        public final String spriteId;
        public final String name;
        public final int progressionStep;
        public final float x, y;
        boolean defeated;

        StoryNpc(String spriteId, String name, int progressionStep, float x, float y, boolean defeated) {
            this.spriteId = spriteId;
            this.name = name;
            this.progressionStep = progressionStep;
            this.x = x;
            this.y = y;
            this.defeated = defeated;
        }
    }
}