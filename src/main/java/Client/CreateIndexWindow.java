package Client;

import Server.Database;
import Server.Json;
import Server.Table;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CreateIndexWindow implements Initializable {
    private MainWindow mainWindow;

    @FXML
    private VBox attributesMenuBox;
    @FXML
    private ChoiceBox<String> attributesChoice;
    @FXML
    private TextField indexNameTextField;
    @FXML
    private Button removeButton, createButton;

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
                .filter(attribute -> attribute.getIndex().equals(""))
                .forEach(
                        attribute -> attributesChoice.getItems().add(attribute.getName())
                );
    }

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public void addMoreAttribute() {
        attributesMenuBox.getChildren().add(new ChoiceBox<>(attributesChoice.getItems()));
        if (attributesMenuBox.getChildren().size() > 1)
            removeButton.setDisable(false);
    }

    public void removeLast() {
        attributesMenuBox.getChildren().remove(attributesMenuBox.getChildren().size() - 1);
        if (attributesMenuBox.getChildren().size() <= 1)
            removeButton.setDisable(true);
    }

    public void enableCreateButton() {
        createButton.setDisable(indexNameTextField.getText().equals(""));
    }

    public void create() {
        mainWindow.createIndex(indexNameTextField.getText(),
                attributesMenuBox.getChildren().stream()
                .map(attribute -> ((ChoiceBox<String>)attribute).getValue())
                .collect(Collectors.joining(","))
        );
    }
}
