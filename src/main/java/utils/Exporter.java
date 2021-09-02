package utils;

import exportable.*;
import extension.RoomDuplicator;
import gearth.protocol.HPacket;
import javafx.scene.paint.Color;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Exporter {
    private final RoomDuplicator extension;
    private final Executor executor;

    public Exporter(RoomDuplicator extension) {
        this.extension = extension;
        this.executor = new Executor(extension);
    }

    public void runExport() {
        Logger.log(Cacher.dir);
        Map<String, HPacket> packets = Utils.requestRoomEntryPackets(executor);
        System.out.println(packets.toString());
        if(packets.values().stream().noneMatch(Objects::nonNull) || packets.get("GetGuestRoomResult") == null || packets.get("RoomVisualizationSettings") == null) {
            Logger.log(Color.RED, "Move Habbo in to a room to start an export!");
            return;
        }

        List<Exportable> exportables = getSelectedExportablesFromPackets(packets);

        JSONObject exportingJson = new JSONObject();

        exportables.forEach(exportable -> {
                    Logger.log(Color.TEAL, "Exporting " + exportable.getClass().getAnnotation(ExportableInfo.class).Name());
                    exportingJson.put(exportable.getClass().getAnnotation(ExportableInfo.class).JsonTag(), exportable.export(this::setProgress));
                });

        HPacket roomResult = packets.get("GetGuestRoomResult");
        HPacket visualization = packets.get("RoomVisualizationSettings");
        RoomData room = new RoomData(roomResult, visualization);

        Cacher.updateCache(exportingJson.toString(4), room.id + "-" + room.name.replaceAll("[^A-Za-z0-9]", "") + ".roomJson");
        Logger.log("Export completed");
    }

    private List<Exportable> getSelectedExportablesFromPackets(Map<String, HPacket> packetMap) {
        List<Exportable> exportables = new ArrayList<>();

        if(extension.exportFloorplan.isSelected()) {
            if(packetMap.getOrDefault("RoomEntryTile", null) != null
                    && packetMap.getOrDefault("FloorHeightMap", null) != null) {
                exportables.add(new FloorPlan(packetMap.get("FloorHeightMap"), packetMap.get("RoomEntryTile")));
            } else {
                Logger.log(Color.RED, "Couldn't receive packets necessary for floorplan export, floorplan skipped!");
            }
        }

        if(extension.exportRoomSettings.isSelected()) {
            if(packetMap.getOrDefault("GetGuestRoomResult", null) != null) {
                exportables.add(new RoomData(packetMap.get("GetGuestRoomResult"), packetMap.get("RoomVisualizationSettings")));
            } else {
                Logger.log(Color.RED, "Couldn't receive packets necessary for room settings export, room settings skipped!");
            }
        }

        if(extension.exportWallItems.isSelected()) {
            if(packetMap.getOrDefault("Items", null) != null) {
                exportables.add(new WallItems(packetMap.get("Items")));
            } else {
                Logger.log(Color.RED, "Couldn't receive packets necessary for wall items export, wall items skipped!");
            }
        }

        if(extension.exportFloorItems.isSelected()) {
            if(packetMap.getOrDefault("Objects", null) != null) {
                exportables.add(new FloorItems(packetMap.get("Objects")));
            } else {
                Logger.log(Color.RED, "Couldn't receive packets necessary for floor items export, floor items skipped!");
            }
        }

        return exportables;
    }

    public void setProgress(double p) {
        this.extension.exportProgress.setProgress(p);
    }
}
