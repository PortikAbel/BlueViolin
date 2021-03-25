package Client;

import Server.Attribute;
import Server.Database;
import Server.Json;
import Server.Table;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuItem;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class CreateIndexWindow implements Initializable {
    private MainWindow mainWindow;

    @FXML
    private ChoiceBox<String> attributesMenu;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<Database> databases = null;
        try {
            databases = Json.buildDatabases();

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        assert databases != null;
        Database db = databases.stream()
                .filter(database ->
                        database.getName().equals(
                                MainWindow
                                        .getSelectedItem()
                                        .getParent()
                                        .getValue()
                        ))
                .findAny().orElse(null);

        assert db != null;
        List<Table> tables = db.getTables();
        Table tbl = tables.stream()
                .filter(table ->
                        table.getName().equals(
                                MainWindow.getSelectedItem().getValue()
                        ))
                .findAny().orElse(null);

        assert tbl != null;
        tbl.getAttributes().stream()
                .filter(Predicate.not(Attribute::isIndex))
                .forEach(
                        attribute -> attributesMenu.getItems().add(attribute.getName())
                );
    }

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public void create() {
        mainWindow.createIndex();
    }
}
