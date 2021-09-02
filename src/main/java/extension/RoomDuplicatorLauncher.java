package extension;

import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class RoomDuplicatorLauncher extends ExtensionFormCreator {

    @Override
    protected ExtensionForm createForm(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(RoomDuplicator.class.getClassLoader().getResource("fxml/roomduplicator.fxml"));
        Parent root = loader.load();

        stage.setTitle("RoomDuplicator");
        stage.setScene(new Scene(root));
        stage.getIcons().add(new Image(this.getClass().getResource("/images/duck_icon.png").openStream()));

        stage.setResizable(false);

        return loader.getController();
    }

    public static void main(String[] args) {
        runExtensionForm(args, RoomDuplicatorLauncher.class);
    }
}
