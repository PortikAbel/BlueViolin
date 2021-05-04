package Client.Control;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class NewDatabase {
    @FXML
    private TextField databaseNameTextField;

    @FXML
    private MainWindow mainWindow;

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public void create(){
        mainWindow.addDatabase(databaseNameTextField.getText());
        Stage stage = (Stage) databaseNameTextField.getScene().getWindow();
        stage.close();
    }
}
