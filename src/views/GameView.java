package views;

import models.GameBoard;
import models.Pacman;
import models.Ghost;
import models.Dot;
import models.PowerUp;
import models.Fruit;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.event.ActionEvent;
import javax.swing.KeyStroke;
import java.util.List;
import java.util.ArrayList;

public class GameView extends JFrame {
    private JTable gameBoard;
    private GameBoardModel boardModel;
    private JLabel scoreLabel;
    private JLabel livesLabel;
    private JLabel timeLabel;
    private JLabel levelLabel;
    private JLabel powerUpLabel;
    private JLabel activePowerUpsLabel;
    private BufferedImage pacmanIdleImage;
    private BufferedImage[][] pacmanAnimImages = new BufferedImage[4][2]; // [direction][frame]
    private int pacmanAnimFrame = 0;
    private Pacman.Direction pacmanAnimDirection = Pacman.Direction.RIGHT;
    private BufferedImage dotImage;
    private BufferedImage powerDotImage;
    private BufferedImage wallImage;
    private BufferedImage scaredGhostImage;
    private BufferedImage[] powerUpImages;  // Array of power-up images
    private Pacman pacman;
    private Ghost[] ghosts;
    private boolean pacmanIsMoving = false;
    private Thread pacmanAnimThread;
    private volatile boolean animRunning = false;
    private BufferedImage[] pacmanDeathFrames = new BufferedImage[14]; // 0=start, 1-11=frame1..frame11
    private volatile boolean pacmanDeathActive = false;
    private volatile int pacmanDeathFrameIdx = 0;
    private volatile int pacmanDeathX = 0, pacmanDeathY = 0;
    public final Object deathAnimLock = new Object();
    private List<PowerUp> powerUps;
    private BufferedImage[] ghostEyesDirectional = new BufferedImage[4]; // right, left, up, down
    // Ghost animation: [color][direction][frame]
    private BufferedImage[][][] ghostAnimImages = new BufferedImage[4][4][2];
    private int ghostAnimFrame = 0;
    private Fruit currentFruit = null;
    private BufferedImage[] fruitImages = new BufferedImage[4]; // cherry, strawberry, apple, peach

