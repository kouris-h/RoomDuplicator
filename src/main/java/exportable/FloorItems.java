package exportable;

import furnidata.FurniDataSearcher;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HPoint;
import gearth.protocol.HPacket;
import org.json.JSONArray;
import org.json.JSONObject;
import parsers.Inventory;
import utils.Executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ExportableInfo(
    Name = "Floor Items",
    JsonTag = "floorItems"
)
public class FloorItems extends Exportable {
    public final List<FloorItem> floorItems = new ArrayList<>();

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
    public void doImport(Executor executor, Exportable currentState, Inventory inventory, ProgressListener progressListener) {
        // TODO
    }

    protected String getItemState(HFloorItem hItem) {
        if(hItem.getCategory() == 0) {
            return (String) hItem.getStuff()[0];
        }

        if(hItem.getCategory() == 2) {
            return (String) hItem.getStuff()[1];
        }

        return "0";
    }

    protected static class FloorItem extends Exportable {
        public String classname, state;
        public int id, typeId, x, y, dir, cat;
        public double z;

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

            this.typeId = FurniDataSearcher
                    .getFurniDetailsByClassName(this.classname, FurniDataSearcher.FurniType.FLOOR)
                    .getTypeID();
        }

        @Override
        public void doImport(Executor executor, Exportable currentState, Inventory inventory, ProgressListener progressListener) {
            // TODO
        }
    }
}
