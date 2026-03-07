package com.sojourners.chess.linker;

import com.sojourners.chess.board.ChessBoard;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.util.XiangqiUtils;
import com.sojourners.chess.yolo.OnnxModel;
import com.sojourners.chess.yolo.Yolo11Model;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * 连线核心抽象基类。
 *
 * <p>职责：
 * 1) 识别目标窗口中的棋盘位置与棋子布局；
 * 2) 对比“外部平台棋盘”与“本地引擎棋盘”，推断下一步动作；
 * 3) 在自动走棋模式下，向目标窗口发送点击事件；
 * 4) 对接不同系统的平台实现（Windows/Linux/Mac）。
 */
public abstract class AbstractGraphLinker implements GraphLinker, Runnable {

    /** 扫描线程（虚拟线程）。 */
    private Thread thread;
    /** 当前识别到的棋盘区域（相对目标窗口）。 */
    private Rectangle boardPos;
    /** 识别棋盘暂存缓冲区。 */
    private char[][] board2 = new char[10][9];

    /** 动画确认时的上一帧缓冲区。 */
    private char[][] board1 = new char[10][9];

    /** ONNX 棋盘识别模型。 */
    private OnnxModel aiModel;

    /** 与控制层交互的回调。 */
    private LinkerCallBack callBack;

    /** 前台截图与鼠标模拟。 */
    private Robot robot;

    /** 连续疑似“新棋局”计数，达到阈值后触发重初始化。 */
    private int count;

    /** 暂停扫描标记（局面切换时短暂停顿）。 */
    private volatile boolean pause;

    /**
     * 自动点击后的等待锁：
     * 防止同一手未在目标平台落稳前被重复识别并重复点击，导致界面闪烁。
     */
    private volatile boolean pendingAutoMove;
    private volatile long pendingAutoMoveUntil;

    private Properties prop;

    public AbstractGraphLinker(LinkerCallBack callBack) throws AWTException {
        this.callBack = callBack;
        robot = new Robot();
        this.count = 0;
        this.aiModel = new Yolo11Model();
        this.prop = Properties.getInstance();
        this.pause = false;
        this.pendingAutoMove = false;
        this.pendingAutoMoveUntil = 0L;
    }

    /**
     * 开始连线流程。
     *
     * <p>具体“如何选择目标窗口”由子类实现。
     */
    @Override
    public void start() {
        getTargetWindowId();
    }

    void scan() {
        // 统一由后台线程执行识别与同步循环。
        this.thread = Thread.ofVirtual().unstarted(this);
        this.thread.start();
    }

