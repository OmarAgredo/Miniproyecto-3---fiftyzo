# 50ZO - Cincuentazo JavaFX Game

50ZO is a JavaFX implementation of the Cincuentazo card game with a custom warm backrooms, liminal circus, cursed arcade visual style. The project combines a playable desktop GUI with an MVC-oriented Java codebase, custom card assets, themed screens, custom exceptions, asynchronous machine turns, and JUnit tests.

## Game Objective

The goal is to stay in the game without making the table sum exceed 50.

- Players take turns playing one card onto the table.
- Each card changes the current table sum according to its value.
- The resulting table sum must stay at or below 50.
- If a player cannot play any valid card, that player is eliminated.
- The last active player wins.

## Card Rules

| Card | Value |
| --- | --- |
| 2 to 8 | Adds its numeric value |
| 9 | Adds 0 |
| 10 | Adds 10 |
| J, Q, K | Subtracts 10 |
| Ace | Can be played as 1 or 10 |

The table sum may reach exactly 50, but it may not exceed 50.

## Main Features

- Playable JavaFX GUI.
- Start screen with machine player selection.
- Support for 1 to 3 machine players.
- Dynamic card rendering from custom card assets.
- Special Ace image support.
- Event log for turn-by-turn feedback.
- In-game options menu.
- End screen with final result and final event log.
- Asynchronous machine turns using JavaFX timing tools.
- Custom exceptions for invalid moves, empty deck state, no playable cards, and game-over actions.
- JUnit tests for core model behavior.
- MVC-style project structure.
- Custom visual theme with backgrounds, card backs, and fonts.

## Technologies

- Java 17
- JavaFX
- Maven
- JUnit 5
- FXML
- CSS

## Architecture

The project follows an MVC-oriented structure:

- `model` contains the game rules, card values, deck, table, players, turn management, and machine strategy.
- `controller` connects the visual interface to the game model and handles scene flow.
- `view` resources contain the FXML layouts.
- `css`, `images`, and `fonts` resources define the themed presentation layer.

`GameController` is the main bridge between the JavaFX game screen and the UI-independent `Game` model. It renders the current state, handles human card clicks, schedules machine turns, updates the event log, and transitions to the end screen when the model reports a winner.

## Data Structures

The project uses several explicit data structures:

- `ArrayList` for player hands and collections of machine players.
- `ArrayDeque` for the deck and the table's played-card flow.
- A custom circular linked turn structure through `CircularPlayerList`, `PlayerNode`, and `TurnManager`.
- A shallow machine decision tree through `DecisionTreeStrategy` and `DecisionNode`.

## Custom Exceptions

- `InvalidMoveException`: thrown when a selected card value is not legal or would make the table sum exceed 50.
- `EmptyDeckException`: thrown when the deck cannot provide a card.
- `NoPlayableCardException`: used when a machine player has no legal card to play.
- `GameOverException`: thrown when an action is attempted after the game has ended.

## Threads and Asynchronous Behavior

Machine turns are delayed with JavaFX `PauseTransition` so the interface stays responsive and machine actions feel more natural. The controller schedules separate pauses for machine thinking and machine drawing instead of blocking the JavaFX application thread.

## Unit Tests

JUnit tests are included under `src/test/java`. They cover:

- Card value rules, including Ace, 9, 10, and face cards.
- Deck construction, drawing, shuffling, and empty-deck behavior.
- Table boundary behavior at 50.
- Game startup, initial dealing, turn flow, valid and invalid moves.
- Ace value handling.
- Player elimination and winner detection.
- Deck recycling from previously played table cards.

## How to Run

### IntelliJ IDEA

1. Open the project folder in IntelliJ IDEA.
2. Configure the project SDK to Java 17.
3. Load the Maven project when prompted.
4. Run `src/main/java/com/project/fiftyzo/Main.java`.

### Maven

If your local Java and Maven setup is configured for Java 17, the game can also be run with:

```bash
mvn javafx:run
```

On systems without a global Maven installation, use the included Maven wrapper:

```bash
./mvnw javafx:run
```

On Windows:

```powershell
.\mvnw.cmd javafx:run
```

## How to Run Tests

Run the test suite with:

```bash
mvn test
```

Or with the Maven wrapper:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

Tests can also be run from the IntelliJ Maven panel or directly from the test classes.

## Project Structure

```text
src/main/java/com/project/fiftyzo/
  Main.java
  FiftyzoApplication.java
  controller/
  exception/
  model/
    ai/
    turn/
  util/

src/main/resources/com/project/fiftyzo/
  css/
  fonts/
  images/
    backgrounds/
    cards/
    stickers/
    ui/
  view/

src/test/java/com/project/fiftyzo/
  model/
```

## Visual Design

50ZO uses a custom visual style inspired by warm backrooms, liminal circus spaces, and cursed arcade interfaces. The interface includes:

- Custom warm background artwork.
- Themed start, game, and end screens.
- Custom card backs and card image assets.
- Special Ace artwork.
- Custom fonts for headings and functional UI text.
- Dark translucent panels with cream text and subtle lime/chartreuse accents.

## Academic Requirements Demonstrated

This project demonstrates:

- JavaFX GUI development.
- MVC-style separation of model, controller, and view resources.
- Event handling through JavaFX controllers.
- Data structures such as lists, deques, circular linked turn management, and a decision tree.
- Custom exception handling.
- Unit testing with JUnit 5.
- Asynchronous UI behavior with JavaFX timing tools.
- Code documentation through class and method comments.

## Authors

- Omar Esteban Agredo
