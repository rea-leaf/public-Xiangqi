package com.chf.chess.config;

import com.chf.chess.board.ChessBoard;
import com.chf.chess.enginee.Engine;
import com.chf.chess.model.EngineConfig;
import com.chf.chess.openbook.MoveRule;
import com.chf.chess.util.BuiltinEngineLoader;
import com.chf.chess.util.PathUtils;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Properties 类。
 * 项目核心类型。
 */
public class Properties implements Serializable {

    private static final long serialVersionUID = -1410031608529065857L;
    private static final double DEFAULT_STAGE_WIDTH = 1100;
    private static final double DEFAULT_STAGE_HEIGHT = 630;

    /** 单例配置对象。 */
    private static Properties prop;

    private ChessBoard.BoardSize boardSize;
    private ChessBoard.BoardStyle boardStyle = ChessBoard.BoardStyle.CUSTOM;

    private boolean stepTip;

    private boolean stepSound;

    private boolean moveVoice;

    private boolean showNumber = true;

    private boolean topWindow = false;

    private int threadNum;

    private int hashSize;

    private String engineName;

    private List<EngineConfig> engineConfigList = new ArrayList<>();

    private Engine.AnalysisModel analysisModel;

    private long analysisValue;

    private double stageWidth;

    private double stageHeight;

    private double splitPos;
    private double splitPos2;

    private long linkScanTime;
    private int linkThreadNum;
    private boolean linkAnimation;
    private boolean linkShowInfo;

    private boolean showEngineLog = false;
    private boolean linkBackMode;

    private List<String> openBookList;

    private Boolean localBookFirst;

    private Boolean useCloudBook;

    private Boolean onlyCloudFinalPhase;

    private Integer cloudBookTimeout;

    private Integer offManualSteps;

    private MoveRule moveRule;

    private Boolean bookSwitch;

    private int engineDelayStart = 0;
    private int engineDelayEnd = 0;

    private int bookDelayStart = 0;
    private int bookDelayEnd = 0;

    private int autoBattleOpeningMinTime = 1;
    private int autoBattleOpeningMaxTime = 10;
    private int autoBattleMiddleMinTime = 10;
    private int autoBattleMiddleMaxTime = 60;
    private int autoBattleEndMinTime = 40;
    private int autoBattleEndMaxTime = 90;

    private int mouseClickDelay = 2;
    private int mouseMoveDelay = 0;
    /*
     * 显示棋谱管理
     */
    private boolean showChessNotation = false;

    private String chessManualPath;

    private boolean manualTip = true;

    private boolean colloquialReviewStyle = true;

    private double annotationTitleFontSize = 18;

    private double annotationBodyFontSize = 15;

    private String annotationTitleColor = "#2F2A26";

    private String annotationBodyColor = "#332D28";

    private boolean annotationShowOwnSide = true;

    private boolean annotationShowOpponentSide = false;

    private Properties(ChessBoard.BoardSize boardSize, boolean stepTip,
                       int threadNum, int hashSize, String engineName, Engine.AnalysisModel analysisModel, long analysisValue,
                       boolean stepSound, boolean moveVoice, double stageWidth, double stageHeight, double splitPos, double splitPos2,
                       long linkScanTime, int linkThreadNum, boolean linkAnimation, boolean linkShowInfo, boolean linkBackMode,
                       Boolean localBookFirst, Boolean useCloudBook, Boolean onlyCloudFinalPhase, Integer cloudBookTimeout, Integer offManualSteps,
                       MoveRule moveRule, Boolean bookSwitch, List<String> openBookList) {
        this.boardSize = boardSize;
        this.stepTip = stepTip;
        this.threadNum = threadNum;
        this.hashSize = hashSize;
        this.engineName = engineName;
        this.analysisModel = analysisModel;
        this.analysisValue = analysisValue;
        this.stepSound = stepSound;
        this.moveVoice = moveVoice;
        this.stageWidth = stageWidth;
        this.stageHeight = stageHeight;
        this.splitPos = splitPos;
        this.splitPos2 = splitPos2;
        this.linkScanTime = linkScanTime;
        this.linkThreadNum = linkThreadNum;
        this.linkAnimation = linkAnimation;
        this.linkShowInfo = linkShowInfo;
        this.linkBackMode = linkBackMode;
        this.localBookFirst = localBookFirst;
        this.useCloudBook = useCloudBook;
        this.onlyCloudFinalPhase = onlyCloudFinalPhase;
        this.cloudBookTimeout = cloudBookTimeout;
        this.offManualSteps = offManualSteps;
        this.moveRule = moveRule;
        this.bookSwitch = bookSwitch;
        this.openBookList = openBookList;
    }

