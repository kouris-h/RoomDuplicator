package exportable;

import org.json.JSONObject;
import parsers.Inventory;
import utils.Executor;

import java.lang.reflect.Field;
import java.util.Map;

public abstract class Exportable {
    public Object export(ProgressListener progressListener) {
        JSONObject jsonObject = new JSONObject();

        Field[] params = this.getClass().getDeclaredFields();

        for(int i = 0; i < params.length; i++) {
            try {
                jsonObject.put(params[i].getName(), String.valueOf(params[i].get(this)));
                progressListener.setProgress((double) (i + 1) / params.length);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return jsonObject;
    }

    public abstract void doImport(Executor executor, Map<String, Exportable> currentStates, Inventory inventory, ProgressListener progressListener);

    public interface ProgressListener {
        void setProgress(double p);
    }
}
