# About

A Java implementation of the classic Pacman arcade game using Java Swing. This project was made as an assignment for my university (PJATK) Java GUI course. 

## Project Structure

The project follows the Model-View-Controller (MVC) architecture:

- `src/models/` - Contains the game's data models and business logic
- `src/views/` - Contains the UI components and game rendering
- `src/controllers/` - Contains the game controllers that handle user input and game flow
- `assets/` - Contains game assets like sprites and images
- `highscores.ser` - Stores the game's high scores

## Requirements

- Java Development Kit (JDK) 8 or higher
- Java Runtime Environment (JRE)

## How to Run

1. Make sure you have Java installed on your system
2. Compile the project using your preferred Java IDE or the command line
3. Run the `Main` class to start the game

```bash
javac src/Main.java
java -cp src Main
```

## Features

- Classic Pacman gameplay
- High score tracking
- Modern UI with system look and feel
- Smooth animations and controls

## Development

The project uses Java Swing for the graphical user interface and follows the MVC pattern for clean code organization. The game state is managed by the `GameController` class, which coordinates between the models and views.

## License

This project is licensed under the NOT FOR USE License - see the LICENSE file for details.
