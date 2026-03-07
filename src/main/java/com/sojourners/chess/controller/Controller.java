package com.sojourners.chess.controller;

import com.sojourners.chess.App;
import com.sojourners.chess.board.ChessBoard;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.controller.handle.ChessManualCallBack;
import com.sojourners.chess.controller.handle.ChessManualHandle;
import com.sojourners.chess.enginee.Engine;
import com.sojourners.chess.enginee.EngineCallBack;
import com.sojourners.chess.linker.*;
import com.sojourners.chess.menu.BoardContextMenu;
import com.sojourners.chess.model.BookData;
import com.sojourners.chess.model.EngineConfig;
import com.sojourners.chess.model.ManualRecord;
import com.sojourners.chess.model.ThinkData;
import com.sojourners.chess.openbook.OpenBookManager;
import com.sojourners.chess.util.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 主界面控制器。
 *
 * <p>这是应用的业务编排中心，负责协调：
 * - 棋盘与走子交互
 * - 引擎加载与搜索
 * - 连线识别与自动走棋
 * - 开局库展示与出招
 * - 棋谱浏览、编辑与保存
 *
 * <p>设计上该类偏“总控”，因此状态较多，阅读时建议按以下主线理解：
 * 1) initialize() 初始化所有组件
 * 2) goCallBack() 在每次落子后推进全局状态
 * 3) engineGo()/engineStop() 管理引擎思考生命周期
 * 4) linkerXXX 回调管理连线模式与同步
 */
public class Controller implements EngineCallBack, LinkerCallBack, ChessManualCallBack {

    @FXML
    private Canvas canvas;

    @FXML
    private BorderPane borderPane;
    @FXML
    private Label infoShowLabel;
    @FXML
    private ToolBar statusToolBar;
    @FXML
    private Label timeShowLabel;
    @FXML
    private SplitPane splitPane;
    @FXML
    private SplitPane splitPane2;

    @FXML
    private BorderPane listViewPane;

    @FXML
    private ListView<ThinkData> listView;

    private ListView<String> annotationListView;

    @FXML
    private ComboBox<String> engineComboBox;

    @FXML
    private ToolBar engineTopToolBar;

    @FXML
    private Label engineTopTitleLabel;

    @FXML
    private Label hashUnitLabel;

    @FXML
    private ComboBox<String> linkComboBox;

    @FXML
    private ComboBox<String> hashComboBox;

    @FXML
    private ComboBox<String> threadComboBox;

    @FXML
    private RadioMenuItem menuOfLargeBoard;
    @FXML
    private RadioMenuItem menuOfBigBoard;
    @FXML
    private RadioMenuItem menuOfMiddleBoard;
    @FXML
    private RadioMenuItem menuOfSmallBoard;
    @FXML
    private RadioMenuItem menuOfAutoFitBoard;

    @FXML
    private RadioMenuItem menuOfDefaultBoard;
    @FXML
    private RadioMenuItem menuOfCustomBoard;

    @FXML
    private CheckMenuItem menuOfStepTip;
    @FXML
    private CheckMenuItem menuOfStepSound;
    @FXML
    private CheckMenuItem menuOfLinkBackMode;
    @FXML
    private CheckMenuItem menuOfLinkAnimation;
    @FXML
    private CheckMenuItem menuOfShowStatus;
    @FXML
    private CheckMenuItem menuOfShowNumber;
    @FXML
    private CheckMenuItem menuOfColloquialReview;
    @FXML
    private RadioMenuItem menuOfShowEngineLog;
    @FXML
    private RadioMenuItem menuOfShowAnnotation;

    @FXML
    private CheckMenuItem menuOfTopWindow;

    private Properties prop;

    private Engine engine;

    private ChessBoard board;

    private AbstractGraphLinker graphLinker;

    @FXML
    private Button analysisButton;
    @FXML
    private Button blackButton;
    @FXML
    private Button redButton;
    @FXML
    private Button reverseButton;
    @FXML
    private Button newButton;
    @FXML
    private Button copyButton;
    @FXML
    private Button pasteButton;
    @FXML
    private Button regretButton;

    @FXML
    private BorderPane charPane;
    private XYChart.Series lineChartSeries;

    @FXML
    private Button immediateButton;
    @FXML
    private Button bookSwitchButton;
    @FXML
    private Button linkButton;
    @FXML
    private Button changeTacticButton;

    @FXML
    private TableView<ManualRecord> recordTable;

    @FXML
    private TableView<BookData> bookTable;

    private SimpleObjectProperty<Boolean> robotRed = new SimpleObjectProperty<>(false);
    private SimpleObjectProperty<Boolean> robotBlack = new SimpleObjectProperty<>(false);
    private SimpleObjectProperty<Boolean> robotAnalysis = new SimpleObjectProperty<>(false);
    private SimpleObjectProperty<Boolean> isReverse = new SimpleObjectProperty<>(false);
    private SimpleObjectProperty<Boolean> linkMode = new SimpleObjectProperty<>(false);
    private SimpleObjectProperty<Boolean> useOpenBook = new SimpleObjectProperty<>(false);

    /**
     * 走棋方
     */
    private boolean redGo;

    /**
     * 正在思考（用于连线判断）
     */
    private volatile boolean isThinking;

    private static final int AUTO_BATTLE_MIN_TIME = 1000;
    private static final int AUTO_BATTLE_MAX_TIME = 90000;
    private static final int AUTO_BATTLE_OPENING_SPLIT = 24;
    private static final int AUTO_BATTLE_MIDDLE_SPLIT = 80;

    private long lastHumanThinkTime = -1L;
    private int lastComplexityBand = -1;
    private int sameComplexityBandStreak = 0;
    private int redComboStreak = 0;
    private int blackComboStreak = 0;

    /**
     * 变招列表
     */
    private List<String> tacticList;

    private final Map<String, String> engineDescMap = new LinkedHashMap<>();

    @FXML
    public void newButtonClick(ActionEvent event) {
        if (linkMode.getValue()) {
            stopGraphLink();
        }

        newChessBoard(null);
    }

    @FXML
    void boardStyleSelected(ActionEvent event) {
        RadioMenuItem item = (RadioMenuItem) event.getTarget();
        if (item.equals(menuOfDefaultBoard)) {
            prop.setBoardStyle(ChessBoard.BoardStyle.DEFAULT);
        } else {
            prop.setBoardStyle(ChessBoard.BoardStyle.CUSTOM);
        }
        board.setBoardStyle(prop.getBoardStyle(), this.canvas);
    }

    @FXML
    void boardSizeSelected(ActionEvent event) {
        RadioMenuItem item = (RadioMenuItem) event.getTarget();
        if (item.equals(menuOfLargeBoard)) {
            prop.setBoardSize(ChessBoard.BoardSize.LARGE_BOARD);
        } else if (item.equals(menuOfBigBoard)) {
            prop.setBoardSize(ChessBoard.BoardSize.BIG_BOARD);
        } else if (item.equals(menuOfMiddleBoard)) {
            prop.setBoardSize(ChessBoard.BoardSize.MIDDLE_BOARD);
        } else if (item.equals(menuOfAutoFitBoard)) {
            prop.setBoardSize(ChessBoard.BoardSize.AUTOFIT_BOARD);
        } else {
            prop.setBoardSize(ChessBoard.BoardSize.SMALL_BOARD);
        }
        board.setBoardSize(prop.getBoardSize());
        if (prop.getBoardSize() == ChessBoard.BoardSize.AUTOFIT_BOARD) {
            board.autoFitSize(borderPane.getWidth(), borderPane.getHeight(), splitPane.getDividerPositions()[0]);
        }
    }
    @FXML
    void stepTipChecked(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setStepTip(item.isSelected());
        board.setStepTip(prop.isStepTip());
    }

    @FXML
    void showNumberClick(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setShowNumber(item.isSelected());
        board.setShowNumber(prop.isShowNumber());
    }

    @FXML
    void topWindowClick(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setTopWindow(item.isSelected());
        App.topWindow(prop.isTopWindow());
    }

