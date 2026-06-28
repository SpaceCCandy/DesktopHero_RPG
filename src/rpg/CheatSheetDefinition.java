package rpg;

/**
 * A cheat sheet is a one-time-use battle item. Using one consumes it from
 * the player's backpack and applies its effect immediately (instant effects)
 * and/or over several upcoming turns (duration effects).
 *
 * EffectType meanings:
 *   ENERGY_GAIN        -> instantly restores `magnitude` energy
 *   DAMAGE              -> instantly deals `magnitude` damage to the enemy
 *   HEAL_OVER_TIME       -> heals `magnitude` HP at the end of each of the
 *                           next `duration` turns (instant heal this turn too)
 *   PLAYER_DAMAGE_MULT  -> multiplies the player's outgoing card damage by
 *                           `magnitude` (e.g. 1.5) for the next `duration` turns
 *   ENEMY_DAMAGE_MULT   -> multiplies the enemy's outgoing damage by
 *                           `magnitude` (e.g. 0.5) for the next `duration` turns
 */
public class CheatSheetDefinition {

    public enum EffectType { ENERGY_GAIN, DAMAGE, HEAL_OVER_TIME, PLAYER_DAMAGE_MULT, ENEMY_DAMAGE_MULT }

    public static final CheatSheetDefinition[] ALL = {
            // name, description, fillColor, effect, magnitude, duration (turns), gumball cost
            new CheatSheetDefinition("Extra Time", "Energy +35", 0xFFFFF0C2,
                    EffectType.ENERGY_GAIN, 35, 0, 80),
            new CheatSheetDefinition("Process of Elimination", "Deal 30 damage", 0xFFDFF2FF,
                    EffectType.DAMAGE, 30, 0, 60),
            new CheatSheetDefinition("Curve", "Heal 30 HP", 0xFFE6FFE2,
                    EffectType.HEAL_OVER_TIME, 30, 1, 60),
            new CheatSheetDefinition("Photosynthesis", "Heal 10 after every turn for 5 turns", 0xFFD7F5D0,
                    EffectType.HEAL_OVER_TIME, 10, 5, 80),
            new CheatSheetDefinition("Industrial Revolution", "Your damage x1.5 for 2 turns", 0xFFFFD9C2,
                    EffectType.PLAYER_DAMAGE_MULT, 1.5f, 2, 90),
            new CheatSheetDefinition("Symbolism", "Energy +10", 0xFFFFF0C2,
                    EffectType.ENERGY_GAIN, 10, 0, 40),
            new CheatSheetDefinition("Rubber Duck Debugging", "Heal 10 after every turn for 5 turns", 0xFFD7F5D0,
                    EffectType.HEAL_OVER_TIME, 10, 5, 60),
            new CheatSheetDefinition("Sugar Rush", "Energy +35", 0xFFFFF0C2,
                    EffectType.ENERGY_GAIN, 35, 0, 80),
            new CheatSheetDefinition("Did It All", "Enemy damage x0.5 for 1 turn", 0xFFE0D7FF,
                    EffectType.ENEMY_DAMAGE_MULT, 0.5f, 1, 70),
    };

    public final String name;
    public final String description;
    public final int fillColor;
    public final EffectType effect;
    public final float magnitude;
    public final int duration;   // in turns; 0 means "instant, no lingering effect"
    public final int cost;       // gumball cost in the shop

    private CheatSheetDefinition(String name, String description, int fillColor,
                                 EffectType effect, float magnitude, int duration, int cost) {
        this.name = name;
        this.description = description;
        this.fillColor = fillColor;
        this.effect = effect;
        this.magnitude = magnitude;
        this.duration = duration;
        this.cost = cost;
    }

    public static CheatSheetDefinition findByName(String name) {
        for (CheatSheetDefinition sheet : ALL) {
            if (sheet.name.equals(name)) {
                return sheet;
            }
        }
        return ALL[0];
    }
}
