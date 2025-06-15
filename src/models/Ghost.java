package models;

public class Ghost {
    private int x;
    private int y;
    private Direction direction;
    private double speed;
    private GhostType type;
    private GhostState state;
    private int currentFrame;
    private int spawnX;
    private int spawnY;
    private int targetX;
    private int targetY;
    private boolean isScared;
    private boolean isFrozen;
    private boolean isDead;

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public enum GhostType {
        RED, PINK, BLUE, ORANGE
    }

    public enum GhostState {
        NORMAL, SCARED, DEAD, RETURNING
    }

    public Ghost(int x, int y, GhostType type) {
        this.x = x;
        this.y = y;
        this.spawnX = x;
        this.spawnY = y;
        this.direction = Direction.RIGHT;
        this.speed = 1;
        this.type = type;
        this.state = GhostState.NORMAL;
        this.currentFrame = 0;
        this.isScared = false;
        this.isFrozen = false;
        this.isDead = false;
    }

    // Getters and setters
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }
    public double getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }
    public GhostType getType() { return type; }
    public void setType(GhostType type) { this.type = type; }
    public GhostState getState() { return state; }
    public void setState(GhostState state) { this.state = state; }
    public int getCurrentFrame() { return currentFrame; }
    public void setCurrentFrame(int currentFrame) { this.currentFrame = currentFrame; }
    public int getTargetX() { return targetX; }
    public void setTargetX(int targetX) { this.targetX = targetX; }
    public int getTargetY() { return targetY; }
    public void setTargetY(int targetY) { this.targetY = targetY; }
    public boolean isScared() { return isScared; }
    public void setScared(boolean scared) { isScared = scared; }
    public boolean isFrozen() { return isFrozen; }
    public void setFrozen(boolean frozen) { isFrozen = frozen; }
    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { isDead = dead; }

    public void move() {
        if (!isFrozen && !isDead) {
            int moveAmount = (int)Math.round(speed);
            switch (direction) {
                case UP:
                    y -= moveAmount;
                    break;
                case DOWN:
                    y += moveAmount;
                    break;
                case LEFT:
                    x -= moveAmount;
                    break;
                case RIGHT:
                    x += moveAmount;
                    break;
            }
        }
    }

    public void respawn() {
        this.x = spawnX;
        this.y = spawnY;
        this.state = GhostState.NORMAL;
        this.isScared = false;
        this.isFrozen = false;
        this.isDead = false;
        this.setState(GhostState.SCARED);
    }

    public void becomeScared() {
        isScared = true;
        isFrozen = false;
        isDead = false;
        this.state = GhostState.SCARED;
        this.speed = 0.5; // Optionally slow down when scared
    }

    public void becomeNormal() {
        isScared = false;
        isFrozen = false;
        isDead = false;
        this.state = GhostState.NORMAL;
        this.speed = 1;
    }

    public void freeze() {
        isFrozen = true;
    }

    public void unfreeze() {
        isFrozen = false;
    }

    public void die() {
        this.state = GhostState.DEAD;
        this.isDead = true;
    }

    public void updateTarget(int pacmanX, int pacmanY) {
        // Different targeting behavior based on ghost type
        switch (type) {
            case RED:
                // Direct targeting
                targetX = pacmanX;
                targetY = pacmanY;
                break;
            case PINK:
                // Target 4 spaces ahead of Pacman
                targetX = pacmanX + 4;
                targetY = pacmanY;
                break;
            case BLUE:
                // Target 2 spaces ahead of Pacman
                targetX = pacmanX + 2;
                targetY = pacmanY;
                break;
            case ORANGE:
                // Random targeting when far, direct when close
                if (Math.abs(x - pacmanX) + Math.abs(y - pacmanY) > 8) {
                    targetX = pacmanX;
                    targetY = pacmanY;
                } else {
                    targetX = (int) (Math.random() * 20);
                    targetY = (int) (Math.random() * 20);
                }
                break;
        }
    }

    public void respawn(int x, int y) {
        this.x = x;
        this.y = y;
        this.state = GhostState.NORMAL;
        this.isScared = false;
        this.isFrozen = false;
        this.isDead = false;
    }

    public void becomeReturning() {
        this.state = GhostState.RETURNING;
        this.isScared = false;
        this.isFrozen = false;
        this.isDead = false;
        this.speed = 1; // Move faster when returning
        System.out.println("Ghost at (" + x + "," + y + ") is now RETURNING");
    }

    public boolean isReturning() {
        return this.state == GhostState.RETURNING;
    }
} 