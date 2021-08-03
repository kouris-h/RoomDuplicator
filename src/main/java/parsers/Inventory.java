package parsers;

import exportable.Exportable.ProgressListener;
import furnidata.FurniDataSearcher.FurniType;
import gearth.extensions.parsers.HStuff;
import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Inventory {
    private final List<InvItem> invItems = new ArrayList<>();
    private int itemTotal = 0;
    final Object lock = new Object();

    public Inventory(HPacket ...furniListPackets) {
        this(p -> {}, furniListPackets);
    }

    public Inventory(ProgressListener progressListener, HPacket ...furniListPackets) {
        Arrays.stream(furniListPackets).parallel()
                .forEach(furniListPacket -> {
                    furniListPacket.setReadIndex(10);
                    itemTotal += furniListPacket.readInteger();
                    int n = furniListPacket.readInteger();
                    for(int i = 0; i < n; i++) {
                        InvItem invItem = new InvItem(furniListPacket);
                        synchronized (lock) {
                            invItems.add(invItem);
                        }
                        progressListener.setProgress((double) invItems.size() / itemTotal);
                    }
                });
    }

    public static class InvItem {
        private final FurniType furniType;
        private final int itemID, typeID, category;
        private final Object[] stuffData;

        private final boolean is_groupable, is_tradeable, market_place_allowed, has_rent_period_started;
        private final int seconds_to_expiration, room_Id;

        private String slotID;

        public InvItem(HPacket packet) {
            packet.readInteger();
            String type = packet.readString();
            furniType = type.equals("S") ? FurniType.FLOOR : FurniType.WALL;


            itemID = packet.readInteger();
            typeID = packet.readInteger();

            packet.readInteger();

            category = packet.readInteger();

            stuffData = HStuff.readData(packet, category);

            is_groupable = packet.readBoolean();
            is_tradeable = packet.readBoolean();
            packet.readBoolean();
            market_place_allowed = packet.readBoolean();
            seconds_to_expiration = packet.readInteger();
            has_rent_period_started = packet.readBoolean();
            room_Id = packet.readInteger();

            if (this.furniType == FurniType.FLOOR) {
                slotID = packet.readString();
                packet.readInteger();
            }
        }

        public FurniType getFurniType() {
            return furniType;
        }

        public int getItemID() {
            return itemID;
        }

        public int getTypeID() {
            return typeID;
        }
    }
}
