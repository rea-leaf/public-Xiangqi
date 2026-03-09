package com.chf.chess.board;

import com.chf.chess.config.Properties;
import com.chf.chess.media.SoundPlayer;
import com.chf.chess.util.PathUtils;
import com.chf.chess.util.StringUtils;
import com.chf.chess.util.XiangqiUtils;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

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

    private boolean moveVoice;

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

    private String lastMoveAnnotation;

    private LinkedHashMap<String, String> lastMoveOpponentPlans = new LinkedHashMap<>();

    private boolean colloquialReviewStyle = true;

    private int lastReviewTheme = -1;
    private int reviewThemeStreak = 0;

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
        CUSTOM,
        JADE,
        INK,
        LIGHT,
        AUTUMN,
        CHINESE,
        ANTIQUE,
        PALACE,
        CELADON,
        LANDSCAPE,
        XUAN_PAPER,
        CINNABAR,
        EBONY,
        BRONZE,
        PINE_SOOT;
    }

    public ChessBoard(Canvas canvas, BoardSize bs, BoardStyle style, boolean stepTip, boolean manualTip,
                      boolean showMultiPV, boolean stepSound, boolean moveVoice, boolean showNumber, String fenCode) {
        if (this.boardRender == null) {
            this.boardRender = createBoardRender(style, canvas);
        }

        this.stepTip = stepTip;
        this.manualTip = manualTip;
        this.stepSound = stepSound;
        this.moveVoice = moveVoice;
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
        this.boardRender = createBoardRender(style, canvas);
        this.paint();
    }

    public static BaseBoardRender createBoardRender(BoardStyle style, Canvas canvas) {
        BoardStyle actualStyle = style == null ? BoardStyle.CUSTOM : style;
        return switch (actualStyle) {
            case CUSTOM -> new CustomBoardRender(canvas);
            case DEFAULT, JADE, INK, LIGHT, AUTUMN, CHINESE, ANTIQUE, PALACE, CELADON,
                    LANDSCAPE, XUAN_PAPER, CINNABAR, EBONY, BRONZE, PINE_SOOT -> new DefaultBoardRender(canvas, actualStyle);
        };
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
            lastMoveAnnotation = null;
            lastMoveOpponentPlans.clear();
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
        if (moveVoice) {
            sound.speakMove(sb.toString());
        }

        lastMoveCommentary = buildMoveCommentary(sb.toString(), moveCode);
        lastMoveAnnotation = buildMoveAnnotation(movingPiece, tmp, check, checkMate, x1, y1, x2, y2, lastMoveCommentary);

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

    private String buildMoveAnnotation(char movingPiece, char captured, boolean check, boolean checkMate,
                                       int x1, int y1, int x2, int y2, String notation) {
        final int THEME_FINISH = 1;
        final int THEME_FORCING = 2;
        final int THEME_TRADE = 3;
        final int THEME_MAJOR = 4;
        final int THEME_KNIGHT = 5;
        final int THEME_PAWN = 6;
        final int THEME_DEFENSE = 7;
        final int THEME_TUNE = 8;
        final int THEME_TEMPO = 9;

        String prefix = StringUtils.isNotEmpty(notation) ? notation : stepForEngine(x1, y1, x2, y2).toUpperCase();
        String pieceName = pieceTeachName(movingPiece);
        String pieceRef = pieceInstanceName(movingPiece, x1, y1);
        boolean myRed = XiangqiUtils.isRed(movingPiece);
        boolean forward = y2 < y1;
        int delta = Math.abs(y2 - y1) + Math.abs(x2 - x1);
        boolean center = x2 == 4;
        int seed = x1 * 97 + y1 * 89 + x2 * 83 + y2 * 79 + (captured == ' ' ? 0 : 37);

        String purpose;
        String risk;
        String follow;

        if (checkMate) {
            purpose = "通过" + pieceRef + "形成连续威胁并完成杀棋，直接结束对局";
            risk = "如果不这样走，可能会给对手留下逃将或反击的最后机会";
            follow = "同类局面优先找“先手将军+封锁退路”的组合，而不是只看单步得子";
            return withOpponentPlans(formatTeaching(prefix, THEME_FINISH, purpose, risk, follow, seed), myRed);
        }
        if (check) {
            purpose = "先将军抢节奏，迫使对手先解将，主动权回到自己手里";
            risk = "如果不将军，节奏可能被对手抢走，自己的进攻点会被先手化解";
            follow = "下一步优先衔接补将位、牵制大子或简化转换，持续保持先手压力";
            return withOpponentPlans(formatTeaching(prefix, THEME_FORCING, purpose, risk, follow, seed), myRed);
        }
        if (captured != ' ') {
            purpose = "先用" + pieceRef + "吃子扩大子力优势，同时削弱对方关键防守点";
            risk = "如果不吃，等于把现成收益让给对手，后面可能要付出更大代价才能再拿回来";
            follow = "吃完后先做安全检查：是否漏将、是否被反先；确认安全再考虑继续扩大战果";
            return withOpponentPlans(formatTeaching(prefix, THEME_TRADE, purpose, risk, follow, seed), myRed);
        }

        char p = Character.toLowerCase(movingPiece);
        if (p == 'r' || p == 'c') {
            purpose = center
                    ? "让" + pieceRef + "抢占中路，提高对两翼与纵线的同时控制"
                    : "调整" + pieceRef + "线路，先把攻防通道打通";
            risk = "如果不先调整大子，后续常见问题是想进攻却没有通路，反而容易被对手先手反击";
            follow = center
                    ? "下一步可考虑借中路做牵制或串打，优先攻击对方薄弱点"
                    : "线路打通后再选择是压制中路还是转翼侧，不急于一手见胜负";
            return withOpponentPlans(formatTeaching(prefix, THEME_MAJOR, purpose, risk, follow, seed), myRed);
        }
        if (p == 'n') {
            purpose = "先活" + pieceRef + "，把这匹马从原位调到更活跃线路，提升机动性并增加未来两三步的攻击点";
            risk = "如果不先活这匹马，这一路子力会继续拥堵，常见后果是进攻少一枚参与子，防守也来不及回补";
            follow = "下一步重点看这匹马能否和车炮形成联动，优先争取“马炮牵制”或“马车压线”的连续手";
            return withOpponentPlans(formatTeaching(prefix, THEME_KNIGHT, purpose, risk, follow, seed), myRed);
        }
        if (p == 'p') {
            purpose = "通过" + pieceRef + "前进争取空间，逐步压缩对手活动范围";
            risk = "如果不推进，阵型可能长期被动，给对手留下从容整子的时间";
            follow = "兵卒过河后价值更高，后续要确认后方有车炮接应，避免孤兵深入";
            return withOpponentPlans(formatTeaching(prefix, THEME_PAWN, purpose, risk, follow, seed), myRed);
        }
        if (p == 'a' || p == 'b' || p == 'k') {
            purpose = "先用" + pieceRef + "稳住将位与防守结构，降低被突袭和连续将军的风险";
            risk = "如果忽略防守，后续容易被对手借先手连续压制，局面会快速恶化";
            follow = "防守到位后再考虑反击，优先寻找能兼顾安全与反先的转换点";
            return withOpponentPlans(formatTeaching(prefix, THEME_DEFENSE, purpose, risk, follow, seed), myRed);
        }
        if (delta <= 1) {
            purpose = "做小幅调整，优化子力协同和关键点控制";
            risk = "如果不做这类整理手，后续战术常因子力站位不佳而打不出来";
            follow = "整理完成后再考虑提速进攻，效率会更高且风险更可控";
            return withOpponentPlans(formatTeaching(prefix, THEME_TUNE, purpose, risk, follow, seed), myRed);
        }
        purpose = "通过" + (forward ? "前压" : "横向/后向") + "调整节奏，先完善子力位置";
        risk = "如果急于硬攻，常见问题是进攻点单薄，容易被对手反先化解";
        follow = "下一步优先观察是否出现将军、牵制或得子的转换机会，再决定是否提速";
        return withOpponentPlans(formatTeaching(prefix, THEME_TEMPO, purpose, risk, follow, seed), myRed);
    }

    private String withOpponentPlans(String text, boolean myRed) {
        List<OpponentPlan> plans = analyzeOpponentPlans(!myRed);
        lastMoveOpponentPlans = new LinkedHashMap<>();
        if (plans.isEmpty()) {
            return text + "；对手威胁较少，先稳住阵型。";
        }
        StringBuilder sb = new StringBuilder(text);
        sb.append("；对手常见：");
        for (int i = 0; i < plans.size(); i++) {
            OpponentPlan p = plans.get(i);
            lastMoveOpponentPlans.put(p.cnMove, p.response);
            if (i > 0) {
                sb.append("；");
            }
            sb.append(i + 1).append("）").append(p.cnMove).append(" -> ").append(p.response);
        }
        sb.append("。");
        return sb.toString();
    }

    private List<OpponentPlan> analyzeOpponentPlans(boolean opponentRed) {
        List<OpponentPlan> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                char piece = board[y][x];
                if (piece == ' ' || XiangqiUtils.isRed(piece) != opponentRed) {
                    continue;
                }
                for (int y2 = 0; y2 < 10; y2++) {
                    for (int x2 = 0; x2 < 9; x2++) {
                        if ((x == x2 && y == y2) || !XiangqiUtils.canGo(board, y, x, y2, x2)) {
                            continue;
                        }
                        String move = stepForEngine(x, y, x2, y2);
                        String cnMove = translatePreview(board, move);
                        char captured = board[y2][x2];

                        board[y2][x2] = board[y][x];
                        board[y][x] = ' ';

                        boolean illegal = XiangqiUtils.isJiang(board, opponentRed);
                        boolean givesCheck = !illegal && XiangqiUtils.isJiang(board, !opponentRed);

                        board[y][x] = board[y2][x2];
                        board[y2][x2] = captured;

                        if (illegal || seen.contains(cnMove)) {
                            continue;
                        }
                        seen.add(cnMove);

                        int score = 0;
                        if (givesCheck) {
                            score += 1200;
                        }
                        score += pieceValue(captured) * 10;
                        if (x2 == 4) {
                            score += 120;
                        }
                        char p = Character.toLowerCase(piece);
                        if (p == 'r' || p == 'c' || p == 'n') {
                            score += 40;
                        }

                        String response = suggestResponse(piece, captured, givesCheck, x2, opponentRed);
                        candidates.add(new OpponentPlan(cnMove, response, score));
                    }
                }
            }
        }

        Collections.sort(candidates, Comparator.comparingInt(OpponentPlan::score).reversed());
        if (candidates.size() > 2) {
            return new ArrayList<>(candidates.subList(0, 2));
        }
        return candidates;
    }

    private String suggestResponse(char movingPiece, char captured, boolean givesCheck, int targetX, boolean opponentRed) {
        if (givesCheck) {
            return "先解将，优先挡线或兑子。";
        }
        if (captured != ' ' && pieceValue(captured) >= 5) {
            return "先算交换，能反吃就反吃。";
        }
        if (Character.toLowerCase(movingPiece) == 'p') {
            return "先封兵线，别让兵卒持续前压。";
        }
        if ((Character.toLowerCase(movingPiece) == 'r' || Character.toLowerCase(movingPiece) == 'c') && targetX == 4) {
            return "先控中路，必要时小子顶住。";
        }
        if (opponentRed) {
            return "先稳将位，再找反先。";
        }
        return "先稳将位，再找反先。";
    }

    private int pieceValue(char c) {
        char p = Character.toLowerCase(c);
        if (p == 'k') return 100;
        if (p == 'r') return 9;
        if (p == 'c') return 5;
        if (p == 'n') return 4;
        if (p == 'b' || p == 'a') return 2;
        if (p == 'p') return 1;
        return 0;
    }

    private String translatePreview(char[][] srcBoard, String move) {
        char[][] tmp = new char[10][9];
        for (int i = 0; i < srcBoard.length; i++) {
            System.arraycopy(srcBoard[i], 0, tmp[i], 0, srcBoard[i].length);
        }
        StringBuilder sb = new StringBuilder();
        XiangqiUtils.translate(tmp, sb, move, false);
        return sb.toString();
    }

    private static class OpponentPlan {
        private final String cnMove;
        private final String response;
        private final int score;

        private OpponentPlan(String cnMove, String response, int score) {
            this.cnMove = cnMove;
            this.response = response;
            this.score = score;
        }

        private int score() {
            return score;
        }
    }

    private String formatTeaching(String prefix, int theme, String purpose, String risk, String follow, int seed) {
        if (!colloquialReviewStyle) {
            return prefix + "：目的=" + purpose + "；风险=" + risk + "；后续=" + follow + "。";
        }
        if (theme == lastReviewTheme) {
            reviewThemeStreak++;
        } else {
            lastReviewTheme = theme;
            reviewThemeStreak = 1;
        }

        String[] intros = new String[]{"目的", "核心", "先手点"};
        String[] riskLeads = new String[]{"风险", "不走会", "代价"};
        String[] followLeads = new String[]{"后续", "下一步", "衔接"};

        int base = Math.abs(seed + reviewThemeStreak * 13);
        String intro = intros[base % intros.length];
        String riskLead = riskLeads[(base / 3) % riskLeads.length];
        String followLead = followLeads[(base / 5) % followLeads.length];

        return prefix + "：" + intro + "=" + purpose + "；" + riskLead + "=" + risk + "；" + followLead + "=" + follow + "。";
    }

    private String tagForTheme(int theme) {
        if (theme == 1) return "终结转换";
        if (theme == 2) return "先手压制";
        if (theme == 3) return "子力交换";
        if (theme == 4) return "大子调线";
        if (theme == 5) return "活马提子";
        if (theme == 6) return "空间推进";
        if (theme == 7) return "稳固防守";
        if (theme == 8) return "子力整理";
        return "节奏转换";
    }

    private String pieceTeachName(char c) {
        char p = Character.toLowerCase(c);
        if (p == 'r') return "车";
        if (p == 'n') return "马";
        if (p == 'c') return "炮";
        if (p == 'p') return "兵卒";
        if (p == 'b') return "象";
        if (p == 'a') return "士";
        if (p == 'k') return "将帅";
        return "棋子";
    }

    private String pieceInstanceName(char c, int x, int y) {
        char p = Character.toLowerCase(c);
        String base = pieceTeachName(c);
        boolean red = XiangqiUtils.isRed(c);

        if (p == 'n' || p == 'r' || p == 'c') {
            String side = isLeftFromSelf(red, x) ? "左路" : "右路";
            return side + base;
        }
        if (p == 'p') {
            String line = fileNameForSide(red, x);
            return line + (red ? "兵" : "卒");
        }
        if (p == 'a') {
            return isLeftFromSelf(red, x) ? "左士" : "右士";
        }
        if (p == 'b') {
            return isLeftFromSelf(red, x) ? "左象" : "右象";
        }
        if (p == 'k') {
            return red ? "红方将位" : "黑方将位";
        }
        return base + "(" + (char) ('a' + x) + (9 - y) + ")";
    }

    private boolean isLeftFromSelf(boolean red, int x) {
        return red ? x <= 4 : x >= 4;
    }

    private String fileNameForSide(boolean red, int x) {
        String[] nums = {"九", "八", "七", "六", "五", "四", "三", "二", "一"};
        int idx = red ? x : 8 - x;
        if (idx < 0 || idx >= nums.length) {
            return "边线";
        }
        return nums[idx] + "路";
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

    public void setMoveVoice(boolean moveVoice) {
        this.moveVoice = moveVoice;
    }

    public void setColloquialReviewStyle(boolean colloquialReviewStyle) {
        this.colloquialReviewStyle = colloquialReviewStyle;
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

    public String getLastMoveAnnotation() {
        return lastMoveAnnotation;
    }

    public LinkedHashMap<String, String> getLastMoveOpponentPlans() {
        return new LinkedHashMap<>(lastMoveOpponentPlans);
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
