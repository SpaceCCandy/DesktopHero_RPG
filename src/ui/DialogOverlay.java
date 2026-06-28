package ui;

import processing.core.PApplet;
import processing.core.PImage;

public class DialogOverlay {
    public enum Outcome {
        NONE,
        CLOSED,
        START_FIGHT
    }

    public static class Line {
        public final String speaker;
        public final String text;
        public final String[] responses;
        public final String itemText;
        public final boolean startsFight;

        public Line(String speaker, String text, String[] responses, String itemText, boolean startsFight) {
            this.speaker = speaker;
            this.text = text;
            this.responses = responses;
            this.itemText = itemText;
            this.startsFight = startsFight;
        }
    }

    private Line[] lines = new Line[0];
    private PImage portrait;
    private int index;
    private Outcome outcome = Outcome.NONE;

    public void start(Line[] lines, PImage portrait) {
        this.lines = lines == null ? new Line[0] : lines;
        this.portrait = portrait;
        this.index = 0;
        this.outcome = Outcome.NONE;
    }

    public void draw(PApplet p) {
        if (outcome != Outcome.NONE || lines.length == 0) {
            return;
        }

        Line line = lines[index];
        int boxX = 54;
        int boxY = p.height - 188;
        int boxW = p.width - 108;
        int boxH = 150;

        p.pushStyle();
        p.fill(0, 90);
        p.noStroke();
        p.rect(0, 0, p.width, p.height);

        drawResponseButtons(p, line, boxX, boxY);

        p.fill(255);
        p.stroke(45);
        p.strokeWeight(2);
        p.rect(boxX, boxY, boxW, boxH, 6);

        p.fill(25);
        p.textAlign(PApplet.LEFT, PApplet.TOP);
        p.textSize(16);
        p.text(line.speaker, boxX + 22, boxY + 18);
        p.textSize(15);
        p.text(line.itemText != null ? line.itemText : line.text, boxX + 22, boxY + 48, boxW - 185, 76);

        drawPortrait(p, boxX + boxW - 140, boxY + 18, 108, 108);

        if (line.responses == null || line.responses.length == 0) {
            drawCloseButton(p, boxX + boxW - 44, boxY + boxH - 40);
        }
        p.popStyle();
    }

    public void mousePressed(int mx, int my, int screenW, int screenH) {
        if (outcome != Outcome.NONE || lines.length == 0) {
            return;
        }

        Line line = lines[index];
        int boxX = 54;
        int boxY = screenH - 188;
        int boxW = screenW - 108;
        int responseY = boxY - 44;

        if (line.responses != null && line.responses.length > 0) {
            for (int i = 0; i < line.responses.length; i++) {
                int x = boxX + i * 190;
                if (inside(mx, my, x, responseY, 176, 34)) {
                    advance();
                    return;
                }
            }
            return;
        }

        if (inside(mx, my, boxX + boxW - 44, boxY + 110, 28, 28)) {
            advance();
        }
    }

    public Outcome getOutcome() {
        return outcome;
    }

    private void advance() {
        Line line = lines[index];
        if (line.startsFight) {
            outcome = Outcome.START_FIGHT;
            return;
        }

        if (index < lines.length - 1) {
            index++;
        } else {
            outcome = Outcome.CLOSED;
        }
    }

    private void drawResponseButtons(PApplet p, Line line, int boxX, int boxY) {
        if (line.responses == null || line.responses.length == 0) {
            return;
        }

        int y = boxY - 44;
        for (int i = 0; i < line.responses.length; i++) {
            int x = boxX + i * 190;
            p.fill(255, 245, 190);
            p.stroke(55);
            p.strokeWeight(1);
            p.rect(x, y, 176, 34, 5);
            p.fill(20);
            p.textAlign(PApplet.CENTER, PApplet.CENTER);
            p.textSize(13);
            p.text(line.responses[i], x + 88, y + 17);
        }
    }

    private void drawPortrait(PApplet p, int x, int y, int w, int h) {
        p.fill(238);
        p.stroke(80);
        p.rect(x, y, w, h, 4);
        if (portrait == null || portrait.width <= 0 || portrait.height <= 0) {
            return;
        }

        float drawH = h - 12;
        float drawW = drawH * portrait.width / (float) portrait.height;
        if (drawW > w - 12) {
            drawW = w - 12;
            drawH = drawW * portrait.height / (float) portrait.width;
        }
        p.image(portrait, x + w / 2f - drawW / 2f, y + h - drawH - 6, drawW, drawH);
    }

    private void drawCloseButton(PApplet p, int x, int y) {
        p.fill(245, 235, 235);
        p.stroke(70);
        p.rect(x, y, 28, 28, 4);
        p.fill(30);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(16);
        p.text("x", x + 14, y + 13);
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
