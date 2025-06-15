package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private State currentState;
    private int currentLevel;
    private int totalScore;
    private List<HighScore> highScores;
    private long gameTime;
    private boolean isPaused;

    public enum State {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    public static class HighScore implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String playerName;
        private int score;
        private long date;

        public HighScore(String playerName, int score) {
            this.playerName = playerName;
            this.score = score;
            this.date = System.currentTimeMillis();
        }

        // Getters
        public String getPlayerName() { return playerName; }
        public int getScore() { return score; }
        public long getDate() { return date; }
    }

    public GameState() {
        this.currentState = State.MENU;
        this.currentLevel = 1;
        this.totalScore = 0;
        this.highScores = new ArrayList<>();
        this.gameTime = 0;
        this.isPaused = false;
    }

    // Getters and setters
    public State getCurrentState() { return currentState; }
    public void setCurrentState(State currentState) { this.currentState = currentState; }
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public List<HighScore> getHighScores() { return highScores; }
    public long getGameTime() { return gameTime; }
    public void setGameTime(long gameTime) { this.gameTime = gameTime; }
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { isPaused = paused; }

    public void addScore(int points) {
        this.totalScore += points;
    }

    public void addHighScore(String playerName, int score) {
        HighScore newScore = new HighScore(playerName, score);
        highScores.add(newScore);
        // Sort high scores in descending order
        highScores.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        // Keep only top 10 scores
        if (highScores.size() > 10) {
            highScores = highScores.subList(0, 10);
        }
    }

    public void nextLevel() {
        currentLevel++;
    }

    public void reset() {
        currentState = State.MENU;
        currentLevel = 1;
        totalScore = 0;
        gameTime = 0;
        isPaused = false;
    }

    public void updateGameTime(long deltaTime) {
        if (!isPaused && currentState == State.PLAYING) {
            gameTime += deltaTime;
        }
    }
} 