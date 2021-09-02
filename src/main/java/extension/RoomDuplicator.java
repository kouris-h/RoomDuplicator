package extension;

import furnidata.FurniDataSearcher;
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.protocol.connection.HClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import utils.Exporter;
import utils.Importer;
import utils.Logger;

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
    public RadioButton exportWallItems, exportFloorItems, exportFloorplan, exportRoomSettings;
    public ProgressBar exportProgress;
    public Button exportButton;
    // -- FX Import pane
    public AnchorPane importPane;
    public RadioButton importWallItems, importFloorItems, importFloorplan, importRoomSettings;
    public TextField importPath;
    public ProgressBar importProgress;
    public Button importButton;
    // -- FX Log pane
    public ScrollPane logScroll;
    public TextFlow txt_logField;

    private Importer importer;
    private Exporter exporter;

    @Override
    protected void initExtension() {
        Logger.setup(logScroll, txt_logField);
        this.onConnect(this::doOnConnect);

        importer = new Importer(this);
        exporter = new Exporter(this);
    }

    private void doOnConnect(String host, int i, String s1, String s2, HClient hClient) {
        new Thread(() -> FurniDataSearcher.fetch(host, this::updateUI)).start();
    }

    private void updateUI() {
        Platform.runLater(() -> {
            Logger.log("Furnidata loaded");
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

    public void clearLog(ActionEvent actionEvent) {
        txt_logField.getChildren().remove(0, txt_logField.getChildren().size());
    }
}
