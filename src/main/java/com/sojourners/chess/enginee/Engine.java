package com.sojourners.chess.enginee;


import com.sojourners.chess.config.Properties;
import com.sojourners.chess.model.BookData;
import com.sojourners.chess.model.EngineConfig;
import com.sojourners.chess.model.ThinkData;
import com.sojourners.chess.openbook.OpenBookManager;
import com.sojourners.chess.util.PathUtils;
import com.sojourners.chess.util.StringUtils;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 引擎进程封装。
 *
 * <p>职责边界：
 * 1) 启动/关闭 UCI(UCCI) 引擎进程；
 * 2) 发送 position/go/stop 等协议指令；
 * 3) 解析引擎输出（info/bestmove）并回调到 UI 控制层；
 * 4) 在真正发起搜索前，按配置优先查询开局库。
 *
 * <p>注意：本类并不直接修改 UI，所有 UI 更新交给回调实现方处理。
 */
public class Engine {

    /**
     * 外部引擎进程句柄。
     */
    private Process process;

    /**
     * 协议类型："uci" 或 "ucci"。
     */
    private String protocol;

    /** 当前搜索模式（固定时间/固定深度/无限分析）。 */
    private AnalysisModel analysisModel;
    /** 搜索参数值：时间(毫秒)或深度。 */
    private long analysisValue;

    /** 标记 Threads 配置是否需要在下一次搜索前下发。 */
    private volatile boolean threadNumChange;
    private int threadNum;

    /** 标记 Hash 配置是否需要在下一次搜索前下发。 */
    private volatile boolean hashSizeChange;
    private int hashSize;

    /**
     * 停止标志位。
     *
     * <p>在收到 bestmove 时，如果该标志为 true，说明这是上一轮搜索
     * 的延迟返回结果，需要忽略并复位标志。
     */
    private volatile boolean stopFlag;
    private volatile long time;

    /** 读取引擎标准输出。 */
    private BufferedReader reader;

    /** 写入引擎标准输入。 */
    private BufferedWriter writer;

    /** 向上层汇报 bestmove / info 的回调。 */
    private EngineCallBack cb;

    /** 持续消费引擎输出的后台线程（虚拟线程）。 */
    private Thread thread;

    /** 用于随机延迟出招（模拟人类节奏）。 */
    private Random random;

    /** 当前引擎 MultiPV 值，决定 UI 是否展示多条主变。 */
    private int multiPV;

    public enum AnalysisModel {
        FIXED_TIME,
        FIXED_STEPS,
        INFINITE;
    }

