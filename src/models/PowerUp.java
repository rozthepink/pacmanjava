package models;

public class PowerUp {
    private int x;
    private int y;
    private PowerUpType type;
    private int duration;
    private boolean isActive;
    private int currentFrame;
    private long spawnTime;

    public enum PowerUpType {
        SPEED_BOOST,    // Increases Pacman's speed by 50% <- supposed to do that, is a blank powerup
        GHOST_SCARE,    // Makes ghosts vulnerable
        EXTRA_LIFE,     // Gives an extra life
        SCORE_MULTIPLIER, // Doubles score for a duration
        GHOST_FREEZE,    // Freezes ghosts in place
        INVINCIBILITY
    }

    public PowerUp(int x, int y, PowerUpType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.duration = 0;
        this.isActive = false;
        this.currentFrame = 0;
        this.spawnTime = System.currentTimeMillis();
    }

    // Getters and setters
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public PowerUpType getType() { return type; }
    public void setType(PowerUpType type) { this.type = type; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public int getCurrentFrame() { return currentFrame; }
    public void setCurrentFrame(int currentFrame) { this.currentFrame = currentFrame; }
    public long getSpawnTime() { return spawnTime; }
    public boolean shouldDespawn() {
        return !isActive && (System.currentTimeMillis() - spawnTime) > 10000;
    }

    public void activate() {
        this.isActive = true;
        switch (type) {
            case SPEED_BOOST:
                this.duration = 5000; // 5 seconds
                break;
            case GHOST_SCARE:
                this.duration = 7000; // 7 seconds
                break;
            case SCORE_MULTIPLIER:
                this.duration = 10000; // 10 seconds
                break;
            case GHOST_FREEZE:
                this.duration = 3000; // 3 seconds
                break;
            case EXTRA_LIFE:
                this.duration = 0; // Instant effect
                break;
        }
    }

    public void deactivate() {
        this.isActive = false;
        this.duration = 0;
    }

    public void updateDuration(int deltaTime) {
        if (isActive && duration > 0) {
            duration -= deltaTime;
            if (duration <= 0) {
                deactivate();
            }
        }
    }
} 