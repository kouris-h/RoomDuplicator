package exportable;

import gearth.protocol.HPacket;
import org.json.JSONArray;
import org.json.JSONObject;
import parsers.Inventory;
import utils.Executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ExportableInfo(
        Name = "Wired Settings",
        JsonTag = "wiredSettings"
)
public class WiredSettings extends Exportable {
    private List<Wired> wireds = new ArrayList<>();

    public WiredSettings() {}

    public WiredSettings(JSONArray jsonArray) {
        // TODO
    }

    public void addWired(HPacket packet) {
        // TODO
    }

    @Override
    public JSONObject export(ProgressListener progressListener) {
        // TODO
        return null;
    }

    @Override
    public void doImport(Executor executor, List<Exportable> importingStates, Map<String, Exportable> currentStates, Inventory inventory, ProgressListener progressListener) {
        // TODO
    }

    public static class Wired {
        private String classname;
        private int typeid, id, delay;
        private List<Integer> selectionIds;
        private String text;
        private List<Integer> modes;
    }
}
