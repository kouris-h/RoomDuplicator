package exportable;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.scene.control.Alert;
import javafx.util.Pair;
import org.json.JSONObject;
import parsers.Inventory;
import utils.Executor;

import java.util.Map;

@ExportableInfo(
    Name = "Floorplan",
    JsonTag = "floorPlan"
)
public class FloorPlan extends Exportable {
    public final String floorPlan;
    public final int doorX, doorY, doorDir;

    public FloorPlan(HPacket floorPlanPacket, HPacket doorPacket) {
        this.floorPlan = floorPlanPacket.readString(11);
        this.doorX = doorPacket.readInteger(6);
        this.doorY = doorPacket.readInteger(10);
        this.doorDir = doorPacket.readInteger(14);
    }

    public FloorPlan(JSONObject floorImport) {
        this.floorPlan = floorImport.getString("floorPlan");
        this.doorX = floorImport.getInt("doorX");
        this.doorY = floorImport.getInt("doorY");
        this.doorDir = floorImport.getInt("doorDir");
    }

    public Pair<Integer, Integer> getOpenSpot(int minSize) {
        String[] splitFloorPlan = floorPlan.split("\r");
        for(int y = 0; y < splitFloorPlan.length - minSize; y++) {
            for(int x = 0; x < splitFloorPlan[y].length() - minSize; x++) {
                if(splitFloorPlan[y].charAt(x) != 'x') {
                    boolean possible = true;
                    char height = splitFloorPlan[y].charAt(x);
                    for(int i = 0; i < minSize; i++) {
                        for(int j = 0; j < minSize; j++) {
                            possible = splitFloorPlan[y + i].charAt(x + j) == height;
                                if(!possible) break;
                        }
                        if(!possible) break;
                    }
                    if(possible) return new Pair<>(x, y);
                }
            }
        }
        return new Pair<>(1, 1);
    }

    @Override
    public void doImport(Executor executor, Map<String, Exportable> currentStates, Inventory inventory, ProgressListener progressListener) {
        progressListener.setProgress(0d);
        executor.sendToServer("UpdateFloorProperties", floorPlan, doorX, doorY, doorDir, 0, 0);
        progressListener.setProgress(0.5d);
        executor.awaitPacket(new Executor.AwaitingPacket("GetGuestRoomResult", HMessage.Direction.TOCLIENT, 1000)
                .addConditions(HPacket::readBoolean));
        progressListener.setProgress(1d);
    }
}
