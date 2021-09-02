package exportable.live;

import exportable.WallItems;
import gearth.extensions.ExtensionBase;
import gearth.extensions.parsers.HWallItem;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.util.Arrays;

public class LiveWallItems extends WallItems {
    final Object lock = new Object();

    public LiveWallItems(ExtensionBase extension, HPacket objectsPacket) {
        super(objectsPacket);

        extension.intercept(HMessage.Direction.TOCLIENT, "Items", this::onItems);
        extension.intercept(HMessage.Direction.TOCLIENT, "ItemAdd", this::onItemAdd);
        extension.intercept(HMessage.Direction.TOCLIENT, "ItemRemove", this::onItemRemove);
        extension.intercept(HMessage.Direction.TOCLIENT, "MoveWallItem", this::onMoveWallItem);
        extension.intercept(HMessage.Direction.TOCLIENT, "ItemUpdate", this::onItemUpdate);
    }

    private void onItems(HMessage hMessage) {
        synchronized (lock) {
            this.wallItems.clear();
            HPacket packet = hMessage.getPacket();
            packet.resetReadIndex();
            System.out.println(packet.toExpression());
            Arrays.stream(HWallItem.parse(packet))
                    .map(WallItems.WallItem::new)
                    .forEach(this.wallItems::add);
        }
    }

    // {in:ItemAdd}{s:"43942084"}{i:4685}{s:":w=0,26 l=12,32 l"}{s:"1"}{i:-1}{i:1}{i:11927526}{s:"WiredSpast"}
    private void onItemAdd(HMessage hMessage) {
        synchronized (lock) {
            HPacket packet = hMessage.getPacket();
            packet.resetReadIndex();
            System.out.println(packet.toExpression());
            this.wallItems.add(new WallItems.WallItem(new HWallItem(packet)));
        }
    }

    //{in:ItemRemove}{s:"43942084"}{i:11927526}
    private void onItemRemove(HMessage hMessage) {
        synchronized (lock) {
            HPacket packet = hMessage.getPacket();
            packet.resetReadIndex();
            System.out.println(packet.toExpression());
            this.wallItems.remove(getWallItemById(Integer.parseInt(packet.readString())));
        }
    }

    //{out:MoveWallItem}{i:43942084}{s:":w=0,24 l=0,36 l"}
    private void onMoveWallItem(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        packet.resetReadIndex();
        System.out.println(packet.toExpression());
        WallItems.WallItem item = getWallItemById(Integer.parseInt(packet.readString()));
        if(item != null) {
            item.position = packet.readString();
        }
    }

    //{in:ItemUpdate}{s:"43942084"}{i:4685}{s:":w=0,24 l=0,36 l"}{s:"2"}{i:-1}{i:1}{i:11927526}
    private void onItemUpdate(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        packet.resetReadIndex();
        System.out.println(packet.toExpression());
        WallItem updatedWallItem = new WallItem(new HWallItem(packet));
        synchronized (lock) {
            this.wallItems.replaceAll(wallItem -> {
                if (wallItem.id == updatedWallItem.id) {
                    return updatedWallItem;
                }
                return wallItem;
            });
        }
    }

    public WallItem getWallItemById(int id) {
        synchronized (lock) {
            return this.wallItems.stream().filter(wallItem -> wallItem.id == id).findAny().orElse(null);
        }
    }
}
