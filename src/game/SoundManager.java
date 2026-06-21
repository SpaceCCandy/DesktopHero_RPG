package game;

import processing.core.PApplet;
import processing.sound. *;

/**
 * Central manager for background music and sound effects.
 *
 * Music:   ChillMusic.mp3  — loops the entire game; volume controlled in Settings
 * Effects: Jump.mp3        — plays on every player jump
 *          Bling_.mp3      — plays when a story-progressing enemy is defeated
 *
 * SETUP NOTE: SoundFile must be constructed after Processing's sketch thread
 * is fully running (i.e. inside draw() or setup() AFTER size() completes).
 * Call init(app) from setup() in Main, then startMusic() immediately after.
 */
public class SoundManager {

    private static final String SETTINGS_FILE    = "data/settings.csv";
    private static final float  DEFAULT_VOLUME   = 0.0f;

    private final PApplet app;
    private SoundFile music;
    private SoundFile jumpSfx;
    private SoundFile blingSfx;

    private float   musicVolume = DEFAULT_VOLUME;
    private boolean available   = false;
    private boolean musicLooping = false;

    public SoundManager(PApplet app) {
        this.app = app;
    }

    /**
     * Call this from setup() in Main, after size() has been called.
     * Loads all three audio files and prints a clear error if anything fails.
     */
    public void init() {
        try {
            String basePath = app.sketchPath("src/assets/sound/");
            // Fallback: some IntelliJ/Processing setups have "src" as the sketch root
            // already (so sketchPath() points straight at it), in which case the
            // "src/assets/sound" folder above won't exist — try "assets/sound" too.
            java.io.File probe = new java.io.File(basePath);
            if (!probe.exists()) {
                basePath = app.sketchPath("assets/sound/");
            }

            music    = new SoundFile(app, basePath + "ChillMusic.wav");
            jumpSfx  = new SoundFile(app, basePath + "Jump.wav");
            blingSfx = new SoundFile(app, basePath + "Bling_.wav");
            available = true;
            loadVolume();
            System.out.println("[SoundManager] Loaded OK from: " + basePath + " | Volume: " + musicVolume);
        } catch (Exception e) {
            System.out.println("[SoundManager] Failed to load sounds: " + e.getMessage());
            System.out.println("[SoundManager] Tried sketchPath: " + app.sketchPath(""));
            System.out.println("[SoundManager] Make sure ChillMusic.wav, Jump.wav, Bling_.wav exist under src/assets/sound/");
            available = false;
        }
    }

    public boolean isAvailable() { return available; }

    /** Start looping background music. Safe to call multiple times. */
    public void startMusic() {
        if (!available || music == null || musicLooping) return;
        music.amp(musicVolume);
        music.loop();
        musicLooping = true;
    }

    public void playJump() {
        if (available && jumpSfx != null)
            jumpSfx.play();
            jumpSfx.amp(0.3f);
    }

    public void playBling() {
        if (available && blingSfx != null) blingSfx.play();
    }

    public float getMusicVolume() { return musicVolume; }

    public void setMusicVolume(float v) {
        musicVolume = PApplet.constrain(v, 0f, 1f);
        if (available && music != null) music.amp(musicVolume);
        saveVolume();
    }

    // ── Volume persistence ────────────────────────────────────────────────────

    private void loadVolume() {
        java.io.File f = new java.io.File(app.sketchPath(SETTINGS_FILE));
        if (!f.exists()) return;
        String[] lines = app.loadStrings(f.getAbsolutePath());
        if (lines == null || lines.length == 0) return;
        try { musicVolume = PApplet.constrain(Float.parseFloat(lines[0].trim()), 0f, 1f); }
        catch (NumberFormatException ignored) {}
    }

    private void saveVolume() {
        java.io.File f = new java.io.File(app.sketchPath(SETTINGS_FILE));
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        app.saveStrings(f.getAbsolutePath(), new String[]{ String.valueOf(musicVolume) });
    }
}