package exportable;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.scene.control.Alert;
import org.json.JSONObject;
import parsers.Inventory;
import utils.Executor;

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

    @Override
    public void doImport(Executor executor, Exportable currentState, Inventory inventory, ProgressListener progressListener) {
        progressListener.setProgress(0d);
        executor.sendToServer("UpdateFloorProperties", floorPlan, doorX, doorY, doorDir, 0, 0);
        progressListener.setProgress(0.5d);
        executor.awaitPacket(new Executor.AwaitingPacket("GetGuestRoomResult", HMessage.Direction.TOCLIENT, 1000)
                .addConditions(HPacket::readBoolean));
        progressListener.setProgress(1d);
    }
}