    public Engine(EngineConfig ec, EngineCallBack cb) throws IOException {
        this.protocol = ec.getProtocol();
        this.cb = cb;
        this.random = new SecureRandom();

        this.time = Integer.MAX_VALUE;

        if (ec.getOptions().get("MultiPV") != null) {
            multiPV = Integer.parseInt(ec.getOptions().get("MultiPV"));
        } else {
            multiPV = 1;
        }

        // 在引擎可执行文件所在目录启动，避免相对路径资源加载失败。
        process = Runtime.getRuntime().exec(ec.getPath(), null, PathUtils.getParentDir(ec.getPath()));
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        // 单独线程持续读取输出：
        // - info 行 -> thinkDetail
        // - bestmove 行 -> bestMove
        thread = Thread.startVirtualThread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    if (line.contains("depth") || line.contains("nps")) {
                        thinkDetail(line);
                    } else if (line.contains("bestmove")) {
                        bestMove(line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 初始化协议握手。
        cmd(protocol);

        // 下发引擎自定义选项。
        for (Map.Entry<String, String> entry : ec.getOptions().entrySet()) {
            if ("uci".equals(this.protocol)) {
                cmd("setoption name " + entry.getKey() + " value " + entry.getValue());
            } else if ("ucci".equals(this.protocol)) {
                cmd("setoption " + entry.getKey() + " " + entry.getValue());
            }
        }
    }

    public int getMultiPV() {
        return multiPV;
    }

    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String test(String filePath, LinkedHashMap<String, String> options) {
        Process p = null;
        Thread h = null;
        BufferedWriter bw = null;
        BufferedReader br = null;
        try {
            p = Runtime.getRuntime().exec(filePath);
            bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            AtomicBoolean f = new AtomicBoolean(false);
            BufferedReader finalBr = br;
            (h = Thread.ofVirtual().unstarted(() -> {
                try {
                    String line;
                    while ((line = finalBr.readLine()) != null) {
                        if ("uciok".equals(line) || "ucciok".equals(line) ) {
                            f.set(true);
                        }
                        if (line.startsWith("option") && line.contains("name") && line.contains("type") && line.contains("default")
                                && !line.contains("Threads") && !line.contains("Hash")) {

                            String[] str = line.split("name|type|default");
                            String key = str[1].trim();
                            String value = str[3].trim().split(" ")[0];
                            options.put(key, value);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            })).start();

            // 先尝试 UCI，再尝试 UCCI。
            bw.write("uci" + System.getProperty("line.separator"));
            bw.flush();
            Thread.sleep(1000);
            if (f.get()) {
                return "uci";
            }

            bw.write("ucci" + System.getProperty("line.separator"));
            bw.flush();
            Thread.sleep(1000);
            if (f.get()) {
                return "ucci";
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (p != null) {
                p.destroy();
            }
            if (h.isAlive()) {
                h.interrupt();
            }
            try {
                if (bw != null) {
                    bw.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean validateMove(String move) {
        // 引擎步法格式校验：如 a0a1
        if (StringUtils.isEmpty(move) || move.length() != 4) {
            return false;
        }
        if (move.charAt(0) < 'a' || move.charAt(0) > 'i' || move.charAt(2) < 'a' || move.charAt(2) > 'i') {
            return false;
        }
        if (move.charAt(1) < '0' || move.charAt(1) > '9' || move.charAt(3) < '0' || move.charAt(3) > '9') {
            return false;
        }
        return true;
    }
    private void bestMove(String msg) {
        // stop 后迟到的 bestmove 直接丢弃。
        if (stopFlag) {
            stopFlag = false;
            return;
        }

        String[] str = msg.split(" ");
        if (str.length < 2 || !validateMove(str[1])) {
            return;
        }
        // 出招延迟：用于降低“秒出”观感，范围内随机。
        if (Properties.getInstance().getEngineDelayEnd() > 0 && Properties.getInstance().getEngineDelayEnd() >= Properties.getInstance().getEngineDelayStart()) {
            int t = random.nextInt(Properties.getInstance().getEngineDelayStart(), Properties.getInstance().getEngineDelayEnd());
            sleep(t);
        }
        cb.bestMove(str[1], str.length == 4 ? str[3] : null);
    }
    private void thinkDetail(String msg) {
        // 解析 info 行（depth/score/nps/time/pv/multipv 等字段）。
        String[] str = msg.split(" ");
        ThinkData td = new ThinkData();
        List<String> detail = new ArrayList<>();
        td.setDetail(detail);
        int flag = 0;
        for (int i = 0; i < str.length; i++) {
            if (flag != 0) {
                if (flag == 6) {
                    detail.add(str[i]);
                } else {
                    if (StringUtils.isDigit(str[i])) {
                        if (flag == 1) {
                            td.setNps(Long.parseLong(str[i]));

                        } else if (flag == 2) {
                            td.setTime(Long.parseLong(str[i]));

                        } else if (flag == 3) {
                            td.setDepth(Integer.parseInt(str[i]));

                        } else if (flag == 4) {
                            td.setMate(Integer.parseInt(str[i]));

                        } else if (flag == 5) {
                            td.setScore(Integer.parseInt(str[i]));

                        } else if (flag == 7) {
                            td.setPv(Integer.parseInt(str[i]));
                        }
                        flag = 0;
                    } else {
                        continue;
                    }
                }
            } else {
                if ("depth".equals(str[i])) {
                    flag = 3;
                } else if ("score".equals(str[i])) {
                    if ("mate".equals(str[i + 1])) {
                        flag = 4;
                    } else {
                        flag = 5;
                    }
                } else if ("mate".equals(str[i])) {
                    flag = 4;
                } else if ("nps".equals(str[i])) {
                    flag = 1;
                } else if ("time".equals(str[i])) {
                    flag = 2;
                } else if ("pv".equals(str[i])) {
                    flag = 6;
                } else if ("multipv".equals(str[i])) {
                    flag = 7;
                }
            }
        }

        // 低深度或极短时间阶段，允许 stopFlag 被清理，避免误伤后续正常 bestmove。
        if (td.getDepth() != null && td.getDepth() < 5) {
            stopFlag = false;
        }
        if (td.getTime() != null) {
            if (td.getTime() < this.time || td.getTime() > 0 && td.getTime() < 70) {
                stopFlag = false;
            }
            this.time = td.getTime();
        }

        if (td.getDetail().size() > 0) {
            cb.thinkDetail(td);
        }
    }

    public void analysis(String fenCode, List<String> moves, char[][] board, boolean redGo) {
        Thread.startVirtualThread(() -> {
            // 开启库招时，优先查询开局库。
            if (Properties.getInstance().getBookSwitch()) {
                long s = System.currentTimeMillis();
                List<BookData> results = OpenBookManager.getInstance().queryBook(board, redGo, moves.size() / 2 >= Properties.getInstance().getOffManualSteps());
                System.out.println("查询库时间" + (System.currentTimeMillis() - s));
                this.cb.showBookResults(results);
                // 有库招且不是无限分析：直接返回库招，不再启动引擎搜索。
                if (results.size() > 0 && this.analysisModel != AnalysisModel.INFINITE) {
                    if (Properties.getInstance().getBookDelayEnd() > 0 && Properties.getInstance().getBookDelayEnd() >= Properties.getInstance().getBookDelayStart()) {
                        int t = random.nextInt(Properties.getInstance().getBookDelayStart(), Properties.getInstance().getBookDelayEnd());
                        sleep(t);
                    }
                    this.cb.bestMove(results.get(0).getMove(), null);
                    return;
                }

            }
            this.analysis(fenCode, moves, null);
        });
    }

    public void analysis(String fenCode, List<String> moves, List<String> tacticList) {
        // 新搜索前先 stop，清理旧搜索状态。
        stop();

        if (threadNumChange) {
            cmd(("uci".equals(this.protocol) ? "setoption name Threads value " : "setoption Threads ") + threadNum);
            this.threadNumChange = false;
        }
        if (hashSizeChange) {
            cmd(("uci".equals(this.protocol) ? "setoption name Hash value " : "setoption Hash ") + hashSize);
            this.hashSizeChange = false;
        }

        // 下发当前局面 + 历史走子。
        StringBuilder sb = new StringBuilder();
        sb.append("position fen ").append(fenCode);
        if (moves != null && moves.size() > 0) {
            sb.append(" moves");
            for (String move : moves) {
                sb.append(" ").append(move);
            }
        }
        cmd(sb.toString());

        // 变招模式：限制 searchmoves。
        boolean hasTactics = tacticList != null && !tacticList.isEmpty();
        if (hasTactics) {
            sb = new StringBuilder();
            sb.append(" searchmoves");
            for (String tactic : tacticList) {
                sb.append(" ").append(tactic);
            }
        }
        if (analysisModel == AnalysisModel.FIXED_STEPS) {
            cmd("go depth " + analysisValue + (hasTactics ? sb.toString() : ""));
        } else if (analysisModel == AnalysisModel.FIXED_TIME) {
            cmd("go movetime " + analysisValue + (hasTactics ? sb.toString() : ""));
        } else {
            cmd("go infinite" + (hasTactics ? sb.toString() : ""));
        }
    }

    public void moveNow() {
        cmd("stop");
    }

    public void stop() {
        // 协议层 stop + 逻辑层 stopFlag 双保险。
        stopFlag = true;
        cmd("stop");
    }

    private void cmd(String command) {
        System.out.println(command);
        try {
            writer.write(command + System.getProperty("line.separator"));
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setThreadNum(int threadNum) {
        if (threadNum != this.threadNum) {
            this.threadNum = threadNum;
            this.threadNumChange = true;
        }

    }

    public void setHashSize(int hashSize) {
        if (hashSize != this.hashSize) {
            this.hashSize = hashSize;
            this.hashSizeChange = true;
        }
    }

    public void setAnalysisModel(AnalysisModel model, long v) {
        this.analysisModel = model;
        this.analysisValue = v;
    }

    public void close() {
        try {
            // 正常退出顺序：quit -> 中断读线程 -> destroy -> 关闭 IO。
            if (process.isAlive()) {
                cmd("quit");
            }

            if (thread.isAlive()) {
                thread.interrupt();
            }

            if (process.isAlive()) {
                process.destroy();
            }

            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
