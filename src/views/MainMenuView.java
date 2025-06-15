package views;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class MainMenuView extends JFrame {
    private JButton newGameButton;
    private JButton highScoresButton;
    private JButton exitButton;
    private BufferedImage backgroundImage;
    private BufferedImage pacmanImage;

    public MainMenuView() {
        setTitle("Pacman");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        try {
            backgroundImage = ImageIO.read(new File("assets/wall.png"));
            pacmanImage = ImageIO.read(new File("assets/pacman/pacmandefault.png"));
            setIconImage(ImageIO.read(new File("assets/favicon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    // Draw tiled background
                    for (int x = 0; x < getWidth(); x += backgroundImage.getWidth()) {
                        for (int y = 0; y < getHeight(); y += backgroundImage.getHeight()) {
                            g.drawImage(backgroundImage, x, y, null);
                        }
                    }
                }
            }
        };
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel("PACMAN");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(Color.YELLOW);
        titlePanel.add(titleLabel);
        mainPanel.add(titlePanel);
        mainPanel.add(Box.createVerticalStrut(50));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);

        Dimension buttonSize = new Dimension(200, 40);
        Font buttonFont = new Font("Arial", Font.BOLD, 20);

        newGameButton = createStyledButton("New Game", buttonSize, buttonFont);
        buttonPanel.add(newGameButton);
        buttonPanel.add(Box.createVerticalStrut(20));

        highScoresButton = createStyledButton("High Scores", buttonSize, buttonFont);
        buttonPanel.add(highScoresButton);
        buttonPanel.add(Box.createVerticalStrut(20));

        exitButton = createStyledButton("Exit", buttonSize, buttonFont);
        buttonPanel.add(exitButton);

        mainPanel.add(buttonPanel);
        add(mainPanel);

        setResizable(true);
        pack();
        setLocationRelativeTo(null);
    }

    private JButton createStyledButton(String text, Dimension size, Font font) {
        JButton button = new JButton(text);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setFont(font);
        button.setForeground(Color.BLACK);
        button.setBackground(Color.YELLOW);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setOpaque(true);
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.ORANGE);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.YELLOW);
            }
        });

        return button;
    }

    public JButton getNewGameButton() {
        return newGameButton;
    }

    public JButton getHighScoresButton() {
        return highScoresButton;
    }

    public JButton getExitButton() {
        return exitButton;
    }
} 