package exportable;

import exportable.live.LiveFloorItems;
import furnidata.FurniDataSearcher;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HPoint;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import parsers.Inventory;
import utils.Executor;
import utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

@ExportableInfo(
    Name = "Floor Items",
    JsonTag = "floorItems"
)
public class FloorItems extends Exportable {
    public List<FloorItem> floorItems = new ArrayList<>();
    public Inventory.StackTiles stackTiles;
    public List<Object> unstackables;

    public FloorItems(HPacket objectsPacket) {
        objectsPacket.resetReadIndex();
        Arrays.stream(HFloorItem.parse(objectsPacket))
                .map(FloorItem::new)
                .forEach(this.floorItems::add);
    }

    public FloorItems(JSONArray floorItemsImport) {
        floorItemsImport.forEach(o -> {
            JSONObject floorItemImport = (JSONObject) o;
            this.floorItems.add(new FloorItem(floorItemImport));
        });
    }

    @Override
    public Object export(ProgressListener progressListener) {
        JSONArray exportedItems = new JSONArray();
        for(FloorItem item: floorItems) {
            exportedItems.put(item.export((p) -> {}));
            progressListener.setProgress((double) floorItems.indexOf(item) / floorItems.size());
        }
        return exportedItems;
    }

    @Override
    public void doImport(Executor executor, List<Exportable> importingStates, Map<String, Exportable> currentStates, Inventory inventory, ProgressListener progressListener) {
        this.floorItems = this.floorItems.stream().filter(item -> item.typeId > 0).collect(Collectors.toList());

        this.floorItems.forEach(item -> item.fixState(executor, currentStates, inventory, progressListener));
        this.stackTiles = inventory.getStackTiles();

        this.unstackables = new JSONArray(new JSONTokener(Objects.requireNonNull(this.getClass().getResourceAsStream("/json/unstackable.json")))).toList();

        this.floorItems.sort(Comparator.comparingInt(item -> item.x)); // Work from back to front
        this.floorItems.sort(Comparator.comparingInt(item -> item.y)); // Work from back to front
        this.floorItems.sort(Comparator.comparingDouble(item -> item.z)); // Work from bottom to top

        // First place unstackables
        this.floorItems.stream().filter(i -> unstackables.contains(i.classname)).forEach(item -> item.doImport(executor, importingStates, currentStates, inventory, progressListener));

        // Place stacktiles
        Pair<Integer, Integer> stackSpace = ((FloorPlan) currentStates.get("FloorPlan")).getOpenSpot(2);
        this.stackTiles.placeAll(executor, stackSpace.getKey(), stackSpace.getValue());

        // Place stackables
        this.floorItems.stream().filter(i -> !unstackables.contains(i.classname)).forEach(item -> item.doImport(executor, importingStates, currentStates, inventory, progressListener));
        this.stackTiles.pickUp(executor);
    }

    protected class FloorItem extends Exportable {
        public String classname, state;
        public int id, typeId, x, y, dir, cat;
        public double z;

        public int newId, xDim, yDim;

        public FloorItem(HFloorItem hItem) {
            this.id = hItem.getId();
            this.typeId = hItem.getTypeId();
            HPoint tile = hItem.getTile();
            this.x = tile.getX();
            this.y = tile.getY();
            this.z = tile.getZ();
            this.dir = hItem.getFacing().ordinal();

            if(hItem.getCategory() == 0) {
                this.state = (String) hItem.getStuff()[0];
            }

            if(hItem.getCategory() == 2) {
                this.state = (String) hItem.getStuff()[1];
            }

            this.classname = FurniDataSearcher
                    .getFurniDetailsByTypeID(this.typeId, FurniDataSearcher.FurniType.FLOOR)
                    .getClassName();
        }

        public FloorItem(JSONObject floorItemImport) {
            this.id = floorItemImport.getInt("id");
            this.classname = floorItemImport.getString("classname");
            this.x = floorItemImport.getInt("x");
            this.y = floorItemImport.getInt("y");
            this.z = floorItemImport.getDouble("z");
            this.dir = floorItemImport.getInt("dir");
            this.state = floorItemImport.getString("state");

            FurniDataSearcher.FurniDetails furniDetails = FurniDataSearcher
                    .getFurniDetailsByClassName(this.classname, FurniDataSearcher.FurniType.FLOOR);
            System.out.println(this.classname);
            if(furniDetails != null) {
                this.typeId = furniDetails.getTypeID();
                this.xDim = furniDetails.getXDim();
                this.yDim = furniDetails.getYDim();
            }
        }

