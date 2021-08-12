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
        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "SlideObjectBundle", this::onSlideObjectBundle);
    }

    private void onObjects(HMessage hMessage) {
        synchronized (lock) {
            this.floorItems.clear();
            Arrays.stream(HFloorItem.parse(hMessage.getPacket()))
                    .map(FloorItem::new)
                    .forEach(this.floorItems::add);
        }
    }

    private void onObjectAdd(HMessage hMessage) {
        synchronized (lock) {
            this.floorItems.add(new FloorItem(new HFloorItem(hMessage.getPacket())));
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
        synchronized (lock) {
            this.floorItems.replaceAll(floorItem -> {
                if (floorItem.id == updatedFloorItem.id) {
                    return updatedFloorItem;
                }
                return floorItem;
            });
        }
    }

    // {in:ObjectDataUpdate}{s:"183318488"}{i:0}{s:"3"}
    // {in:ObjectDataUpdate}{s:"165121049"}{i:2}{i:5}{s:"8"}{s:"172690"}{s:"b02064s02035s01057s85014s1709899bf1e8afbf33ec733d4721ce8198f24"}{s:"4ab2e7"}{s:"00539b"}
    private void onObjectDataUpdate(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        FloorItem item = getFloorItemById(Integer.parseInt(packet.readString()));
        if(item != null) {
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

    // {in:SlideObjectBundle}{i:7}{i:24}{i:6}{i:24}{i:1}{i:127188621}{s:"0.0"}{s:"0.0"}{i:-1}
    // {in:SlideObjectBundle}{i:12}{i:22}{i:12}{i:21}{i:2}{i:282751805}{s:"0.45"}{s:"0.45"}{i:282751804}{s:"0.45"}{s:"0.45"}{i:254014123}
    private void onSlideObjectBundle(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        packet.readInteger(); // oldX
        packet.readInteger(); // oldY
        int newX = packet.readInteger();
        int newY = packet.readInteger();
        int n = packet.readInteger();
        for(int i = 0; i < n; i++) {
            FloorItem item = getFloorItemById(packet.readInteger());
            packet.readString(); // oldZ
            double newZ = Double.parseDouble(packet.readString());
            if(item != null) {
                item.x = newX;
                item.y = newY;
                item.z = newZ;
            }
        }

        // int for roller or wired id
    }

    public FloorItem getFloorItemById(int id) {
        synchronized (lock) {
            return this.floorItems.stream().filter(floorItem -> floorItem.id == id).findAny().orElse(null);
        }
    }
}