    public GameView(int rows, int cols) {
        setTitle("Pacman - Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        // Load images
        try {
            // Idle
            pacmanIdleImage = ImageIO.read(new File("assets/pacman/pacmandefault.png"));
            
            // Animated frames for each direction
            String[] dirs = {"right", "left", "up", "down"};
            for (int d = 0; d < 4; d++) {
                for (int f = 0; f < 2; f++) {
                    pacmanAnimImages[d][f] = ImageIO.read(new File("assets/pacman/" + dirs[d] + "/frame" + (f+1) + ".png"));
                }
            }
            // Ghost animation frames
            String[] colors = {"redghost", "pinkghost", "blueghost", "orangeghost"};
            for (int c = 0; c < 4; c++) {
                for (int d = 0; d < 4; d++) {
                    for (int f = 0; f < 2; f++) {
                        String path = "assets/ghosts/" + colors[c] + "/" + dirs[d] + "/frame" + (f+1) + ".png";
                        ghostAnimImages[c][d][f] = ImageIO.read(new File(path));
                    }
                }
            }
            // Death animation
            pacmanDeathFrames[0] = ImageIO.read(new File("assets/pacman/death/start.png"));
            for (int i = 1; i <= 11; i++) {
                pacmanDeathFrames[i] = ImageIO.read(new File("assets/pacman/death/frame" + i + ".png"));
            }

            dotImage = ImageIO.read(new File("assets/food.png"));
            powerDotImage = ImageIO.read(new File("assets/powerFood.png"));
            wallImage = ImageIO.read(new File("assets/wall.png"));
            scaredGhostImage = ImageIO.read(new File("assets/ghosts/scared/frame1.png")); // Use frame1 for static scared
            // Load directional ghost eyes
            ghostEyesDirectional[0] = ImageIO.read(new File("assets/ghosts/eyes/right/frame1.png"));
            ghostEyesDirectional[1] = ImageIO.read(new File("assets/ghosts/eyes/left/frame1.png"));
            ghostEyesDirectional[2] = ImageIO.read(new File("assets/ghosts/eyes/up/frame1.png"));
            ghostEyesDirectional[3] = ImageIO.read(new File("assets/ghosts/eyes/down/frame1.png"));
            
            // Load power-up images
            powerUpImages = new BufferedImage[6];
            powerUpImages[0] = ImageIO.read(new File("assets/powerups/speed.png"));
            powerUpImages[1] = ImageIO.read(new File("assets/powerups/scare.png"));
            powerUpImages[2] = ImageIO.read(new File("assets/powerups/life.png"));
            powerUpImages[3] = ImageIO.read(new File("assets/powerups/score.png"));
            powerUpImages[4] = ImageIO.read(new File("assets/powerups/freeze.png"));
            powerUpImages[5] = ImageIO.read(new File("assets/powerups/shield.png"));

            // Load fruit images
            fruitImages[0] = ImageIO.read(new File("assets/fruits/cherry.png"));
            fruitImages[1] = ImageIO.read(new File("assets/fruits/strawberry.png"));
            fruitImages[2] = ImageIO.read(new File("assets/fruits/apple.png"));
            fruitImages[3] = ImageIO.read(new File("assets/fruits/peach.png"));

            setIconImage(ImageIO.read(new File("assets/favicon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create game board model
        boardModel = new GameBoardModel(rows, cols);
        gameBoard = new JTable(boardModel);
        gameBoard.setRowHeight(20);
        gameBoard.setIntercellSpacing(new Dimension(0, 0));
        gameBoard.setShowGrid(false);
        gameBoard.setFocusable(true);
        gameBoard.requestFocusInWindow();

        // Set custom cell renderer
        gameBoard.setDefaultRenderer(Object.class, new GameCellRenderer());

        // Set background colors to black
        gameBoard.setBackground(Color.BLACK);
        getContentPane().setBackground(Color.BLACK);

        // Create status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        scoreLabel = new JLabel("Score: 0");
        livesLabel = new JLabel("Lives: 3");
        timeLabel = new JLabel("Time: 0");
        levelLabel = new JLabel("Level: 1");
        powerUpLabel = new JLabel("");
        powerUpLabel.setForeground(Color.YELLOW);
        activePowerUpsLabel = new JLabel("");
        activePowerUpsLabel.setForeground(Color.BLACK);
        statusPanel.add(scoreLabel);
        statusPanel.add(livesLabel);
        statusPanel.add(timeLabel);
        statusPanel.add(levelLabel);
        statusPanel.add(powerUpLabel);
        statusPanel.add(activePowerUpsLabel);

        // Add components to frame
        setLayout(new BorderLayout());
        add(gameBoard, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        // Set frame size based on board size
        int cellSize = 20;
        int width = cols * cellSize + 20;
        int height = rows * cellSize + 100;
        setSize(width, height);
        setLocationRelativeTo(null);

        // Make sure the frame can receive key events
        setFocusable(true);
        requestFocusInWindow();

        // Add component listener to resize cells dynamically
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                resizeGameBoard();
            }
        });

        // Add key binding for Ctrl+Shift+Q to return to main menu
        KeyStroke keyStroke = KeyStroke.getKeyStroke("control shift Q");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "returnToMainMenu");
        getRootPane().getActionMap().put("returnToMainMenu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controllers.GameController.returnToMainMenuStatic();
            }
        });

        // Start Pacman animation thread
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                startPacmanAnimThread();
            }
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                stopPacmanAnimThread();
            }
        });

        powerUps = new ArrayList<>();
    }

    private void resizeGameBoard() {
        int availableWidth = getContentPane().getWidth();
        int availableHeight = getContentPane().getHeight() - 40; // Subtract status panel height
        int cellWidth = availableWidth / boardModel.getColumnCount();
        int cellHeight = availableHeight / boardModel.getRowCount();
        int cellSize = Math.max(10, Math.min(cellWidth, cellHeight));
        gameBoard.setRowHeight(cellSize);
        for (int i = 0; i < gameBoard.getColumnCount(); i++) {
            gameBoard.getColumnModel().getColumn(i).setPreferredWidth(cellSize);
        }
    }

    private class GameCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setIcon(null);
            setText("");
            setBackground(Color.BLACK);
            setBorder(javax.swing.BorderFactory.createEmptyBorder());

            int cellWidth = table.getColumnModel().getColumn(column).getWidth();
            int cellHeight = table.getRowHeight(row);

            // Helper to scale and set icon with nearest-neighbor
            java.util.function.Consumer<BufferedImage> setScaledIcon = img -> {
                if (img != null && cellWidth > 0 && cellHeight > 0) {
                    BufferedImage scaled = new BufferedImage(cellWidth, cellHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = scaled.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2d.drawImage(img, 0, 0, cellWidth, cellHeight, null);
                    g2d.dispose();
                    setIcon(new ImageIcon(scaled));
                }
            };

            // Draw Pacman death animation if active (always takes precedence)
            if (pacmanDeathActive && pacmanDeathX == column && pacmanDeathY == row) {
                setScaledIcon.accept(pacmanDeathFrames[pacmanDeathFrameIdx]);
                return c;
            }
            // Draw Pacman if at this cell (only if not in death animation)
            if (pacman != null && pacman.getX() == column && pacman.getY() == row && !pacmanDeathActive) {
                BufferedImage img = pacmanIdleImage;
                if (pacmanIsMoving && pacman.getDirection() != null) {
                    int dirIdx = 0;
                    switch (pacmanAnimDirection) {
                        case RIGHT: dirIdx = 0; break;
                        case LEFT: dirIdx = 1; break;
                        case UP: dirIdx = 2; break;
                        case DOWN: dirIdx = 3; break;
                    }
                    img = pacmanAnimImages[dirIdx][pacmanAnimFrame % 2];
                }
                setScaledIcon.accept(img);
                return c;
            }
            // Draw ghost if at this cell
            if (ghosts != null) {
                for (int i = 0; i < ghosts.length; i++) {
                    if (ghosts[i] != null && ghosts[i].getX() == column && ghosts[i].getY() == row) {
                        if (ghosts[i].isReturning()) {
                            // Directional ghost eyes
                            int dirIdx = 0;
                            switch (ghosts[i].getDirection()) {
                                case RIGHT: dirIdx = 0; break;
                                case LEFT: dirIdx = 1; break;
                                case UP: dirIdx = 2; break;
                                case DOWN: dirIdx = 3; break;
                            }
                            setScaledIcon.accept(ghostEyesDirectional[dirIdx]);
                        } else if (ghosts[i].isScared()) {
                            // Animate scared ghost
                            int frame = ghostAnimFrame % 2;
                            BufferedImage scaredFrame = null;
                            try {
                                scaredFrame = ImageIO.read(new File("assets/ghosts/scared/frame" + (frame+1) + ".png"));
                            } catch (IOException e) {
                                scaredFrame = scaredGhostImage;
                            }
                            setScaledIcon.accept(scaredFrame);
                        } else {
                            // Animated ghost
                            int colorIdx = 0;
                            switch (ghosts[i].getType()) {
                                case RED: colorIdx = 0; break;
                                case PINK: colorIdx = 1; break;
                                case BLUE: colorIdx = 2; break;
                                case ORANGE: colorIdx = 3; break;
                            }
                            int dirIdx = 0;
                            switch (ghosts[i].getDirection()) {
                                case RIGHT: dirIdx = 0; break;
                                case LEFT: dirIdx = 1; break;
                                case UP: dirIdx = 2; break;
                                case DOWN: dirIdx = 3; break;
                            }
                            int frame = ghostAnimFrame % 2;
                            setScaledIcon.accept(ghostAnimImages[colorIdx][dirIdx][frame]);
                        }
                        return c;
                    }
                }
            }

            // Draw power-ups
            if (gameBoard != null) {
                for (PowerUp powerUp : new ArrayList<>(powerUps)) {
                    if (powerUp.getX() == column && powerUp.getY() == row && !powerUp.isActive()) {
                        setScaledIcon.accept(powerUpImages[powerUp.getType().ordinal()]);
                        return c;
                    }
                }
            }

            // Draw fruit if present
            if (currentFruit != null && currentFruit.getX() == column && currentFruit.getY() == row) {
                int idx = 0;
                switch (currentFruit.getType()) {
                    case CHERRY: idx = 0; break;
                    case STRAWBERRY: idx = 1; break;
                    case APPLE: idx = 2; break;
                    case PEACH: idx = 3; break;
                }
                setScaledIcon.accept(fruitImages[idx]);
                return c;
            }

            if (value instanceof GameBoard.Cell) {
                GameBoard.Cell cell = (GameBoard.Cell) value;
                switch (cell) {
                    case WALL:
                        setScaledIcon.accept(wallImage);
                        break;
                    case DOT:
                        setScaledIcon.accept(dotImage);
                        break;
                    case POWER_DOT:
                        setScaledIcon.accept(powerDotImage);
                        break;
                    default:
                        setIcon(null);
                }
            }

            return c;
        }
    }

    // Custom Table Model
    public class GameBoardModel extends AbstractTableModel {
        private final int rows;
        private final int cols;
        private GameBoard.Cell[][] data;

        public GameBoardModel(int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
            this.data = new GameBoard.Cell[rows][cols];
            initializeBoard();
        }

        private void initializeBoard() {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    data[i][j] = GameBoard.Cell.EMPTY;
                }
            }
        }

        @Override
        public int getRowCount() {
            return rows;
        }

        @Override
        public int getColumnCount() {
            return cols;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data[rowIndex][columnIndex];
        }

        public void setCell(int row, int col, GameBoard.Cell cell) {
            data[row][col] = cell;
            fireTableCellUpdated(row, col);
        }

        public GameBoard.Cell getCell(int row, int col) {
            return data[row][col];
        }
    }

    // Getters and setters
    public JTable getGameBoard() {
        return gameBoard;
    }

    public GameBoardModel getBoardModel() {
        return boardModel;
    }

    public void updateScore(int score) {
        scoreLabel.setText("Score: " + score);
    }

    public void updateLives(int lives) {
        livesLabel.setText("Lives: " + lives);
    }

    public void updateTime(long time) {
        timeLabel.setText("Time: " + time);
    }

    public void updateLevel(int level) {
        levelLabel.setText("Level: " + level);
    }

    public void updatePowerUpStatus(boolean active, int remainingTime) {
        if (active) {
            powerUpLabel.setText("Power-Up: " + remainingTime + "s");
            powerUpLabel.setForeground(Color.YELLOW);
        } else {
            powerUpLabel.setText("");
        }
    }

    public void updatePowerUpStatus(boolean active, int remainingTime, boolean isWarning) {
        if (active) {
            powerUpLabel.setText("Power-Up: " + remainingTime + "s");
            powerUpLabel.setForeground(isWarning ? Color.RED : Color.YELLOW);
        } else {
            powerUpLabel.setText("");
        }
    }

    public void setPacman(Pacman pacman) {
        this.pacman = pacman;
    }

    public void setGhosts(Ghost[] ghosts) {
        this.ghosts = ghosts;
    }

    public void setPacmanAnim(int frame, Pacman.Direction direction, boolean isMoving) {
        this.pacmanAnimFrame = frame;
        this.pacmanAnimDirection = direction;
        this.pacmanIsMoving = isMoving;
    }

    private void startPacmanAnimThread() {
        if (pacmanAnimThread != null && pacmanAnimThread.isAlive()) return;
        animRunning = true;
        pacmanAnimThread = new Thread(() -> {
            while (animRunning) {
                try {
                    Thread.sleep(120); // Animation speed
                } catch (InterruptedException e) {
                    break;
                }
                pacmanAnimFrame = (pacmanAnimFrame + 1) % 2;
                ghostAnimFrame = (ghostAnimFrame + 1) % 2;
                repaint();
            }
        });
        pacmanAnimThread.start();
    }

    private void stopPacmanAnimThread() {
        animRunning = false;
        if (pacmanAnimThread != null) {
            pacmanAnimThread.interrupt();
        }
    }

    // Play Pacman death animation at (x, y), then run callback when done
    public void playPacmanDeathAnimation(int x, int y, Runnable onFinish) {
        new Thread(() -> {
            synchronized (deathAnimLock) {
                pacmanDeathActive = true;
                pacmanDeathX = x;
                pacmanDeathY = y;
                // Pause Pacman animation thread
                boolean wasAnimRunning = animRunning;
                animRunning = false;
                if (pacmanAnimThread != null) pacmanAnimThread.interrupt();
                for (int i = 0; i < pacmanDeathFrames.length; i++) {
                    pacmanDeathFrameIdx = i;
                    repaint();
                    try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                }
                pacmanDeathActive = false;
                pacmanDeathFrameIdx = 0;
                repaint();
                // Resume Pacman animation thread if it was running
                if (wasAnimRunning) startPacmanAnimThread();
            }
            if (onFinish != null) {
                javax.swing.SwingUtilities.invokeLater(onFinish);
            }
        }).start();
    }

    public void setPowerUps(List<PowerUp> powerUps) {
        this.powerUps = powerUps;
        gameBoard.repaint();
    }

    public List<PowerUp> getPowerUps() {
        return powerUps;
    }

    public void updateActivePowerUpsLabel(String text) {
        activePowerUpsLabel.setText(text);
    }

    public void setFruit(Fruit fruit) {
        this.currentFruit = fruit;
        gameBoard.repaint();
    }
} 