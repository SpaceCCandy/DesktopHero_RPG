package rpg;

public class CheatSheetDefinition {
    public static final CheatSheetDefinition[] ALL = {
            new CheatSheetDefinition("Extra Time", "Gain 35 energy.", 0xFFFFF0C2),
            new CheatSheetDefinition("Process of Elimination", "Deal 30 damage.", 0xFFDFF2FF),
            new CheatSheetDefinition("Curve", "Heal 30 HP.", 0xFFE6FFE2)
    };

    public final String name;
    public final String description;
    public final int fillColor;

    private CheatSheetDefinition(String name, String description, int fillColor) {
        this.name = name;
        this.description = description;
        this.fillColor = fillColor;
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
