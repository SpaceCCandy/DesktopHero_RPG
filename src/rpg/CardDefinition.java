package rpg;

import processing.core.PApplet;

public class CardDefinition {

    public static final CardDefinition[] ALL = {
            // name, rarity, damage, fillColor, strokeColor, drawWeight, subject
            new CardDefinition("Studying",         "Common",       30,  0xFFFFE6B3, 0xFFFFB62E, 40, "Math"),
            new CardDefinition("Studying",         "Common",       30,  0xFFFFE6B3, 0xFFFFB62E, 40, "Science"),
            new CardDefinition("Studying",         "Common",       30,  0xFFFFE6B3, 0xFFFFB62E, 40, "English"),
            new CardDefinition("Studying",         "Common",       30,  0xFFFFE6B3, 0xFFFFB62E, 40, "History"),
            new CardDefinition("Bonus answers",    "Common",       20,  0xFFFFE6B3, 0xFFFFB62E, 40, "Comp Sci"),
            new CardDefinition("ChatGPT",          "Common",       40,  0xFFFFE6B3, 0xFFFFB62E, 40, "Comp Sci"),
            new CardDefinition("MAGIC WAND",       "Rare",         70,  0xFFC8F2D1, 0xFF4CC56A, 30, "Science"),
            new CardDefinition("Textbook",         "Rare",         70,  0xFFC8F2D1, 0xFF4CC56A, 30, "Math"),
            new CardDefinition("Textbook",         "Rare",         70,  0xFFC8F2D1, 0xFF4CC56A, 30, "English"),
            new CardDefinition("Textbook",         "Rare",         70,  0xFFC8F2D1, 0xFF4CC56A, 30, "History"),
            new CardDefinition("Billy tuants",     "Rare",         50,  0xFFC8F2D1, 0xFF4CC56A, 30, "Math"),
            new CardDefinition("Deodorant",        "Rare",        100,  0xFFC8F2D1, 0xFF4CC56A, 30, "Comp Sci"),
            new CardDefinition("Claude",           "Legendary",    99,  0xFFBDF4F0, 0xFF35C8C8,  9, "Comp Sci"),
            new CardDefinition("Punching Ghost",   "Legendary",   100,  0xFFBDF4F0, 0xFF35C8C8,  9, "Gym"),
            new CardDefinition("Emotional Damage", "Legendary",   100,  0xFFBDF4F0, 0xFF35C8C8,  9, "Math"),
            new CardDefinition("Emotional Damage", "Legendary",   100,  0xFFBDF4F0, 0xFF35C8C8,  9, "Science"),
            new CardDefinition("Emotional Damage", "Legendary",   100,  0xFFBDF4F0, 0xFF35C8C8,  9, "English"),
            new CardDefinition("WannaCry",         "Cyber Special",999, 0xFF7F4BEF, 0xFF4C20A8,  1, "Comp Sci"),
    };

    public final String name;
    public final String rarity;
    public final int damage;
    public final int fillColor;
    public final int strokeColor;
    public final int drawWeight;
    public final String subject;
    public final int energyCost;

    private CardDefinition(String name, String rarity, int damage,
                           int fillColor, int strokeColor, int drawWeight, String subject) {
        this.name = name;
        this.rarity = rarity;
        this.damage = damage;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.drawWeight = drawWeight;
        this.subject = subject;  // NEW
        int baseEnergyCost = Math.max(8, damage / 3 + Math.abs((name + subject + rarity).hashCode()) % 12);
        this.energyCost = Math.min(100, (int) Math.ceil(baseEnergyCost * 1.44f));
    }

    public static CardDefinition findByName(String name) {
        for (CardDefinition card : ALL) {
            if (card.name.equals(name)) {
                return card;
            }
        }

        return ALL[0];
    }

    public static CardDefinition randomCard(PApplet app) {
        int totalWeight = 0;

        for (CardDefinition card : ALL) {
            totalWeight += card.drawWeight;
        }

        int roll = (int) app.random(totalWeight);

        for (CardDefinition card : ALL) {
            roll -= card.drawWeight;

            if (roll < 0) {
                return card;
            }
        }

        return ALL[0];
    }
}