    public static synchronized Properties getInstance() {
        if (prop == null) {
            // 配置文件位于用户数据目录，文件名固定为 "properties"（Java 序列化）。
            String path = PathUtils.getDataPath() + "properties";
            File file = new File(path);
            if (!file.exists()) {
                // 兼容历史版本：首次切换到新目录时，迁移旧位置配置。
                File legacy = new File(PathUtils.getJarPath() + "properties");
                if (legacy.exists()) {
                    try {
                        copyFile(legacy, file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (file.exists()) {
                ObjectInputStream os = null;
                try {
                    os = new ObjectInputStream(new FileInputStream(file));
                    prop = (Properties) os.readObject();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (os != null)
                            os.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // 首次启动默认配置。
                try {
                    List<EngineConfig> engineConfigList = new ArrayList<>();
                    prop = new Properties(ChessBoard.BoardSize.AUTOFIT_BOARD, true,
                            1, 16, "",
                            Engine.AnalysisModel.FIXED_TIME, 5000, false,
                            false,
                            DEFAULT_STAGE_WIDTH, DEFAULT_STAGE_HEIGHT, 0.58, 0.47,
                            100, 2, true, true, false,
                            true, true, false, 2000, 9999,
                            MoveRule.BEST_SCORE, true, new ArrayList<>());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            normalizeAutoBattleTime(prop);
            normalizeAnnotationStyle(prop);
            normalizeLayout(prop);
            BuiltinEngineLoader.autoLoad(prop);
        }
        return prop;
    }

    private static void normalizeAutoBattleTime(Properties prop) {
        if (prop == null) {
            return;
        }
        // 兼容旧版本序列化字段缺失：缺失时会反序列化为 0，这里回填默认区间。
        if (prop.autoBattleOpeningMinTime <= 1 && prop.autoBattleOpeningMaxTime <= 1) {
            prop.autoBattleOpeningMinTime = 1;
            prop.autoBattleOpeningMaxTime = 10;
        }
        if (prop.autoBattleMiddleMinTime <= 1 && prop.autoBattleMiddleMaxTime <= 1) {
            prop.autoBattleMiddleMinTime = 10;
            prop.autoBattleMiddleMaxTime = 60;
        }
        if (prop.autoBattleEndMinTime <= 1 && prop.autoBattleEndMaxTime <= 1) {
            prop.autoBattleEndMinTime = 40;
            prop.autoBattleEndMaxTime = 90;
        }

        prop.autoBattleOpeningMinTime = clampAutoBattleTime(prop.autoBattleOpeningMinTime);
        prop.autoBattleOpeningMaxTime = clampAutoBattleTime(prop.autoBattleOpeningMaxTime);
        prop.autoBattleMiddleMinTime = clampAutoBattleTime(prop.autoBattleMiddleMinTime);
        prop.autoBattleMiddleMaxTime = clampAutoBattleTime(prop.autoBattleMiddleMaxTime);
        prop.autoBattleEndMinTime = clampAutoBattleTime(prop.autoBattleEndMinTime);
        prop.autoBattleEndMaxTime = clampAutoBattleTime(prop.autoBattleEndMaxTime);

        if (prop.autoBattleOpeningMinTime > prop.autoBattleOpeningMaxTime) {
            prop.autoBattleOpeningMaxTime = prop.autoBattleOpeningMinTime;
        }
        if (prop.autoBattleMiddleMinTime > prop.autoBattleMiddleMaxTime) {
            prop.autoBattleMiddleMaxTime = prop.autoBattleMiddleMinTime;
        }
        if (prop.autoBattleEndMinTime > prop.autoBattleEndMaxTime) {
            prop.autoBattleEndMaxTime = prop.autoBattleEndMinTime;
        }
    }

    private static int clampAutoBattleTime(int value) {
        if (value < 1) {
            return 1;
        }
        if (value > 90) {
            return 90;
        }
        return value;
    }

    private static void normalizeAnnotationStyle(Properties prop) {
        if (prop == null) {
            return;
        }
        if (prop.annotationTitleFontSize < 12 || prop.annotationTitleFontSize > 40) {
            prop.annotationTitleFontSize = 18;
        }
        if (prop.annotationBodyFontSize < 12 || prop.annotationBodyFontSize > 36) {
            prop.annotationBodyFontSize = 15;
        }
        if (prop.annotationTitleColor == null || prop.annotationTitleColor.isBlank()) {
            prop.annotationTitleColor = "#2F2A26";
        }
        if (prop.annotationBodyColor == null || prop.annotationBodyColor.isBlank()) {
            prop.annotationBodyColor = "#332D28";
        }
        if (!prop.annotationShowOwnSide && !prop.annotationShowOpponentSide) {
            prop.annotationShowOwnSide = true;
        }
    }

    private static void normalizeLayout(Properties prop) {
        if (prop == null) {
            return;
        }
        if (prop.boardStyle == null) {
            prop.boardStyle = ChessBoard.BoardStyle.CUSTOM;
        }
        if (prop.stageWidth <= 0) {
            prop.stageWidth = DEFAULT_STAGE_WIDTH;
        }
        if (prop.stageHeight <= 0) {
            prop.stageHeight = DEFAULT_STAGE_HEIGHT;
        }
        if (prop.splitPos <= 0 || prop.splitPos >= 1
                || nearlyEquals(prop.splitPos, 0.64)
                || nearlyEquals(prop.splitPos, 0.56)
                || nearlyEquals(prop.splitPos, 0.49)
                || nearlyEquals(prop.splitPos, 0.6416122004357299)) {
            prop.splitPos = 0.58;
        }
        if (prop.splitPos2 <= 0 || prop.splitPos2 >= 1
                || nearlyEquals(prop.splitPos2, 0.6)
                || nearlyEquals(prop.splitPos2, 0.54)
                || nearlyEquals(prop.splitPos2, 0.6461538461538462)) {
            prop.splitPos2 = 0.47;
        }
    }

    private static boolean nearlyEquals(double value, double expected) {
        return Math.abs(value - expected) < 0.02;
    }

    public void save() {
        ObjectOutputStream os = null;
        try {
            // 全量序列化保存当前配置。
            String path = PathUtils.getDataPath() + "properties";
            File file = new File(path);
            os = new ObjectOutputStream(new FileOutputStream(file));
            os.writeObject(this);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null)
                    os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public ChessBoard.BoardStyle getBoardStyle() {
        return boardStyle;
    }

    public void setBoardStyle(ChessBoard.BoardStyle boardStyle) {
        this.boardStyle = boardStyle;
    }

    public int getEngineDelayStart() {
        return engineDelayStart;
    }

    public void setEngineDelayStart(int engineDelayStart) {
        this.engineDelayStart = engineDelayStart;
    }

    public int getEngineDelayEnd() {
        return engineDelayEnd;
    }

    public void setEngineDelayEnd(int engineDelayEnd) {
        this.engineDelayEnd = engineDelayEnd;
    }

    public int getBookDelayStart() {
        return bookDelayStart;
    }

    public void setBookDelayStart(int bookDelayStart) {
        this.bookDelayStart = bookDelayStart;
    }

    public int getBookDelayEnd() {
        return bookDelayEnd;
    }

    public void setBookDelayEnd(int bookDelayEnd) {
        this.bookDelayEnd = bookDelayEnd;
    }

    public int getAutoBattleOpeningMinTime() {
        return autoBattleOpeningMinTime;
    }

    public void setAutoBattleOpeningMinTime(int autoBattleOpeningMinTime) {
        this.autoBattleOpeningMinTime = clampAutoBattleTime(autoBattleOpeningMinTime);
    }

    public int getAutoBattleOpeningMaxTime() {
        return autoBattleOpeningMaxTime;
    }

    public void setAutoBattleOpeningMaxTime(int autoBattleOpeningMaxTime) {
        this.autoBattleOpeningMaxTime = clampAutoBattleTime(autoBattleOpeningMaxTime);
    }

    public int getAutoBattleMiddleMinTime() {
        return autoBattleMiddleMinTime;
    }

    public void setAutoBattleMiddleMinTime(int autoBattleMiddleMinTime) {
        this.autoBattleMiddleMinTime = clampAutoBattleTime(autoBattleMiddleMinTime);
    }

    public int getAutoBattleMiddleMaxTime() {
        return autoBattleMiddleMaxTime;
    }

    public void setAutoBattleMiddleMaxTime(int autoBattleMiddleMaxTime) {
        this.autoBattleMiddleMaxTime = clampAutoBattleTime(autoBattleMiddleMaxTime);
    }

    public int getAutoBattleEndMinTime() {
        return autoBattleEndMinTime;
    }

    public void setAutoBattleEndMinTime(int autoBattleEndMinTime) {
        this.autoBattleEndMinTime = clampAutoBattleTime(autoBattleEndMinTime);
    }

    public int getAutoBattleEndMaxTime() {
        return autoBattleEndMaxTime;
    }

    public void setAutoBattleEndMaxTime(int autoBattleEndMaxTime) {
        this.autoBattleEndMaxTime = clampAutoBattleTime(autoBattleEndMaxTime);
    }

    public int getMouseClickDelay() {
        return mouseClickDelay;
    }

    public void setMouseClickDelay(int mouseClickDelay) {
        this.mouseClickDelay = mouseClickDelay;
    }

    public int getMouseMoveDelay() {
        return mouseMoveDelay;
    }

    public void setMouseMoveDelay(int mouseMoveDelay) {
        this.mouseMoveDelay = mouseMoveDelay;
    }

    public List<String> getOpenBookList() {
        return openBookList;
    }

    public void setOpenBookList(List<String> openBookList) {
        this.openBookList = openBookList;
    }

    public Boolean getLocalBookFirst() {
        return localBookFirst;
    }

    public void setLocalBookFirst(Boolean localBookFirst) {
        this.localBookFirst = localBookFirst;
    }

    public Boolean getUseCloudBook() {
        return useCloudBook;
    }

    public void setUseCloudBook(Boolean useCloudBook) {
        this.useCloudBook = useCloudBook;
    }

    public Boolean getOnlyCloudFinalPhase() {
        return onlyCloudFinalPhase;
    }

    public void setOnlyCloudFinalPhase(Boolean onlyCloudFinalPhase) {
        this.onlyCloudFinalPhase = onlyCloudFinalPhase;
    }

    public Integer getCloudBookTimeout() {
        return cloudBookTimeout;
    }

    public void setCloudBookTimeout(Integer cloudBookTimeout) {
        this.cloudBookTimeout = cloudBookTimeout;
    }

    public Integer getOffManualSteps() {
        return offManualSteps;
    }

    public void setOffManualSteps(Integer offManualSteps) {
        this.offManualSteps = offManualSteps;
    }

    public MoveRule getMoveRule() {
        return moveRule;
    }

    public void setMoveRule(MoveRule moveRule) {
        this.moveRule = moveRule;
    }

    public Boolean getBookSwitch() {
        return bookSwitch;
    }

    public void setBookSwitch(Boolean bookSwitch) {
        this.bookSwitch = bookSwitch;
    }

    public long getLinkScanTime() {
        return linkScanTime;
    }

    public void setLinkScanTime(long linkScanTime) {
        this.linkScanTime = linkScanTime;
    }

    public int getLinkThreadNum() {
        return linkThreadNum;
    }

    public void setLinkThreadNum(int linkThreadNum) {
        this.linkThreadNum = linkThreadNum;
    }

    public boolean isLinkAnimation() {
        return linkAnimation;
    }

    public void setLinkAnimation(boolean linkAnimation) {
        this.linkAnimation = linkAnimation;
    }

    public boolean isLinkShowInfo() {
        return linkShowInfo;
    }

    public void setLinkShowInfo(boolean linkShowInfo) {
        this.linkShowInfo = linkShowInfo;
    }

    public boolean isShowEngineLog() {
        return showEngineLog;
    }

    public void setShowEngineLog(boolean showEngineLog) {
        this.showEngineLog = showEngineLog;
    }

    public boolean isLinkBackMode() {
        return linkBackMode;
    }

    public void setLinkBackMode(boolean linkBackMode) {
        this.linkBackMode = linkBackMode;
    }

    public double getSplitPos2() {
        return splitPos2;
    }

    public void setSplitPos2(double splitPos2) {
        this.splitPos2 = splitPos2;
    }

    public double getStageWidth() {
        return stageWidth;
    }

    public void setStageWidth(double stageWidth) {
        this.stageWidth = stageWidth;
    }

    public double getStageHeight() {
        return stageHeight;
    }

    public void setStageHeight(double stageHeight) {
        this.stageHeight = stageHeight;
    }

    public double getSplitPos() {
        return splitPos;
    }

    public void setSplitPos(double splitPos) {
        this.splitPos = splitPos;
    }

    public boolean isStepSound() {
        return stepSound;
    }

    public void setStepSound(boolean stepSound) {
        this.stepSound = stepSound;
    }

    public boolean isMoveVoice() {
        return moveVoice;
    }

    public void setMoveVoice(boolean moveVoice) {
        this.moveVoice = moveVoice;
    }

    public Engine.AnalysisModel getAnalysisModel() {
        return analysisModel;
    }

    public void setAnalysisModel(Engine.AnalysisModel analysisModel) {
        this.analysisModel = analysisModel;
    }

    public long getAnalysisValue() {
        return analysisValue;
    }

    public void setAnalysisValue(long analysisValue) {
        this.analysisValue = analysisValue;
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public int getHashSize() {
        return hashSize;
    }

    public void setHashSize(int hashSize) {
        this.hashSize = hashSize;
    }

    public List<EngineConfig> getEngineConfigList() {
        return engineConfigList;
    }

    public void setEngineConfigList(List<EngineConfig> engineConfigList) {
        this.engineConfigList = engineConfigList;
    }

    public ChessBoard.BoardSize getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(ChessBoard.BoardSize boardSize) {
        this.boardSize = boardSize;
    }

    public boolean isStepTip() {
        return stepTip;
    }

    public void setStepTip(boolean stepTip) {
        this.stepTip = stepTip;
    }

    public boolean isShowNumber() {
        return showNumber;
    }

    public void setShowNumber(boolean showNumber) {
        this.showNumber = showNumber;
    }

    public boolean isTopWindow() {
        return topWindow;
    }

    public void setTopWindow(boolean topWindow) {
        this.topWindow = topWindow;
    }

    public boolean isShowChessNotation() {
        return showChessNotation;
    }

    public void setShowChessNotation(boolean showChessNotation) {
        this.showChessNotation = showChessNotation;
    }

    public String getChessManualPath() {
        return chessManualPath;
    }

    public void setChessManualPath(String chessManualPath) {
        this.chessManualPath = chessManualPath;
    }

    public boolean isManualTip() {
        return manualTip;
    }

    public void setManualTip(boolean manualTip) {
        this.manualTip = manualTip;
    }

    public boolean isColloquialReviewStyle() {
        return colloquialReviewStyle;
    }

    public void setColloquialReviewStyle(boolean colloquialReviewStyle) {
        this.colloquialReviewStyle = colloquialReviewStyle;
    }

    public double getAnnotationTitleFontSize() {
        return annotationTitleFontSize;
    }

    public void setAnnotationTitleFontSize(double annotationTitleFontSize) {
        this.annotationTitleFontSize = annotationTitleFontSize;
    }

    public double getAnnotationBodyFontSize() {
        return annotationBodyFontSize;
    }

    public void setAnnotationBodyFontSize(double annotationBodyFontSize) {
        this.annotationBodyFontSize = annotationBodyFontSize;
    }

    public String getAnnotationTitleColor() {
        return annotationTitleColor;
    }

    public void setAnnotationTitleColor(String annotationTitleColor) {
        this.annotationTitleColor = annotationTitleColor;
    }

    public String getAnnotationBodyColor() {
        return annotationBodyColor;
    }

    public void setAnnotationBodyColor(String annotationBodyColor) {
        this.annotationBodyColor = annotationBodyColor;
    }

    public boolean isAnnotationShowOwnSide() {
        return annotationShowOwnSide;
    }

    public void setAnnotationShowOwnSide(boolean annotationShowOwnSide) {
        this.annotationShowOwnSide = annotationShowOwnSide;
    }

    public boolean isAnnotationShowOpponentSide() {
        return annotationShowOpponentSide;
    }

    public void setAnnotationShowOpponentSide(boolean annotationShowOpponentSide) {
        this.annotationShowOpponentSide = annotationShowOpponentSide;
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }
}
