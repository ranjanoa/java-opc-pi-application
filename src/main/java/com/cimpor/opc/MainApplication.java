package com.cimpor.opc;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL fxmlLocation = getClass().getResource("/main_view.fxml");
        Parent root = FXMLLoader.load(fxmlLocation);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Data@Glance Java Archiver");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
