package Client;

import Server.Database;
import Server.Json;
import Server.Table;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainWindow implements Initializable {
    private TreeItem<String> root;
    private TreeItem<String> selectedDatabase;

    private TreeView<String> treeView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        root = new TreeItem<>("databases");
        root.setExpanded(true);

        try {
            List<Database> databases = Json.buildDatabases();
            for(Database database : databases) {
                TreeItem<String> newDatabaseItem = new TreeItem<>(database.getName());
                for (Table table : database.getTables()) {
                    newDatabaseItem.getChildren().add(new TreeItem<>(table.getName()));
                }
                root.getChildren().add(newDatabaseItem);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        treeView.setRoot(root);
        treeView.setOnMouseClicked( e -> {
            if (e.getButton().equals(MouseButton.SECONDARY)) {

            }
        });
    }
}
