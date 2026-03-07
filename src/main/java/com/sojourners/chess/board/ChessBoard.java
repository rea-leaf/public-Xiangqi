package com.sojourners.chess.board;

import com.sojourners.chess.config.Properties;
import com.sojourners.chess.media.SoundPlayer;
import com.sojourners.chess.util.PathUtils;
import com.sojourners.chess.util.StringUtils;
import com.sojourners.chess.util.XiangqiUtils;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 棋盘领域模型 + 渲染入口。
 *
 * <p>职责：
 * 1) 维护当前局面(board)与交互状态(选中点、上一步、提示箭头)；
 * 2) 处理鼠标落子、合法性校验、走子音效；
 * 3) FEN 与引擎步法互转；
 * 4) 将状态交给 Render 层完成绘制。
 */
public class ChessBoard {

    /** 当前棋盘渲染器（默认主题/自定义主题）。 */
    private static BaseBoardRender boardRender;

    /** 棋盘局面：10x9。 */
    private static volatile char[][] board = new char[10][9];

    /** 走法翻译时使用的临时棋盘副本。 */
    private static char[][] copyBoard = new char[10][9];

    private BoardSize boardSize;

    private boolean stepTip;

    private boolean showNumber;

    private boolean stepSound;

    private boolean manualTip;
    private List<Step> manualList = new ArrayList<>();

    /** 棋盘音效播放器。 */
    private static SoundPlayer sound;

    static {
        sound = new SoundPlayer(PathUtils.getJarPath() + "sound/click.wav",
                PathUtils.getJarPath() + "sound/move.wav",
                PathUtils.getJarPath() + "sound/capture.wav",
                PathUtils.getJarPath() + "sound/check.wav",
                PathUtils.getJarPath() + "sound/win.wav");
    }

    private Point remark;

    private Step prevStep;

    private boolean showMultiPV;

    private List<MoveTip> moveTips = new ArrayList<>();

    private boolean isReverse;

    private Timeline moveAnimationTimeline;

    private static final int MOVE_ANIMATION_DURATION = 180;

    private String lastMoveCommentary;

    public static class Point {
        int x;
        int y;
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }
    public static class Step {
        Point start;
        Point end;
        public Step(Point start, Point end) {
            this.start = start;
            this.end = end;
        }

        public Point getStart() {
            return start;
        }

        public void setStart(Point start) {
            this.start = start;
        }

        public Point getEnd() {
            return end;
        }

        public void setEnd(Point end) {
            this.end = end;
        }
    }

    public class MoveTip {
        Step first;
        Step second;

        public MoveTip(Step first, Step second) {
            this.first = first;
            this.second = second;
        }

        public Step getFirst() {
            return first;
        }

        public void setFirst(Step first) {
            this.first = first;
        }

        public Step getSecond() {
            return second;
        }

        public void setSecond(Step second) {
            this.second = second;
        }
    }

    public enum BoardSize {
        LARGE_BOARD,
        BIG_BOARD,
        MIDDLE_BOARD,
        SMALL_BOARD,
        AUTOFIT_BOARD
    }
    public enum BoardStyle {
        DEFAULT,
        CUSTOM;
    }

    public ChessBoard(Canvas canvas, BoardSize bs, BoardStyle style, boolean stepTip, boolean manualTip,
                      boolean showMultiPV, boolean stepSound, boolean showNumber, String fenCode) {
        if (this.boardRender == null) {
            this.boardRender = style == BoardStyle.CUSTOM ? new CustomBoardRender(canvas) : new DefaultBoardRender(canvas);
        }

        this.stepTip = stepTip;
        this.manualTip = manualTip;
        this.stepSound = stepSound;
        this.showNumber = showNumber;
        this.showMultiPV = showMultiPV;
        // 设置局面（为空则初始化标准开局）
        setNewBoard(fenCode);
        // 设置棋盘大小
        this.boardSize = bs;
        // 默认不翻转
        isReverse = false;

        this.paint();
    }

