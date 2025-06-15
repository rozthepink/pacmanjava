    package controllers;

import models.*;
import views.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import java.awt.GridLayout;
import javax.swing.JComboBox;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class GameController {
    private MainMenuView mainMenuView;
    private GameView gameView;
    private HighScoresView highScoresView;
    private GameBoard gameBoard;
    private Pacman pacman;
    private Ghost[] ghosts;
    private GameUpdateThread gameUpdateThread;
    private int score;
    private int lives;
    private int level;
    private long startTime;
    private boolean isGameRunning;
    private final Object gameLock = new Object();
    private int prevPacmanX, prevPacmanY;
    private int[] prevGhostX = new int[4];
    private int[] prevGhostY = new int[4];
    private boolean firstUpdate = true;

    private GameBoard.MazeType selectedMazeType = GameBoard.MazeType.HYBRID;

    // Power-up timer
    private long powerUpEndTime = 0;
    private static final long POWER_UP_DURATION_MS = 7000;
    private static final long POWER_UP_WARNING_MS = 2000; // Warning when 2 seconds left
    private int powerUpScoreMultiplier = 1;
    private static final int[] GHOST_SCORES = {200, 400, 800, 1600}; // Increasing scores for consecutive ghost eats

    // Ghost power-up dropping system
    private static final long GHOST_POWERUP_INTERVAL = 5000; // 5 seconds
    private static final double GHOST_POWERUP_CHANCE = 0.25; // 25% chance
    private Map<Ghost, Long> ghostPowerUpTimers;
    private List<PowerUp> activePowerUps;

    private int pacmanAnimFrame = 0;

    private boolean pacmanIsMoving = false;

    // Add invincibility state
    private boolean pacmanInvincible = false;
    private long invincibilityEndTime = 0;
    private static final long INVINCIBILITY_DURATION_MS = 7000;

    // Fruit bonus system
    private Fruit currentFruit = null;
    private int nextFruitIndex = 0;
    private static class FruitSpawnRule {
        int scoreThreshold;
        Fruit.FruitType type;
        int points;
        FruitSpawnRule(int scoreThreshold, Fruit.FruitType type, int points) {
            this.scoreThreshold = scoreThreshold;
            this.type = type;
            this.points = points;
        }
    }
    private static final FruitSpawnRule[] fruitRules = new FruitSpawnRule[] {
        new FruitSpawnRule(1000, Fruit.FruitType.CHERRY, 100),
        new FruitSpawnRule(3000, Fruit.FruitType.STRAWBERRY, 300),
        new FruitSpawnRule(5000, Fruit.FruitType.APPLE, 500),
        new FruitSpawnRule(7000, Fruit.FruitType.PEACH, 700)
    };

    public static GameController instance;

    private Pacman.Direction desiredDirection = Pacman.Direction.RIGHT;

    public GameController() {
        instance = this;
        // Initialize views
        mainMenuView = new MainMenuView();
        highScoresView = new HighScoresView();

        // Add action listeners to main menu buttons
        mainMenuView.getNewGameButton().addActionListener(e -> startNewGame());
        mainMenuView.getHighScoresButton().addActionListener(e -> showHighScores());
        mainMenuView.getExitButton().addActionListener(e -> System.exit(0));

        // Add action listener to high scores return button
        highScoresView.addReturnButtonListener(e -> returnToMainMenu());

        // Show main menu initially
        mainMenuView.setVisible(true);

        this.ghostPowerUpTimers = new HashMap<>();
        this.activePowerUps = new ArrayList<>();
    }

    private void startNewGame() {
        // Stop any existing game loop before starting a new one
        stopGameLoop();
        // Hide main menu
        mainMenuView.setVisible(false);

        // Set to force full repaint on new game
        firstUpdate = true;

        // Show board size selection dialog
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTextField rowsField = new JTextField("20");
        JTextField colsField = new JTextField("20");
        panel.add(new JLabel("Rows:"));
        panel.add(rowsField);
        panel.add(new JLabel("Columns:"));
        panel.add(colsField);

        // Maze type selection
        String[] mazeTypes = {"Classic Maze", "Hybrid Maze", "Blocky Maze", "Recursive Division Maze"};
        JComboBox<String> mazeTypeBox = new JComboBox<>(mazeTypes);
        mazeTypeBox.setEnabled(false);
        panel.add(new JLabel("Maze Type:"));
        panel.add(mazeTypeBox);

        int result = JOptionPane.showConfirmDialog(
            null,
            panel,
            "Select Board Size and Maze Type",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            mainMenuView.setVisible(true);
            return;
        }

        // Parse board size
        int rows, cols;
        try {
            rows = Integer.parseInt(rowsField.getText());
            cols = Integer.parseInt(colsField.getText());
            if (rows < 10 || cols < 10 || rows > 100 || cols > 100) {
                JOptionPane.showMessageDialog(
                    null,
                    "Board size must be between 10 and 100",
                    "Invalid Size",
                    JOptionPane.ERROR_MESSAGE
                );
                mainMenuView.setVisible(true);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                null,
                "Please enter valid numbers",
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE
            );
            mainMenuView.setVisible(true);
            return;
        }

        // Get maze type
        int mazeTypeIndex = mazeTypeBox.getSelectedIndex();
        switch (mazeTypeIndex) {
            case 0: selectedMazeType = GameBoard.MazeType.CLASSIC; break;
            case 1: selectedMazeType = GameBoard.MazeType.HYBRID; break;
            case 2: selectedMazeType = GameBoard.MazeType.BLOCKY; break;
            case 3: selectedMazeType = GameBoard.MazeType.RECURSIVE_DIVISION; break;
            default: selectedMazeType = GameBoard.MazeType.HYBRID;
        }

        // Initialize game state
        score = 0;
        lives = 3;
        level = 1;
        isGameRunning = true;

        // Create game board with selected size and maze type
        gameBoard = new GameBoard(rows, cols, selectedMazeType);
        
        // Find Pacman spawn position
        int[] pacmanPos = findSpawnPosition(GameBoard.Cell.PACMAN_SPAWN);
        pacman = new Pacman(pacmanPos[0], pacmanPos[1]);

        // Determine number of ghosts based on map size
        int ghostCount;
        if (rows <= 10 || cols <= 10) {
            ghostCount = 1;
        } else if (rows <= 20 || cols <= 20) {
            ghostCount = 2;
        } else if (rows <= 30 || cols <= 30) {
            ghostCount = 3;
        } else {
            ghostCount = 4;
        }

        // Find ghost spawn positions (anywhere on the board)
        List<int[]> ghostSpawns = findGhostSpawnPositions();
        ghosts = new Ghost[ghostCount];
        Ghost.GhostType[] ghostTypes = {
            Ghost.GhostType.RED,
            Ghost.GhostType.PINK,
            Ghost.GhostType.BLUE,
            Ghost.GhostType.ORANGE
        };
        for (int i = 0; i < ghostCount && i < ghostSpawns.size(); i++) {
            int[] ghostPos = ghostSpawns.get(i);
            ghosts[i] = new Ghost(ghostPos[0], ghostPos[1], ghostTypes[i % ghostTypes.length]);
        }

        // Initialize ghost power-up timers
        ghostPowerUpTimers.clear();
        activePowerUps.clear();
        for (Ghost ghost : ghosts) {
            ghostPowerUpTimers.put(ghost, System.currentTimeMillis());
        }

        // Create and show game view
        gameView = new GameView(gameBoard.getRows(), gameBoard.getCols());
        gameView.setVisible(true);

        // Add key listener for Pacman movement
        gameView.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (!isGameRunning) return;
                synchronized (gameLock) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP:
                            desiredDirection = Pacman.Direction.UP;
                            break;
                        case KeyEvent.VK_DOWN:
                            desiredDirection = Pacman.Direction.DOWN;
                            break;
                        case KeyEvent.VK_LEFT:
                            desiredDirection = Pacman.Direction.LEFT;
                            break;
                        case KeyEvent.VK_RIGHT:
                            desiredDirection = Pacman.Direction.RIGHT;
                            break;
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });

        // Start game timer
        startTime = System.currentTimeMillis();
        startGameLoop();

        currentFruit = null;
        nextFruitIndex = 0;
        gameView.setFruit(null);

        desiredDirection = Pacman.Direction.RIGHT;
    }

    private int[] findSpawnPosition(GameBoard.Cell spawnType) {
        for (int i = 0; i < gameBoard.getRows(); i++) {
            for (int j = 0; j < gameBoard.getCols(); j++) {
                if (gameBoard.getCell(j, i) == spawnType) {
                    return new int[]{j, i};
                }
            }
        }
        // Fallback to random position if spawn not found
        return gameBoard.generateRandomValidPosition();
    }

    // Find all positions marked as GHOST_SPAWN anywhere on the board
    private List<int[]> findGhostSpawnPositions() {
        List<int[]> positions = new ArrayList<>();
        for (int y = 0; y < gameBoard.getRows(); y++) {
            for (int x = 0; x < gameBoard.getCols(); x++) {
                if (gameBoard.getCell(x, y) == GameBoard.Cell.GHOST_SPAWN) {
                    positions.add(new int[]{x, y});
                }
            }
        }
        return positions;
    }

    private void startGameLoop() {
        gameUpdateThread = new GameUpdateThread();
        gameUpdateThread.start();
    }

    private class GameUpdateThread extends Thread {
        private static final long UPDATE_INTERVAL = 100; // 100ms between updates

        @Override
        public void run() {
            while (true) {
                long startUpdate = System.currentTimeMillis();

                if (!isGameRunning) {
                    try {
                        Thread.sleep(UPDATE_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                synchronized (gameLock) {
                    // Update game state
                    updateGameState();

                    // Check for game over conditions
                    if (lives <= 0) {
                        gameOver();
                        break;
                    }
                }

                // Calculate sleep time to maintain consistent update interval
                long updateTime = System.currentTimeMillis() - startUpdate;
                long sleepTime = Math.max(0, UPDATE_INTERVAL - updateTime);

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void updateGameState() {
        long currentTime = System.currentTimeMillis();

        // Track previous positions
        int oldPacmanX = pacman.getX();
        int oldPacmanY = pacman.getY();
        prevPacmanX = oldPacmanX;
        prevPacmanY = oldPacmanY;
        for (int i = 0; i < ghosts.length; i++) {
            if (ghosts[i] != null) {
                prevGhostX[i] = ghosts[i].getX();
                prevGhostY[i] = ghosts[i].getY();
            }
        }

        // Move Pacman
        pacmanIsMoving = false;
        int moveSteps = (int)Math.round(pacman.getSpeed());
        for (int step = 0; step < moveSteps; step++) {
            // Try to turn if possible
            if (gameBoard.isValidMoveInDirection(pacman.getX(), pacman.getY(), Ghost.Direction.valueOf(desiredDirection.name()))) {
                pacman.setDirection(desiredDirection);
            }
            // Move in current direction if possible
            if (gameBoard.isValidMoveInDirection(pacman.getX(), pacman.getY(), Ghost.Direction.valueOf(pacman.getDirection().name()))) {
                int oldX = pacman.getX();
                int oldY = pacman.getY();
                pacman.move();
                if (pacman.getX() != oldX || pacman.getY() != oldY) {
                    pacmanIsMoving = true;
                }
            } else {
                // Can't move in current or desired direction, so stop
                break;
            }
        }

        // Animate Pacman
        pacmanAnimFrame = (pacmanAnimFrame + 1) % 2;
        gameView.setPacmanAnim(pacmanAnimFrame, pacman.getDirection(), pacmanIsMoving);

        // Move ghosts and check for power-up drops
        for (int i = 0; i < ghosts.length; i++) {
            Ghost ghost = ghosts[i];
            if (ghost == null) continue;

            // Handle returning ghosts
            if (ghost.isReturning()) {
                int[] base = getGhostBaseDoor();
                java.util.List<int[]> path = findPath(ghost.getX(), ghost.getY(), base[0], base[1]);
                if (!path.isEmpty()) {
                    int[] next = path.get(0);
                    int dx = next[0] - ghost.getX();
                    int dy = next[1] - ghost.getY();
                    if (dx == 1) ghost.setDirection(Ghost.Direction.RIGHT);
                    else if (dx == -1) ghost.setDirection(Ghost.Direction.LEFT);
                    else if (dy == 1) ghost.setDirection(Ghost.Direction.DOWN);
                    else if (dy == -1) ghost.setDirection(Ghost.Direction.UP);
                    ghost.move();
                }
                // If at base, respawn as normal
                if (ghost.getX() == base[0] && ghost.getY() == base[1]) {
                    System.out.println("Ghost at base, respawning at (" + base[0] + "," + base[1] + ")");
                    ghost.respawn(base[0], base[1]);
                }
                continue;
            }

            // Check for ghost power-up drops
            if (!ghost.isDead() && !ghost.isScared() && !ghost.isFrozen()) {
                long lastDrop = ghostPowerUpTimers.getOrDefault(ghost, 0L);
                if (currentTime - lastDrop >= GHOST_POWERUP_INTERVAL) {
                    if (Math.random() < GHOST_POWERUP_CHANCE) {
                        // Create a random power-up at ghost's position
                        PowerUp.PowerUpType type = PowerUp.PowerUpType.values()[(int)(Math.random() * PowerUp.PowerUpType.values().length)];
                        PowerUp powerUp = new PowerUp(ghost.getX(), ghost.getY(), type);
                        activePowerUps.add(powerUp);
                        System.out.println("PowerUp spawned at: " + powerUp.getX() + "," + powerUp.getY() + " type: " + powerUp.getType());
                        gameView.setPowerUps(activePowerUps);
                        gameBoard.addPowerUp(powerUp);
                    }
                    ghostPowerUpTimers.put(ghost, currentTime);
                }
            }

            if (isGhostInBase(ghost)) {
                // If not at the door, force move down toward the door
                int[] door = getGhostBaseDoor();
                if (ghost.getX() == door[0] && ghost.getY() == door[1]) {
                    // At the door, allow normal movement
                } else {
                    // Move down if possible
                    if (gameBoard.isValidMoveInDirection(ghost.getX(), ghost.getY(), Ghost.Direction.DOWN)) {
                        ghost.setDirection(Ghost.Direction.DOWN);
                        ghost.move();
                        continue;
                    }
                }
            }
            if (gameBoard.isValidMoveInDirection(ghost.getX(), ghost.getY(), ghost.getDirection())) {
                ghost.move();
            } else {
                // Choose new direction if current one is blocked
                List<int[]> validMoves = gameBoard.getValidMoves(ghost.getX(), ghost.getY());
                if (!validMoves.isEmpty()) {
                    int[] newPos = validMoves.get((int) (Math.random() * validMoves.size()));
                    // Determine direction based on the new position
                    if (newPos[0] > ghost.getX()) ghost.setDirection(Ghost.Direction.RIGHT);
                    else if (newPos[0] < ghost.getX()) ghost.setDirection(Ghost.Direction.LEFT);
                    else if (newPos[1] > ghost.getY()) ghost.setDirection(Ghost.Direction.DOWN);
                    else if (newPos[1] < ghost.getY()) ghost.setDirection(Ghost.Direction.UP);
                }
            }
        }

        // Update active power-ups
        Iterator<PowerUp> iterator = activePowerUps.iterator();
        boolean removedAny = false;
        boolean freezeExpired = false;
        while (iterator.hasNext()) {
            PowerUp powerUp = iterator.next();
            if (powerUp.shouldDespawn()) {
                iterator.remove();
                gameBoard.getPowerUps().remove(powerUp);
                removedAny = true;
                continue;
            }
            if (powerUp.isActive()) {
                powerUp.updateDuration(100); // Update every 100ms
                if (!powerUp.isActive()) {
                    if (powerUp.getType() == PowerUp.PowerUpType.GHOST_FREEZE) {
                        freezeExpired = true;
                    }
                    iterator.remove();
                    gameBoard.getPowerUps().remove(powerUp);
                    removedAny = true;
                }
            }
        }
        if (removedAny) {
            gameView.setPowerUps(activePowerUps);
        }
        if (freezeExpired) {
            for (Ghost ghost : ghosts) {
                if (ghost != null) ghost.unfreeze();
            }
        }

        // Handle power-up timer
        if (powerUpEndTime > 0) {
            long remainingTime = powerUpEndTime - currentTime;
            if (remainingTime <= 0) {
                // Power-up ended
                for (Ghost ghost : ghosts) {
                    if (ghost != null) ghost.becomeNormal();
                }
                powerUpEndTime = 0;
                powerUpScoreMultiplier = 1;
                gameView.updatePowerUpStatus(false, 0);
            } else {
                // Update power-up status display
                boolean isWarning = remainingTime <= POWER_UP_WARNING_MS;
                gameView.updatePowerUpStatus(true, (int)(remainingTime / 1000) + 1, isWarning);
            }
        }

        // Handle invincibility timer
        if (pacmanInvincible) {
            long remaining = invincibilityEndTime - System.currentTimeMillis();
            if (remaining <= 0) {
                pacmanInvincible = false;
                gameView.updateActivePowerUpsLabel("");
                gameView.updatePowerUpStatus(false, 0);
            } else {
                boolean isWarning = remaining <= POWER_UP_WARNING_MS;
                gameView.updatePowerUpStatus(true, (int)(remaining / 1000) + 1, isWarning);
            }
        }

        // Fruit spawn logic
        if (nextFruitIndex < fruitRules.length && score >= fruitRules[nextFruitIndex].scoreThreshold && currentFruit == null) {
            // Spawn fruit at a random valid position
            int[] pos = gameBoard.generateRandomValidPosition();
            int fx = pos[0];
            int fy = pos[1];
            FruitSpawnRule rule = fruitRules[nextFruitIndex];
            currentFruit = new Fruit(fx, fy, rule.type, rule.points);
            gameView.setFruit(currentFruit);
            nextFruitIndex++;
            if (nextFruitIndex == fruitRules.length) nextFruitIndex = 0;
        }
        // Remove fruit if expired
        if (currentFruit != null && currentFruit.shouldDespawn()) {
            currentFruit = null;
            gameView.setFruit(null);
        }

        // Check for collisions
        checkCollisions();

        // Update view
        updateView();

        if (gameBoard.getRemainingDots() == 0) {
            level++;
            gameBoard = new GameBoard(gameBoard.getRows(), gameBoard.getCols(), selectedMazeType);
            // Find Pacman spawn position
            int[] pacmanPos = findSpawnPosition(GameBoard.Cell.PACMAN_SPAWN);
            pacman.setX(pacmanPos[0]);
            pacman.setY(pacmanPos[1]);
            // Find ghost spawn positions
            List<int[]> ghostSpawns = findGhostSpawnPositions();
            for (int i = 0; i < ghosts.length && i < ghostSpawns.size(); i++) {
                int[] ghostPos = ghostSpawns.get(i);
                ghosts[i].respawn(ghostPos[0], ghostPos[1]);
            }
            // Reset power-ups and timers
            activePowerUps.clear();
            ghostPowerUpTimers.clear();
            for (Ghost ghost : ghosts) {
                ghostPowerUpTimers.put(ghost, System.currentTimeMillis());
            }
            gameView.setPowerUps(activePowerUps);
            firstUpdate = true;
            // Reset timers and power-up status
            powerUpEndTime = 0;
            pacmanInvincible = false;
            gameView.updateActivePowerUpsLabel("");
            gameView.updatePowerUpStatus(false, 0);
            // Reset fruit sequence for new level
            currentFruit = null;
            nextFruitIndex = 0;
            gameView.setFruit(null);
        }
    }

    // Returns true if the ghost is inside the base (not just at the door)
    private boolean isGhostInBase(Ghost ghost) {
        int centerRow = gameBoard.getRows() / 2, centerCol = gameBoard.getCols() / 2;
        int baseHeight, baseWidth;
        if (gameBoard.getRows() <= 20 && gameBoard.getCols() <= 20) {
            baseHeight = 3;
            baseWidth = 3;
        } else {
            baseHeight = 3;
            baseWidth = 5;
        }
        int baseTop = centerRow - baseHeight / 2, baseLeft = centerCol - baseWidth / 2;
        int x = ghost.getX(), y = ghost.getY();
        int doorY = baseTop + baseHeight - 1;
        int doorX = baseLeft + baseWidth / 2;
        return (y >= baseTop + 1 && y < baseTop + baseHeight - 1 && x >= baseLeft + 1 && x < baseLeft + baseWidth - 1)
            || (y == doorY && x == doorX);
    }

    // Returns the coordinates of the door of the ghost base
    private int[] getGhostBaseDoor() {
        int centerRow = gameBoard.getRows() / 2, centerCol = gameBoard.getCols() / 2;
        int baseHeight, baseWidth;
        if (gameBoard.getRows() <= 20 && gameBoard.getCols() <= 20) {
            baseHeight = 3;
            baseWidth = 3;
        } else {
            baseHeight = 3;
            baseWidth = 5;
        }
        int baseTop = centerRow - baseHeight / 2, baseLeft = centerCol - baseWidth / 2;
        int doorY = baseTop + baseHeight - 1;
        int doorX = baseLeft + baseWidth / 2;
        return new int[]{doorX, doorY};
    }

    private void checkCollisions() {
        // Check for dot collection
        if (gameBoard.getCell(pacman.getX(), pacman.getY()) == GameBoard.Cell.DOT) {
            score += 10;
            gameBoard.setCell(pacman.getX(), pacman.getY(), GameBoard.Cell.EMPTY);
            gameBoard.collectDot(pacman.getX(), pacman.getY());
        }
        // Check for power dot collection
        else if (gameBoard.getCell(pacman.getX(), pacman.getY()) == GameBoard.Cell.POWER_DOT) {
            score += 50;
            gameBoard.setCell(pacman.getX(), pacman.getY(), GameBoard.Cell.EMPTY);
            gameBoard.collectDot(pacman.getX(), pacman.getY());
            // Power-up effect: set all ghosts to SCARED
            for (Ghost ghost : ghosts) {
                if (ghost != null) ghost.becomeScared();
            }
            powerUpEndTime = System.currentTimeMillis() + POWER_UP_DURATION_MS;
            powerUpScoreMultiplier = 1; // Reset multiplier when new power-up is collected
        }

        // Check for power-up collection
        boolean collected = false;
        for (PowerUp powerUp : activePowerUps) {
            if (!powerUp.isActive() && pacman.getX() == powerUp.getX() && pacman.getY() == powerUp.getY()) {
                powerUp.activate();
                applyPowerUpEffect(powerUp);
                collected = true;
                break;
            }
        }
        if (collected) {
            gameView.setPowerUps(activePowerUps);
        }

        // Check for ghost collisions (same cell)
        for (int i = 0; i < ghosts.length; i++) {
            Ghost ghost = ghosts[i];
            if (ghost == null) continue;
            if (ghost.isReturning()) continue; // Ignore returning ghosts
            if (pacman.getX() == ghost.getX() && pacman.getY() == ghost.getY()) {
                if (pacmanInvincible) {
                    // Ignore collision
                    continue;
                }
                if (ghost.isScared()) {
                    // Set ghost to RETURNING state
                    ghost.becomeReturning();
                    powerUpScoreMultiplier = Math.min(powerUpScoreMultiplier + 1, GHOST_SCORES.length);
                } else {
                    handlePacmanDeath();
                    break;
                }
            }
        }
        // Check for position swaps (Pacman and ghost swapped places in one tick)
        for (int i = 0; i < ghosts.length; i++) {
            Ghost ghost = ghosts[i];
            if (ghost == null) continue;
            if (ghost.isReturning()) continue; // Ignore returning ghosts
            if (pacman.getX() == prevGhostX[i] && pacman.getY() == prevGhostY[i] &&
                prevPacmanX == ghost.getX() && prevPacmanY == ghost.getY()) {
                if (pacmanInvincible) {
                    // Ignore collision
                    continue;
                }
                if (ghost.isScared()) {
                    int ghostScore = GHOST_SCORES[Math.min(powerUpScoreMultiplier - 1, GHOST_SCORES.length - 1)];
                    score += ghostScore;
                    powerUpScoreMultiplier = Math.min(powerUpScoreMultiplier + 1, GHOST_SCORES.length);
                    ghost.becomeReturning();
                } else {
                    handlePacmanDeath();
                    break;
                }
            }
        }

        // Check for fruit collection
        if (currentFruit != null && pacman.getX() == currentFruit.getX() && pacman.getY() == currentFruit.getY()) {
            score += currentFruit.getPoints();
            // Optionally: show a message or play a sound
            currentFruit = null;
            gameView.setFruit(null);
        }
    }

    private void applyPowerUpEffect(PowerUp powerUp) {
        switch (powerUp.getType()) {
            case SPEED_BOOST:
                System.err.println("Speed boost");
                break;
            case GHOST_SCARE:
                for (Ghost ghost : ghosts) {
                    if (ghost != null) ghost.becomeScared();
                }
                powerUpEndTime = System.currentTimeMillis() + POWER_UP_DURATION_MS;
                break;
            case EXTRA_LIFE:
                lives++;
                gameView.updateLives(lives);
                break;
            case SCORE_MULTIPLIER:
                powerUpScoreMultiplier = 2;
                break;
            case GHOST_FREEZE:
                for (Ghost ghost : ghosts) {
                    if (ghost != null) ghost.freeze();
                }
                break;
            case INVINCIBILITY:
                pacmanInvincible = true;
                invincibilityEndTime = System.currentTimeMillis() + INVINCIBILITY_DURATION_MS;
                break;
        }
    }

    // Handles Pacman death animation and respawn
    private void handlePacmanDeath() {
        lives--;
        isGameRunning = false;
        // Hide ghosts
        Ghost[] oldGhosts = ghosts.clone();
        ghosts = new Ghost[ghosts.length];
        gameView.setGhosts(ghosts);
        gameView.repaint();
        // Play death animation and respawn after animation finishes
        gameView.playPacmanDeathAnimation(pacman.getX(), pacman.getY(), () -> {
            // Respawn Pacman
            int[] pacmanPos = gameBoard.generateRandomValidPosition();
            pacman.setX(pacmanPos[0]);
            pacman.setY(pacmanPos[1]);
            // Respawn ghosts
            List<int[]> ghostSpawns = findGhostSpawnPositions();
            for (int i = 0; i < ghosts.length && i < ghostSpawns.size(); i++) {
                int[] ghostPos = ghostSpawns.get(i);
                oldGhosts[i].respawn(ghostPos[0], ghostPos[1]);
            }
            ghosts = oldGhosts;
            gameView.setGhosts(ghosts);
            isGameRunning = true;
        });
    }

    private void updateView() {
        // Update score, lives, and time
        gameView.updateScore(score);
        gameView.updateLives(lives);
        gameView.updateTime((System.currentTimeMillis() - startTime) / 1000);
        gameView.updateLevel(level);

        // Update Pacman and ghosts in the view
        gameView.setPacman(pacman);
        gameView.setGhosts(ghosts);

        GameView.GameBoardModel boardModel = gameView.getBoardModel();
        if (firstUpdate) {
            // On first update, update the whole board
            for (int i = 0; i < gameBoard.getRows(); i++) {
                for (int j = 0; j < gameBoard.getCols(); j++) {
                    boardModel.setCell(i, j, gameBoard.getCell(j, i));
                }
            }
            firstUpdate = false;
        } else {
            // Only update changed cells
            // Pacman
            boardModel.setCell(prevPacmanY, prevPacmanX, gameBoard.getCell(prevPacmanX, prevPacmanY));
            boardModel.setCell(pacman.getY(), pacman.getX(), gameBoard.getCell(pacman.getX(), pacman.getY()));
            // Ghosts
            for (int i = 0; i < ghosts.length; i++) {
                if (ghosts[i] == null) continue;
                boardModel.setCell(prevGhostY[i], prevGhostX[i], gameBoard.getCell(prevGhostX[i], prevGhostY[i]));
                boardModel.setCell(ghosts[i].getY(), ghosts[i].getX(), gameBoard.getCell(ghosts[i].getX(), ghosts[i].getY()));
            }
        }

        // After updating power-up durations and handling expirations, update the label
        StringBuilder activePowerUpsText = new StringBuilder("Active: ");
        boolean anyActive = false;
        for (PowerUp powerUp : activePowerUps) {
            if (powerUp.isActive()) {
                PowerUp.PowerUpType type = powerUp.getType();
                if (type == PowerUp.PowerUpType.EXTRA_LIFE || type == PowerUp.PowerUpType.SPEED_BOOST) continue;
                activePowerUpsText.append(type.name().replace('_', ' ').toLowerCase()).append(", ");
                anyActive = true;
            }
        }
        if (pacmanInvincible) {
            activePowerUpsText.append("invincibility, ");
            anyActive = true;
        }
        if (anyActive) {
            // Remove trailing comma and space
            if (activePowerUpsText.length() > 8)
                activePowerUpsText.setLength(activePowerUpsText.length() - 2);
            gameView.updateActivePowerUpsLabel(activePowerUpsText.toString());
        } else {
            gameView.updateActivePowerUpsLabel("");
        }
    }

    private void gameOver() {
        isGameRunning = false;
        gameView.dispose();
        // Prompt for nickname and ensure uniqueness
        String nickname;
        do {
            nickname = JOptionPane.showInputDialog(null, "Game Over! Enter your nickname for the high score (must be unique):", "Game Over", JOptionPane.PLAIN_MESSAGE);
            if (nickname == null) {
                nickname = "Player";
                break;
            }
            nickname = nickname.trim();
            if (nickname.isEmpty()) {
                nickname = "Player";
                break;
            }
            if (highScoresView.playerNameExists(nickname)) {
                JOptionPane.showMessageDialog(null, "That name is already taken. Please enter a different nickname.", "Name Not Unique", JOptionPane.WARNING_MESSAGE);
            }
        } while (highScoresView.playerNameExists(nickname));
        // Add score to high scores
        highScoresView.addScore(1, nickname, score, level, 
            new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
        // Show high scores
        highScoresView.setVisible(true);
    }

    private void showHighScores() {
        mainMenuView.setVisible(false);
        highScoresView.setVisible(true);
    }

    private void returnToMainMenu() {
        // Hide or dispose of the game view if it exists
        if (gameView != null && gameView.isVisible()) {
            gameView.dispose();
            isGameRunning = false;
        }
        stopGameLoop();
        highScoresView.setVisible(false);
        mainMenuView.setVisible(true);
    }

    public static void returnToMainMenuStatic() {
        if (instance != null) {
            instance.returnToMainMenu();
        }
    }

    // Helper for BFS pathfinding for returning ghosts
    private List<int[]> findPath(int startX, int startY, int goalX, int goalY) {
        int rows = gameBoard.getRows();
        int cols = gameBoard.getCols();
        boolean[][] visited = new boolean[cols][rows];
        int[][][] prev = new int[cols][rows][2];
        for (int x = 0; x < cols; x++) for (int y = 0; y < rows; y++) prev[x][y][0] = prev[x][y][1] = -1;
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            if (curr[0] == goalX && curr[1] == goalY) break;
            for (int[] d : dirs) {
                int nx = curr[0] + d[0], ny = curr[1] + d[1];
                if (nx >= 0 && ny >= 0 && nx < cols && ny < rows && !visited[nx][ny]
                    && gameBoard.isValidMoveInDirection(curr[0], curr[1], d[0] == 1 ? Ghost.Direction.RIGHT : d[0] == -1 ? Ghost.Direction.LEFT : d[1] == 1 ? Ghost.Direction.DOWN : Ghost.Direction.UP)) {
                    queue.add(new int[]{nx, ny});
                    visited[nx][ny] = true;
                    prev[nx][ny][0] = curr[0];
                    prev[nx][ny][1] = curr[1];
                }
            }
        }
        // Reconstruct path
        java.util.List<int[]> path = new java.util.ArrayList<>();
        int cx = goalX, cy = goalY;
        if (!visited[goalX][goalY]) return path; // No path
        while (cx != startX || cy != startY) {
            path.add(0, new int[]{cx, cy});
            int px = prev[cx][cy][0], py = prev[cx][cy][1];
            cx = px; cy = py;
        }
        return path;
    }

    private void stopGameLoop() {
        if (gameUpdateThread != null && gameUpdateThread.isAlive()) {
            gameUpdateThread.interrupt();
            try {
                gameUpdateThread.join(200); // Wait briefly for thread to stop
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        gameUpdateThread = null;
    }
} 