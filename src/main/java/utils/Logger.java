package utils;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class Logger {
    private static ScrollPane logScroll;
    private static TextFlow txt_logField;

    public static void setup(ScrollPane logScroll, TextFlow txt_logField) {
        Logger.logScroll = logScroll;
        Logger.txt_logField = txt_logField;
    }

    public static void log(Color color, String text) {
        Platform.runLater(() -> {
            String color2 = "#" + color.toString().substring(2, 8);
            Text otherText = new Text(text + "\n");
            otherText.setStyle("-fx-fill: " + color2 + ";");
            otherText.setFont(Font.font("Helvetica", FontPosture.REGULAR, 14));

            txt_logField.getChildren().add(otherText);
            logScroll.setVvalue(1.1d);
        });
    }

    public static void log(String text) {
        log(Color.WHITE, text);
    }
}
