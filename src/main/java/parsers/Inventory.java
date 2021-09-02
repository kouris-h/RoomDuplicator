package parsers;

import exportable.Exportable.ProgressListener;
import furnidata.FurniDataSearcher;
import furnidata.FurniDataSearcher.FurniType;
import gearth.extensions.parsers.HStuff;
import gearth.protocol.HPacket;
import utils.Executor;
import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

    public InvItem getItemByTypeId(int typeId, FurniType furniType) {
        synchronized (lock) {
            return invItems.stream()
                    .filter(invItem -> invItem.furniType.equals(furniType))
                    .filter(invItem -> invItem.typeID == typeId)
                    .findAny().orElse(null);
        }
    }

    public void removeItemById(int id) {
        synchronized (lock) {
            invItems.removeIf(item -> item.itemID == id);
        }
    }

    public StackTiles getStackTiles() {
        FurniDataSearcher.FurniDetails smallStackTile = FurniDataSearcher.getFurniDetailsByClassName("tile_stackmagic", FurniType.FLOOR);
        FurniDataSearcher.FurniDetails mediumStackTile = FurniDataSearcher.getFurniDetailsByClassName("tile_stackmagic1", FurniType.FLOOR);
        FurniDataSearcher.FurniDetails largeStackTile = FurniDataSearcher.getFurniDetailsByClassName("tile_stackmagic2", FurniType.FLOOR);

        return new StackTiles(invItems.parallelStream()
                .filter(invItem -> invItem.getTypeID() == smallStackTile.getTypeID() ||
                        invItem.getTypeID() == mediumStackTile.getTypeID() ||
                        invItem.getTypeID() == largeStackTile.getTypeID())
                .collect(Collectors.toList()));
    }

    public static class InvItem {
        private final FurniType furniType;
        private final int itemID, typeID, category;
        private final Object[] stuffData;

        private final boolean recyclable, tradeable, groupable, sellable, rentPeriodStarted;
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

            recyclable = packet.readBoolean();
            tradeable = packet.readBoolean();
            groupable = packet.readBoolean();
            sellable = packet.readBoolean();
            seconds_to_expiration = packet.readInteger();
            rentPeriodStarted = packet.readBoolean();
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

    public static class StackTiles {
        private final HashMap<InvItem, Boolean> usedTiles = new HashMap<>();
        private final HashMap<InvItem, FurniDataSearcher.FurniDetails> tilesDetails = new HashMap<>();

        public StackTiles(List<InvItem> tiles) {
            tiles.forEach(tile -> {
                this.usedTiles.put(tile, false);
                this.tilesDetails.put(tile, FurniDataSearcher.getFurniDetailsByTypeID(tile.getTypeID(), FurniType.FLOOR));
            });
        }

        public InvItem useStackTile(String classname) {
            InvItem tile = usedTiles.keySet().stream()
                    .filter(i -> !usedTiles.get(i))
                    .filter(i -> tilesDetails.get(i).getClassName().equals(classname))
                    .findFirst().orElse(null);
            usedTiles.replace(tile, true);
            return tile;
        }

        public boolean isEmpty() {
            return usedTiles.isEmpty();
        }

        public void reset() {
            usedTiles.keySet().forEach(i -> usedTiles.replace(i, false));
        }

        public List<InvItem> getAsList() {
            return new ArrayList<>(usedTiles.keySet());
        }

        public void setHeight(Executor executor, int z) {
            usedTiles.keySet().forEach(i -> {
                executor.sendToServer("SetCustomStackingHeight", i.getItemID(), z);
                Utils.sleep(16);
            });
        }

        public void pickUp(Executor executor) {
            usedTiles.keySet().forEach(tile -> {
                executor.sendToServer("PickupObject", 2, tile.getItemID());
                Utils.sleep(50);
            });
        }

        public void placeAll(Executor executor, int x, int y) {
            usedTiles.keySet().forEach(tile -> Utils.placeFloorItem(executor, tile.itemID, x, y, 0));
        }

        public boolean supportItem(Executor executor, int x, int y, int xDim, int yDim, double z) {
            reset();
            boolean[][] covered = new boolean[xDim][yDim];
            boolean fullyCovered = true;
            for(int i = 0; i < covered.length; i++) {
                for(int j = 0; j < covered[i].length; j++) {
                    if(!covered[i][j]) {
                        if (i + 1 < covered.length && j + 1 < covered[i].length) {
                            InvItem tile = useStackTile("tile_stackmagic2");
                            if(tile != null) {
                                Utils.moveObject(executor, tile.itemID, x + i, y + j, 0);
                                covered[i][j] = true;
                                covered[i+1][j] = true;
                                covered[i][j+1] = true;
                                covered[i+1][j+1] = true;
                            }
                        }

                        if(!covered[i][j] && i + 1 < covered.length) {
                            InvItem tile = useStackTile("tile_stackmagic1");
                            if(tile != null) {
                                Utils.moveObject(executor, tile.itemID, x + i, y + j, 0);
                                covered[i][j] = true;
                                covered[i+1][j] = true;
                            }
                        }

                        if(!covered[i][j] && j + 1 < covered[i].length) {
                            InvItem tile = useStackTile("tile_stackmagic1");
                            if(tile != null) {
                                Utils.moveObject(executor, tile.itemID, x + i, y + j, 2);
                                covered[i][j] = true;
                                covered[i][j+1] = true;
                            }
                        }

                        if(!covered[i][j]) {
                            InvItem tile = useStackTile("tile_stackmagic");
                            if(tile != null) {
                                Utils.moveObject(executor, tile.itemID, x + i, y + j, 0);
                                covered[i][j] = true;
                            } else {
                                fullyCovered = false;
                            }
                        }
                    }
                }
            }
            setHeight(executor, (int) Math.floor(z * 100));
            Utils.sleep(50);
            return fullyCovered;
        }
    }
}
