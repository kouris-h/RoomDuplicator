package exportable;

import org.json.JSONArray;
import org.json.JSONObject;
import parsers.Inventory;
import utils.Executor;

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
    public void doImport(Executor executor, Exportable currentState, Inventory inventory, ProgressListener progressListener) {
        // TODO
    }
}
