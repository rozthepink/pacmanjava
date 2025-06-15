package views;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.KeyStroke;
import java.io.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class HighScoresView extends JFrame {
    private JTable scoresTable;
    private DefaultTableModel tableModel;
    private JButton returnButton;
    private BufferedImage backgroundImage;
    private static final String HIGHSCORES_FILE = "highscores.ser";
    private List<HighScoreEntry> highScoreList = new ArrayList<>();

    public static class HighScoreEntry implements Serializable, Comparable<HighScoreEntry> {
        private static final long serialVersionUID = 1L;
        public String player;
        public int score;
        public int level;
        public String date;
        public HighScoreEntry(String player, int score, int level, String date) {
            this.player = player;
            this.score = score;
            this.level = level;
            this.date = date;
        }
        @Override
        public int compareTo(HighScoreEntry o) {
            return Integer.compare(o.score, this.score); // Descending
        }
    }

    public HighScoresView() {
        setTitle("Pacman - High Scores");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        try {
            backgroundImage = ImageIO.read(new File("assets/wall.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    // Tile the background image
                    for (int x = 0; x < getWidth(); x += backgroundImage.getWidth()) {
                        for (int y = 0; y < getHeight(); y += backgroundImage.getHeight()) {
                            g.drawImage(backgroundImage, x, y, null);
                        }
                    }
                }
            }
        };

        JLabel titleLabel = new JLabel("HIGH SCORES", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.YELLOW);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        String[] columnNames = {"Rank", "Player", "Score", "Level", "Date"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };

        scoresTable = new JTable(tableModel) {
            @Override
            protected void processMouseEvent(java.awt.event.MouseEvent e) {
                // Ignore all mouse events
            }
            @Override
            protected void processMouseMotionEvent(java.awt.event.MouseEvent e) {
                // Ignore all mouse motion events
            }
        };
        scoresTable.setRowHeight(30);
        scoresTable.setBackground(new Color(0, 0, 0, 180));
        scoresTable.setForeground(Color.WHITE);
        scoresTable.setGridColor(Color.YELLOW);
        scoresTable.setSelectionBackground(Color.YELLOW.darker());
        scoresTable.setSelectionForeground(Color.BLACK);
        scoresTable.setFont(new Font("Arial", Font.PLAIN, 14));
        scoresTable.setRowSelectionAllowed(false);
        scoresTable.setColumnSelectionAllowed(false);
        scoresTable.setCellSelectionEnabled(false);
        scoresTable.setFocusable(false);
        scoresTable.setSelectionModel(new javax.swing.DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                // Do nothing
            }
        });

        // Customize table header
        JTableHeader header = new JTableHeader(scoresTable.getColumnModel()) {
            @Override
            protected void processMouseEvent(java.awt.event.MouseEvent e) {
                // Ignore all mouse events
            }
            @Override
            protected void processMouseMotionEvent(java.awt.event.MouseEvent e) {
                // Ignore all mouse motion events
            }
        };
        scoresTable.setTableHeader(header);
        header.setBackground(Color.BLACK);
        header.setForeground(Color.YELLOW);
        header.setFont(new Font("Arial", Font.BOLD, 16));
        header.setReorderingAllowed(false);
        header.setFocusable(false);

        JScrollPane scrollPane = new JScrollPane(scoresTable);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 50, 20, 50));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        returnButton = new JButton("Return to Main Menu");
        returnButton.setFont(new Font("Arial", Font.BOLD, 16));
        returnButton.setForeground(Color.BLACK);
        returnButton.setBackground(Color.YELLOW);
        returnButton.setFocusPainted(false);
        returnButton.setBorderPainted(false);
        returnButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        returnButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                returnButton.setBackground(Color.YELLOW.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                returnButton.setBackground(Color.YELLOW);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.add(returnButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        setPreferredSize(new Dimension(600, 500));
        pack();
        setLocationRelativeTo(null);

        // Add key binding for Ctrl+Shift+Q to return to main menu
        KeyStroke keyStroke = KeyStroke.getKeyStroke("control shift Q");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "returnToMainMenu");
        getRootPane().getActionMap().put("returnToMainMenu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controllers.GameController.returnToMainMenuStatic();
            }
        });

        loadHighScoresFromFile();

        try {
            setIconImage(javax.imageio.ImageIO.read(new java.io.File("assets/favicon.png")));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void addScore(int rank, String player, int score, int level, String date) {
        highScoreList.add(new HighScoreEntry(player, score, level, date));
        Collections.sort(highScoreList);
        updateTableModel();
        saveHighScoresToFile();
    }

    private void updateTableModel() {
        tableModel.setRowCount(0);
        int rank = 1;
        for (HighScoreEntry entry : highScoreList) {
            tableModel.addRow(new Object[]{rank++, entry.player, entry.score, entry.level, entry.date});
        }
    }

    public void clearScores() {
        highScoreList.clear();
        tableModel.setRowCount(0);
        saveHighScoresToFile();
    }

    public void addReturnButtonListener(ActionListener listener) {
        returnButton.addActionListener(listener);
    }

    private void saveHighScoresToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HIGHSCORES_FILE))) {
            oos.writeObject(highScoreList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadHighScoresFromFile() {
        File file = new File(HIGHSCORES_FILE);
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                highScoreList = (List<HighScoreEntry>) obj;
                updateTableModel();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean playerNameExists(String playerName) {
        for (HighScoreEntry entry : highScoreList) {
            if (entry.player.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }
} 