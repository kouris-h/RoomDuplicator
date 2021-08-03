package utils;

import exportable.*;
import extension.RoomDuplicator;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.StageStyle;
import parsers.Inventory;

import java.util.*;

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

    public static List<Exportable> getExportablesFromPackets(RoomDuplicator extension, Map<String, HPacket> packetMap) {
        List<Exportable> exportables = new ArrayList<>();

        if(packetMap.getOrDefault("RoomEntryTile", null) != null
                && packetMap.getOrDefault("FloorHeightMap", null) != null) {
            exportables.add(new FloorPlan(packetMap.get("FloorHeightMap"), packetMap.get("RoomEntryTile")));
        }

        if(packetMap.getOrDefault("GetGuestRoomResult", null) != null) {
            exportables.add(new RoomData(packetMap.get("GetGuestRoomResult"), packetMap.get("RoomVisualizationSettings")));
        }

        if(packetMap.getOrDefault("Items", null) != null) {
            exportables.add(new WallItems(packetMap.get("Items")));
        }

        if(packetMap.getOrDefault("Objects", null) != null) {
            exportables.add(new FloorItems(packetMap.get("Objects")));
        }

        return exportables;
    }

    public static boolean requestEjectall(Executor executor) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ejectall confirmation");
        alert.setHeaderText("Importing floorplan and/or floor items requires an ejectall");
        alert.setContentText("Do you want to ejectall and continue with the import?");
        alert.initStyle(StageStyle.UNDECORATED);

        if(alert.showAndWait().get() == ButtonType.OK) {
            executor.sendToServer("Chat", ":ejectall", 0, -1);
            executor.awaitPacket(new Executor.AwaitingPacket("ObjectRemove", HMessage.Direction.TOCLIENT, 1000));
            return true;
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
}
