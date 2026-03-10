package com.zhizun.licenseadmin;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LicenseAdminApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(LicenseAdminApp.class.getResource("/fxml/license-admin.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(LicenseAdminApp.class.getResource("/style/app.css").toExternalForm());

        stage.setTitle("授权管理器");
        stage.setMinWidth(860);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }
}
