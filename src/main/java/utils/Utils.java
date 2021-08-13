package utils;

import exportable.*;
import exportable.live.LiveFloorItems;
import exportable.live.LiveFloorPlan;
import exportable.live.LiveWallItems;
import extension.RoomDuplicator;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.StageStyle;
import parsers.Inventory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class Utils {
    public static Map<String, HPacket> requestRoomEntryPackets(Executor executor) {
        executor.sendToServer("GetHeightMap");
        Executor.AwaitingPacket[] awaitingPackets = new Executor.AwaitingPacket[] {
                new Executor.AwaitingPacket("GetGuestRoomResult", HMessage.Direction.TOCLIENT, 500)
                        .addConditions(HPacket::readBoolean),
                new Executor.AwaitingPacket("RoomEntryTile", HMessage.Direction.TOCLIENT, 500),
                new Executor.AwaitingPacket("FloorHeightMap", HMessage.Direction.TOCLIENT, 500),
                new Executor.AwaitingPacket("Items", HMessage.Direction.TOCLIENT, 500),
                new Executor.AwaitingPacket("Objects", HMessage.Direction.TOCLIENT, 500),
                new Executor.AwaitingPacket("RoomVisualizationSettings", HMessage.Direction.TOCLIENT, 500)
        };

        executor.awaitPacketList(awaitingPackets);

        Map<String, HPacket> packetMap = new HashMap<>();
        Arrays.stream(awaitingPackets).forEach(p ->
                packetMap.put(p.headerName, p.getPacket()));

        return packetMap;
    }

    public static Map<String, Exportable> getLiveExportablesFromPackets(RoomDuplicator extension, Map<String, HPacket> packetMap) {
        Map<String, Exportable> exportables = new HashMap<>();

        if(packetMap.getOrDefault("RoomEntryTile", null) != null
                && packetMap.getOrDefault("FloorHeightMap", null) != null) {
            exportables.put("FloorPlan", new LiveFloorPlan(extension, packetMap.get("FloorHeightMap"), packetMap.get("RoomEntryTile")));
        }

        if(packetMap.getOrDefault("GetGuestRoomResult", null) != null) {
            exportables.put("RoomData", new RoomData(packetMap.get("GetGuestRoomResult"), packetMap.get("RoomVisualizationSettings")));
        }

        if(packetMap.getOrDefault("Items", null) != null) {
            exportables.put("WallItems", new LiveWallItems(extension, packetMap.get("Items")));
        }

        if(packetMap.getOrDefault("Objects", null) != null) {
            exportables.put("FloorItems", new LiveFloorItems(extension, packetMap.get("Objects")));
        }

        return exportables;
    }

    public static boolean requestEjectall(Executor executor) {
        final FutureTask<Boolean> alertDialog = new FutureTask<>(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Ejectall confirmation");
            alert.setHeaderText("Importing floorplan and/or floor items requires an ejectall");
            alert.setContentText("Do you want to ejectall and continue with the import?");
            alert.initStyle(StageStyle.UNDECORATED);

            AtomicBoolean result = new AtomicBoolean(false);
            final Button ok = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            ok.addEventFilter(ActionEvent.ACTION, event -> result.set(true));

            final Button cancel = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
            cancel.addEventFilter(ActionEvent.ACTION, event -> result.set(false));

            alert.showAndWait();

            return result.get();
        });

        Platform.runLater(alertDialog);

        try {
            if(alertDialog.get()) {
                executor.sendToServer("Chat", ":ejectall", 0, -1);
                executor.awaitPacket(new Executor.AwaitingPacket("ObjectRemove", HMessage.Direction.TOCLIENT, 1000));
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Inventory requestInventory(Executor executor) {
        executor.sendToServer("RequestFurniInventory");
        HPacket received = executor.awaitPacket(new Executor.AwaitingPacket("FurniList", HMessage.Direction.TOCLIENT, 500));
        if(received == null) {
            System.out.println("No Packet received");
            return null;
        }
        int packetCount = received.readInteger();
        Executor.AwaitingPacket[] awaitingPackets = new Executor.AwaitingPacket[packetCount];
        for(int i = 0; i < packetCount; i++) {
            int index = i;
            System.out.println(i);
            awaitingPackets[i] = new Executor.AwaitingPacket("FurniList", HMessage.Direction.TOCLIENT, 5000)
                    .addConditions(packet -> {
                        packet.readInteger();
                        return packet.readInteger() == index;
                    });
        }
        executor.sendToServer("RequestFurniInventory");
        List<HPacket> invPackets = executor.awaitPacketList(awaitingPackets);
        if(invPackets == null) {
            System.out.println("Inventory packets missed");
            return null;
        }

        return new Inventory(invPackets.toArray(new HPacket[0]));
    }

    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void placeFloorItem(Executor executor, int id, int x, int y, int dir) {
        int tries = 0;
        String floorString = String.format("-%d %d %d %d", id, x, y, dir);
        while(tries < 10) {
            executor.sendToServer("PlaceObject", floorString);
            HPacket response = executor.awaitPacket(new Executor.AwaitingPacket("ObjectAdd", HMessage.Direction.TOCLIENT, 50)
                    .addConditions(p -> p.readInteger() == id));
            if(response != null) return;
            tries++;
        }
    }

    public static void moveObject(Executor executor, int id, int x, int y, int dir) {
        int tries = 0;
        while(tries < 10) {
            executor.sendToServer("MoveObject", id, x, y, dir);
            HPacket response = executor.awaitPacket(new Executor.AwaitingPacket("ObjectUpdate", HMessage.Direction.TOCLIENT, 50)
                    .addConditions(p -> p.readInteger() == id));
            if(response != null) return;
            tries++;
        }
    }

    public static void placeWallItem(Executor executor, int id, String position) {
        int tries = 0;
        String wallString = String.format("%d %s", id, position);
        while(tries < 10) {
            executor.sendToServer("PlaceObject", wallString);
            HPacket response = executor.awaitPacket(new Executor.AwaitingPacket("ItemAdd", HMessage.Direction.TOCLIENT, 50)
                    .addConditions(p -> Integer.parseInt(p.readString()) == id));
            if(response != null) return;
            tries++;
        }
    }
}
