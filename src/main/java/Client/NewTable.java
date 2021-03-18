package Client;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class NewTable {
    @FXML
    private TextField tableNameTextField;

    @FXML
    private MainWindow mainWindow;

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public void create(){
        mainWindow.addTable(tableNameTextField.getText());
        Stage stage = (Stage) tableNameTextField.getScene().getWindow();
        stage.close();
    }
}
