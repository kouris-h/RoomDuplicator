package exportable;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.StageStyle;
import org.json.JSONObject;
import parsers.Inventory;
import utils.Executor;

@ExportableInfo(
    Name = "Room Settings",
    JsonTag = "roomData"
)
public class RoomData extends Exportable {
    public String password;
    public final String name, description, ownerName;
    public final int id, ownerId, lockType, maxUsers, tradingMode, category, freeflow, chatSize, scrollMethod, chatDistance, floodMode, muteRights, kickRights, banRights, wallThickness, floorThickness;
    public final boolean wallsHidden, allowPets;

    public RoomData(HPacket roomPacket, HPacket visualizationPacket) {
        roomPacket.setReadIndex(7);

        this.id = roomPacket.readInteger();
        this.name = roomPacket.readString();
        this.ownerId = roomPacket.readInteger();
        this.ownerName = roomPacket.readString();
        this.lockType = roomPacket.readInteger();

        roomPacket.readInteger();

        this.maxUsers = roomPacket.readInteger();
        this.description = roomPacket.readString();
        this.tradingMode = roomPacket.readInteger();

        roomPacket.readInteger();
        roomPacket.readInteger();

        this.category = roomPacket.readInteger();

        // Skip all tags
        int tagCount = roomPacket.readInteger();
        for(int i = 0; i < tagCount; i ++) {
            roomPacket.readString();
        }

        final int THUMBNAIL_BITMASK = 1;
        final int GROUPDATA_BITMASK = 2;
        final int ROOMAD_BITMASK = 4;
        final int SHOWOWNER_BITMASK  = 8;
        final int ALLOWPETS_BITMASK = 16;
        final int DISPLAYROOMENTRYAD_BITMASK = 32;

        int multiUse = roomPacket.readInteger();

        // skip Official room pic if present
        if((multiUse & THUMBNAIL_BITMASK) > 0) {
            roomPacket.readString();
        }

        // Skip group info if present
        if((multiUse & GROUPDATA_BITMASK) > 0) {
            roomPacket.readInteger();
            roomPacket.readString();
            roomPacket.readString();
        }

        // Skip event info if present
        if((multiUse & ROOMAD_BITMASK) > 0) {
            roomPacket.readString();
            roomPacket.readString();
            roomPacket.readInteger();
        }

        this.allowPets = (multiUse & ALLOWPETS_BITMASK) > 0;

        roomPacket.readInteger();

        this.muteRights = roomPacket.readInteger();
        this.kickRights = roomPacket.readInteger();
        this.banRights = roomPacket.readInteger();

        roomPacket.readBoolean();

        this.freeflow = roomPacket.readInteger();
        this.chatSize = roomPacket.readInteger();
        this.scrollMethod = roomPacket.readInteger();
        this.chatDistance = roomPacket.readInteger();
        this.floodMode = roomPacket.readInteger();

        visualizationPacket.resetReadIndex();
        this.wallsHidden = visualizationPacket.readBoolean();
        this.wallThickness = visualizationPacket.readInteger();
        this.floorThickness = visualizationPacket.readInteger();
    }

    public RoomData(JSONObject roomDataImport) {
        this.id = roomDataImport.getInt("id");
        this.name = roomDataImport.getString("name");
        this.ownerId = roomDataImport.getInt("ownerId");
        this.ownerName = roomDataImport.getString("ownerName");
        this.lockType = roomDataImport.getInt("lockType");
        if(this.lockType == 2) {
            this.password = roomDataImport.getString("password");
        }
        this.maxUsers = roomDataImport.getInt("maxUsers");
        this.description = roomDataImport.getString("description");
        this.tradingMode = roomDataImport.getInt("tradingMode");
        this.category = roomDataImport.getInt("category");

        this.allowPets = roomDataImport.getBoolean("allowPets");

        this.muteRights = roomDataImport.getInt("muteRights");
        this.kickRights = roomDataImport.getInt("kickRights");
        this.banRights = roomDataImport.getInt("banRights");
        this.freeflow = roomDataImport.getInt("freeflow");
        this.chatSize = roomDataImport.getInt("chatSize");
        this.scrollMethod = roomDataImport.getInt("scrollMethod");
        this.chatDistance = roomDataImport.getInt("chatDistance");
        this.floodMode = roomDataImport.getInt("floodMode");
        this.wallsHidden = roomDataImport.getBoolean("wallsHidden");
        this.wallThickness = roomDataImport.getInt("wallThickness");
        this.floorThickness = roomDataImport.getInt("floorThickness");
    }

    @Override
    public Object export(ProgressListener progressListener) {
        JSONObject export = (JSONObject) super.export(progressListener);
        if(export.getInt("lockType") == 2 && this.password == null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setHeaderText("Locktype is set to password, enter a new room password below or select a different locktype!");
            dialog.setContentText("Password:");
            dialog.setTitle("Locktype");
            dialog.initStyle(StageStyle.UNDECORATED);

            ButtonType openButton = new ButtonType("Open", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType doorbellButton = new ButtonType("Doorbell", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType invisibleButton = new ButtonType("Invisible", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType setPasswordButton = new ButtonType("SetPassWord", ButtonBar.ButtonData.RIGHT);

            dialog.getDialogPane().getButtonTypes().clear();
            dialog.getDialogPane().getButtonTypes()
                    .addAll(openButton,
                            doorbellButton,
                            invisibleButton,
                            setPasswordButton);

            dialog.getDialogPane().lookupButton(setPasswordButton).setDisable(true);

            dialog.getEditor().textProperty().addListener((observable, oldValue, newValue) -> dialog.getDialogPane().lookupButton(setPasswordButton).setDisable(newValue.isEmpty()));

            dialog.getDialogPane().lookupButton(openButton).addEventHandler(ActionEvent.ACTION, event -> export.put("lockType", 0));
            dialog.getDialogPane().lookupButton(doorbellButton).addEventHandler(ActionEvent.ACTION, event -> export.put("lockType", 1));
            dialog.getDialogPane().lookupButton(invisibleButton).addEventHandler(ActionEvent.ACTION, event -> export.put("lockType", 3));
            dialog.getDialogPane().lookupButton(setPasswordButton).addEventHandler(ActionEvent.ACTION, event -> export.put("password", dialog.getEditor().getText()));

            dialog.showAndWait();
        }
        return export;
    }

    @Override
    public void doImport(Executor executor, Exportable currentState, Inventory inventory, ProgressListener progressListener) {
        executor.sendToServer("SaveRoomSettings",
                ((RoomData) currentState).id, name, description, lockType, password != null ? password : "",
                maxUsers, category, /*tagCount*/ 0, tradingMode, allowPets, /*allowFoodConsume*/ false, /*allowWalkThrough*/ true,
                wallsHidden, wallThickness, floorThickness, muteRights, kickRights, banRights,
                freeflow, chatSize, scrollMethod, chatDistance, floodMode, /*showRoomByFurniInNav*/ false);
    }
}
