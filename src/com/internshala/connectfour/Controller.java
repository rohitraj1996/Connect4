package com.internshala.connectfour;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Controller implements Initializable {

    private static final int COLUMNS = 7;
    private static final int ROWS = 6;
    private static final int CIRCLE_DIAMETER = 80;
    private static final String DISC_COLOR1 = "#24303E";
    private static final String DISC_COLOR2 = "#4CAA88";

    private static String PLAYER_ONE = "Player One";
    private static String PLAYER_TWO = "Player Two";

    private boolean isPlayerOneTurn = true;

    private boolean isAllowedToInsert = true; //Flag to avoid same color disc being added.

    private boolean isPlayerNameSet = false;

    private Disc[][] insertedDiscsArray = new Disc[ROWS][COLUMNS];  //For structural changes. For developers only.

    @FXML
    public GridPane rootGridPane;

    @FXML
    public Pane insertedDiscsPane;

    @FXML
    public Label playerNameLabel;

    @FXML
    public TextField playerOneTextField, playerTwoTextField;

    @FXML
    public Button setNamesButton;

    public void createPlayground() {

        playerOneTextField.setDisable(false);
        playerTwoTextField.setDisable(false);
        setNamesButton.setDisable(false);

        Shape rectangleWithHoles = createGameStructuralGrid();
        rootGridPane.add(rectangleWithHoles, 0, 1);

        List<Rectangle> rectangleList = createClickableColumns();

        for (Rectangle rectangle : rectangleList) {
            rootGridPane.add(rectangle, 0, 1);
        }

        setNamesButton.setOnAction(event -> {

            PLAYER_ONE = playerOneTextField.getText();
            PLAYER_TWO = playerTwoTextField.getText();

            setPlayerName(PLAYER_ONE, PLAYER_TWO);
        });
    }

    private void setPlayerName(String playerOne, String playerTwo) {   //Sets the players name.

        if (!isPlayerNameSet) {
            playerOneTextField.setText(playerOne);
            playerTwoTextField.setText(playerTwo);

        }

        playerNameLabel.setText(playerOne);
        playerOneTextField.setDisable(true);
        playerTwoTextField.setDisable(true);
        setNamesButton.setDisable(true);

        isPlayerNameSet = true;
    }

    private Shape createGameStructuralGrid() {

        Shape rectangleWithHoles = new Rectangle((COLUMNS + 1) * CIRCLE_DIAMETER, (ROWS + 1) * CIRCLE_DIAMETER);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                Circle circle = new Circle();
                circle.setRadius(CIRCLE_DIAMETER / 2.0);
                circle.setCenterX(CIRCLE_DIAMETER / 2.0);
                circle.setCenterY(CIRCLE_DIAMETER / 2.0);
                circle.setSmooth(true);

                circle.setTranslateX(col * (CIRCLE_DIAMETER + 5) + CIRCLE_DIAMETER / 4);
                circle.setTranslateY(row * (CIRCLE_DIAMETER + 5) + CIRCLE_DIAMETER / 4);
                rectangleWithHoles = Shape.subtract(rectangleWithHoles, circle);
            }
        }

        rectangleWithHoles.setFill(Color.WHITE);
        return rectangleWithHoles;
    }

    private List<Rectangle> createClickableColumns() {

        List<Rectangle> rectangleList = new ArrayList<>();

        for (int col = 0; col < COLUMNS; col++) {
            Rectangle rectangle = new Rectangle(CIRCLE_DIAMETER, (ROWS + 1) * CIRCLE_DIAMETER);
            rectangle.setFill(Color.TRANSPARENT);
            rectangle.setTranslateX(col * (CIRCLE_DIAMETER + 5) + CIRCLE_DIAMETER / 4);

            rectangle.setOnMouseEntered(event -> rectangle.setFill(Color.valueOf("#eeeeee26")));
            rectangle.setOnMouseExited(event -> rectangle.setFill(Color.TRANSPARENT));

            final int column = col;

            rectangle.setOnMouseClicked(event -> {

                if (!isPlayerNameSet){
                    alertWhenPlayerNameNotSet();
                    return;
                }

                if(isAllowedToInsert && insertedDiscsArray[0][column] == null){
                    isAllowedToInsert = false;           //When disc is being dropped then no more disc will be inserted.
                    insertDisc(new Disc(isPlayerOneTurn), column);
                }
            });
            rectangleList.add(rectangle);
        }

        return rectangleList;
    }

    private void alertWhenPlayerNameNotSet() {          //It gives the alert when player starts the game without setting the name.

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("WARNING");
        alert.setHeaderText("Player Name Not Set");
        alert.setContentText("Player Name will be set to default. " + "Click Yes to continue ");

        ButtonType yesBtn = new ButtonType("YES");
        ButtonType noBtn = new ButtonType("NO");

        alert.getButtonTypes().setAll(yesBtn, noBtn);

        Optional<ButtonType> buttonClicked = alert.showAndWait();
        if (buttonClicked.isPresent() && buttonClicked.get() == yesBtn){
            setPlayerName(PLAYER_ONE, PLAYER_TWO);
        }
    }

    private void insertDisc(Disc disc, int column) {

        int row = ROWS - 1;
        while (row >= 0) {

            if (getDiscIfPresent(row, column) == null) {
                break;
            }
            row--;
        }

        if (row < 0) {
            return;
        }

        insertedDiscsArray[row][column] = disc;       // For Structural changes: For Developers.
        insertedDiscsPane.getChildren().add(disc);

        disc.setTranslateX(column * (CIRCLE_DIAMETER + 5) + CIRCLE_DIAMETER / 4);

        TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.5), disc);
        translateTransition.setToY(row * (CIRCLE_DIAMETER + 5) + CIRCLE_DIAMETER / 4);

        int currentRow = row;
        translateTransition.setOnFinished(event -> {

            isAllowedToInsert = true;                 // Finally, when disc is dropped, allow next player to insert disc.
            if (gameEnded(currentRow, column)) {
                gameOver();
            }

            isPlayerOneTurn = !isPlayerOneTurn;
            playerNameLabel.setText(isPlayerOneTurn ? PLAYER_ONE : PLAYER_TWO);
        });
        translateTransition.play();
    }

    private boolean gameEnded(int row, int column) {

        List<Point2D> verticalPoints = IntStream.rangeClosed(row - 3, row + 3)                      //range of row values = 0,1,2,3,4,5
                .mapToObj(r -> new Point2D(r, column))     // eg. 0,3  1,3  2,3  3,3  4,3  5,3 --> Point2D x,y
                .collect(Collectors.toList());


        List<Point2D> horizontalPoints = IntStream.rangeClosed(column - 3, column + 3)                      //range of row values = 0,1,2,3,4,5
                .mapToObj(col -> new Point2D(row, col))     // eg. 0,3  1,3  2,3  3,3  4,3  5,3 --> Point2D x,y
                .collect(Collectors.toList());

        Point2D startPoint1 = new Point2D(row - 3, column + 3);
        List<Point2D> diagonal1Points = IntStream.rangeClosed(0, 6)
                .mapToObj(i -> startPoint1.add(i, -i))
                .collect(Collectors.toList());

        Point2D startPoint2 = new Point2D(row - 3, column - 3);
        List<Point2D> diagonal2Points = IntStream.rangeClosed(0, 6)
                .mapToObj(i -> startPoint2.add(i, i))
                .collect(Collectors.toList());

        boolean isEnded = checkCombinations(verticalPoints) || checkCombinations(horizontalPoints)
                || checkCombinations(diagonal1Points) || checkCombinations(diagonal2Points);

        return isEnded;
    }

    private boolean checkCombinations(List<Point2D> points) {

        int chain = 0;

        for (Point2D point : points) {

            int rowIndexForArray = (int) point.getX();
            int columnIndexForArray = (int) point.getY();

            Disc disc = getDiscIfPresent(rowIndexForArray, columnIndexForArray);

            if (disc != null && disc.isPlayerOneMove == isPlayerOneTurn) {  // If the last inserted Disc belongs to the current player.

                chain++;
                if (chain == 4) {
                    return true;
                }
            } else {
                chain = 0;
            }
        }
        return false;
    }

    private Disc getDiscIfPresent(int row, int column) {     //To prevent ArrayIndexOutOfBoundsException.

        if (row >= ROWS || row < 0 || column >= COLUMNS || column < 0) {  // If row or column is invalid.
            return null;
        }

        return insertedDiscsArray[row][column];
    }

    private void gameOver() {
        String winner = isPlayerOneTurn ? PLAYER_ONE : PLAYER_TWO;
        System.out.println("Winner is: " + winner);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Connect Four");
        alert.setHeaderText("The winner is " + winner);
        alert.setContentText("Want to play again? ");

        ButtonType yesBtn = new ButtonType("Yes");
        ButtonType noBtn = new ButtonType("No, Exit");
        alert.getButtonTypes().setAll(yesBtn, noBtn);

        Platform.runLater(() -> {

            Optional<ButtonType> btnClicked = alert.showAndWait();
            if (btnClicked.isPresent() && btnClicked.get() == yesBtn) {
                resetGame();
            } else {
                Platform.exit();
                System.exit(0);
            }
        });
    }

    public void resetGame() {

        insertedDiscsPane.getChildren().clear();  //Remove all inserted disc from pane.

        for (int row = 0; row < ROWS; row++) {              // Structurally, make all elements of insertedDiscsArray[][] to null.
            for (int col = 0; col < COLUMNS; col++) {
                insertedDiscsArray[row][col] = null;
            }
        }

        isPlayerOneTurn = true;                 // Let player 1 start the game.

        PLAYER_ONE = "Player One";
        PLAYER_TWO = "Player Two";
        playerNameLabel.setText(PLAYER_ONE);
        isPlayerNameSet = false;

        playerOneTextField.clear();
        playerTwoTextField.clear();

        createPlayground();              //Prepare the fresh playground.
    }

    private static class Disc extends Circle {

        private final boolean isPlayerOneMove;

        public Disc(boolean isPlayerOneMove) {

            this.isPlayerOneMove = isPlayerOneMove;
            setRadius(CIRCLE_DIAMETER / 2);
            setFill(isPlayerOneMove ? Color.valueOf(DISC_COLOR1) : Color.valueOf(DISC_COLOR2));
            setCenterX(CIRCLE_DIAMETER / 2);
            setCenterY(CIRCLE_DIAMETER / 2);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