        public void fixState(Executor executor, Map<String, Exportable> currentStates, Inventory inventory, ProgressListener progressListener) {
            LiveFloorItems currentFloorItems = (LiveFloorItems) currentStates.get("FloorItems");
            FloorPlan currentFloorPlan = (FloorPlan) currentStates.get("FloorPlan");

            Inventory.InvItem usedItem = inventory.getItemByTypeId(this.typeId, FurniDataSearcher.FurniType.FLOOR);
            if(usedItem != null) {
                this.newId = usedItem.getItemID();
                Pair<Integer, Integer> openSpot = currentFloorPlan.getOpenSpot(Math.max(this.xDim, this.yDim));

                Utils.placeFloorItem(executor, this.newId, openSpot.getKey(), openSpot.getValue(), this.dir);

                FloorItem currentItem = currentFloorItems.getFloorItemById(this.newId);
                if (currentItem != null) {
                    int tries = 0;
                    boolean done = false;
                    while(!done && tries < 10) {
                        executor.sendToServer("GetRoomEntryTile");
                        HPacket occupiedTilesPacket = executor.awaitPacket(new Executor.AwaitingPacket("RoomOccupiedTiles", HMessage.Direction.TOCLIENT, 500));
                        if(occupiedTilesPacket != null) {
                            int n = occupiedTilesPacket.readInteger();
                            int xMin = 50, xMax = 0, yMin = 50, yMax = 0;
                            for (int i = 0; i < n; i++) {
                                int x = occupiedTilesPacket.readInteger();
                                int y = occupiedTilesPacket.readInteger();
                                if (xMin > x) xMin = x;
                                if (xMax < x) xMax = x;
                                if (yMin > y) yMin = y;
                                if (yMax < y) yMax = y;
                            }
                            this.yDim = yMax - yMin + 1;
                            this.xDim = xMax - xMin + 1;
                            done = true;
                        }
                        tries++;
                    }

                    tries = 0;
                    while(tries < 50
                            && this.state != null
                            && currentItem.state != null
                            && !this.state.equals(currentFloorItems.getFloorItemById(this.newId).state)) {
                        executor.sendToServer("UseFurniture", this.newId, 0);
                        executor.awaitPacketList(
                                new Executor.AwaitingPacket("ObjectDataUpdate", HMessage.Direction.TOCLIENT, 150)
                                        .addConditions(packet -> Integer.parseInt(packet.readString()) == this.newId)
                                        .setMinWaitingTime(100),
                                new Executor.AwaitingPacket("ObjectUpdate", HMessage.Direction.TOCLIENT, 150)
                                        .addConditions(packet -> packet.readInteger() == this.newId)
                                        .setMinWaitingTime(100)
                        );
                        tries++;
                    }
                }

                do {
                    executor.sendToServer("PickupObject", 2, this.newId);
                    Utils.sleep(30);
                } while(currentFloorItems.getFloorItemById(this.newId) != null);

                inventory.removeItemById(this.newId);
            } else {
                // TODO log item not found
                System.out.println(this.classname + " skipped");
            }

            progressListener.setProgress((double) floorItems.stream().filter(i -> i.newId > 0).count() / floorItems.size());
        }

        @Override
        public void doImport(Executor executor, List<Exportable> importingStates, Map<String, Exportable> currentStates, Inventory inventory, ProgressListener progressListener) {
            if(this.newId > 0) {
                if(!unstackables.contains(this.classname)) {
                    if (!stackTiles.supportItem(executor, this.x, this.y, this.xDim, this.yDim, this.z)) {
                        System.out.println("Object not fully supported by stacktiles might not be placed");
                        // TODO log
                    }
                }
                Utils.placeFloorItem(executor, this.newId, this.x, this.y, this.dir);
            }

            LiveFloorItems currentFloorItems = (LiveFloorItems) currentStates.get("FloorItems");

            progressListener.setProgress((double) currentFloorItems.floorItems.size() / floorItems.stream().filter(i -> i.newId > 0).count());
        }

        @Override
        public String toString() {
            return "FloorItem{" +
                    "classname='" + classname + '\'' +
                    ", state='" + state + '\'' +
                    ", id=" + id +
                    ", typeId=" + typeId +
                    ", x=" + x +
                    ", y=" + y +
                    ", dir=" + dir +
                    ", cat=" + cat +
                    ", z=" + z +
                    '}';
        }
    }
}