    @FXML
    void linkBackModeChecked(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        if (linkMode.getValue()) {
            stopGraphLink();
        }
        prop.setLinkBackMode(item.isSelected());
    }

    @FXML
    void linkAnimationChecked(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setLinkAnimation(item.isSelected());
    }

    @FXML
    void stepSoundClick(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setStepSound(item.isSelected());
        board.setStepSound(prop.isStepSound());
    }

    @FXML
    void showEngineLogModeClick(ActionEvent event) {
        prop.setShowEngineLog(true);
        applyEngineLogVisibility();
    }

    @FXML
    void showAnnotationModeClick(ActionEvent event) {
        prop.setShowEngineLog(false);
        applyEngineLogVisibility();
    }

    @FXML
    void colloquialReviewClick(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setColloquialReviewStyle(item.isSelected());
        if (board != null) {
            board.setColloquialReviewStyle(prop.isColloquialReviewStyle());
        }
    }

    @FXML
    void showStatusBarClick(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setLinkShowInfo(item.isSelected());
        statusToolBar.setVisible(item.isSelected());
        board.autoFitSize(borderPane.getWidth(), borderPane.getHeight(), splitPane.getDividerPositions()[0]);
    }

    @FXML
    public void analysisButtonClick(ActionEvent event) {
        if (engine == null) {
            DialogUtils.showWarningDialog("提示", "引擎未加载");
            return;
        }

        robotAnalysis.setValue(!robotAnalysis.getValue());
        if (robotAnalysis.getValue()) {
            robotRed.setValue(false);
            robotBlack.setValue(false);
            engineGo();
        } else {
            engineStop();
        }

        redButton.setDisable(robotAnalysis.getValue());
        blackButton.setDisable(robotAnalysis.getValue());
        immediateButton.setDisable(robotAnalysis.getValue());

        if (linkMode.getValue() && !robotAnalysis.getValue()) {
            stopGraphLink();
        }
    }

    private void engineStop() {
        if (engine != null) {
            engine.stop();
        }
    }

