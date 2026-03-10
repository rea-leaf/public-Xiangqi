package com.chf.chess.controller;

import com.chf.chess.license.LicenseInfo;
import com.chf.chess.license.LicenseManager;
import com.chf.chess.util.ClipboardUtils;
import com.chf.chess.util.DialogUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * 授权管理窗口。
 */
public class LicenseController {

    @FXML
    private Label statusLabel;
    @FXML
    private Label holderLabel;
    @FXML
    private Label editionLabel;
    @FXML
    private Label expireLabel;
    @FXML
    private TextField machineCodeField;
    @FXML
    private TextArea featuresArea;
    @FXML
    private TextField filePathField;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void initialize() {
        refreshView();
    }

    @FXML
    void refreshClick(ActionEvent event) {
        LicenseManager.getInstance().reload();
        refreshView();
    }

    @FXML
    void importClick(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择授权文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("License", "*.lic", "*.properties"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        if (!LicenseManager.getInstance().importLicense(file)) {
            DialogUtils.showWarningDialog("授权导入失败", LicenseManager.getInstance().getStatusText());
        }
        refreshView();
    }

    @FXML
    void clearClick(ActionEvent event) {
        LicenseManager.getInstance().clearLicense();
        refreshView();
    }

    @FXML
    void copyMachineCodeClick(ActionEvent event) {
        ClipboardUtils.setText(machineCodeField.getText());
    }

    @FXML
    void closeClick(ActionEvent event) {
        if (stage != null) {
            stage.close();
        }
    }

    private void refreshView() {
        LicenseManager manager = LicenseManager.getInstance();
        LicenseInfo info = manager.getCurrent();
        statusLabel.setText(manager.getStatusText());
        machineCodeField.setText(manager.getMachineCode());
        filePathField.setText(manager.getLicensePath());
        if (info == null) {
            holderLabel.setText("-");
            editionLabel.setText("-");
            expireLabel.setText("-");
            featuresArea.setText("未导入有效授权");
            return;
        }
        holderLabel.setText(info.getHolder().isBlank() ? "-" : info.getHolder());
        editionLabel.setText(info.getEdition().isBlank() ? "-" : info.getEdition());
        expireLabel.setText(info.getExpiresAt() == null ? "永久" : info.getExpiresAt().toString());
        featuresArea.setText(info.getFeatureText());
    }
}
