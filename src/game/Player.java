package game;

import processing.core.PApplet;
import processing.core.PImage;

public class Player {

    private final PApplet app;
    private final PImage idleSprite;
    private final PImage[] walkSprites;
    private final PImage jumpSprite;

    private float x;
    private float y;

    private final float baseWidth = 70;
    private final float baseHeight = 90;
    private float width = baseWidth;
    private float height = baseHeight;
    private float previousX;
    private float previousY;
    private float scale = 1f;
    private float scaleStart = 1f;
    private float scaleTarget = 1f;
    private float scaleProgress = 1f;

    private float velX;
    private float velY;

    private final float speed = 5;
    private final float gravity = 0.8f;
    private final float jumpForce = -12;

    private boolean grounded;
    private boolean platformLayer;
    private int animationTick;
    private boolean facingRight = true;

    public boolean moveLeft;
    public boolean moveRight;

    public Player(PApplet app,
                  float x,
                  float y,
                  PImage idleSprite,
                  PImage[] walkSprites, PImage jumpSprite) {

        this.app = app;
        this.x = x;
        this.y = y;

        this.idleSprite = idleSprite;
        this.walkSprites = walkSprites;
        this.jumpSprite = jumpSprite;
    }

    public void update() {
        update(1f);
    }

    public void update(float frameScale) {
        frameScale = PApplet.constrain(frameScale, 0.25f, 2.5f);
        previousX = x;
        previousY = y;
        updateScale();

        velX = 0;

        if(moveLeft) {
            velX = -speed * frameScale;
            facingRight = false;
        }

        if(moveRight) {
            velX = speed * frameScale;
            facingRight = true;
        }

        velY += gravity * frameScale;

        x += velX;
        y += velY * frameScale;
        animationTick++;

        // Ground collision
        if (y + height >= 500) {
            y = 500 - height;
            velY = 0;
            grounded = true;
            platformLayer = false;
        } else {
            grounded = false;
        }
    }

    public void setSchoolZoom(boolean enabled) {
        float nextTarget = enabled ? 1.50f : 1f;
        if (Math.abs(nextTarget - scaleTarget) < 0.001f) {
            return;
        }

        scaleStart = scale;
        scaleTarget = nextTarget;
        scaleProgress = 0f;
    }

    private void updateScale() {
        if (scaleProgress >= 1f) {
            return;
        }

        float footY = y + height;
        scaleProgress = Math.min(1f, scaleProgress + 0.08f);
        float eased = scaleProgress * scaleProgress;
        scale = scaleStart + (scaleTarget - scaleStart) * eased;
        width = baseWidth * scale;
        height = baseHeight * scale;
        y = footY - height;
    }

    public void jump() {

        if(grounded) {
            velY = jumpForce;
        }
    }

    public void landOnPlatform(float platformY) {
        if (velY >= 0) {
            y = platformY - height;
            velY = 0;
            grounded = true;
            platformLayer = true;
        }
    }

    public void render() {
        PImage sprite = currentSprite();

        if (sprite != null) {
            float drawHeight = height;
            float drawWidth = drawHeight * sprite.width / (float) sprite.height;
            float drawX = x + width / 2f - drawWidth / 2f;

            app.pushMatrix();

            if (!facingRight) {
                app.translate(drawX + drawWidth, y);
                app.scale(-1, 1);
                app.image(sprite, 0, 0, drawWidth, drawHeight);
            }
            else {
                app.image(sprite, drawX, y, drawWidth, drawHeight);
            }

            app.popMatrix();
        }
    }

    private PImage currentSprite() {
        if (!grounded) {
            // Rising → jump sprite, Falling → Walk3
            return velY < 0 ? jumpSprite : walkSprites[2];
        }
        if (!moveLeft && !moveRight) return idleSprite;
        int frame = (animationTick / 5) % walkSprites.length;
        PImage s = walkSprites[frame];
        return s != null ? s : idleSprite;
    }

    public PImage getCurrentSprite() {
        int frame = (animationTick / 5) % walkSprites.length;
        PImage walkSprite = walkSprites[frame];

        if (velY < 0) {
            // Rising — use jump sprite
            return jumpSprite;
        } else if (velY > 0) {
            // Falling — use Walk3
            return walkSprites[2]; // PlayerWalk3 is index 2
        } else {
            // Grounded
            if (!moveLeft && !moveRight) {
                return idleSprite;
            }
            // Walk cycle
            return walkSprite;
        }
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getScale() {
        return scale;
    }

    public float getPreviousX() {
        return previousX;
    }

    public float getPreviousY() {
        return previousY;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void stopHorizontal() {
        velX = 0;
    }

    public void stopVertical() {
        velY = 0;
    }

    public boolean isMovingUp() {
        return velY < 0;
    }

    public boolean isOnPlatformLayer() {
        return platformLayer;
    }
}
