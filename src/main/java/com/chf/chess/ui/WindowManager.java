package com.chf.chess.ui;

import com.chf.chess.App;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 统一处理窗口与场景创建，并挂载全局主题。
 */
public final class WindowManager {

    private static final String THEME_CSS = "/style/theme.css";

    private WindowManager() {
    }

    public static LoadedView load(String resource) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource(resource));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        applyTheme(scene);
        return new LoadedView(loader, root, scene);
    }

    public static Stage createModalStage(String resource, String title, Stage owner) throws IOException {
        LoadedView view = load(resource);
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(view.scene());
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        return stage;
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        String css = WindowManager.class.getResource(THEME_CSS).toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
    }
}
