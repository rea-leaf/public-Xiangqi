package com.chf.chess.controller;

import com.chf.chess.App;
import com.chf.chess.config.Properties;
import com.chf.chess.enginee.Engine;
import com.chf.chess.util.DialogUtils;
import com.chf.chess.util.StringUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;


/**
 * TimeSettingController 类。
 * JavaFX 界面控制器，负责对应对话框/页面交互。
 */
public class TimeSettingController {

    @FXML
    private RadioButton fixTimeButton;

    @FXML
    private TextField timeText;

    @FXML
    private RadioButton fixDepthButton;

    @FXML
    private TextField depthText;

    @FXML
    private TextField engineDelayStart;

    @FXML
    private TextField engineDelayEnd;

    @FXML
    private TextField bookDelayStart;

    @FXML
    private TextField bookDelayEnd;

    @FXML
    private TextField autoOpeningMinTime;

    @FXML
    private TextField autoOpeningMaxTime;

    @FXML
    private TextField autoMiddleMinTime;

    @FXML
    private TextField autoMiddleMaxTime;

    @FXML
    private TextField autoEndMinTime;

    @FXML
    private TextField autoEndMaxTime;


    private Properties prop;

    @FXML
    void cancelButtonClick(ActionEvent e) {
        App.closeTimeSetting();
    }

    @FXML
    void okButtonClick(ActionEvent e) {
        if (fixDepthButton.isSelected()) {
            String txt = depthText.getText();
            if (!StringUtils.isPositiveInt(txt)) {
                DialogUtils.showErrorDialog("失败", "层数错误");
                return;
            }
            prop.setAnalysisModel(Engine.AnalysisModel.FIXED_STEPS);
            prop.setAnalysisValue(Long.parseLong(txt));
        } else {
            String txt = timeText.getText();
            if (!StringUtils.isPositiveInt(txt)) {
                DialogUtils.showErrorDialog("失败", "时间错误");
                return;
            }
            prop.setAnalysisModel(Engine.AnalysisModel.FIXED_TIME);
            prop.setAnalysisValue(Long.parseLong(txt));
        }

        String txt = engineDelayStart.getText();
        if (!StringUtils.isNonNegativeInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入引擎出招延迟错误");
            return;
        }
        prop.setEngineDelayStart(Integer.parseInt(txt));
        txt = engineDelayEnd.getText();
        if (!StringUtils.isNonNegativeInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入引擎出招延迟错误");
            return;
        }
        prop.setEngineDelayEnd(Integer.parseInt(txt));

        txt = bookDelayStart.getText();
        if (!StringUtils.isNonNegativeInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入库招出招延迟错误");
            return;
        }
        prop.setBookDelayStart(Integer.parseInt(txt));
        txt = bookDelayEnd.getText();
        if (!StringUtils.isNonNegativeInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入库招出招延迟错误");
            return;
        }
        prop.setBookDelayEnd(Integer.parseInt(txt));

        txt = autoOpeningMinTime.getText();
        if (!StringUtils.isPositiveInt(txt)) {
            DialogUtils.showErrorDialog("失败", "开局最小时间错误");
            return;
        }
        int openingMin = Integer.parseInt(txt);
        if (openingMin < 1 || openingMin > 90) {
            DialogUtils.showErrorDialog("失败", "开局最小时间应在1-90秒");
            return;
        }

        txt = autoOpeningMaxTime.getText();
        if (!StringUtils.isPositiveInt(txt)) {
            DialogUtils.showErrorDialog("失败", "开局最大时间错误");
            return;
        }
        int openingMax = Integer.parseInt(txt);
        if (openingMax < 1 || openingMax > 90) {
            DialogUtils.showErrorDialog("失败", "开局最大时间应在1-90秒");
            return;
        }
        if (openingMax < openingMin) {
            DialogUtils.showErrorDialog("失败", "开局最大时间不能小于最小时间");
            return;
        }

        txt = autoMiddleMinTime.getText();
        if (!StringUtils.isPositiveInt(txt)) {
            DialogUtils.showErrorDialog("失败", "总局最小时间错误");
            return;
        }
        int middleMin = Integer.parseInt(txt);
        if (middleMin < 1 || middleMin > 90) {
            DialogUtils.showErrorDialog("失败", "总局最小时间应在1-90秒");
            return;
        }

        txt = autoMiddleMaxTime.getText();
        if (!StringUtils.isPositiveInt(txt)) {
            DialogUtils.showErrorDialog("失败", "总局最大时间错误");
            return;
        }
        int middleMax = Integer.parseInt(txt);
        if (middleMax < 1 || middleMax > 90) {
            DialogUtils.showErrorDialog("失败", "总局最大时间应在1-90秒");
            return;
        }
        if (middleMax < middleMin) {
            DialogUtils.showErrorDialog("失败", "总局最大时间不能小于最小时间");
            return;
        }

        txt = autoEndMinTime.getText();
        if (!StringUtils.isPositiveInt(txt)) {
            DialogUtils.showErrorDialog("失败", "残局最小时间错误");
            return;
        }
        int endMin = Integer.parseInt(txt);
        if (endMin < 1 || endMin > 90) {
            DialogUtils.showErrorDialog("失败", "残局最小时间应在1-90秒");
            return;
        }

        txt = autoEndMaxTime.getText();
        if (!StringUtils.isPositiveInt(txt)) {
            DialogUtils.showErrorDialog("失败", "残局最大时间错误");
            return;
        }
        int endMax = Integer.parseInt(txt);
        if (endMax < 1 || endMax > 90) {
            DialogUtils.showErrorDialog("失败", "残局最大时间应在1-90秒");
            return;
        }
        if (endMax < endMin) {
            DialogUtils.showErrorDialog("失败", "残局最大时间不能小于最小时间");
            return;
        }

        prop.setAutoBattleOpeningMinTime(openingMin);
        prop.setAutoBattleOpeningMaxTime(openingMax);
        prop.setAutoBattleMiddleMinTime(middleMin);
        prop.setAutoBattleMiddleMaxTime(middleMax);
        prop.setAutoBattleEndMinTime(endMin);
        prop.setAutoBattleEndMaxTime(endMax);

        App.closeTimeSetting();
    }


