package exportable;

import exportable.live.LiveWallItems;
import furnidata.FurniDataSearcher;
import gearth.extensions.parsers.HWallItem;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.scene.paint.Color;
import org.json.JSONArray;
import org.json.JSONObject;
import parsers.Inventory;
import utils.Executor;
import utils.Logger;
import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ExportableInfo(
    Name = "Wall Items",
    JsonTag = "wallItems"
)
public class WallItems extends Exportable {
    public List<WallItem> wallItems = new ArrayList<>();

    public WallItems(HPacket objectsPacket) {
        Arrays.stream(HWallItem.parse(objectsPacket))
                .map(WallItem::new)
                .forEach(this.wallItems::add);
    }

    public WallItems(JSONArray wallItemsImport) {
        wallItemsImport.forEach(o -> {
            JSONObject wallItemImport = (JSONObject) o;
            this.wallItems.add(new WallItem(wallItemImport));
        });
    }

    @Override
    public Object export(ProgressListener progressListener) {
        JSONArray exportedItems = new JSONArray();
        for(WallItem item: wallItems) {
            exportedItems.put(item.export((p) -> {}));
            progressListener.setProgress((double) wallItems.indexOf(item) / wallItems.size());
        }
        return exportedItems;
    }

    @Override
    public void doImport(Executor executor, List<Exportable> importingStates, Map<String, Exportable> currentStates, Inventory inventory, ProgressListener progressListener) {
        this.wallItems.stream()
                .filter(item -> item.typeId > 0)
                .forEach(item -> item.doImport(executor, importingStates, currentStates, inventory, progressListener));
    }

    protected class WallItem extends Exportable {
        public String classname;
        public String position;
        public String state;
        public int id, typeId;

        public WallItem(HWallItem item) {
            this.id = item.getId();
            this.typeId = item.getTypeId();
            this.position = item.getLocation();
            this.state = item.getState();

            this.classname = FurniDataSearcher
                    .getFurniDetailsByTypeID(this.typeId, FurniDataSearcher.FurniType.WALL)
                    .getClassName();
        }

        public WallItem(JSONObject wallItemImport) {
            this.classname = wallItemImport.getString("classname");
            this.id = wallItemImport.getInt("id");
            this.position = wallItemImport.getString("position");
            this.state = wallItemImport.getString("state");

            FurniDataSearcher.FurniDetails furniDetails =  FurniDataSearcher
                    .getFurniDetailsByClassName(this.classname, FurniDataSearcher.FurniType.WALL);
            if(furniDetails != null) {
                this.typeId = furniDetails.getTypeID();
            }
        }

        @Override
        public void doImport(Executor executor, List<Exportable> importingStates, Map<String, Exportable> currentStates, Inventory inventory, ProgressListener progressListener) {
            LiveWallItems currentWallItems = (LiveWallItems) currentStates.get("WallItems");

            Inventory.InvItem usedItem = inventory.getItemByTypeId(this.typeId, FurniDataSearcher.FurniType.WALL);
            if(usedItem != null) {
                Utils.placeWallItem(executor, usedItem.getItemID(), this.position);
                Utils.sleep(30);
                if(currentWallItems.getWallItemById(usedItem.getItemID()) != null) {
                    int tries = 0;
                    while (tries < 50
                            && this.state != null
                            && currentWallItems.getWallItemById(usedItem.getItemID()).state != null
                            && !this.state.equals(currentWallItems.getWallItemById(usedItem.getItemID()).state)) {
                        // {out:UseWallItem}{i:23211828}{i:0}
                        executor.sendToServer("UseWallItem", usedItem.getItemID(), 0);
                        // {in:ItemUpdate}{s:"23211828"}{i:4374}{s:":w=0,22 l=4,26 l"}{s:"0"}{i:-1}{i:1}{i:11927526}
                        executor.awaitPacket(
                                new Executor.AwaitingPacket("ItemUpdate", HMessage.Direction.TOCLIENT, 50)
                                        .addConditions(packet -> Integer.parseInt(packet.readString()) == usedItem.getItemID())
                                        .setMinWaitingTime(30)
                        );
                        tries++;
                    }
                } else {
                    Logger.log(Color.ORANGE, "Failed to place " + this.classname);
                }

                inventory.removeItemById(usedItem.getItemID());
            } else {
                Logger.log(Color.RED, this.classname + " not found, skipped!");
            }

            progressListener.setProgress((double) (currentWallItems.wallItems.size() + 1) / wallItems.size());
        }
    }
}