    private boolean isSame(char[][] board1, char[][] board2) {
        if (board1 == null || board2 == null) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                if (board1[i][j] != board2[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    public void pause() {
        this.pause = true;
    }
    public void resume() {
        this.pause = false;
    }

    @Override
    public void run() {
        // 外层循环：负责“找到窗口与初始化棋盘”。
        while (!Thread.currentThread().isInterrupted()) {
            if (!findBoardPosition()) {
                sleep(1000);
                continue;
            }
            if (!initChessBoard()) {
                sleep(1000);
                continue;
            }
            // 内层循环：稳定扫描局面变化并执行同步动作。
            while (!Thread.currentThread().isInterrupted()) {
                sleep(prop.getLinkScanTime());
                if (!callBack.isThinking() && !pause) {

                    if (!findChessBoard(board2)) {
                        continue;
                    }

                    boolean isReverse;
                    try {
                        isReverse = reverse(board2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }

                    // 自动点击后先等待目标平台棋盘收敛，避免重复点击同一步。
                    if (pendingAutoMove) {
                        if (isSame(board2, callBack.getEngineBoard())) {
                            pendingAutoMove = false;
                            pendingAutoMoveUntil = 0L;
                            continue;
                        }
                        if (System.currentTimeMillis() < pendingAutoMoveUntil) {
                            continue;
                        }
                        pendingAutoMove = false;
                        pendingAutoMoveUntil = 0L;
                    }

                    if (isSame(board2, callBack.getEngineBoard())) {
                        continue;
                    }

                    // 对比连线棋盘与引擎棋盘，推断应执行的动作。
                    Action action = compareBoard(board2, callBack.getEngineBoard(), isReverse, callBack.isWatchMode());
                    if (prop.isLinkAnimation() && needConfirm(board2, callBack.getEngineBoard(), action)) {
                        // 动画确认：等待两帧收敛，降低过渡动画导致的误判。
                        boolean f = false;
                        do {
                            char[][] tmp = board1;
                            board1 = board2;
                            board2 = tmp;

                            if (!findChessBoard(board2)) {
                                f = true;
                                break;
                            }

                            try {
                                isReverse = reverse(board2);
                            } catch (Exception e) {
                                e.printStackTrace();
                                f = true;
                                break;
                            }
                        } while (!isSame(board1, board2));

                        if (f) continue;

                        action = compareBoard(board2, callBack.getEngineBoard(), isReverse, callBack.isWatchMode());
                    }

                    if (action != null) {
                        System.out.println("action " + action);
                        if (action.flag == 1) {
                            // 对方已走棋：同步到本地引擎棋盘。
                            callBack.linkerMove(action.x1, action.y1, action.x2, action.y2);

                        } else if (action.flag == 2) {
                            // 引擎已走棋：自动走棋模式下由 Controller.bestMove 触发点击，
                            // 扫描线程不再重复点击，避免“只选中不落子”的重复干扰。

                        } else if (action.flag == 3) {
                            // 明确识别到新棋局：退出内循环并重新初始化。
                            break;
                        }
                        if (action.flag == 4) {
                            // 可能是新棋局：连续出现多次再触发重置，避免瞬时误判。
                            count++;
                            if (count > 9) {
                                break;
                            }
                        } else {
                            count = 0;
                        }
                    }

                }
            }
        }
    }

    /**
     * 棋盘对比后的动作描述。
     * flag:
     * 1 = 对方走子（需同步到引擎）
     * 2 = 引擎走子（需同步到平台）
     * 3 = 识别到新棋局（重初始化）
     * 4 = 疑似新棋局（继续观察）
     */
    class Action {
        int flag;
        int x1;
        int y1;
        int x2;
        int y2;
        public Action(int flag) {
            this.flag = flag;
        }
        public Action(int flag, int x1, int y1, int x2, int y2) {
            this.flag = flag;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public String toString() {
            return "Action{" +
                    "flag=" + flag +
                    ", x1=" + x1 +
                    ", y1=" + y1 +
                    ", x2=" + x2 +
                    ", y2=" + y2 +
                    '}';
        }
    }

    private boolean needConfirm(char[][] linkBoard, char[][] engineBoard, Action action) {
        // 对“吃车/吃炮”等容易受动画影响的场景做额外确认。
        if (action == null) {
            return false;
        }
        if (action.flag == 3) {
            return true;
        }
        if (action.flag != 1 || !(linkBoard[action.y2][action.x2] == 'r' || linkBoard[action.y2][action.x2] == 'R' || linkBoard[action.y2][action.x2] == 'c' || linkBoard[action.y2][action.x2] == 'C') || !(engineBoard[action.y2][action.x2] == ' ')) {
            return false;
        }
        if (linkBoard[action.y2][action.x2] == 'r' || linkBoard[action.y2][action.x2] == 'R') {
            int x = -1, y = -1;
            if (action.x1 == action.x2) {
                x = action.x1;
                if (action.y2 > action.y1) {
                    y = action.y2 + 1;
                } else {
                    y = action.y2 - 1;
                }
            }
            if (action.y1 == action.y2) {
                y = action.y1;
                if (action.x2 > action.x1) {
                    x = action.x2 + 1;
                } else {
                    x = action.x2 - 1;
                }
            }
            if (x < 0 || x > 8 || y < 0 || y > 9 || engineBoard[y][x] != ' ' && XiangqiUtils.isRed(engineBoard[action.y1][action.x1]) == XiangqiUtils.isRed(engineBoard[y][x])) {
                return false;
            }
        }
        if (linkBoard[action.y2][action.x2] == 'c' || linkBoard[action.y2][action.x2] == 'C') {
            if (action.x1 == action.x2) {
                int x = action.x1, y;
                int p;
                if (action.y2 > action.y1) {
                    y = action.y2 + 1;
                    p = 1;
                } else {
                    y = action.y2 - 1;
                    p = -1;
                }
                if (y < 0 || y > 9) {
                    return false;
                }
                if (engineBoard[y][x] != ' ') {
                    for (int i = y + p; i >= 0 && i <= 9; i += p) {
                        if (engineBoard[i][x] != ' ' && XiangqiUtils.isRed(engineBoard[i][x]) == XiangqiUtils.isRed(engineBoard[action.y1][action.x1])) {
                            return false;
                        } else if (engineBoard[i][x] != ' ' && XiangqiUtils.isRed(engineBoard[i][x]) != XiangqiUtils.isRed(engineBoard[action.y1][action.x1])) {
                            return true;
                        }
                    }
                    return false;
                }
            }
            if (action.y1 == action.y2) {
                int x, y = action.y1;
                int p;
                if (action.x2 > action.x1) {
                    x = action.x2 + 1;
                    p = 1;
                } else {
                    x = action.x2 - 1;
                    p = -1;
                }
                if (x < 0 || x > 8 || y < 0 || y > 9) {
                    return false;
                }
                if (engineBoard[y][x] != ' ') {
                    for (int j = x + p; j >= 0 && j <= 8; j += p) {
                        if (engineBoard[y][j] != ' ' && XiangqiUtils.isRed(engineBoard[y][j]) == XiangqiUtils.isRed(engineBoard[action.y1][action.x1])) {
                            return false;
                        } else if (engineBoard[y][j] != ' ' && XiangqiUtils.isRed(engineBoard[y][j]) != XiangqiUtils.isRed(engineBoard[action.y1][action.x1])) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 对比棋盘并推断动作。
     *
     * <p>核心思想：统计差异点并寻找“唯一合法走法”映射。
     * 如果差异过多，则判定为新棋局或疑似新棋局。
     */
    private Action compareBoard(char[][] linkBoard, char[][] engineBoard, boolean robotBlack, boolean analysisMode) {
        int diff1 = 0, diff2 = 0, diff3 = 0;

        List<Point> diffList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                if (linkBoard[i][j] != engineBoard[i][j]) {
                    diffList.add(new Point(i, j));
                    if (linkBoard[i][j] != ' ' && engineBoard[i][j] != ' ') {
                        diff1++;
                    } else if (linkBoard[i][j] != ' ' && engineBoard[i][j] == ' ') {
                        diff2++;
                    } else {
                        diff3++;
                    }
                }
            }
        }

        if (diff1 > 2 || diff2 >= 2 && diff3 > 2) {
            return new Action(3);
        }

        Action action = null;
        int flag = 0, sum = 0;
        Point from = null, to = null;
        for (int i = 0; i < diffList.size(); i++) {
            for (int j = i + 1; j < diffList.size(); j++) {
                Point p1 = diffList.get(i), p2 = diffList.get(j);
                boolean f = false;
                if (linkBoard[p1.x][p1.y] == engineBoard[p2.x][p2.y] && linkBoard[p1.x][p1.y] != ' ') {
                    if (linkBoard[p2.x][p2.y] == ' ' && engineBoard[p1.x][p1.y] == ' ') {
                        if (analysisMode || robotBlack && XiangqiUtils.isRed(linkBoard[p1.x][p1.y]) || !robotBlack && !XiangqiUtils.isRed(linkBoard[p1.x][p1.y])) {
                            flag = 1;
                            from = p2;
                            to = p1;
                            f = true;
                        } else if (robotBlack && !XiangqiUtils.isRed(linkBoard[p1.x][p1.y]) || !robotBlack && XiangqiUtils.isRed(linkBoard[p1.x][p1.y])) {
                            flag = 2;
                            from = p1;
                            to = p2;
                            f = true;
                        }
                    }
                    if (linkBoard[p2.x][p2.y] == ' ' && engineBoard[p1.x][p1.y] != ' ' && XiangqiUtils.isRed(linkBoard[p1.x][p1.y]) != XiangqiUtils.isRed(engineBoard[p1.x][p1.y])) {
                        flag = 1;
                        from = p2;
                        to = p1;
                        f = true;
                    }
                    if (!analysisMode && engineBoard[p1.x][p1.y] == ' ' && linkBoard[p2.x][p2.y] != ' ' && XiangqiUtils.isRed(engineBoard[p2.x][p2.y]) != XiangqiUtils.isRed(linkBoard[p2.x][p2.y])) {
                        flag = 2;
                        from = p1;
                        to = p2;
                        f = true;
                    }
                }
                if (linkBoard[p2.x][p2.y] == engineBoard[p1.x][p1.y] && linkBoard[p2.x][p2.y] != ' ') {
                    if (linkBoard[p1.x][p1.y] == ' ' && engineBoard[p2.x][p2.y] == ' ') {
                        if (analysisMode || robotBlack && XiangqiUtils.isRed(linkBoard[p2.x][p2.y]) || !robotBlack && !XiangqiUtils.isRed(linkBoard[p2.x][p2.y])) {
                            flag = 1;
                            from = p1;
                            to = p2;
                            f = true;
                        } else if (robotBlack && !XiangqiUtils.isRed(linkBoard[p2.x][p2.y]) || !robotBlack && XiangqiUtils.isRed(linkBoard[p2.x][p2.y])) {
                            flag = 2;
                            from = p2;
                            to = p1;
                            f = true;
                        }
                    }
                    if (linkBoard[p1.x][p1.y] == ' ' && engineBoard[p2.x][p2.y] != ' ' && XiangqiUtils.isRed(linkBoard[p2.x][p2.y]) != XiangqiUtils.isRed(engineBoard[p2.x][p2.y])) {
                        flag = 1;
                        from = p1;
                        to = p2;
                        f = true;
                    }
                    if (!analysisMode && engineBoard[p2.x][p2.y] == ' ' && linkBoard[p1.x][p1.y] != ' ' && XiangqiUtils.isRed(engineBoard[p1.x][p1.y]) != XiangqiUtils.isRed(linkBoard[p1.x][p1.y])) {
                        flag = 2;
                        from = p2;
                        to = p1;
                        f = true;
                    }
                }
                if (f && (flag == 1 && XiangqiUtils.canGo(engineBoard, from.x, from.y, to.x, to.y) || flag == 2 && XiangqiUtils.canGo(linkBoard, from.x, from.y, to.x, to.y))) {
                    sum++;
                    action = new Action(flag, from.y, from.x, to.y, to.x);
                }
            }
        }

        if (sum == 1) {
            return action;
        }

//        if (diff1 + diff2 + diff3 == 1) {
//            return new Action(3);
//        }

        if (diff1 + diff2 + diff3 > 2) {
            return new Action(4);
        }

        return null;
    }

    void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    /** 前台截图（目标窗口可见区域）。 */
    public BufferedImage screenshotByFront(Rectangle windowPos) {
        if (windowPos.width == 0 || windowPos.height == 0) {
            return null;
        }
        return robot.createScreenCapture(windowPos);
    }

    /** 前台点击（按起点->终点执行一次走子）。 */
    @Override
    public void mouseClickByFront(Rectangle windowPos, Point p1, Point p2) {

        Point mouse = MouseInfo.getPointerInfo().getLocation();
        int clickDelay = Math.max(prop.getMouseClickDelay(), 30);
        int moveDelay = Math.max(prop.getMouseMoveDelay(), 120);

        robot.mouseMove(windowPos.x + p1.x, windowPos.y+ p1.y);

        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(clickDelay);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        robot.delay(moveDelay);
        robot.mouseMove(windowPos.x + p2.x, windowPos.y + p2.y);

        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(clickDelay);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        robot.mouseMove((int) mouse.getX(), (int) mouse.getY());

    }

    /** 在窗口截图中定位棋盘矩形。 */
    boolean findBoardPosition() {
        BufferedImage img = screenshot(true);
        this.boardPos = this.aiModel.findBoardPosition(img);
        return this.boardPos != null;
    }

    /**
     * 统一截图入口。
     * fullScreen=true 时用于找棋盘区域；false 时截取棋盘区域用于识别棋子。
     */
    BufferedImage screenshot(boolean fullScreen) {
        if (prop.isLinkBackMode()) {
            BufferedImage img = screenshotByBack(fullScreen ? null : boardPos);
            return img;

        } else {
            Rectangle pos = getTargetWindowPosition();
            if (!fullScreen) {
                pos.setLocation(pos.x + boardPos.x, pos.y + boardPos.y);
                pos.setSize(boardPos.width, boardPos.height);
            }
            BufferedImage img = screenshotByFront(pos);
            return img;
        }
    }


    private boolean findChessBoard(char[][] board) {
        // 截图
        BufferedImage img = screenshot(false);
        // ai识别棋盘棋子
        if (!this.aiModel.findChessBoard(img, board)) {
            return false;
        }
        boolean f = XiangqiUtils.validateChessBoard(board);
        if (!f) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 9; j++) {
                    System.out.print(board[i][j]);
                }
                System.out.println();
            }
        }
        return f;
    }
    private boolean reverse(char[][] board) throws Exception {
        // 基于将帅位置判断是否上下颠倒，并在必要时翻转为统一视角。
        // 是否翻转
        int rowRedKing = -1, rowBlackKing = -1;
        for (int i = 0; i < 10; i++) {
            for (int j = 3; j < 6; j++) {
                if (board[i][j] == 'k') {
                    rowBlackKing = i;
                } else if (board[i][j] == 'K') {
                    rowRedKing = i;
                }
            }
        }
        if (rowBlackKing == -1 && rowRedKing == -1) {
            throw new Exception("find king failed.");
        }
        boolean isReverse = rowRedKing >= 0 && rowRedKing <= 2 || rowBlackKing >= 7 && rowBlackKing <= 9;
        if (isReverse) {
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 9; j++) {
                    char tmp = board[i][j];
                    board[i][j] = board[9 - i][8 - j];
                    board[9 - i][8 - j] = tmp;
                }
            }
        }
        return isReverse;
    }

    /** 初始化连线棋盘，并回调控制层创建对应局面。 */
    private boolean initChessBoard() {
        pendingAutoMove = false;
        pendingAutoMoveUntil = 0L;
        if (!findChessBoard(board2)) {
            return false;
        }

        boolean isReverse = false;
        try {
            isReverse = reverse(board2);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        // 是否红走
        String fenCode = ChessBoard.fenCode(board2, null);
        boolean redGo = !isReverse || "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR".equals(fenCode);
        fenCode = ChessBoard.fenCode(board2, redGo);
        // 回调，初始化棋盘
        callBack.linkerInitChessBoard(fenCode, isReverse);
        return true;
    }

    /** 自动点击走棋（按配置走前台/后台点击路径）。 */
    public void autoClick(int x1, int y1, int x2, int y2) {

        Point p1 = getPosition(x1, y1);
        Point p2 = getPosition(x2, y2);
        if (prop.isLinkBackMode()) {
            mouseClickByBack(p1, p2);
        } else {
            Rectangle windowPos = getTargetWindowPosition();
            mouseClickByFront(windowPos, p1, p2);
        }
    }

    private void markPendingAutoMove() {
        pendingAutoMove = true;
        // 给目标平台动画/网络延迟留缓冲，超时后允许重试。
        long wait = Math.max(1500L, prop.getLinkScanTime() * 8L);
        pendingAutoMoveUntil = System.currentTimeMillis() + wait;
    }
    private Point getPosition(int x, int y) {
        double pieceWith = boardPos.width / (8 + OnnxModel.PADDING * 2);
        double pieceHeight = boardPos.height / (9 + OnnxModel.PADDING * 2);
        Point p = new Point((int) (boardPos.x + pieceWith * OnnxModel.PADDING + (x * pieceWith)),
                (int) (boardPos.y + pieceHeight * OnnxModel.PADDING + (y * pieceHeight)));
        if (x == 0) {
            p.x += 0.2 * pieceWith;
        } else if (x == 8) {
            p.x -= 0.2 * pieceWith;
        }
        if (y == 0) {
            p.y += 0.2 * pieceHeight;
        } else if (y == 9) {
            p.y -= 0.2 * pieceHeight;
        }
        return p;
    }

    /** 停止连线扫描线程。 */
    @Override
    public void stop() {
        pendingAutoMove = false;
        pendingAutoMoveUntil = 0L;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    // find chess board from image
    public char[][] findChessBoard(BufferedImage img) {
        char[][] tmp = new char[10][9];
        if (this.aiModel.findChessBoard(img, tmp)) {
            return tmp;
        } else {
            return null;
        }
    }
}
