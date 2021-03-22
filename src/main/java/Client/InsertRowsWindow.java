package Client;

import Server.Database;
import Server.Json;
import Server.Table;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class InsertRowsWindow implements Initializable {
    private MainWindow mainWindow;
    @FXML
    private TableView<ArrayList<String>> tableOfAttributes;
    @FXML
    private HBox valuesBox;
    @FXML
    private Label tableNameLabel;

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
        tableNameLabel.setText(tbl.getName());
        tbl.getAttributes().forEach(
                attribute -> {
                    TableColumn<ArrayList<String>, String> newColumn = new TableColumn<>();
                    newColumn.setText(attribute.getName());
                    tableOfAttributes.getColumns().add(newColumn);

                    TextField textField = new TextField();
                    textField.setPromptText(attribute.getName());
                    valuesBox.getChildren().add(textField);
                }
        );
    }

    public void setMainWindow(MainWindow mainWindow){
        this.mainWindow = mainWindow;
    }

    public void insert() {
        mainWindow.insertRows(tableOfAttributes.getItems());
    }
}