    public static void initChessBoard(char[][] board) {
        // 标准中国象棋初始局面。
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                if (i == 0 && (j == 0 || j == 8)) {
                    board[i][j] = 'r';
                } else if (i == 0 && (j == 1 || j == 7)) {
                    board[i][j] = 'n';
                } else if (i == 0 && (j == 2 || j == 6)) {
                    board[i][j] = 'b';
                } else if (i == 0 && (j == 3 || j == 5)) {
                    board[i][j] = 'a';
                } else if (i == 0 && j == 4) {
                    board[i][j] = 'k';
                } else if (i == 2 && (j == 1 || j == 7)) {
                    board[i][j] = 'c';
                } else if (i == 3 && (j == 0 || j == 2 || j == 4 || j == 6 || j == 8)) {
                    board[i][j] = 'p';
                } else if (i == 9 && (j == 0 || j == 8)) {
                    board[i][j] = 'R';
                } else if (i == 9 && (j == 1 || j == 7)) {
                    board[i][j] = 'N';
                } else if (i == 9 && (j == 2 || j == 6)) {
                    board[i][j] = 'B';
                } else if (i == 9 && (j == 3 || j == 5)) {
                    board[i][j] = 'A';
                } else if (i == 9 && j == 4) {
                    board[i][j] = 'K';
                } else if (i == 7 && (j == 1 || j == 7)) {
                    board[i][j] = 'C';
                } else if (i == 6 && (j == 0 || j == 2 || j == 4 || j == 6 || j == 8)) {
                    board[i][j] = 'P';
                } else {
                    board[i][j] = ' ';
                }
            }
        }
    }

    public void showMultiPV(boolean showMultiPV) {
        this.showMultiPV = showMultiPV;
    }

    private void setNewBoard(String fenCode) {
        if (StringUtils.isEmpty(fenCode)) {
            initChessBoard(board);
        } else {
            setBoard(fenCode);
        }
    }

    public void setBoardStyle(BoardStyle style, Canvas canvas) {
        this.boardRender = style == BoardStyle.CUSTOM ? new CustomBoardRender(canvas) : new DefaultBoardRender(canvas);
        this.paint();
    }

    public String mouseClick(int x, int y, boolean canRedGo, boolean canBlackGo) {
        // 将画布坐标映射为棋盘坐标（考虑 padding、棋子尺寸、翻转状态）。
        int padding = boardRender.getPadding(this.boardSize);
        int piece = boardRender.getPieceSize(this.boardSize);
        int i = (x - padding) / piece;
        int j = (y - padding) / piece;
        i = boardRender.getReverseX(i, isReverse);
        j = boardRender.getReverseY(j, isReverse);

        if (i < 0 || i > 8 || j < 0 || j > 9) {
            return null;
        }

        if (remark != null) {
            // 已选中棋子：本次点击可能是改选同色子，也可能是尝试落子。
            boolean isRed = XiangqiUtils.isRed(board[remark.y][remark.x]);
            if (isRed && !canRedGo || !isRed && !canBlackGo) {
                return null;
            } else if (board[j][i] != ' ' && XiangqiUtils.isRed(board[j][i]) == isRed) {
                if (stepSound) sound.pick();
                remark = new Point(i, j);
                paint();
                return null;
            } else if (!XiangqiUtils.canGo(board, remark.y, remark.x, j, i)) {
                return null;
            } else {
                return move(remark.x, remark.y, i, j);
            }
        } else {
            // 未选中棋子：仅在可行动方点击己方棋子时进入选中态。
            if (board[j][i] != ' ') {
                boolean isRed = XiangqiUtils.isRed(board[j][i]);
                if (!(isRed && !canRedGo || !isRed && !canBlackGo)) {
                    if (stepSound) sound.pick();
                    remark = new Point(i, j);
                    paint();
                }
            }
            return null;
        }

    }

    private void setBoard(String fenCode) {
        XiangqiUtils.fenToBoard(this.board, fenCode);
    }

    public String fenCode(boolean redGo) {
        return fenCode(this.board, redGo);
    }

    public static String fenCode(char[][] board, Boolean redGo) {
        // 生成 FEN 主体 + 可选行棋方字段。
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < board.length; i++) {
            int count = 0;
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] != ' ') {
                    if (count != 0) {
                        sb.append(count);
                        count = 0;
                    }
                    sb.append(board[i][j]);
                } else {
                    count++;
                }
            }
            if (count != 0) {
                sb.append(count);
            }
            if (i != board.length - 1) {
                sb.append("/");
            }
        }
        if (redGo != null) {
            if (redGo) {
                sb.append(" w - - 0 1");
            } else {
                sb.append(" b - - 0 1");
            }
        }
        return sb.toString();
    }

    /**
     * 浏览棋谱
     * @param fenCode
     * @param moveList
     */
    public void browseChessRecord(String fenCode, List<String> moveList) {
        setBoard(fenCode);
        if (moveList == null || moveList.isEmpty()) {
            // 开始局面
            prevStep = null;
            moveTips.clear();
            remark = null;
            manualList.clear();
            paint();
        } else {
            for (int i = 0; i < moveList.size() - 1; i++) {
                Step s = stepForBoard(moveList.get(i));
                board[s.getEnd().y][s.getEnd().x] = board[s.getStart().y][s.getStart().x];
                board[s.getStart().y][s.getStart().x] = ' ';
            }
            Step s = stepForBoard(moveList.get(moveList.size() - 1));
            move(s.getStart().x, s.getStart().y, s.getEnd().x, s.getEnd().y);
        }
    }

    public void setTip(String firstMove, String secondMove, int pv) {
        if (pv < moveTips.size()) {
            moveTips.clear();
        }
        if (pv > moveTips.size()) {
            moveTips.add(new MoveTip(stepForBoard(firstMove), stepForBoard(secondMove)));
        } else {
            moveTips.set(pv - 1, new MoveTip(stepForBoard(firstMove), stepForBoard(secondMove)));
        }
        if (stepTip) {
            paint();
        }
    }

    public void setManualList(List<String> list) {
        manualList.clear();
        for (String move : list) {
            manualList.add(stepForBoard(move));
        }
        if (manualTip)
            paint();
    }

    public Step stepForBoard(String step) {
        if (step == null) {
            return null;
        }
        char c = step.charAt(0);
        int x1 = c - 'a';
        c = step.charAt(1);
        int y1 = 9 - Integer.parseInt(String.valueOf(c));
        c = step.charAt(2);
        int x2 = c - 'a';
        c = step.charAt(3);
        int y2 = 9 - Integer.parseInt(String.valueOf(c));
        return new Step(new Point(x1, y1), new Point(x2, y2));
    }

    public Step move(String step) {
        if (step == null || step.length() != 4) {
            return null;
        }
        Step s = stepForBoard(step);
        move(s.getStart().x, s.getStart().y, s.getEnd().x, s.getEnd().y);
        return s;
    }

    public String move(int x1, int y1, int x2, int y2) {
        // 先试走，再校验“是否送将”；非法则回滚。
        String moveCode = stepForEngine(x1, y1, x2, y2);
        StringBuilder sb = new StringBuilder();
        XiangqiUtils.translate(this.board, sb, moveCode, false);

        char tmp = board[y2][x2];
        char movingPiece = board[y1][x1];
        boolean isRed = XiangqiUtils.isRed(movingPiece);
        board[y2][x2] = board[y1][x1];
        board[y1][x1] = ' ';
        if (XiangqiUtils.isJiang(board, isRed)) {
            // 不可送将
            if (stepSound) {
                sound.check();
            }
            board[y1][x1] = board[y2][x2];
            board[y2][x2] = tmp;
            lastMoveCommentary = null;
            return null;
        }
        boolean checkMate = XiangqiUtils.isSha(board, !isRed);
        boolean check = XiangqiUtils.isJiang(board, !isRed);
        if (stepSound) {
            // 根据局面结果选择不同音效：绝杀/将军/吃子/平移。
            if (checkMate) {
                // 绝杀
                sound.over();
            } else if (check) {
                // 将军
                sound.check();
            } else {
                // 是否吃子
                if (tmp == ' ') {
                    sound.move();
                } else {
                    sound.eat();
                }
            }
        }

        lastMoveCommentary = buildMoveCommentary(sb.toString(), moveCode);

        prevStep = new Step(new Point(x1, y1), new Point(x2, y2));
        moveTips.clear();
        remark = null;
        manualList.clear();
        playMoveAnimation(movingPiece, x1, y1, x2, y2);
        return moveCode;
    }

    private void playMoveAnimation(char movingPiece, int x1, int y1, int x2, int y2) {
        if (!Platform.isFxApplicationThread()) {
            paint();
            return;
        }
        if (moveAnimationTimeline != null) {
            moveAnimationTimeline.stop();
        }

        SimpleDoubleProperty progress = new SimpleDoubleProperty(0);
        progress.addListener((observable, oldValue, newValue) ->
                boardRender.paintAnimation(boardSize, this.board, prevStep, remark, stepTip,
                        showMultiPV, moveTips, isReverse, showNumber, manualTip, manualList,
                        movingPiece, x1, y1, x2, y2, newValue.doubleValue()));

        moveAnimationTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progress, 0d)),
                new KeyFrame(Duration.millis(MOVE_ANIMATION_DURATION), new KeyValue(progress, 1d, Interpolator.EASE_BOTH))
        );
        moveAnimationTimeline.setOnFinished(event -> paint());
        moveAnimationTimeline.play();
    }

    private String buildMoveCommentary(String notation, String moveCode) {
        if (StringUtils.isNotEmpty(notation)) {
            return notation;
        }
        return moveCode;
    }

    public List<String> getTacticList(boolean redGo) {
        // 枚举当前走棋方的全部合法着法，用于“变招搜索”限定 searchmoves。
        List<String> list = new ArrayList<>();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] != ' ' && XiangqiUtils.isRed(board[i][j]) == redGo) {
                    for (int i2 = 0; i2 < board.length; i2++) {
                        for (int j2 = 0; j2 < board[0].length; j2++) {
                            if ((i != i2 || j != j2) && XiangqiUtils.canGo(board, i, j, i2, j2)) {
                                list.add(stepForEngine(j, i, j2, i2));
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    public static String stepForEngine(int x1, int y1, int x2, int y2) {
        StringBuffer sb = new StringBuffer();
        sb.append((char)('a' + x1));
        sb.append(9 - y1);
        sb.append((char)('a' + x2));
        sb.append(9 - y2);
        return sb.toString();
    }

    private void paint() {
        // 所有绘制统一收口到 Render 层，避免 UI 逻辑散落。
        this.boardRender.paint(boardSize, this.board, prevStep, remark, stepTip,
                showMultiPV, moveTips, isReverse, showNumber, manualTip, manualList);
    }

    /**
     * 设置翻转
     * @param isReverse
     */
    public void reverse(boolean isReverse) {
        if (this.isReverse != isReverse) {
            this.isReverse = isReverse;
            paint();
        }
    }

    /**
     * 设置棋盘样式
     * @param bs
     */
    public void setBoardSize(BoardSize bs) {
        this.boardSize = bs;
        paint();
    }

    /**
     * 设置棋步提示
     * @param f
     */
    public void setStepTip(boolean f) {
        this.stepTip = f;
        paint();
    }

    public void setManualTip(boolean f) {
        this.manualTip = f;
        paint();
    }

    public void setShowNumber(boolean showNumber) {
        this.showNumber = showNumber;
        paint();
    }

    /**
     * 设置走棋音效
     * @param f
     */
    public void setStepSound(boolean f) {
        this.stepSound = f;
    }

    /**
     * 翻译着法(记录棋谱)
     * @param move
     * @return
     */
    public String translate(String move, boolean hasGo) {
        StringBuilder sb = new StringBuilder();
        XiangqiUtils.translate(this.board, sb, move, hasGo);
        return sb.toString();
    }

    /**
     * 翻译引擎着法(思考细节)
     * @param moveList
     * @return
     */
    public String translate(List<String> moveList) {
        for (int i = 0; i < board.length; i++) {
            System.arraycopy(board[i], 0, copyBoard[i], 0, copyBoard[i].length);
        }
        StringBuilder sb = new StringBuilder();
        for (String move : moveList) {
            char a = move.charAt(0), b = move.charAt(1), c = move.charAt(2), d = move.charAt(3);
            int fromJ = a - 'a', toJ = c - 'a';
            int fromI = 9 - Integer.parseInt(String.valueOf(b)), toI = 9 - Integer.parseInt(String.valueOf(d));
            XiangqiUtils.translate(copyBoard, sb, move, false);
            sb.append("  ");
            copyBoard[toI][toJ] = copyBoard[fromI][fromJ];
            copyBoard[fromI][fromJ] = ' ';
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }

    public char[][] getBoard() {
        return this.board;
    }

    public String getLastMoveCommentary() {
        return lastMoveCommentary;
    }

    public void autoFitSize(double width, double height, double position) {
        // 自适应策略：依据可用区域反推棋子像素，并设下限避免过小不可读。
        if (boardSize == BoardSize.AUTOFIT_BOARD) {
            if (Properties.getInstance().isShowChessNotation()) {
                width = width - 240;
            }
            position = Math.abs(position);
            width = width * position;
            height = height - 56;
            if (Properties.getInstance().isLinkShowInfo()) {
                height = height - 27;
            }
            int pieceSize;
            if (width / height > 1120 / 1240d) {
                pieceSize = (int) (height / (10 + 1/3d));
            } else {
                pieceSize = (int) (width / (9 + 1/3d));
            }
            if (pieceSize < 42) {
                pieceSize = 42;
            }
            boardRender.setAutoPieceSize(pieceSize);

            paint();
        }
    }
}
