package exportable.live;

import exportable.FloorItems;
import gearth.extensions.ExtensionBase;
import gearth.extensions.extra.tools.PacketInfoSupport;
import gearth.extensions.parsers.HFloorItem;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.util.Arrays;

public class LiveFloorItems extends FloorItems {
    final Object lock = new Object();

    public LiveFloorItems(ExtensionBase extension, HPacket objectsPacket) {
        super(objectsPacket);
        PacketInfoSupport packetInfoSupport = new PacketInfoSupport(extension);

        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "Objects", this::onObjects);
        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", this::onObjectAdd);
        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "ObjectRemove", this::onObjectRemove);
        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", this::onObjectUpdate);
        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "ObjectDataUpdate", this::onObjectDataUpdate);
    }

    private void onObjectAdd(HMessage hMessage) {
        synchronized (lock) {
            this.floorItems.add(new FloorItem(new HFloorItem(hMessage.getPacket())));
        }
    }

    private void onObjects(HMessage hMessage) {
        synchronized (lock) {
            this.floorItems.clear();
            Arrays.stream(HFloorItem.parse(hMessage.getPacket()))
                    .map(FloorItem::new)
                    .forEach(this.floorItems::add);
        }
    }

    // {in:ObjectRemove}{s:"293310992"}{b:false}{i:11927526}{i:0}
    private void onObjectRemove(HMessage hMessage) {
        synchronized (lock) {
            this.floorItems.remove(getFloorItemById(Integer.parseInt(hMessage.getPacket().readString())));
        }
    }

    // {in:ObjectUpdate}{i:183318488}{i:4539}{i:5}{i:27}{i:0}{s:"0.0"}{s:"1.0E-6"}{i:0}{i:0}{s:"0"}{i:-1}{i:1}{i:11927526}
    private void onObjectUpdate(HMessage hMessage) {
        FloorItem updatedFloorItem = new FloorItem(new HFloorItem(hMessage.getPacket()));
        this.floorItems.replaceAll(floorItem -> {
            if(floorItem.id == updatedFloorItem.id) {
                return updatedFloorItem;
            }
            return floorItem;
        });
    }

    // {in:ObjectDataUpdate}{s:"183318488"}{i:0}{s:"3"}
    // {in:ObjectDataUpdate}{s:"165121049"}{i:2}{i:5}{s:"8"}{s:"172690"}{s:"b02064s02035s01057s85014s1709899bf1e8afbf33ec733d4721ce8198f24"}{s:"4ab2e7"}{s:"00539b"}
    private void onObjectDataUpdate(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        FloorItem item = getFloorItemById(Integer.parseInt(packet.readString()));
        if(item != null) {
            System.out.println(item.state);
            switch (packet.readInteger()) {
                case 0:
                    item.state = packet.readString();
                    break;
                case 2:
                    packet.readInteger();
                    item.state = packet.readString();
                    break;
            }
        }
    }

    public FloorItem getFloorItemById(int id) {
        synchronized (lock) {
            return this.floorItems.stream().filter(floorItem -> floorItem.id == id).findAny().orElse(null);
        }
    }
}
