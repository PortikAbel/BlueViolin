package Client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ClientMain extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("BlueViolin");

        StackPane layout = new StackPane();
        Scene scene = new Scene(layout, 450, 600);

        Button btnStart = new Button("START");
        layout.getChildren().add(btnStart);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args){
        launch(args);
    }
}