    @FXML
    public void immediateButtonClick(ActionEvent event) {
        if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue()) {
            if (engine != null) {
                engine.moveNow();
            }
        }
    }

    @FXML
    public void changeTacticButtonClick(ActionEvent event) {
        if (robotRed.getValue() && redGo || robotBlack.getValue() && !redGo || robotAnalysis.getValue()) {
            engineStop();
            if (tacticList == null || tacticList.size() <= 1) {
                tacticList = board.getTacticList(redGo);
            }
            if (!listView.getItems().isEmpty()) {
                for (ThinkData td : listView.getItems()) {
                    if (td.getPv() == 1) {
                        tacticList.remove(td.getDetail().get(0));
                        break;
                    }
                }
            }
            engine.setThreadNum(prop.getThreadNum());
            engine.setHashSize(prop.getHashSize());
            engine.setAnalysisModel(robotAnalysis.getValue() ? Engine.AnalysisModel.INFINITE : prop.getAnalysisModel(), prop.getAnalysisValue());
            engine.analysis(chessManualHandle.getFenCode(), chessManualHandle.getMoveList(), tacticList);
        }
    }

    @FXML
    public void blackButtonClick(ActionEvent event) {
        if (engine == null) {
            DialogUtils.showWarningDialog("提示", "引擎未加载");
            return;
        }

        robotBlack.setValue(!robotBlack.getValue());
        if (robotBlack.getValue() && !redGo) {
            engineGo();
        }
        if (!robotBlack.getValue() && !redGo) {
            engineStop();
        }

        if (linkMode.getValue() && !robotBlack.getValue()) {
            stopGraphLink();
        }
    }

    @FXML
    public void engineManageClick(ActionEvent e) {
        App.openEngineDialog();
        // 重新设置引擎列表
        refreshEngineComboBox();
        // 如果引擎被卸载，则关闭
        if (StringUtils.isEmpty(prop.getEngineName())) {
            // 重置按钮
            robotRed.setValue(false);
            robotBlack.setValue(false);
            robotAnalysis.setValue(false);
            // 关闭引擎
            if (engine != null) {
                engine.close();
                engine = null;
            }
        }
    }

    @FXML
    public void redButtonClick(ActionEvent event) {
        if (engine == null) {
            DialogUtils.showWarningDialog("提示", "引擎未加载");
            return;
        }

        robotRed.setValue(!robotRed.getValue());
        if (robotRed.getValue() && redGo) {
            engineGo();
        }
        if (!robotRed.getValue() && redGo) {
            engineStop();
        }

        if (linkMode.getValue() && !robotRed.getValue()) {
            stopGraphLink();
        }
    }

    private void stopGraphLink() {
        graphLinker.stop();

        engineStop();

        redButton.setDisable(false);
        robotRed.setValue(false);

        blackButton.setDisable(false);
        robotBlack.setValue(false);

        analysisButton.setDisable(false);
        robotAnalysis.setValue(false);

        linkMode.setValue(false);
    }

    private void engineGo() {
        if (engine == null) {
            DialogUtils.showWarningDialog("提示", "引擎未加载");
            return;
        }

        if (robotRed.getValue() && redGo || robotBlack.getValue() && !redGo) {
            this.isThinking = true;
        } else {
            this.isThinking = false;
        }

        // 重置变招列表
        tacticList = null;

        engine.setThreadNum(prop.getThreadNum());
        engine.setHashSize(prop.getHashSize());
        Engine.AnalysisModel model = robotAnalysis.getValue() ? Engine.AnalysisModel.INFINITE : prop.getAnalysisModel();
        long value = prop.getAnalysisValue();
        boolean autoBattle = !robotAnalysis.getValue() && robotRed.getValue() && robotBlack.getValue();
        if (autoBattle) {
            model = Engine.AnalysisModel.FIXED_TIME;
            value = nextHumanLikeThinkTime(redGo);
            if (prop.isLinkShowInfo()) {
                timeShowLabel.setText("自动对弈思考" + value / 1000d + "s");
            }
        }
        engine.setAnalysisModel(model, value);
        if (autoBattle) {
            // 自动对弈时跳过库招，确保每步都按随机 movetime 真正思考。
            engine.analysis(chessManualHandle.getFenCode(), chessManualHandle.getMoveList(), (List<String>) null);
        } else {
            engine.analysis(chessManualHandle.getFenCode(), chessManualHandle.getMoveList(), this.board.getBoard(), redGo);
        }
    }

    private long nextHumanLikeThinkTime(boolean currentRedGo) {
        int ply = chessManualHandle.getP();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        int stageMin;
        int stageMax;
        if (ply < AUTO_BATTLE_OPENING_SPLIT) {
            stageMin = prop.getAutoBattleOpeningMinTime();
            stageMax = prop.getAutoBattleOpeningMaxTime();
        } else if (ply < AUTO_BATTLE_MIDDLE_SPLIT) {
            stageMin = prop.getAutoBattleMiddleMinTime();
            stageMax = prop.getAutoBattleMiddleMaxTime();
        } else {
            stageMin = prop.getAutoBattleEndMinTime();
            stageMax = prop.getAutoBattleEndMaxTime();
        }

        long minRange = Math.max(AUTO_BATTLE_MIN_TIME, stageMin * 1000L);
        long maxRange = Math.min(AUTO_BATTLE_MAX_TIME, stageMax * 1000L);
        if (minRange > maxRange) {
            maxRange = minRange;
        }
        long range = maxRange - minRange;
        double complexity = estimatePositionComplexity(currentRedGo);
        int complexityBand = complexity < 0.38d ? 0 : complexity < 0.62d ? 1 : 2;
        if (complexityBand == lastComplexityBand) {
            sameComplexityBandStreak++;
        } else {
            sameComplexityBandStreak = 1;
            lastComplexityBand = complexityBand;
        }

        // 复杂局面更靠近区间上沿，简单局面更靠近下沿。
        double targetRatio = 0.35d + complexity * 0.45d;
        // 双方设置轻微风格差异：红方稍稳，黑方稍快。
        targetRatio += currentRedGo ? 0.04d : -0.04d;
        targetRatio = clamp(targetRatio, 0.10d, 0.95d);

        long target = minRange + (long) (range * targetRatio);
        long jitter = Math.max(1000L, range / 6L);
        long jitterValue = random.nextLong(-jitter, jitter + 1L);
        long think = target + jitterValue;

        // 简单局面更容易快速出手，复杂局面降低短考概率。
        int quickProb;
        if (complexity >= 0.65d) {
            quickProb = 2;
        } else if (complexity >= 0.45d) {
            quickProb = 5;
        } else {
            quickProb = 12;
        }
        if (random.nextInt(100) < quickProb) {
            long shortMax = Math.min(maxRange, minRange + Math.max(5000L, range / 4L));
            think = random.nextLong(minRange, shortMax + 1L);
        }

        // 复杂局面增加“长考”概率，更像真人纠结关键步。
        if (complexity >= 0.60d && random.nextInt(100) < 28) {
            long longMin = minRange + (long) (range * 0.70d);
            if (longMin < maxRange) {
                think = random.nextLong(longMin, maxRange + 1L);
            }
        }

        // 连招节奏：同一方连续打出将军/吃子等强制手时，下一手通常更快。
        int comboStreak = currentRedGo ? redComboStreak : blackComboStreak;
        if (comboStreak > 0) {
            // 稳健型：连招只做轻度提速，保留足够思考。
            double comboReduce = Math.min(0.12d, comboStreak * 0.04d);
            // 复杂局面保留更多思考，不把节奏压得过快。
            comboReduce *= (1.0d - complexity * 0.5d);
            think = (long) (think * (1.0d - comboReduce));
        }

        // 同类局面连续出现时，思考时间逐步收敛，避免每步波动过大。
        if (lastHumanThinkTime > 0) {
            double smooth = Math.min(0.65d, sameComplexityBandStreak * 0.12d);
            think = (long) (think * (1.0d - smooth) + lastHumanThinkTime * smooth);
        }

        if (think < AUTO_BATTLE_MIN_TIME) {
            think = AUTO_BATTLE_MIN_TIME;
        } else if (think > AUTO_BATTLE_MAX_TIME) {
            think = AUTO_BATTLE_MAX_TIME;
        }
        lastHumanThinkTime = think;
        return think;
    }

    private double estimatePositionComplexity(boolean currentRedGo) {
        char[][] boardState = board.getBoard();
        boolean inCheck = XiangqiUtils.isJiang(boardState, currentRedGo);
        int legalMoves = 0;
        int captureMoves = 0;
        int checkMoves = 0;
        int pieces = 0;

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                char piece = boardState[i][j];
                if (piece == ' ') {
                    continue;
                }
                pieces++;
                if (XiangqiUtils.isRed(piece) != currentRedGo) {
                    continue;
                }

                for (int i2 = 0; i2 < 10; i2++) {
                    for (int j2 = 0; j2 < 9; j2++) {
                        if ((i != i2 || j != j2) && XiangqiUtils.canGo(boardState, i, j, i2, j2)) {
                            char eaten = boardState[i2][j2];
                            boardState[i2][j2] = boardState[i][j];
                            boardState[i][j] = ' ';

                            boolean selfCheck = XiangqiUtils.isJiang(boardState, currentRedGo);
                            if (!selfCheck) {
                                legalMoves++;
                                if (eaten != ' ') {
                                    captureMoves++;
                                }
                                if (XiangqiUtils.isJiang(boardState, !currentRedGo)) {
                                    checkMoves++;
                                }
                            }

                            boardState[i][j] = boardState[i2][j2];
                            boardState[i2][j2] = eaten;
                        }
                    }
                }
            }
        }

        double complexity = 0.30d;
        if (inCheck) {
            complexity += 0.35d;
        }
        if (legalMoves <= 8) {
            complexity += 0.32d;
        } else if (legalMoves <= 16) {
            complexity += 0.20d;
        } else if (legalMoves >= 34) {
            complexity -= 0.10d;
        }
        if (captureMoves >= 5) {
            complexity += 0.18d;
        } else if (captureMoves >= 2) {
            complexity += 0.10d;
        } else {
            complexity -= 0.06d;
        }
        if (checkMoves >= 2) {
            complexity += 0.15d;
        } else if (checkMoves == 0) {
            complexity -= 0.05d;
        }
        if (pieces <= 10) {
            complexity += 0.12d;
        } else if (pieces >= 24) {
            complexity -= 0.05d;
        }

        return clamp(complexity, 0.05d, 0.95d);
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    @FXML
    public void canvasClick(MouseEvent event) {

        if (event.getButton() == MouseButton.PRIMARY) {
            String move = board.mouseClick((int) event.getX(), (int) event.getY(),
                    redGo && !robotRed.getValue(), !redGo && !robotBlack.getValue());

            if (move != null) {
                goCallBack(move);
            }

            BoardContextMenu.getInstance().hide();

        } else if (event.getButton() == MouseButton.SECONDARY) {

            BoardContextMenu.getInstance().show(this.canvas, Side.RIGHT, event.getX() - this.canvas.widthProperty().doubleValue(), event.getY());
        }

    }
    private void goCallBack(String move) {
        // 统一落子后的收敛入口：
        // 1. 更新棋谱 2. 更新曲线 3. 切换行棋方 4. 决定引擎还是库招继续接管
        // 记录棋谱
        List<String> nextList = chessManualHandle.boardMove(
                move,
                board.translate(move, true),
                board.getLastMoveAnnotation(),
                board.getLastMoveOpponentPlans());
        showAnnotationOnTop(board.getLastMoveAnnotation(), redGo);
        appendAnnotationToPanel(board.getLastMoveAnnotation(), chessManualHandle.getP(), redGo);
        board.setManualList(nextList);
        // 趋势图
        refreshLineChart();
        // 切换行棋方
        redGo = !redGo;
        // 触发引擎走棋
        if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue() || robotAnalysis.getValue()) {
            engineGo();
        } else {
            doOpenBook();
        }
    }

    @Override
    public void refreshLineChart() {
        List<XYChart.Data> oldList = lineChartSeries.getData();
        List<XYChart.Data> newList = chessManualHandle.getScoreList();
        int i = 0;
        while (i < oldList.size() && i < newList.size()) {
            XYChart.Data o = oldList.get(i);
            XYChart.Data n = newList.get(i);
            if (!o.getXValue().equals(n.getXValue()) || !o.getYValue().equals(n.getYValue())) {
                for (int j = oldList.size() - 1; j >= i; j--) {
                    oldList.remove(j);
                }
                break;
            }
            i++;
        }
        if (i < oldList.size()) {
            for (int j = oldList.size() - 1; j >= i; j--) {
                oldList.remove(j);
            }
        } else if (i < newList.size()) {
            oldList.addAll(newList.subList(i, newList.size()));
        }
    }

    private void doOpenBook() {
        // 开局库查询放在虚拟线程，避免阻塞 JavaFX UI 线程。
        if (useOpenBook.getValue()) {
            Thread.startVirtualThread(() -> {
                List<BookData> results = OpenBookManager.getInstance().queryBook(board.getBoard(), redGo, chessManualHandle.getP() / 2 >= Properties.getInstance().getOffManualSteps());
                this.showBookResults(results);
            });
        } else {
            this.bookTable.getItems().clear();
        }
    }

    @FXML
    public void copyButtonClick(ActionEvent e) {
        String fenCode = board.fenCode(redGo);
        ClipboardUtils.setText(fenCode);
    }

    @FXML
    public void pasteButtonClick(ActionEvent e) {
        String fenCode = ClipboardUtils.getText();
        if (StringUtils.isNotEmpty(fenCode) && fenCode.split("/").length == 10) {
            newFromOriginFen(fenCode);
        }
    }

    @FXML
    public void importImageMenuClick(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(PathUtils.getJarPath()));
        File file = fileChooser.showOpenDialog(App.getMainStage());
        if (file != null) {
            importFromImgFile(file);
        }
    }

    @FXML
    public void exportImageMenuClick(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(PathUtils.getJarPath()));
        fileChooser.setInitialFileName("tchess_export_" + DateUtils.getDateTimeString(new Date()) + ".png");
        File file = fileChooser.showSaveDialog(App.getMainStage());
        if (file != null) {
            try {
                WritableImage writableImage = new WritableImage((int) this.canvas.getWidth(), (int) this.canvas.getHeight());
                canvas.snapshot(null, writableImage);
                RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                ImageIO.write(renderedImage, "png", file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @FXML
    public void aboutClick(ActionEvent e) {
        DialogUtils.showInfoDialog("关于", "TCHESS"
                + System.lineSeparator() + "Built on : " + App.BUILT_ON
                + System.lineSeparator() + "Author : T"
                + System.lineSeparator() + "Version : " + App.VERSION);
    }

    @FXML
    public void upgradeClick(ActionEvent e) {
        SystemUtils.openBrowser("https://github.com/sojourners/public-Xiangqi/releases");
    }

    @FXML
    public void instructionClick(ActionEvent e) {
        SystemUtils.openBrowser("https://github.com/sojourners/public-Xiangqi/blob/master/MANUAL.md");
    }

    @FXML
    public void homeClick(ActionEvent e) {
        SystemUtils.openBrowser("https://github.com/sojourners/public-Xiangqi");
    }

    @FXML
    void localBookManageButtonClick(ActionEvent e) {
        if (App.openLocalBookDialog()) {
            OpenBookManager.getInstance().setLocalOpenBooks();
        }

    }

    @FXML
    void timeSettingButtonClick(ActionEvent e) {
        App.openTimeSetting();
    }

    @FXML
    void bookSettingButtonClick(ActionEvent e) {
        App.openBookSetting();
    }

    @FXML
    void linkSettingClick(ActionEvent e) {
        App.openLinkSetting();

    }

    @FXML
    public void reverseButtonClick(ActionEvent event) {
        isReverse.setValue(!isReverse.getValue());
        board.reverse(isReverse.getValue());
    }

    @FXML
    private void bookSwitchButtonClick(ActionEvent e) {
        useOpenBook.setValue(!useOpenBook.getValue());
        prop.setBookSwitch(useOpenBook.getValue());

        doOpenBook();
    }

    @FXML
    private void linkButtonClick(ActionEvent e) {
        if (engine == null) {
            DialogUtils.showWarningDialog("提示", "引擎未加载");
            return;
        }

        linkMode.setValue(!linkMode.getValue());
        if (linkMode.getValue()) {
            graphLinker.start();
        } else {
            stopGraphLink();
        }
    }

    private void initLineChart() {
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis(-1000, 1000, 500);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setMinorTickVisible(false);
        yAxis.setTickMarkVisible(false);
        yAxis.setMinorTickVisible(false);

        LineChart<Number,Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setMinHeight(100);
        lineChart.setLegendVisible(false);
        lineChart.setCreateSymbols(false);
        lineChart.setVerticalGridLinesVisible(false);
        lineChart.getStylesheets().add(this.getClass().getResource("/style/table.css").toString());

        lineChartSeries = new XYChart.Series();
        lineChart.getData().add(lineChartSeries);

        charPane.setCenter(lineChart);
    }
    public void initialize() {
        // JavaFX 在 FXML 注入完成后自动调用此方法。
        // 这里按“从基础配置到功能模块”的顺序初始化，尽量避免模块间前置依赖冲突。
        // 读取配置
        prop = Properties.getInstance();
        // 思考细节listView
        listView.setCellFactory(new Callback() {
            @Override
            public Object call(Object param) {
                ListCell<ThinkData> cell = new ListCell<ThinkData>() {
                    @Override
                    protected void updateItem(ThinkData item, boolean bln) {
                        super.updateItem(item, bln);
                        if (!bln) {
                            VBox box = new VBox();

                            Label title = new Label();
                            title.setText(item.getTitle());
                            title.setTextFill(item.getScore() >= 0 ? Color.BLUE : Color.RED);
                            box.getChildren().add(title);

                            Label body = new Label();
                            body.setText(item.getBody());
                            body.setTextFill(Color.BLACK);
                            body.setWrapText(true);
                            body.setMaxWidth(listView.getWidth() / 1.124);//bind(listView.widthProperty().divide(1.124));
                            box.getChildren().add(body);

                            setGraphic(box);
                        }
                    }
                };
                return cell;
            }

        });
        initAnnotationView();
        // 按钮
        setButtonTips();
        // 棋盘
        initChessBoard();
        // 库招表
        initBookTable();
        // 引擎view
        initEngineView();
        // 连线器
        initGraphLinker();
        // 按钮监听
        initButtonListener();
        // autofit board size listener
        initAutoFitBoardListener();
        // canvas drag listener
        initCanvasDragListener();
        // line chart
        initLineChart();
        // init chess manual
        chessManualHandle = new ChessManualHandle(chessManualPane, menuOfChessNotation, menuOfShowTactic, notationTree,
                manualTitleLabel, recordTable, subRecordTable, remarkText,
                manualBackButton, manualDeleteButton, manualDownButton, manualFinalButton,
                manualForwardButton, manualFrontButton, manualPlayButton, manualUpButton,
                openManualButton, saveManualButton, manualScoreButton, competitionNameText, competitionCityText, competitionDateText,
                competitionRedText, competitionBlackText, this);

        useOpenBook.setValue(prop.getBookSwitch());
        // 初始化棋局
        newChessBoard(null);
        // 加载引擎
        loadEngine(prop.getEngineName());
    }

    private void importFromBufferImage(BufferedImage img) {
        char[][] result = graphLinker.findChessBoard(img);
        if (result != null) {
            if (!XiangqiUtils.validateChessBoard(result) && !DialogUtils.showConfirmDialog("提示", "检测到局面不合法，可能会导致引擎退出或者崩溃，是否继续？")) {
                return;
            }
            String fenCode = ChessBoard.fenCode(result, true);
            newFromOriginFen(fenCode);
        }
    }

    private void importFromImgFile(File f) {
        if (f.exists() && PathUtils.isImage(f.getAbsolutePath())) {
            try {
                BufferedImage img = ImageIO.read(f);
                importFromBufferImage(img);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initCanvasDragListener() {
        this.canvas.setOnDragDropped(event -> {
            File f = event.getDragboard().getFiles().get(0);
            importFromImgFile(f);
        });
        this.canvas.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        });
    }

    private void initAutoFitBoardListener() {
        borderPane.widthProperty().addListener((observableValue, number, t1) -> {
            board.autoFitSize(t1.doubleValue(), borderPane.getHeight(), splitPane.getDividerPositions()[0]);
        });
        borderPane.heightProperty().addListener((observableValue, number, t1) -> {
            board.autoFitSize(borderPane.getWidth(), t1.doubleValue(), splitPane.getDividerPositions()[0]);
        });
        splitPane.getDividers().get(0).positionProperty().addListener((observableValue, number, t1) -> {
            board.autoFitSize(borderPane.getWidth(), borderPane.getHeight(), t1.doubleValue());
        });
    }

    private void initBookTable() {
        TableColumn moveCol = bookTable.getColumns().get(0);
        moveCol.setCellValueFactory(new PropertyValueFactory<BookData, String>("word"));
        TableColumn scoreCol = bookTable.getColumns().get(1);
        scoreCol.setCellValueFactory(new PropertyValueFactory<BookData, Integer>("score"));
        TableColumn winRateCol = bookTable.getColumns().get(2);
        winRateCol.setCellValueFactory(new PropertyValueFactory<BookData, Double>("winRate"));
        TableColumn winNumCol = bookTable.getColumns().get(3);
        winNumCol.setCellValueFactory(new PropertyValueFactory<BookData, Integer>("winNum"));
        TableColumn drawNumCol = bookTable.getColumns().get(4);
        drawNumCol.setCellValueFactory(new PropertyValueFactory<BookData, Integer>("drawNum"));
        TableColumn loseNumCol = bookTable.getColumns().get(5);
        loseNumCol.setCellValueFactory(new PropertyValueFactory<BookData, Integer>("loseNum"));
        TableColumn noteCol = bookTable.getColumns().get(6);
        noteCol.setCellValueFactory(new PropertyValueFactory<BookData, String>("note"));
        TableColumn sourceCol = bookTable.getColumns().get(7);
        sourceCol.setCellValueFactory(new PropertyValueFactory<BookData, String>("source"));
    }

    public void initStage() {
        borderPane.setPrefWidth(prop.getStageWidth());
        borderPane.setPrefHeight(prop.getStageHeight());
        splitPane.setDividerPosition(0, prop.getSplitPos());
        splitPane2.setDividerPosition(0, prop.getSplitPos2());

        // 窗口置顶
        menuOfTopWindow.setSelected(prop.isTopWindow());
        App.topWindow(prop.isTopWindow());
    }

    private void setButtonTips() {
        newButton.setTooltip(new Tooltip("新局面"));
        copyButton.setTooltip(new Tooltip("复制局面"));
        pasteButton.setTooltip(new Tooltip("粘贴局面"));
        regretButton.setTooltip(new Tooltip("悔棋"));
        reverseButton.setTooltip(new Tooltip("翻转"));
        redButton.setTooltip(new Tooltip("引擎执红"));
        blackButton.setTooltip(new Tooltip("引擎执黑"));
        analysisButton.setTooltip(new Tooltip("分析模式"));
        immediateButton.setTooltip(new Tooltip("立即出招"));
        changeTacticButton.setTooltip(new Tooltip("变招"));
        linkButton.setTooltip(new Tooltip("连线"));
        bookSwitchButton.setTooltip(new Tooltip("启用库招"));

    }

    private void initChessBoard() {
        // 棋步提示
        menuOfStepTip.setSelected(prop.isStepTip());
        // 走棋音效
        menuOfStepSound.setSelected(prop.isStepSound());
        // 连线后台模式
        menuOfLinkBackMode.setSelected(prop.isLinkBackMode());
        // 连线动画确认
        menuOfLinkAnimation.setSelected(prop.isLinkAnimation());
        // show number
        menuOfShowNumber.setSelected(prop.isShowNumber());
        // 显示状态栏
        menuOfShowStatus.setSelected(prop.isLinkShowInfo());
        // 复盘注解口语风格
        menuOfColloquialReview.setSelected(prop.isColloquialReviewStyle());
        // 显示引擎日志
        menuOfShowEngineLog.setSelected(prop.isShowEngineLog());
        menuOfShowAnnotation.setSelected(!prop.isShowEngineLog());
        applyEngineLogVisibility();
        // 棋盘大小
        if (prop.getBoardSize() == ChessBoard.BoardSize.LARGE_BOARD) {
            menuOfLargeBoard.setSelected(true);
        } else if (prop.getBoardSize() == ChessBoard.BoardSize.BIG_BOARD) {
            menuOfBigBoard.setSelected(true);
        } else if (prop.getBoardSize() == ChessBoard.BoardSize.MIDDLE_BOARD) {
            menuOfMiddleBoard.setSelected(true);
        } else if (prop.getBoardSize() == ChessBoard.BoardSize.AUTOFIT_BOARD) {
            menuOfAutoFitBoard.setSelected(true);
        } else {
            menuOfSmallBoard.setSelected(true);
        }
        // 棋盘样式
        if (prop.getBoardStyle() == ChessBoard.BoardStyle.DEFAULT) {
            menuOfDefaultBoard.setSelected(true);
        } else {
            menuOfCustomBoard.setSelected(true);
        }
        // 右键菜单
        initBoardContextMenu();
        // 状态栏
        this.infoShowLabel.prefWidthProperty().bind(statusToolBar.widthProperty().subtract(120));
        this.timeShowLabel.setText(prop.getAnalysisModel() == Engine.AnalysisModel.FIXED_TIME ? "固定时间" + prop.getAnalysisValue() / 1000d + "s" : "固定深度" + prop.getAnalysisValue() + "层");
        this.statusToolBar.setVisible(prop.isLinkShowInfo());
    }

    private void initBoardContextMenu() {
        BoardContextMenu.getInstance().setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                MenuItem item = (MenuItem) event.getTarget();
                if ("复制局面FEN".equals(item.getText())) {
                    copyButtonClick(null);
                } else if ("粘贴局面FEN".equals(item.getText())) {
                    pasteButtonClick(null);
                } else if ("交换行棋方".equals(item.getText())) {
                    switchPlayer(true);
                } else if ("编辑局面".equals(item.getText())) {
                    editChessBoardClick(null);
                } else if ("复制局面图片".equals(item.getText())) {
                    copyImageMenuClick(null);
                } else if ("粘贴局面图片".equals(item.getText())) {
                    pasteImageMenuClick(null);
                }
            }
        });
    }

    @FXML
    public void copyImageMenuClick(ActionEvent event) {
        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, writableImage);
        BufferedImage bi =SwingFXUtils.fromFXImage(writableImage, null);
        ClipboardUtils.setImage(bi);
    }

    @FXML
    public void pasteImageMenuClick(ActionEvent event) {
        Image img = ClipboardUtils.getImage();
        if (img != null) {
            importFromBufferImage((BufferedImage) img);
        }
    }

    @FXML
    public void editChessBoardClick(ActionEvent e) {
        String fenCode = App.openEditChessBoard(board.getBoard(), redGo, isReverse.getValue());
        newFromOriginFen(fenCode);
    }

    /**
     * new from origin fen that maybe reverse, and stop link mode at the same time
     * @param fenCode
     */
    private void newFromOriginFen(String fenCode) {
        if (StringUtils.isNotEmpty(fenCode)) {
            if (linkMode.getValue()) {
                stopGraphLink();
            }

            newChessBoard(fenCode);
            if (XiangqiUtils.isReverse(fenCode)) {
                reverseButtonClick(null);
            }
        }
    }

    private void newChessBoard(String fenCode) {
        newChessBoard(fenCode, false);
    }

    /**
     * 新建局面
     * @param fenCode 传null 新建默认初始局面；传fenCode 则根据fen创建局面
     */
    private void newChessBoard(String fenCode, boolean fromManual) {
        // 新局面重建是全量刷新流程，会清理引擎状态、提示箭头、库招显示等。
        // 重置按钮
        robotRed.setValue(false);
        redButton.setDisable(false);
        robotBlack.setValue(false);
        blackButton.setDisable(false);
        robotAnalysis.setValue(false);
        immediateButton.setDisable(false);
        isReverse.setValue(false);
        lastHumanThinkTime = -1L;
        lastComplexityBand = -1;
        sameComplexityBandStreak = 0;
        redComboStreak = 0;
        blackComboStreak = 0;
        // 引擎停止计算
        engineStop();
        // 绘制棋盘
        board = new ChessBoard(this.canvas, prop.getBoardSize(), prop.getBoardStyle(), prop.isStepTip(), prop.isManualTip(),
                engine != null && engine.getMultiPV() > 1, prop.isStepSound(), prop.isShowNumber(), fenCode);
        board.setColloquialReviewStyle(prop.isColloquialReviewStyle());
        // 设置局面
        redGo = StringUtils.isEmpty(fenCode) ? true : fenCode.contains("w");
        fenCode = board.fenCode(redGo);
        // 设置棋谱
        if (!fromManual)
            chessManualHandle.newChessManual(fenCode);
        // 重置趋势图
        refreshLineChart();
        // 重置引擎思考输出
        listView.getItems().clear();
        // 清空思考状态信息
        this.infoShowLabel.setText("");

        // 库招显示
        doOpenBook();

        System.gc();
    }

    private void initEngineView() {
        // 引擎列表 线程数 哈希表大小
        refreshEngineComboBox();
        for (int i = 1; i <= Runtime.getRuntime().availableProcessors(); i++) {
            threadComboBox.getItems().add(String.valueOf(i));
        }
        hashComboBox.getItems().addAll("16", "32", "64", "128", "256", "512", "1024", "2048", "4096");
        // 加载设置
        threadComboBox.setValue(String.valueOf(prop.getThreadNum()));
        hashComboBox.setValue(String.valueOf(prop.getHashSize()));
    }


    private void initGraphLinker() {
        try {
            this.graphLinker = com.sun.jna.Platform.isWindows() ?
                    new WindowsGraphLinker(this) : (com.sun.jna.Platform.isLinux() ?
                    new LinuxGraphLinker(this) : new MacosGraphLinker(this));
        } catch (Exception e) {
            e.printStackTrace();
        }

        linkComboBox.getItems().addAll("自动走棋", "观战模式");
        linkComboBox.setValue("自动走棋");
    }

    private void refreshEngineComboBox() {
        engineComboBox.getItems().clear();
        engineDescMap.clear();
        for (EngineConfig ec : prop.getEngineConfigList()) {
            engineComboBox.getItems().add(ec.getName());
            engineDescMap.put(ec.getName(), buildEngineDescription(ec));
        }
        engineComboBox.setValue(prop.getEngineName());
        updateEngineDescView(prop.getEngineName());
    }

    private String buildEngineDescription(EngineConfig ec) {
        StringBuilder sb = new StringBuilder();
        String name = ec.getName() == null ? "" : ec.getName();
        if (name.contains("内置-")) {
            sb.append("内置引擎");
            if (name.contains("推荐")) {
                sb.append("（当前机器推荐）");
            }
            if (name.contains("AVX512")) {
                sb.append("，指令集：AVX512，高性能");
            } else if (name.contains("AVX2")) {
                sb.append("，指令集：AVX2，性能较高");
            } else if (name.contains("BMI2")) {
                sb.append("，指令集：BMI2");
            } else if (name.contains("通用")) {
                sb.append("，通用版本，兼容性最好");
            }
        } else {
            sb.append("外部引擎");
        }
        sb.append("，协议：").append(ec.getProtocol() == null ? "未知" : ec.getProtocol().toUpperCase());
        if (ec.getPath() != null) {
            sb.append("，路径：").append(ec.getPath());
        }
        return sb.toString();
    }

    private void updateEngineDescView(String name) {
        String desc = engineDescMap.get(name);
        if (StringUtils.isEmpty(desc)) {
            desc = "点击引擎下拉框可查看引擎说明";
        }
        if (prop.isLinkShowInfo()) {
            infoShowLabel.setText(desc);
        }
        engineComboBox.setTooltip(new Tooltip(desc));
    }

    private void initButtonListener() {
        addListener(redButton, robotRed);
        addListener(blackButton, robotBlack);
        addListener(analysisButton, robotAnalysis);
        addListener(reverseButton, isReverse);
        addListener(linkButton, linkMode);
        addListener(bookSwitchButton, useOpenBook);

        threadComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                int num = Integer.parseInt(t1);
                if (num != prop.getThreadNum()) {
                    prop.setThreadNum(num);
                }
            }
        });
        hashComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                int size = Integer.parseInt(t1);
                if (size != prop.getHashSize()) {
                    prop.setHashSize(size);
                }
            }
        });
        engineComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                updateEngineDescView(t1);
                if (StringUtils.isNotEmpty(t1) && !t1.equals(prop.getEngineName())) {
                    // 保存引擎设置
                    prop.setEngineName(t1);
                    // 重置三个按钮
                    robotRed.setValue(false);
                    redButton.setDisable(false);
                    robotBlack.setValue(false);
                    blackButton.setDisable(false);
                    robotAnalysis.setValue(false);
                    immediateButton.setDisable(false);
                    // 停止连线
                    if (linkMode.getValue()) {
                        stopGraphLink();
                    }
                    // 加载新引擎
                    loadEngine(t1);
                }
            }
        });
        engineComboBox.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                String name = engineComboBox.getSelectionModel().getSelectedItem();
                if (StringUtils.isNotEmpty(name)) {
                    DialogUtils.showInfoDialog("引擎说明", engineDescMap.getOrDefault(name, "暂无说明"));
                }
            }
        });
        linkComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                setLinkMode(t1);
            }
        });
    }

    private void setLinkMode(String t1) {
        // 连线模式切换时，需要同步处理按钮互斥、引擎状态、是否立即触发搜索。
        if (linkMode.getValue()) {
            if ("自动走棋".equals(t1)) {
                // 观战模式切换自动走棋，先停止引擎
                engineStop();
                // 走黑棋/红棋
                if (isReverse.getValue()) {
                    blackButton.setDisable(false);
                    robotBlack.setValue(true);

                    redButton.setDisable(true);
                    robotRed.setValue(false);

                    analysisButton.setDisable(true);
                    robotAnalysis.setValue(false);

                    if (!redGo) {
                        engineGo();
                    }
                } else {
                    redButton.setDisable(false);
                    robotRed.setValue(true);

                    blackButton.setDisable(true);
                    robotBlack.setValue(false);

                    analysisButton.setDisable(true);
                    robotAnalysis.setValue(false);

                    if (redGo) {
                        engineGo();
                    }
                }
            } else {
                analysisButton.setDisable(false);
                robotAnalysis.setValue(true);

                blackButton.setDisable(true);
                robotBlack.setValue(false);

                redButton.setDisable(true);
                robotRed.setValue(false);

                immediateButton.setDisable(true);

                engineGo();
            }
        }
    }

    private void addListener(Button button, ObjectProperty property) {
        property.addListener((ChangeListener<Boolean>) (observableValue, aBoolean, t1) -> {
            if (t1) {
                button.getStylesheets().add(this.getClass().getResource("/style/selected-button.css").toString());
            } else {
                button.getStylesheets().remove(this.getClass().getResource("/style/selected-button.css").toString());
            }
        });
    }

    private void loadEngine(String name) {
        try {
            if (StringUtils.isNotEmpty(name)) {
                for (EngineConfig ec : prop.getEngineConfigList()) {
                    if (name.equals(ec.getName())) {
                        // 热切换引擎：先关闭旧进程，再拉起新进程。
                        if (engine != null) {
                            engine.close();
                        }
                        engine = new Engine(ec, this);
                        board.showMultiPV(engine.getMultiPV() > 1);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 连线模式下自动点击走棋
     * @param step
     */
    private void trickAutoClick(ChessBoard.Step step, long pauseMs) {
        if (step == null || !linkMode.getValue() || isWatchMode()) {
            this.isThinking = false;
            return;
        }
        int x1 = step.getStart().getX(), y1 = step.getStart().getY();
        int x2 = step.getEnd().getX(), y2 = step.getEnd().getY();
        // 自动走棋模式下按当前执棋方决定是否翻转坐标。
        if (robotBlack.getValue()) {
            y1 = 9 - y1;
            y2 = 9 - y2;
            x1 = 8 - x1;
            x2 = 8 - x2;
        }
        final int fx1 = x1, fy1 = y1, fx2 = x2, fy2 = y2;
        final long delay = Math.max(0L, pauseMs);
        Thread.startVirtualThread(() -> {
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            graphLinker.autoClick(fx1, fy1, fx2, fy2);
            this.isThinking = false;
        });
    }

    private long calculateHumanClickPauseMs(ChessBoard.Step step) {
        if (step == null) {
            return 0;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char[][] b = board.getBoard();
        int x1 = step.getStart().getX(), y1 = step.getStart().getY();
        int x2 = step.getEnd().getX(), y2 = step.getEnd().getY();
        char moving = b[y1][x1];
        char captured = b[y2][x2];
        if (moving == ' ') {
            return random.nextLong(200, 500);
        }
        boolean red = XiangqiUtils.isRed(moving);
        int comboStreak = red ? redComboStreak : blackComboStreak;

        char tmp = b[y2][x2];
        b[y2][x2] = moving;
        b[y1][x1] = ' ';
        boolean check = XiangqiUtils.isJiang(b, !red);
        b[y1][x1] = moving;
        b[y2][x2] = tmp;

        if (check || captured != ' ') {
            long pause = random.nextLong(650, 1300);
            if (comboStreak > 0) {
                pause = (long) (pause * Math.max(0.86d, 1.0d - comboStreak * 0.04d));
            }
            return pause;
        }
        char lower = Character.toLowerCase(moving);
        if (lower == 'r' || lower == 'c' || lower == 'n') {
            long pause = random.nextLong(360, 820);
            if (comboStreak > 0) {
                pause = (long) (pause * Math.max(0.86d, 1.0d - comboStreak * 0.04d));
            }
            return pause;
        }
        long pause = random.nextLong(180, 520);
        if (comboStreak > 0) {
            pause = (long) (pause * Math.max(0.86d, 1.0d - comboStreak * 0.04d));
        }
        return pause;
    }

    private boolean isTacticalMove(ChessBoard.Step step) {
        if (step == null) {
            return false;
        }
        char[][] b = board.getBoard();
        int x1 = step.getStart().getX(), y1 = step.getStart().getY();
        int x2 = step.getEnd().getX(), y2 = step.getEnd().getY();
        char moving = b[y1][x1];
        if (moving == ' ') {
            return false;
        }
        char captured = b[y2][x2];
        boolean red = XiangqiUtils.isRed(moving);

        b[y2][x2] = moving;
        b[y1][x1] = ' ';
        boolean check = XiangqiUtils.isJiang(b, !red);
        b[y1][x1] = moving;
        b[y2][x2] = captured;

        return captured != ' ' || check;
    }

    private void updateComboRhythm(boolean moverRed, boolean tactical) {
        if (moverRed) {
            redComboStreak = tactical ? Math.min(4, redComboStreak + 1) : Math.max(0, redComboStreak - 1);
        } else {
            blackComboStreak = tactical ? Math.min(4, blackComboStreak + 1) : Math.max(0, blackComboStreak - 1);
        }
    }

    @Override
    public void bestMove(String first, String second) {
        if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue()) {
            boolean moverRed = redGo;
            ChessBoard.Step s = board.stepForBoard(first);
            boolean tactical = isTacticalMove(s);
            long clickPause = calculateHumanClickPauseMs(s);

            Platform.runLater(() -> {
                // 一切 UI 操作都在 JavaFX 线程执行，避免跨线程 UI 访问异常。
                board.move(s.getStart().getX(), s.getStart().getY(), s.getEnd().getX(), s.getEnd().getY());
                board.setTip(second, null, 1);

                if (linkMode.getValue()) {
                    trickAutoClick(s, clickPause);
                }

                goCallBack(first);
                updateComboRhythm(moverRed, tactical);
            });
        }
    }

    @Override
    public void thinkDetail(ThinkData td) {
        if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue() || robotAnalysis.getValue()) {
            td.generate(redGo, isReverse.getValue(), board);
            if (td.getValid()) {
                Platform.runLater(() -> {
                    if (!prop.isShowEngineLog()) {
                        board.setTip(td.getDetail().get(0), td.getDetail().size() > 1 ? td.getDetail().get(1) : null, td.getPv());
                        if (td.getPv() == 1) {
                            chessManualHandle.setScore(td.getScore(), td.getMate());
                        }
                        return;
                    }
                    // 只保留最近 128 条思考细节，避免列表无限增长。
                    listView.getItems().addFirst(td);
                    if (listView.getItems().size() > 128) {
                        listView.getItems().removeLast();
                    }

                    if (prop.isLinkShowInfo()) {
                        infoShowLabel.setText(td.getTitle() + " | " + td.getBody());
                        infoShowLabel.setTextFill(td.getScore() >= 0 ? Color.BLUE : Color.RED);
                        timeShowLabel.setText(prop.getAnalysisModel() == Engine.AnalysisModel.FIXED_TIME ? "固定时间" + prop.getAnalysisValue() / 1000d + "s" : "固定深度" + prop.getAnalysisValue() + "层");
                    }

                    board.setTip(td.getDetail().get(0), td.getDetail().size() > 1 ? td.getDetail().get(1) : null, td.getPv());

                    if (td.getPv() == 1) {
                        chessManualHandle.setScore(td.getScore(), td.getMate());
                    }
                });
            }
        }
    }

    private void applyEngineLogVisibility() {
        boolean show = prop.isShowEngineLog();
        if (listViewPane != null) {
            listViewPane.setVisible(true);
            listViewPane.setManaged(true);
            if (annotationListView != null) {
                listViewPane.setCenter(show ? listView : annotationListView);
            }
        }
        if (engineTopTitleLabel != null) {
            engineTopTitleLabel.setText(show ? "引擎" : "招法讲解");
        }
        if (engineComboBox != null) {
            engineComboBox.setVisible(show);
            engineComboBox.setManaged(show);
        }
        if (threadComboBox != null) {
            threadComboBox.setVisible(show);
            threadComboBox.setManaged(show);
        }
        if (hashComboBox != null) {
            hashComboBox.setVisible(show);
            hashComboBox.setManaged(show);
        }
        if (hashUnitLabel != null) {
            hashUnitLabel.setVisible(show);
            hashUnitLabel.setManaged(show);
        }
        if (engineTopToolBar != null) {
            engineTopToolBar.requestLayout();
        }
        if (!show && listView != null) {
            listView.getItems().clear();
        }
    }

    private void initAnnotationView() {
        annotationListView = new ListView<>();
        annotationListView.setStyle(listView.getStyle());
        annotationListView.getStylesheets().addAll(listView.getStylesheets());
        annotationListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    Label label = new Label(item);
                    label.setWrapText(true);
                    label.prefWidthProperty().bind(annotationListView.widthProperty().subtract(20));
                    boolean latest = getIndex() == 0;
                    boolean critical = isCriticalAnnotation(item);
                    if (latest) {
                        label.setTextFill(Color.web("#B71C1C"));
                        setStyle("-fx-background-color: #FFECEC;");
                    } else {
                        label.setTextFill(critical ? Color.RED : Color.web("#403e3e"));
                        setStyle("");
                    }
                    setText(null);
                    setGraphic(label);
                }
            }
        });
    }

    private void appendAnnotationToPanel(String annotation, int ply, boolean moverRed) {
        if (StringUtils.isEmpty(annotation) || annotationListView == null) {
            return;
        }
        if (!shouldDisplayOwnSide(moverRed)) {
            return;
        }
        int round = Math.max(1, (ply + 1) / 2);
        String side = moverRed ? "红方" : "黑方";
        String text = "第" + round + "回合(" + side + ")  " + annotation;
        annotationListView.getItems().addFirst(text);
        if (annotationListView.getItems().size() > 128) {
            annotationListView.getItems().removeLast();
        }
    }

    private void showAnnotationOnTop(String annotation, boolean moverRed) {
        if (StringUtils.isEmpty(annotation) || prop.isShowEngineLog() || !prop.isLinkShowInfo()) {
            return;
        }
        if (!shouldDisplayOwnSide(moverRed)) {
            return;
        }
        infoShowLabel.setText(annotation);
        infoShowLabel.setTextFill(isCriticalAnnotation(annotation) ? Color.RED : Color.web("#403e3e"));
    }

    private boolean shouldDisplayOwnSide(boolean moverRed) {
        // 单边人机时：只显示“用户自己方”的注解。
        // robotRed=true 代表引擎走红，用户是黑；robotBlack=true 代表引擎走黑，用户是红。
        if (robotRed.getValue() && !robotBlack.getValue()) {
            return !moverRed;
        }
        if (!robotRed.getValue() && robotBlack.getValue()) {
            return moverRed;
        }
        // 双方都由同一方控制（都开/都关）时，不做过滤。
        return true;
    }

    private boolean isCriticalAnnotation(String annotation) {
        return annotation.contains("杀棋")
                || annotation.contains("将军")
                || annotation.contains("吃子")
                || annotation.contains("终结")
                || annotation.contains("连续威胁");
    }

    @Override
    public void showBookResults(List<BookData> list) {
        this.bookTable.getItems().clear();
        for (BookData bd : list) {
            String move = bd.getMove();
            bd.setWord(board.translate(move, false));
            this.bookTable.getItems().add(bd);
        }
    }

    @FXML
    public void bookTableClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            if (redGo && !robotRed.getValue() || !redGo && !robotBlack.getValue() ||robotAnalysis.getValue()) {
                BookData bd = bookTable.getSelectionModel().getSelectedItem();
                if (bd == null) {
                    return;
                }
                Platform.runLater(() -> {
                    board.move(bd.getMove());
                    goCallBack(bd.getMove());
                });
            }
        }
    }

    @FXML
    public void exit() {
        if (engine != null) {
            engine.close();
        }

        OpenBookManager.getInstance().close();
//        ExecutorsUtils.getInstance().close();

        graphLinker.stop();

        prop.setStageWidth(borderPane.getWidth());
        prop.setStageHeight(borderPane.getHeight());
        prop.setSplitPos(splitPane.getDividerPositions()[0]);
        prop.setSplitPos2(splitPane2.getDividerPositions()[0]);

        prop.save();

        Platform.exit();
    }

    /**
     * 图形连线初始化棋盘
     * @param fenCode
     * @param isReverse
     */
    @Override
    public void linkerInitChessBoard(String fenCode, boolean isReverse) {
        // 连线初始化回调：以识别局面为准重建本地棋盘。
        Platform.runLater(() -> {
            newChessBoard(fenCode);
            if (isReverse) {
                reverseButtonClick(null);
            }
            setLinkMode(linkComboBox.getValue());
        });
    }

    @Override
    public char[][] getEngineBoard() {
        return board.getBoard();
    }

    @Override
    public boolean isThinking() {
        return this.isThinking;
    }

    @Override
    public boolean isWatchMode() {
        return "观战模式".equals(linkComboBox.getValue());
    }

    @Override
    public void linkerMove(int x1, int y1, int x2, int y2) {
        Platform.runLater(() -> {
            String move = board.move(x1, y1, x2, y2);
            if (move != null) {
                boolean red = XiangqiUtils.isRed(board.getBoard()[y2][x2]);
                if (isWatchMode() && (!redGo && red || redGo && !red)) {
                    System.out.println(move + "," + red + ", " + redGo);
                    // 连线识别行棋方错误，自动切换行棋方
                    switchPlayer(false);
                } else {
                    goCallBack(move);
                }
            }
        });
    }

    private void switchPlayer(boolean f) {
        // 交换行棋方本质上是重建 FEN，但要保留当前按钮状态与连线状态。
        engineStop();

        graphLinker.pause();

        boolean tmpRed = robotRed.getValue(), tmpBlack = robotBlack.getValue(), tmpAnalysis = robotAnalysis.getValue(), tmpLink = linkMode.getValue(), tmpReverse = isReverse.getValue();

        String fenCode = board.fenCode(f ? !redGo : redGo);
        newChessBoard(fenCode);

        isReverse.setValue(tmpReverse);
        board.reverse(tmpReverse);
        robotRed.setValue(tmpRed);
        robotBlack.setValue(tmpBlack);
        robotAnalysis.setValue(tmpAnalysis);
        linkMode.setValue(tmpLink);

        graphLinker.resume();
        if (robotRed.getValue() && redGo || robotBlack.getValue() && !redGo || robotAnalysis.getValue()) {
            engineGo();
        }
    }

    // ------------- 棋谱管理 start -----------------
    private ChessManualHandle chessManualHandle;
    @FXML
    private BorderPane chessManualPane;
    @FXML
    private CheckMenuItem menuOfChessNotation;
    @FXML
    private CheckMenuItem menuOfShowTactic;
    @FXML
    private TreeView notationTree;
    @FXML
    private Label manualTitleLabel;
    @FXML
    private ListView subRecordTable;
    @FXML
    private TextArea remarkText;
    @FXML
    private Button manualBackButton;
    @FXML
    private Button manualDeleteButton;
    @FXML
    private Button manualDownButton;
    @FXML
    private Button manualFinalButton;
    @FXML
    private Button manualForwardButton;
    @FXML
    private Button manualFrontButton;
    @FXML
    private Button manualPlayButton;
    @FXML
    private Button manualUpButton;
    @FXML
    private Button openManualButton;
    @FXML
    private Button saveManualButton;
    @FXML
    private Button manualScoreButton;
    @FXML
    private TextField competitionNameText;
    @FXML
    private TextField competitionCityText;
    @FXML
    private TextField competitionDateText;
    @FXML
    private TextField competitionRedText;
    @FXML
    private TextField competitionBlackText;

    @FXML
    void menuOfShowTacticClick(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setManualTip(item.isSelected());
        board.setManualTip(item.isSelected());
    }
    @FXML
    void openChessManualFolder(ActionEvent event) {
        chessManualHandle.openChessNotationFolder(event);
    }
    @FXML
    void deleteButtonClick(ActionEvent event) {
        checkLinkMode();
        chessManualHandle.deleteButtonClick(event);
    }
    @FXML
    void scoreButtonClick(ActionEvent event) {
        if (engine == null) {
            DialogUtils.showWarningDialog("提示", "引擎未加载");
            return;
        }

        checkLinkMode();
        chessManualHandle.scoreButtonClick(event);
    }
    @FXML
    void playButtonClick(ActionEvent event) {
        checkLinkMode();
        chessManualHandle.playButtonClick(event);
    }
    @FXML
    void downwardButtonClick(ActionEvent event) {
        checkLinkMode();
        chessManualHandle.manualButtonClick(8);
    }
    @FXML
    void upwardButtonClick(ActionEvent event) {
        checkLinkMode();
        chessManualHandle.manualButtonClick(7);
    }

    @Override
    public void turnOnAnalysisMode() {
        if (!robotAnalysis.getValue()) {
            analysisButtonClick(null);
        }
    }

    @Override
    public void turnOffAnalysisMode() {
        if (robotAnalysis.getValue()) {
            analysisButtonClick(null);
        }
    }

    @Override
    public void newChessBoardFromManual(String fenCode) {
        newChessBoard(fenCode, true);
    }

    @Override
    public void browseChessRecord(String fenCode, List<String> moveList, boolean redGo, List<String> nextList) {
        checkLinkMode();
        // 棋盘
        board.browseChessRecord(fenCode, moveList);
        board.setManualList(nextList);
        this.redGo = redGo;
        // 趋势图
        refreshLineChart();
        // 引擎走棋
        if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue() || robotAnalysis.getValue()) {
            // 轮到引擎走棋或者分析模式
            engineGo();
        } else {
            // 其他情况，停止引擎思考
            engineStop();
            // 库招显示
            doOpenBook();
        }
    }

    @Override
    public void setNextList(List<String> nextList) {
        board.setManualList(nextList);
    }

    private void checkLinkMode() {
        if (linkMode.getValue()) {
            stopGraphLink();
        }
    }

    @FXML
    void recordTableClick(MouseEvent event) {
        checkLinkMode();
        chessManualHandle.manualButtonClick(5);
    }

    @FXML
    public void backButtonClick(ActionEvent event) {
        checkLinkMode();
        chessManualHandle.manualButtonClick(2);
    }

    @FXML
    public void regretButtonClick(ActionEvent event) {
        checkLinkMode();
        if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue()) {
            chessManualHandle.manualButtonClick(2);
        } else {
            chessManualHandle.manualButtonClick(6);
        }
    }

    @FXML
    void forwardButtonClick(ActionEvent event) {
        checkLinkMode();
        chessManualHandle.manualButtonClick(3);
    }

    @FXML
    void finalButtonClick(ActionEvent event) {
        checkLinkMode();
        chessManualHandle.manualButtonClick(4);
    }

    @FXML
    void frontButtonClick(ActionEvent event) {
        checkLinkMode();
        chessManualHandle.manualButtonClick(1);
    }

    @FXML
    void openChessManualFile(ActionEvent event) {
        chessManualHandle.openChessManualFile(event);
    }

    @FXML
    void saveAsChessManualFile(ActionEvent event) {
        chessManualHandle.saveAsChessManualFile(event);
    }

    @FXML
    void saveChessManualFile(ActionEvent event) {
        chessManualHandle.saveChessManualFile(event);
    }
    // ------------- 棋谱管理 end -----------------
}
