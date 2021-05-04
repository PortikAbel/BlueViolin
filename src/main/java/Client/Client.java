package Client;

import Client.Control.MainWindow;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Client extends Application {
    private FXMLLoader loader;
    @Override
    public void start(Stage stage) throws Exception {
        loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("./View/MainWindow.fxml"));
        Parent root = loader.load();
        stage.setTitle("BlueViolin");
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Override
    public void stop() {
        MainWindow mainWindow = loader.getController();
        mainWindow.exitApplication();
    }

    public static void main(String[] args){
        launch(args);
    }
}
