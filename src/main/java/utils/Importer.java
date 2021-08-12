package utils;

import exportable.*;
import exportable.live.LiveFloorItems;
import extension.RoomDuplicator;
import gearth.misc.Cacher;
import gearth.protocol.HPacket;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.json.JSONException;
import org.json.JSONObject;
import parsers.Inventory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Importer {
    private final RoomDuplicator extension;
    private final Executor executor;
    private JSONObject importingJson;

    public Importer(RoomDuplicator extension) {
        this.extension = extension;
        this.executor = new Executor(extension);
    }

    public void chooseImportFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Import File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("RoomJSON (*.roomJson)", "*.roomJson"));
            fileChooser.setInitialDirectory(new File(Cacher.getCacheDir() + "/RoomDuplicator"));

            File selectedFile = fileChooser.showOpenDialog(extension.getPrimaryStage());

            if (selectedFile != null) {
                String input = new String(Files.readAllBytes(selectedFile.toPath()));
                try {
                    importingJson = new JSONObject(input);
                    extension.importPath.setText(selectedFile.getPath());
                    updateRadioButtons();
                } catch (JSONException e) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setHeaderText("Input not valid");
                    errorAlert.setContentText("The imported file is not a valid RoomJSON");
                    errorAlert.showAndWait();

                    e.printStackTrace();
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void updateRadioButtons() {
        extension.importWiredSettings.setDisable(!importingJson.has(WiredSettings.class.getAnnotation(ExportableInfo.class).JsonTag()));
        extension.importWiredSettings.setSelected(importingJson.has(WiredSettings.class.getAnnotation(ExportableInfo.class).JsonTag()));

        extension.importRoomSettings.setDisable(!importingJson.has(RoomData.class.getAnnotation(ExportableInfo.class).JsonTag()));
        extension.importRoomSettings.setSelected(importingJson.has(RoomData.class.getAnnotation(ExportableInfo.class).JsonTag()));

        extension.importFloorplan.setDisable(!importingJson.has(FloorPlan.class.getAnnotation(ExportableInfo.class).JsonTag()));
        extension.importFloorplan.setSelected(importingJson.has(FloorPlan.class.getAnnotation(ExportableInfo.class).JsonTag()));

        extension.importFloorItems.setDisable(!importingJson.has(FloorItems.class.getAnnotation(ExportableInfo.class).JsonTag()));
        extension.importFloorItems.setSelected(importingJson.has(FloorItems.class.getAnnotation(ExportableInfo.class).JsonTag()));

        extension.importWallItems.setDisable(!importingJson.has(WallItems.class.getAnnotation(ExportableInfo.class).JsonTag()));
        extension.importWallItems.setSelected(importingJson.has(WallItems.class.getAnnotation(ExportableInfo.class).JsonTag()));

        extension.importButton.setDisable(false);
    }

    public void runImport() {
        // TODO

        Map<String, HPacket> packets = Utils.requestRoomEntryPackets(executor);
        if(packets.values().stream().noneMatch(Objects::nonNull) || packets.get("GetGuestRoomResult") == null || packets.get("RoomVisualizationSettings") == null) {
            extension.log(Color.RED, "Move Habbo in to a room to start an import!");
            return;
        }

        Map<String, Exportable> currentStates = Utils.getLiveExportablesFromPackets(extension, packets);
        List<Exportable> exportables = getExportablesFromJson(importingJson);

        if(exportables.stream().map(Exportable::getClass).anyMatch(c -> c.equals(FloorPlan.class) || c.equals(FloorItems.class))) {
            if(!Utils.requestEjectall(executor)) {
                extension.log(Color.RED, "Ejectall rejected, import stopped!");
            }
        }

        Inventory inv = Utils.requestInventory(executor);

        exportables.forEach(exportable -> {
            exportable.doImport(executor, currentStates, inv, (p) -> extension.importProgress.setProgress(p));
            extension.log(Color.SEAGREEN, exportable.getClass().getAnnotation(ExportableInfo.class).Name() + " imported!");
        });
    }

    private List<Exportable> getExportablesFromJson(JSONObject importingJson) {
        List<Exportable> exportables = new ArrayList<>();

        if(importingJson.has(RoomData.class.getAnnotation(ExportableInfo.class).JsonTag()) && extension.importRoomSettings.isSelected()) {
            exportables.add(new RoomData(importingJson.getJSONObject(RoomData.class.getAnnotation(ExportableInfo.class).JsonTag())));
        }
        if(importingJson.has(FloorPlan.class.getAnnotation(ExportableInfo.class).JsonTag()) && extension.importFloorplan.isSelected()) {
            exportables.add(new FloorPlan(importingJson.getJSONObject(FloorPlan.class.getAnnotation(ExportableInfo.class).JsonTag())));
        }
        if(importingJson.has(WallItems.class.getAnnotation(ExportableInfo.class).JsonTag()) && extension.importWallItems.isSelected()) {
            exportables.add(new WallItems(importingJson.getJSONArray(WallItems.class.getAnnotation(ExportableInfo.class).JsonTag())));
        }
        if(importingJson.has(FloorItems.class.getAnnotation(ExportableInfo.class).JsonTag()) && extension.importFloorItems.isSelected()) {
            exportables.add(new FloorItems(importingJson.getJSONArray(FloorItems.class.getAnnotation(ExportableInfo.class).JsonTag())));
        }
        if(importingJson.has(WiredSettings.class.getAnnotation(ExportableInfo.class).JsonTag()) && extension.importWiredSettings.isSelected()) {
            exportables.add(new WiredSettings(importingJson.getJSONArray(WiredSettings.class.getAnnotation(ExportableInfo.class).JsonTag())));
        }

        return exportables;
    }

    public void setProgress(double p) {
        this.extension.importProgress.setProgress(p);
    }
}
