package furnidata;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public class FurniDataSearcher {

    private static final HashMap<Object, FurniDetails> floorFurni = new HashMap<>();
    private static final HashMap<Object, FurniDetails> wallFurni = new HashMap<>();

    protected static JSONObject object;

    public static void fetch(String host, OnLoadedListener onLoadedListener) {
        try {
            object = new JSONObject(IOUtils.toString(new URL(getFurniDataUrl(host)).openStream(), StandardCharsets.UTF_8));
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }

        parseItems(FurniType.FLOOR);
        parseItems(FurniType.WALL);

        onLoadedListener.act();
    }

    private static void parseItems(FurniType furniType) {
        String type = furniType ==  FurniType.FLOOR ? "roomitemtypes" : "wallitemtypes";

        object.getJSONObject(type).getJSONArray("furnitype").forEach(o -> {
            JSONObject item = (JSONObject) o;
            String className = item.getString("classname");
            int typeID = item.getInt("id");
            if(furniType == FurniType.FLOOR) {
                FurniDetails furniDetails = new FurniDetails(typeID, className, item.getInt("xdim"), item.getInt("ydim"));

                floorFurni.put(className, furniDetails);
                floorFurni.put(item.getInt("id"), furniDetails);
            } else {
                FurniDetails furniDetails = new FurniDetails(typeID, className);

                wallFurni.put(className, furniDetails);
                wallFurni.put(item.getInt("id"), furniDetails);
            }
        });
    }

    private static String getFurniDataUrl(String hotelServer) {
        switch(hotelServer) {
            case "game-nl.habbo.com":
                return "https://www.habbo.nl/gamedata/furnidata_json/1";
            case "game-br.habbo.com":
                return "https://www.habbo.com.br/gamedata/furnidata_json/1";
            case "game-tr.habbo.com":
                return "https://www.habbo.com.tr/gamedata/furnidata_json/1";
            case "game-de.habbo.com":
                return "https://www.habbo.de/gamedata/furnidata_json/1";
            case "game-fr.habbo.com":
                return "https://www.habbo.fr/gamedata/furnidata_json/1";
            case "game-fi.habbo.com":
                return "https://www.habbo.fi/gamedata/furnidata_json/1";
            case "game-es.habbo.com":
                return "https://www.habbo.es/gamedata/furnidata_json/1";
            case "game-it.habbo.com":
                return "https://www.habbo.it/gamedata/furnidata_json/1";
            case "game-s2.habbo.com":
                return "https://sandbox.habbo.com/gamedata/furnidata_json/1";
            default:
                return "https://www.habbo.com/gamedata/furnidata_json/1";
        }
    }

    public static FurniDetails getFurniDetailsByClassName(String className, FurniType furniType) {
        // TODO cleanup
        if(furniType == FurniType.FLOOR) {
            return floorFurni.get(className);
        }

        return wallFurni.get(className);
    }

    public static FurniDetails getFurniDetailsByTypeID(int typeID, FurniType furniType) {
        // TODO cleanup
        FurniDetails f;
        if(furniType == FurniType.FLOOR) {
            f = floorFurni.get(typeID);
        } else f = wallFurni.get(typeID);

        return f;
    }

    public interface OnLoadedListener {
        void act();
    }

    public enum FurniType {
        FLOOR,
        WALL
    }

    public static class FurniDetails {
        private final String className;
        private int XDim, YDim;
        private final int typeID;

        public FurniDetails(int typeID, String className, int XDim, int YDim) {
            this.typeID = typeID;
            this.className = className;
            this.XDim = XDim;
            this.YDim = YDim;
        }

        public FurniDetails(int typeID, String className) {
            this.typeID = typeID;
            this.className = className;
        }

        public String getClassName() {
            return className;
        }

        public int getXDim() {
            return XDim;
        }

        public int getYDim() {
            return YDim;
        }

        public int getTypeID() {
            return typeID;
        }

        @Override
        public String toString() {
            return "FurniDetails{" +
                    "className='" + className + '\'' +
                    ", XDim=" + XDim +
                    ", YDim=" + YDim +
                    ", typeID=" + typeID +
                    '}';
        }
    }
}

