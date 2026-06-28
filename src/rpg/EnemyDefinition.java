package rpg;

import java.util.Arrays;
import java.util.List;

/**
 * Data-driven definition of an enemy's combat profile: which subjects it's
 * weak to (bonus damage taken), which it resists (reduced damage taken),
 * how much health it has, and any special move it can use.
 *
 * Lookup is by display name (e.g. "Jamie", "Math Test", "Geek"), matching
 * the enemyName already shown in the battle screen, so both the three
 * generic hallway enemy types and every named story NPC share one system.
 *
 * baseHealth == null means "use the normal scaling random-health system"
 * (the hallway Geek/Ace/Jock encounters, which start around 70 HP near the
 * school entrance and get tougher with distance, rather than a fixed value).
 */
public class EnemyDefinition {

    /**
     * An optional special move an enemy can use instead of (or alongside)
     * its normal attack. Each turn the enemy attacks, there's a chance
     * (SPECIAL_MOVE_CHANCE) it uses this instead of/in addition to a plain
     * hit, depending on the move's own semantics:
     *   SELF_HEAL        -> heals the enemy for `magnitude` HP
     *   SELF_DAMAGE_BUFF -> the enemy's own damage x`magnitude` for `duration` turns
     *   WEAKEN_PLAYER    -> the player's outgoing damage x`magnitude` for `duration` turns
     */
    public enum SpecialMoveType { SELF_HEAL, SELF_DAMAGE_BUFF, WEAKEN_PLAYER }

    public static class SpecialMove {
        public final SpecialMoveType type;
        public final float magnitude;
        public final int duration; // in turns; ignored for SELF_HEAL

        public SpecialMove(SpecialMoveType type, float magnitude, int duration) {
            this.type = type;
            this.magnitude = magnitude;
            this.duration = duration;
        }
    }

    /** Chance (0.0-1.0) that an enemy with a special move uses it on a given turn. */
    public static final float SPECIAL_MOVE_CHANCE = 0.10f;

    public final String name;
    public final List<String> weakTo;
    public final List<String> resistantTo;
    public final Integer baseHealth; // null = use scaling random health
    public final SpecialMove[] specialMoves; // empty array = no special move

    private EnemyDefinition(String name, String[] weakTo, String[] resistantTo,
                            Integer baseHealth, SpecialMove[] specialMoves) {
        this.name = name;
        this.weakTo = Arrays.asList(weakTo);
        this.resistantTo = Arrays.asList(resistantTo);
        this.baseHealth = baseHealth;
        this.specialMoves = specialMoves;
    }

    private static final SpecialMove[] NONE = new SpecialMove[0];

    public static final EnemyDefinition[] ALL = {
            // Generic hallway types — health is scaled/random (baseHealth = null)
            new EnemyDefinition("Geek",
                    new String[]{"Computer Science", "Math", "Science"},
                    new String[]{"English", "History"},
                    null, NONE),
            new EnemyDefinition("Ace",
                    new String[]{"History", "English"},
                    new String[]{"Math", "Science"},
                    null, NONE),
            new EnemyDefinition("Jock",
                    new String[]{"Gym"},
                    new String[]{"Math", "English"},
                    null, NONE),

            // Story test encounters
            new EnemyDefinition("Math Test",
                    new String[]{"Math"},
                    new String[]{"English", "History", "Gym"},
                    250, NONE),
            new EnemyDefinition("Science Test",
                    new String[]{"Science"},
                    new String[]{"English", "History", "Gym"},
                    250, NONE),
            // English Test isn't in the chart but exists in the story; give it
            // a consistent profile mirroring the other two subject tests.
            new EnemyDefinition("English Test",
                    new String[]{"English"},
                    new String[]{"Math", "Science", "Gym"},
                    250, NONE),

            // Named story NPCs
            new EnemyDefinition("Jamie",
                    new String[]{"Math", "English"},
                    new String[]{"Gym", "Computer Science"},
                    300, new SpecialMove[]{ new SpecialMove(SpecialMoveType.SELF_HEAL, 50, 0) }),
            new EnemyDefinition("Rico",
                    new String[]{"Gym", "History"},
                    new String[]{"Math", "Science"},
                    250, NONE),
            new EnemyDefinition("Stacey",
                    new String[]{"English", "Science"},
                    new String[]{"Computer Science", "Math"},
                    250, new SpecialMove[]{ new SpecialMove(SpecialMoveType.SELF_DAMAGE_BUFF, 1.10f, 2) }),
            new EnemyDefinition("Hallway Jock",
                    new String[]{"Gym"},
                    new String[]{"Math", "English"},
                    300, new SpecialMove[]{ new SpecialMove(SpecialMoveType.WEAKEN_PLAYER, 0.6f, 1) }),
            new EnemyDefinition("The Val",
                    new String[]{"Gym", "Computer Science"},
                    new String[]{"Math", "Science", "English", "History"},
                    1000, new SpecialMove[]{
                    new SpecialMove(SpecialMoveType.SELF_HEAL, 100, 0),
                    new SpecialMove(SpecialMoveType.SELF_DAMAGE_BUFF, 1.10f, 2)
            }),
    };

    /**
     * Looks up an enemy's combat profile by display name. Falls back to a
     * neutral profile (no weaknesses/resistances, scaling random health, no
     * special move) for any name not in the table, so unrecognized or
     * future enemy names never crash battle — they just play it straight.
     */
    public static EnemyDefinition findByName(String name) {
        for (EnemyDefinition def : ALL) {
            if (def.name.equalsIgnoreCase(name)) return def;
        }
        return new EnemyDefinition(name, new String[0], new String[0], null, NONE);
    }
}