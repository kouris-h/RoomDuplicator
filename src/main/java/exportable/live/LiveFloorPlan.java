package exportable.live;

import exportable.FloorPlan;
import gearth.extensions.ExtensionBase;
import gearth.extensions.extra.tools.PacketInfoSupport;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

public class LiveFloorPlan extends FloorPlan {
    public LiveFloorPlan(ExtensionBase extension, HPacket floorPlanPacket, HPacket doorPacket) {
        super(floorPlanPacket, doorPacket);

        PacketInfoSupport packetInfoSupport = new PacketInfoSupport(extension);

        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "FloorHeightMap", this::onFloorHeightMap);
        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "RoomEntryTile", this::onRoomEntryTile);
    }

    private void onRoomEntryTile(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        this.doorX = packet.readInteger(6);
        this.doorY = packet.readInteger(10);
        this.doorDir = packet.readInteger(14);
    }

    private void onFloorHeightMap(HMessage hMessage) {
        this.floorPlan = hMessage.getPacket().readString(11);
    }
}
