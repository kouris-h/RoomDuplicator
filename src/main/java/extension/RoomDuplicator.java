package extension;

import furnidata.FurniDataSearcher;
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormLauncher;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.tools.PacketInfoSupport;
import gearth.misc.Cacher;
import gearth.protocol.connection.HClient;
import gearth.ui.GEarthController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import utils.Executor;
import utils.Exporter;
import utils.Importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@ExtensionInfo(
    Title =         "RoomDuplicator",
    Description =   "For duplicating rooms",
    Version =       "1.0",
    Author =        "WiredSpast & Kouris"
)
public class RoomDuplicator extends ExtensionForm {
    // FX components
    public AnchorPane mainPane;
    // -- FX Export pane
    public AnchorPane exportPane;
    public RadioButton exportWallItems, exportFloorItems, exportFloorplan, exportRoomSettings, exportWiredSettings;
    public ProgressBar exportProgress;
    public Button exportButton;
    // -- FX Import pane
    public AnchorPane importPane;
    public RadioButton importWallItems, importFloorItems, importFloorplan, importRoomSettings, importWiredSettings;
    public TextField importPath;
    public ProgressBar importProgress;
    public Button importButton;
    // -- FX Log pane
    public ScrollPane logScroll;
    public TextFlow txt_logField;

    private Importer importer;
    private Exporter exporter;

    public static void main(String[] args) {
        ExtensionFormLauncher.trigger(RoomDuplicator.class, args);
    }

    @Override
    public ExtensionForm launchForm(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(RoomDuplicator.class.getClassLoader().getResource("fxml/roomduplicator.fxml"));
        Parent root = loader.load();

        stage.setTitle("RoomDuplicator");
        stage.setScene(new Scene(root));
        stage.getIcons().add(new Image(this.getClass().getResource("/images/duck_icon.png").openStream()));

        stage.setResizable(false);

        return loader.getController();
    }

    @Override
    protected void initExtension() {
        createCacheFolder();

        this.onConnect(this::doOnConnect);

        importer = new Importer(this);
        exporter = new Exporter(this);
    }

    private void createCacheFolder() {
        try {
            Files.createDirectories(Paths.get(Cacher.getCacheDir() + "/RoomDuplicator"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doOnConnect(String host, int i, String s1, String s2, HClient hClient) {
        new Thread(() -> FurniDataSearcher.fetch(host, this::updateUI)).start();
    }

    private void updateUI() {
        Platform.runLater(() -> {
            log("Furnidata loaded");
            mainPane.setDisable(false);
        });
    }

    public Stage getPrimaryStage() {
        return this.primaryStage;
    }

    public void startExport(ActionEvent actionEvent) {
        exportPane.setDisable(true);
        exporter.runExport();
        exportPane.setDisable(false);
    }

    public void startImport(ActionEvent actionEvent) {
        importPane.setDisable(true);
        Thread importThread = new Thread(() -> {
            importer.runImport();
            Platform.runLater(() -> importPane.setDisable(false));
        });
        importThread.setUncaughtExceptionHandler((thread, exception) -> {
            exception.printStackTrace();
            Platform.runLater(() -> importPane.setDisable(false));
        });
        importThread.start();
    }

    public void onSelectImportFileButton(ActionEvent actionEvent) {
        importPane.setDisable(true);
        importer.chooseImportFile();
        importPane.setDisable(false);
    }

    public void log(Color color, String text) {
        Platform.runLater(() -> {
            String color2 = "#" + color.toString().substring(2, 8);
            Text otherText = new Text(text + "\n");
            otherText.setStyle("-fx-fill: " + color2 + ";");
            otherText.setFont(Font.font("Helvetica", FontPosture.REGULAR, 14));

            txt_logField.getChildren().add(otherText);
            logScroll.setVvalue(1.1d);
        });
    }

    public void log(String text) {
        log(Color.WHITE, text);
    }

    public void clearLog(ActionEvent actionEvent) {
        txt_logField.getChildren().remove(0, txt_logField.getChildren().size());
    }
}
