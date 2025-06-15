package models;

public class Dot {
    private int x;
    private int y;
    private DotType type;
    private boolean isCollected;

    public enum DotType {
        REGULAR,    // Regular dot worth 10 points
        POWER       // Power dot worth 50 points
    }

    public Dot(int x, int y, DotType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.isCollected = false;
    }

    // Getters and setters
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public DotType getType() { return type; }
    public void setType(DotType type) { this.type = type; }
    public boolean isCollected() { return isCollected; }
    public void setCollected(boolean collected) { isCollected = collected; }

    public int getPoints() {
        return type == DotType.REGULAR ? 10 : 50;
    }

    public void reset() {
        this.isCollected = false;
    }
} 