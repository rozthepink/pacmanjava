package models;

public class Pacman {
    private int x;
    private int y;
    private Direction direction;
    private double speed;
    private int lives;
    private int score;
    private boolean[] activePowerUps;
    private int currentFrame;
    private boolean isAlive;

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public Pacman(int x, int y) {
        this.x = x;
        this.y = y;
        this.direction = Direction.RIGHT;
        this.speed = 1;
        this.lives = 3;
        this.score = 0;
        this.activePowerUps = new boolean[5]; // For 5 different power-up types
        this.currentFrame = 0;
        this.isAlive = true;
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
    public int getLives() { return lives; }
    public void setLives(int lives) { this.lives = lives; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public boolean[] getActivePowerUps() { return activePowerUps; }
    public void setActivePowerUps(boolean[] activePowerUps) { this.activePowerUps = activePowerUps; }
    public int getCurrentFrame() { return currentFrame; }
    public void setCurrentFrame(int currentFrame) { this.currentFrame = currentFrame; }
    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { isAlive = alive; }

    public void move() {
        switch (direction) {
            case UP:
                y -= speed;
                break;
            case DOWN:
                y += speed;
                break;
            case LEFT:
                x -= speed;
                break;
            case RIGHT:
                x += speed;
                break;
        }
    }

    public void addScore(int points) {
        this.score += points;
    }

    public void loseLife() {
        this.lives--;
        if (this.lives <= 0) {
            this.isAlive = false;
        }
    }

    public void activatePowerUp(int powerUpType) {
        if (powerUpType >= 0 && powerUpType < activePowerUps.length) {
            activePowerUps[powerUpType] = true;
        }
    }

    public void deactivatePowerUp(int powerUpType) {
        if (powerUpType >= 0 && powerUpType < activePowerUps.length) {
            activePowerUps[powerUpType] = false;
        }
    }
} 