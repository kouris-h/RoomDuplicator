package exportable;

import org.json.JSONArray;
import org.json.JSONObject;
import parsers.Inventory;
import utils.Executor;

import java.util.Map;

@ExportableInfo(
        Name = "Wired Settings",
        JsonTag = "wiredSettings"
)
public class WiredSettings extends Exportable {
    public WiredSettings(JSONArray jsonArray) {
        // TODO
    }
    // TODO

    @Override
    public JSONObject export(ProgressListener progressListener) {
        // TODO
        return null;
    }

    @Override
    public void doImport(Executor executor, Map<String, Exportable> currentStates, Inventory inventory, ProgressListener progressListener) {
        // TODO
    }
}
