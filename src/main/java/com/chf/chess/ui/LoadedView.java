package com.chf.chess.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * FXML 加载结果，统一封装 root、scene 和 controller。
 */
public record LoadedView(FXMLLoader loader, Parent root, Scene scene) {

    @SuppressWarnings("unchecked")
    public <T> T controller() {
        return (T) loader.getController();
    }
}
