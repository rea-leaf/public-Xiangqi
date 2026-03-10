package com.zhizun.licenseadmin.controller;

import com.zhizun.licenseadmin.license.DeviceFingerprint;
import com.zhizun.licenseadmin.license.LicenseInfo;
import com.zhizun.licenseadmin.license.LicenseSigner;
import com.zhizun.licenseadmin.license.LicensedFeature;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.EnumSet;

public class LicenseAdminController {

    @FXML
    private TextField outputLicenseField;
    @FXML
    private TextField holderField;
    @FXML
    private TextField editionField;
    @FXML
    private DatePicker expiresDatePicker;
    @FXML
    private CheckBox perpetualCheckBox;
    @FXML
    private TextField machineCodeField;
    @FXML
    private CheckBox engineFeatureCheckBox;
    @FXML
    private CheckBox analysisFeatureCheckBox;
    @FXML
    private CheckBox openingBookFeatureCheckBox;
    @FXML
    private CheckBox localBookFeatureCheckBox;
    @FXML
    private CheckBox linkFeatureCheckBox;
    @FXML
    private CheckBox moveVoiceFeatureCheckBox;
    @FXML
    private CheckBox manualScoreFeatureCheckBox;
    @FXML
    private TextArea logArea;

    private Stage stage;

    public void initialize() {
        editionField.setText("商业版");
        perpetualCheckBox.setSelected(true);
        machineCodeField.setText(DeviceFingerprint.machineCode());
        engineFeatureCheckBox.setSelected(true);
        analysisFeatureCheckBox.setSelected(true);
        appendLog("授权工具已启动，SM2 私钥已内置。");
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    void chooseOutputClick(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("授权文件输出路径");
        chooser.setInitialFileName("customer-license.lic");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("License", "*.lic"));
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            outputLicenseField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    void fillCurrentMachineCodeClick(ActionEvent event) {
        machineCodeField.setText(DeviceFingerprint.machineCode());
        appendLog("已填入当前机器设备码");
    }

    @FXML
    void generateLicenseClick(ActionEvent event) {
        try {
            validateForm();
            EnumSet<LicensedFeature> features = collectFeatures();
            LocalDate expiresAt = perpetualCheckBox.isSelected() ? null : expiresDatePicker.getValue();
            Path output = Path.of(outputLicenseField.getText().trim());
            LicenseSigner.signToFile(
                    output,
                    holderField.getText().trim(),
                    editionField.getText().trim(),
                    LocalDate.now(),
                    expiresAt,
                    machineCodeField.getText().trim(),
                    features
            );
            appendLog("授权文件生成成功: " + output.toAbsolutePath());
            appendLog("授权功能: " + new LicenseInfo("", "", LocalDate.now(), expiresAt,
                    machineCodeField.getText().trim(), features, "").getFeatureText());
        } catch (Exception ex) {
            appendLog("生成失败: " + ex.getMessage());
        }
    }

    @FXML
    void clearLogClick(ActionEvent event) {
        logArea.clear();
    }

    private void validateForm() {
        if (outputLicenseField.getText() == null || outputLicenseField.getText().isBlank()) {
            throw new IllegalArgumentException("请选择授权文件输出路径");
        }
        if (holderField.getText() == null || holderField.getText().isBlank()) {
            throw new IllegalArgumentException("请输入客户名称");
        }
        if (!perpetualCheckBox.isSelected() && expiresDatePicker.getValue() == null) {
            throw new IllegalArgumentException("请选择到期日期，或勾选永久授权");
        }
        if (machineCodeField.getText() == null || machineCodeField.getText().isBlank()) {
            throw new IllegalArgumentException("请输入设备码");
        }
        if (collectFeatures().isEmpty()) {
            throw new IllegalArgumentException("至少选择一个授权功能");
        }
    }

    private EnumSet<LicensedFeature> collectFeatures() {
        EnumSet<LicensedFeature> features = EnumSet.noneOf(LicensedFeature.class);
        if (engineFeatureCheckBox.isSelected()) {
            features.add(LicensedFeature.ENGINE);
        }
        if (analysisFeatureCheckBox.isSelected()) {
            features.add(LicensedFeature.ANALYSIS);
        }
        if (openingBookFeatureCheckBox.isSelected()) {
            features.add(LicensedFeature.OPENING_BOOK);
        }
        if (localBookFeatureCheckBox.isSelected()) {
            features.add(LicensedFeature.LOCAL_BOOK);
        }
        if (linkFeatureCheckBox.isSelected()) {
            features.add(LicensedFeature.LINK);
        }
        if (moveVoiceFeatureCheckBox.isSelected()) {
            features.add(LicensedFeature.MOVE_VOICE);
        }
        if (manualScoreFeatureCheckBox.isSelected()) {
            features.add(LicensedFeature.MANUAL_SCORE);
        }
        return features;
    }

    private void appendLog(String text) {
        if (logArea.getText() == null || logArea.getText().isBlank()) {
            logArea.setText(text);
        } else {
            logArea.appendText(System.lineSeparator() + text);
        }
    }
}
