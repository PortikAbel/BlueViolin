package Client;

import Server.Attribute;
import Server.Database;
import Server.Json;
import Server.Table;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class InsertRowsWindow implements Initializable {
    private MainWindow mainWindow;
    @FXML
    private TableView<ObservableList<SimpleStringProperty>> tableOfAttributes;
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


        for (int i = 0; i < tbl.getAttributes().size(); i++)
        {
            Attribute attribute = tbl.getAttributes().get(i);

            TextField textField = new TextField();
            textField.setPromptText(attribute.getName());
            valuesBox.getChildren().add(textField);

            TableColumn<ObservableList<SimpleStringProperty>, String> newColumn = new TableColumn<>();
            newColumn.setText(attribute.getName());
            newColumn.setEditable(true);
            setCellValueFactory(newColumn, i, attribute.getDataType().equalsIgnoreCase("int"));
            tableOfAttributes.getColumns().add(newColumn);
        }
    }

    private void setCellValueFactory(TableColumn<ObservableList<SimpleStringProperty>, String> column,
                                     final int index, final boolean integer)
    {
        column.setCellValueFactory(observableListString -> {
            if (integer)
                return observableListString.getValue().get(index);
            else
                return new SimpleStringProperty("'" + observableListString.getValue().get(index).getValue() + "'");
        });
    }

    public void setMainWindow(MainWindow mainWindow){
        this.mainWindow = mainWindow;
    }

    public void addRow() {
        ObservableList<SimpleStringProperty> row = FXCollections.observableArrayList();

        valuesBox.getChildren().forEach(
                attribute -> {
                    row.add(new SimpleStringProperty( ((TextField)attribute).getText() ));
                    ((TextField)attribute).clear();
                }
        );

        tableOfAttributes.getItems().add(row);
    }

    public void contextMenu(){
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteOption = new MenuItem("delete selected rows");
        deleteOption.setOnAction(event -> {
            ObservableList<ObservableList<SimpleStringProperty>> allAttributes, selectedAttributes;
            allAttributes = tableOfAttributes.getItems();
            selectedAttributes = tableOfAttributes.getSelectionModel().getSelectedItems();
            selectedAttributes.forEach(allAttributes::remove);
        });
        contextMenu.getItems().add(deleteOption);
        tableOfAttributes.setContextMenu(contextMenu);
    }

    public void insert() {
        mainWindow.insertRows(tableOfAttributes.getItems());
    }
}
