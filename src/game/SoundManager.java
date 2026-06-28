package game;

import processing.core.PApplet;
import processing.sound.SoundFile;

/**
 * Central manager for background music and sound effects.
 *
 * Music:   ChillMusic.wav  — loops during normal gameplay; paused during battle
 *          battleLoop.wav  — loops only while a battle is active, replacing
 *                            the chill music until the battle ends
 * Effects: Jump.wav        — plays on every player jump
 *          Bling_.wav      — plays when a story-progressing enemy is defeated
 *
 * NOTE ON FILE FORMAT: these are WAV, not MP3. The Processing Sound library's
 * MP3 decoding (via the bundled JSyn engine) is unreliable on modern JDKs —
 * it throws UnsupportedAudioFileException for many otherwise-valid MP3s,
 * since Java has no built-in MP3 decoder and JSyn's fallback path is known
 * to be flaky. WAV is natively supported with zero extra dependencies, so
 * converting once at build time sidesteps the problem entirely.
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
    private SoundFile battleMusic;
    private SoundFile jumpSfx;
    private SoundFile blingSfx;

    private float   musicVolume = DEFAULT_VOLUME;
    private boolean available   = false;
    private boolean musicLooping = false;
    private boolean battleMusicLooping = false;

    public SoundManager(PApplet app) {
        this.app = app;
    }

    /**
     * Call this from setup() in Main, after size() has been called.
     * Loads all audio files and prints a clear error if anything fails.
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

            music       = new SoundFile(app, basePath + "ChillMusic.wav");
            jumpSfx     = new SoundFile(app, basePath + "Jump.wav");
            blingSfx    = new SoundFile(app, basePath + "Bling_.wav");
            available = true;
            loadVolume();

            // Battle music is loaded separately and treated as optional: if
            // the file isn't there yet, the rest of the sound system still
            // works fine and battles simply keep no music rather than crash.
            try {
                battleMusic = new SoundFile(app, basePath + "battleLoop.wav");
            } catch (Exception battleEx) {
                battleMusic = null;
                System.out.println("[SoundManager] battleLoop.wav not loaded (optional): " + battleEx.getMessage());
            }

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

    /**
     * Pauses the regular background music and starts looping the battle
     * track instead. Call when entering the battle screen. Safe to call
     * multiple times in a row (e.g. if already in battle).
     */
    public void startBattleMusic() {
        if (!available) return;
        if (music != null && musicLooping) {
            music.pause();
            musicLooping = false;
        }
        if (battleMusic != null && !battleMusicLooping) {
            battleMusic.amp(musicVolume);
            battleMusic.loop();
            battleMusicLooping = true;
        }
    }

    /**
     * Stops the battle track and resumes the regular background music. Call
     * when leaving the battle screen back to normal gameplay.
     */
    public void stopBattleMusic() {
        if (!available) return;
        if (battleMusic != null && battleMusicLooping) {
            battleMusic.stop();
            battleMusicLooping = false;
        }
        startMusic(); // resumes the chill loop (no-op if already playing)
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
        if (available && battleMusic != null) battleMusic.amp(musicVolume);
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
