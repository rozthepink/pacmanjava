package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameBoard {
    private Cell[][] board;
    private int rows;
    private int cols;
    private List<Dot> dots;
    private List<PowerUp> powerUps;
    private int remainingDots;
    private int level;
    private Random random;

    public enum Cell {
        EMPTY,
        WALL,
        DOT,
        POWER_DOT,
        GHOST_SPAWN,
        PACMAN_SPAWN
    }

    public enum MazeType {
        CLASSIC,
        HYBRID,
        BLOCKY,
        RECURSIVE_DIVISION
    }

    private MazeType mazeType = MazeType.HYBRID;

    public GameBoard(int rows, int cols, MazeType mazeType) {
        this.rows = rows;
        this.cols = cols;
        this.board = new Cell[rows][cols];
        this.dots = new ArrayList<>();
        this.powerUps = new ArrayList<>();
        this.remainingDots = 0;
        this.level = 1;
        this.random = new Random();
        this.mazeType = mazeType;
        initializeBoard();
    }

    private void initializeBoard() {
        // Clear dots and reset counter
        dots.clear();
        remainingDots = 0;
        // Fill with walls
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                board[i][j] = Cell.WALL;
            }
        }

        // Select maze generation algorithm
        switch (mazeType) {
            case CLASSIC:
                generateClassicMaze();
                break;
            case HYBRID:
                generateHybridMaze();
                break;
            case BLOCKY:
                generateBlockyMaze();
                break;
            case RECURSIVE_DIVISION:
                generateRecursiveDivisionMaze();
                break;
            default:
                generateHybridMaze();
        }

        // Ensure outer border is walls
        for (int i = 0; i < rows; i++) {
            board[i][0] = Cell.WALL;
            board[i][cols - 1] = Cell.WALL;
        }
        for (int j = 0; j < cols; j++) {
            board[0][j] = Cell.WALL;
            board[rows - 1][j] = Cell.WALL;
        }

        // --- GHOST BASE (HOUSE) ---
        int baseHeight, baseWidth;
        if (rows <= 20 && cols <= 20) {
            baseHeight = 3;
            baseWidth = 3;
        } else {
            baseHeight = 3;
            baseWidth = 5;
        }
        int baseTop = rows / 2 - baseHeight / 2;
        int baseLeft = cols / 2 - baseWidth / 2;
        // Surround with walls
        for (int i = 0; i < baseHeight; i++) {
            for (int j = 0; j < baseWidth; j++) {
                int y = baseTop + i;
                int x = baseLeft + j;
                // Perimeter is wall, inside is empty
                if (i == 0 || i == baseHeight - 1 || j == 0 || j == baseWidth - 1) {
                    board[y][x] = Cell.WALL;
                } else {
                    board[y][x] = Cell.GHOST_SPAWN;
                }
            }
        }
        // Add a door (opening) at the bottom center
        int doorY = baseTop + baseHeight - 1;
        int doorX = baseLeft + baseWidth / 2;
        board[doorY][doorX] = Cell.EMPTY;
        board[doorY][doorX] = Cell.GHOST_SPAWN;
        // Carve a one-cell-wide corridor around the ghost base
        for (int i = -1; i <= baseHeight; i++) {
            for (int j = -1; j <= baseWidth; j++) {
                int y = baseTop + i;
                int x = baseLeft + j;
                boolean isBaseWall = (i >= 0 && i < baseHeight && j >= 0 && j < baseWidth && (i == 0 || i == baseHeight - 1 || j == 0 || j == baseWidth - 1));
                boolean isInsideBase = (i >= 0 && i < baseHeight && j >= 0 && j < baseWidth);
                boolean isOuterBorder = (y == 0 || y == rows - 1 || x == 0 || x == cols - 1);
                if (!isBaseWall && !isInsideBase && !isOuterBorder && y >= 0 && y < rows && x >= 0 && x < cols) {
                    board[y][x] = Cell.EMPTY;
                }
            }
        }

        // Reserve Pacman spawn area at the top
        int pacmanRow = 1;
        int pacmanCol = cols / 2;
        board[pacmanRow][pacmanCol] = Cell.PACMAN_SPAWN;
        board[pacmanRow][pacmanCol - 1] = Cell.PACMAN_SPAWN;
        board[pacmanRow][pacmanCol + 1] = Cell.PACMAN_SPAWN;

        // --- DOT PLACEMENT: Only in reachable cells ---
        // Find Pacman spawn
        boolean[][] reachable = new boolean[rows][cols];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.add(new int[]{pacmanRow, pacmanCol});
        reachable[pacmanRow][pacmanCol] = true;
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int y = pos[0], x = pos[1];
            for (int d = 0; d < 4; d++) {
                int ny = y + dy[d], nx = x + dx[d];
                if (ny >= 0 && ny < rows && nx >= 0 && nx < cols && !reachable[ny][nx]) {
                    if (board[ny][nx] == Cell.EMPTY || board[ny][nx] == Cell.POWER_DOT || board[ny][nx] == Cell.PACMAN_SPAWN) {
                        reachable[ny][nx] = true;
                        queue.add(new int[]{ny, nx});
                    }
                }
            }
        }
        // Place dots only in reachable empty cells ( not on PACMAN_SPAWN)
        for (int i = 1; i < rows - 1; i++) {
            for (int j = 1; j < cols - 1; j++) {
                if (board[i][j] == Cell.EMPTY && reachable[i][j]) {
                    board[i][j] = Cell.DOT;
                    dots.add(new Dot(j, i, Dot.DotType.REGULAR));
                    remainingDots++;
                }
            }
        }
        // Place power dots in the corners (if open and reachable)
        if (board[1][1] == Cell.DOT && reachable[1][1]) {
            board[1][1] = Cell.POWER_DOT;
            dots.add(new Dot(1, 1, Dot.DotType.POWER));
        }
        if (board[1][cols - 2] == Cell.DOT && reachable[1][cols - 2]) {
            board[1][cols - 2] = Cell.POWER_DOT;
            dots.add(new Dot(cols - 2, 1, Dot.DotType.POWER));
        }
        if (board[rows - 2][1] == Cell.DOT && reachable[rows - 2][1]) {
            board[rows - 2][1] = Cell.POWER_DOT;
            dots.add(new Dot(1, rows - 2, Dot.DotType.POWER));
        }
        if (board[rows - 2][cols - 2] == Cell.DOT && reachable[rows - 2][cols - 2]) {
            board[rows - 2][cols - 2] = Cell.POWER_DOT;
            dots.add(new Dot(cols - 2, rows - 2, Dot.DotType.POWER));
        }
    }


    private void generateHybridMaze() {
        // Generate a perfect maze on the left half using DFS
        int halfCols = cols / 2 + 1;
        boolean[][] visited = new boolean[rows][halfCols];
        java.util.Stack<int[]> stack = new java.util.Stack<>();
        int startY = 1 + random.nextInt((rows - 2) / 2) * 2;
        int startX = 1 + random.nextInt((halfCols - 2) / 2) * 2;
        stack.push(new int[]{startY, startX});
        visited[startY][startX] = true;
        board[startY][startX] = Cell.EMPTY;
        int[] dx = {0, 0, 2, -2};
        int[] dy = {2, -2, 0, 0};
        while (!stack.isEmpty()) {
            int[] cell = stack.peek();
            int y = cell[0], x = cell[1];
            java.util.List<Integer> dirs = new java.util.ArrayList<>();
            for (int d = 0; d < 4; d++) {
                int ny = y + dy[d], nx = x + dx[d];
                if (ny > 0 && ny < rows - 1 && nx > 0 && nx < halfCols - 1 && !visited[ny][nx]) {
                    // Skip if inside ghost base area (including door)
                    if (isInGhostBaseOrDoor(ny, nx)) continue;
                    dirs.add(d);
                }
            }
            if (!dirs.isEmpty()) {
                int d = dirs.get(random.nextInt(dirs.size()));
                int ny = y + dy[d], nx = x + dx[d];
                if (isInGhostBaseOrDoor(ny, nx) || isInGhostBaseOrDoor(y + dy[d] / 2, x + dx[d] / 2)) {
                    stack.pop();
                    continue;
                }
                board[y + dy[d] / 2][x + dx[d] / 2] = Cell.EMPTY; // Remove wall between
                board[ny][nx] = Cell.EMPTY;
                visited[ny][nx] = true;
                stack.push(new int[]{ny, nx});
            } else {
                stack.pop();
            }
        }
        // Mirror the left half to the right
        for (int i = 1; i < rows - 1; i++) {
            for (int j = 1; j < halfCols; j++) {
                int mirrorJ = cols - 1 - j;
                if (!isInGhostBaseOrDoor(i, mirrorJ) && !isNearSpawn(i, mirrorJ)) {
                    board[i][mirrorJ] = board[i][j];
                }
            }
        }
        // Randomly remove some walls to add loops (cycles)
        int loops = (rows * cols) / 12;
        for (int n = 0; n < loops; n++) {
            int i = 1 + random.nextInt(rows - 2);
            int j = 1 + random.nextInt(halfCols - 2);
            if (board[i][j] == Cell.WALL && !isInGhostBaseOrDoor(i, j) && !isNearSpawn(i, j)) {
                board[i][j] = Cell.EMPTY;
                int mirrorJ = cols - 1 - j;
                if (!isInGhostBaseOrDoor(i, mirrorJ) && !isNearSpawn(i, mirrorJ)) {
                    board[i][mirrorJ] = Cell.EMPTY;
                }
            }
        }
        // Ensure the door and the cell outside the door are open
        int baseHeight, baseWidth;
        if (rows <= 20 && cols <= 20) {
            baseHeight = 3;
            baseWidth = 3;
        } else {
            baseHeight = 3;
            baseWidth = 5;
        }
        int baseTop = rows / 2 - baseHeight / 2;
        int baseLeft = cols / 2 - baseWidth / 2;
        int doorY = baseTop + baseHeight - 1;
        int doorX = baseLeft + baseWidth / 2;
        board[doorY][doorX] = Cell.EMPTY;
        if (doorY + 1 < rows - 1) {
            board[doorY + 1][doorX] = Cell.EMPTY;
        }
        // Remove dead ends
        removeDeadEnds();
    }

    // Removes dead ends from the maze by carving extra passages
    private void removeDeadEnds() {
        boolean changed;
        do {
            changed = false;
            for (int i = 1; i < rows - 1; i++) {
                for (int j = 1; j < cols - 1; j++) {
                    if (board[i][j] != Cell.EMPTY) continue;
                    if (isInGhostBaseOrDoor(i, j) || isNearSpawn(i, j)) continue;
                    int openCount = 0;
                    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
                    int lastWallX = -1, lastWallY = -1;
                    for (int[] d : dirs) {
                        int ni = i + d[0], nj = j + d[1];
                        if (board[ni][nj] == Cell.EMPTY) openCount++;
                        else if (board[ni][nj] == Cell.WALL) { lastWallX = ni; lastWallY = nj; }
                    }
                    if (openCount == 1 && lastWallX != -1 && lastWallY != -1) {
                        // Carve a passage through the wall
                        board[lastWallX][lastWallY] = Cell.EMPTY;
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    // Returns true if the cell is inside the ghost base or the door
    private boolean isInGhostBaseOrDoor(int i, int j) {
        int centerRow = rows / 2, centerCol = cols / 2;
        int baseHeight, baseWidth;
        if (rows <= 20 && cols <= 20) {
            baseHeight = 3;
            baseWidth = 3;
        } else {
            baseHeight = 3;
            baseWidth = 5;
        }
        int baseTop = centerRow - baseHeight / 2, baseLeft = centerCol - baseWidth / 2;
        // Inside base
        if (i >= baseTop && i < baseTop + baseHeight && j >= baseLeft && j < baseLeft + baseWidth) return true;
        // Door
        int doorY = baseTop + baseHeight - 1;
        int doorX = baseLeft + baseWidth / 2;
        if (i == doorY && j == doorX) return true;
        return false;
    }

    private boolean isNearSpawn(int i, int j) {
        int centerRow = rows / 2, centerCol = cols / 2;
        int baseHeight, baseWidth;
        if (rows <= 20 && cols <= 20) {
            baseHeight = 3;
            baseWidth = 3;
        } else {
            baseHeight = 3;
            baseWidth = 5;
        }
        if (i >= centerRow - baseHeight / 2 && i < centerRow - baseHeight / 2 + baseHeight &&
            j >= centerCol - baseWidth / 2 && j < centerCol - baseWidth / 2 + baseWidth) return true;
        // Pacman spawn area
        int pacmanRow = 1, pacmanCol = cols / 2;
        if (i == pacmanRow && Math.abs(j - pacmanCol) <= 1) return true;
        return false;
    }

    // Getters and setters
    public Cell[][] getBoard() { return board; }
    public void setBoard(Cell[][] board) { this.board = board; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public List<Dot> getDots() { return dots; }
    public List<PowerUp> getPowerUps() { return powerUps; }
    public int getRemainingDots() { return remainingDots; }
    public void setRemainingDots(int remainingDots) { this.remainingDots = remainingDots; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public Cell getCell(int x, int y) {
        if (isValidPosition(x, y)) {
            return board[y][x];
        }
        return Cell.WALL; // Return wall for invalid positions
    }

    public void setCell(int x, int y, Cell cell) {
        if (isValidPosition(x, y)) {
            board[y][x] = cell;
        }
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < cols && y >= 0 && y < rows;
    }

    public void addDot(Dot dot) {
        dots.add(dot);
        remainingDots++;
    }

    public void addPowerUp(PowerUp powerUp) {
        powerUps.add(powerUp);
    }

    public void collectDot(int x, int y) {
        for (Dot dot : dots) {
            if (dot.getX() == x && dot.getY() == y && !dot.isCollected()) {
                dot.setCollected(true);
                remainingDots--;
                break;
            }
        }
    }

    public void collectPowerUp(int x, int y) {
        for (PowerUp powerUp : powerUps) {
            if (powerUp.getX() == x && powerUp.getY() == y && !powerUp.isActive()) {
                powerUp.activate();
                break;
            }
        }
    }

    public boolean isWall(int x, int y) {
        return getCell(x, y) == Cell.WALL;
    }

    public boolean isDot(int x, int y) {
        return getCell(x, y) == Cell.DOT;
    }

    public boolean isPowerDot(int x, int y) {
        return getCell(x, y) == Cell.POWER_DOT;
    }

    public boolean isGhostSpawn(int x, int y) {
        return getCell(x, y) == Cell.GHOST_SPAWN;
    }

    public boolean isPacmanSpawn(int x, int y) {
        return getCell(x, y) == Cell.PACMAN_SPAWN;
    }

    public void nextLevel() {
        level++;
        // Reset board for next level
        initializeBoard();
        dots.clear();
        powerUps.clear();
        remainingDots = 0;
    }


    public int[] generateRandomValidPosition() {
        int x, y;
        boolean validPosition;
        do {
            x = random.nextInt(cols);
            y = random.nextInt(rows);
            validPosition = !isWall(x, y) && hasValidMove(x, y);
        } while (!validPosition);
        return new int[]{x, y};
    }

    private boolean hasValidMove(int x, int y) {
        // Check all four directions for at least one valid move
        return isValidMovePosition(x + 1, y) ||
                isValidMovePosition(x - 1, y) ||
                isValidMovePosition(x, y + 1) ||
                isValidMovePosition(x, y - 1);
    }
    public boolean isValidMovePosition(int x, int y) {
        return isValidPosition(x, y) && !isWall(x, y);
    }


    public List<int[]> getValidMoves(int x, int y) {
        List<int[]> validMoves = new ArrayList<>();
        
        // Check all four directions
        int[][] directions = {
            {x + 1, y},  // Right
            {x - 1, y},  // Left
            {x, y + 1},  // Down
            {x, y - 1}   // Up
        };

        for (int[] dir : directions) {
            if (isValidMovePosition(dir[0], dir[1])) {
                validMoves.add(dir);
            }
        }

        return validMoves;
    }


    public boolean isValidMoveInDirection(int x, int y, Ghost.Direction direction) {
        switch (direction) {
            case RIGHT:
                return isValidMovePosition(x + 1, y);
            case LEFT:
                return isValidMovePosition(x - 1, y);
            case DOWN:
                return isValidMovePosition(x, y + 1);
            case UP:
                return isValidMovePosition(x, y - 1);
            default:
                return false;
        }
    }

    // Stub methods for new maze types
    private void generateClassicMaze() { generateHybridMaze(); } // TODO: Implement classic maze
    private void generateBlockyMaze() { generateHybridMaze(); } // TODO: Implement blocky maze
    private void generateRecursiveDivisionMaze() { generateHybridMaze(); } // TODO: Implement recursive division maze
} 