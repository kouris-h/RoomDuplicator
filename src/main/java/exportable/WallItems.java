package exportable;

import furnidata.FurniDataSearcher;
import gearth.extensions.parsers.HWallItem;
import gearth.protocol.HPacket;
import org.json.JSONArray;
import org.json.JSONObject;
import parsers.Inventory;
import utils.Executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ExportableInfo(
    Name = "Wall Items",
    JsonTag = "wallItems"
)
public class WallItems extends Exportable {
    public final List<WallItem> wallItems = new ArrayList<>();

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
    public void doImport(Executor executor, Map<String, Exportable> currentStates, Inventory inventory, ProgressListener progressListener) {
        // TODO
    }

    protected static class WallItem extends Exportable {
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

            this.typeId = FurniDataSearcher
                    .getFurniDetailsByClassName(this.classname, FurniDataSearcher.FurniType.WALL)
                    .getTypeID();
        }

        @Override
        public void doImport(Executor executor, Map<String, Exportable> currentStates, Inventory inventory, ProgressListener progressListener) {
            // TODO
        }
    }
}
