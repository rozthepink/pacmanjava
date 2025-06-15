package models;

public class Fruit {
    public enum FruitType { CHERRY, STRAWBERRY, APPLE, PEACH }
    private int x, y;
    private FruitType type;
    private int points;
    private long spawnTime;
    private static final long DESPAWN_TIME_MS = 10000; // 10 seconds

    public Fruit(int x, int y, FruitType type, int points) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.points = points;
        this.spawnTime = System.currentTimeMillis();
    }
    public int getX() { return x; }
    public int getY() { return y; }
    public FruitType getType() { return type; }
    public int getPoints() { return points; }
    public boolean shouldDespawn() {
        return System.currentTimeMillis() - spawnTime > DESPAWN_TIME_MS;
    }
} 