package game;

import processing.core.PApplet;
import processing.sound.SoundFile;

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
    private static final float  DEFAULT_VOLUME   = 0.6f;

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
            music    = new SoundFile(app, app.sketchPath("assets/sound/ChillMusic.mp3"));
            jumpSfx  = new SoundFile(app, app.sketchPath("assets/sound/Jump.mp3"));
            blingSfx = new SoundFile(app, app.sketchPath("assets/sound/Bling_.mp3"));
            available = true;
            loadVolume();
            System.out.println("[SoundManager] Loaded OK. Volume: " + musicVolume);
        } catch (Exception e) {
            System.out.println("[SoundManager] Failed to load sounds: " + e.getMessage());
            System.out.println("  Make sure the sound files are in: " + app.sketchPath("assets/sound/"));
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
        if (available && jumpSfx != null) jumpSfx.play();
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
        String[] lines = app.loadStrings(app.sketchPath(SETTINGS_FILE));
        if (lines == null || lines.length == 0) return;
        try { musicVolume = PApplet.constrain(Float.parseFloat(lines[0].trim()), 0f, 1f); }
        catch (NumberFormatException ignored) {}
    }

    private void saveVolume() {
        app.saveStrings(app.sketchPath(SETTINGS_FILE), new String[]{ String.valueOf(musicVolume) });
    }
}