    public void initialize() {

        ToggleGroup group = new ToggleGroup();
        fixTimeButton.setToggleGroup(group);
        fixDepthButton.setToggleGroup(group);

        prop = Properties.getInstance();
        if (prop.getAnalysisModel() == Engine.AnalysisModel.FIXED_TIME) {
            fixTimeButton.setSelected(true);
            timeText.setText(String.valueOf(prop.getAnalysisValue()));
        } else {
            fixDepthButton.setSelected(true);
            depthText.setText(String.valueOf(prop.getAnalysisValue()));
        }

        engineDelayStart.setText(String.valueOf(prop.getEngineDelayStart()));
        engineDelayEnd.setText(String.valueOf(prop.getEngineDelayEnd()));

        bookDelayStart.setText(String.valueOf(prop.getBookDelayStart()));
        bookDelayEnd.setText(String.valueOf(prop.getBookDelayEnd()));

        autoOpeningMinTime.setText(String.valueOf(prop.getAutoBattleOpeningMinTime()));
        autoOpeningMaxTime.setText(String.valueOf(prop.getAutoBattleOpeningMaxTime()));
        autoMiddleMinTime.setText(String.valueOf(prop.getAutoBattleMiddleMinTime()));
        autoMiddleMaxTime.setText(String.valueOf(prop.getAutoBattleMiddleMaxTime()));
        autoEndMinTime.setText(String.valueOf(prop.getAutoBattleEndMinTime()));
        autoEndMaxTime.setText(String.valueOf(prop.getAutoBattleEndMaxTime()));

    }

}
