package com.chf.chess.util;

import com.chf.chess.config.Properties;
import com.chf.chess.enginee.Engine;
import com.chf.chess.model.EngineConfig;
import com.sun.jna.Platform;

import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * BuiltinEngineLoader 类。
 * 自动准备并加载内置引擎。
 */
public class BuiltinEngineLoader {

    private static final String BUILTIN_NAME = "Pikafish(内置)";
    private static final String BUILTIN_PREFIX = "内置-";

    private static final List<String> WINDOWS_CANDIDATES = Arrays.asList(
            "pikafish.exe",
            "pikafish-avx2.exe",
            "pikafish-avx512.exe",
            "pikafish-windows.exe",
            "pikafish-win.exe"
    );

    private static final List<String> LINUX_CANDIDATES = Arrays.asList(
            "pikafish",
            "pikafish-avx2",
            "pikafish-avx512",
            "pikafish-linux"
    );

    private static final List<String> MACOS_CANDIDATES = Arrays.asList(
            "pikafish-macos",
            "pikafish-mac",
            "pikafish-avx2",
            "pikafish"
    );

    private static final List<String> NETWORK_CANDIDATES = Arrays.asList(
            "pikafish.nnue",
            "default.nnue",
            "nnue.nnue"
    );

    public static synchronized void autoLoad(Properties prop) {
        if (prop == null) {
            return;
        }
        try {
            File enginesDir = new File(PathUtils.getJarPath(), "engines");
            if (!enginesDir.exists()) {
                enginesDir.mkdirs();
            }

            List<String> candidates = osCandidates();
            for (String fileName : candidates) {
                copyBundledEngineIfExists(fileName, new File(enginesDir, fileName));
            }
            for (String fileName : NETWORK_CANDIDATES) {
                copyBundledEngineIfExists(fileName, new File(enginesDir, fileName));
            }

            List<DetectedEngine> detected = detectRunnableEngines(enginesDir);
            if (detected.isEmpty()) {
                return;
            }

            syncBuiltinEngines(prop, detected);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> osCandidates() {
        if (Platform.isWindows()) {
            return WINDOWS_CANDIDATES;
        }
        if (Platform.isLinux()) {
            return LINUX_CANDIDATES;
        }
        return MACOS_CANDIDATES;
    }

    private static void copyBundledEngineIfExists(String fileName, File outFile) {
        String res = "engines/" + fileName;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = BuiltinEngineLoader.class.getClassLoader().getResourceAsStream(res);
            if (in == null) {
                return;
            }
            if (outFile.exists() && outFile.length() > 0) {
                return;
            }

            out = new FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            if (!Platform.isWindows()) {
                outFile.setExecutable(true, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static List<DetectedEngine> detectRunnableEngines(File enginesDir) {
        List<DetectedEngine> detected = new ArrayList<>();
        File[] files = enginesDir.listFiles();
        if (files == null) {
            return detected;
        }

        for (File f : files) {
            if (!f.isFile()) {
                continue;
            }
            String name = f.getName().toLowerCase(Locale.ROOT);
            if (Platform.isWindows()) {
                if (!name.endsWith(".exe")) {
                    continue;
                }
            } else if (!f.canExecute()) {
                continue;
            }

            String protocol = detectProtocol(f);
            if (protocol == null) {
                continue;
            }
            detected.add(new DetectedEngine(f, protocol, engineScore(f)));
        }

        Collections.sort(detected, Comparator.comparingInt(DetectedEngine::score).reversed());
        return detected;
    }

    private static int engineScore(File f) {
        String name = f.getName().toLowerCase(Locale.ROOT);
        int score = 0;
        if (name.startsWith("pikafish")) {
            score += 100;
        }
        if (name.contains("avx512")) {
            score += 40;
        } else if (name.contains("avx2")) {
            score += 30;
        } else if (name.contains("bmi2")) {
            score += 25;
        } else if (name.equals("pikafish") || name.equals("pikafish.exe")) {
            score += 20;
        }
        return score;
    }

    private static String detectProtocol(File file) {
        String lowerName = file.getName().toLowerCase(Locale.ROOT);
        if (lowerName.startsWith("pikafish")) {
            // Pikafish 系列固定走 UCI，避免误判成 UCCI 导致握手失败。
            return "uci";
        }
        try {
            String p = Engine.test(file.getAbsolutePath(), new LinkedHashMap<>());
            if ("uci".equalsIgnoreCase(p) || "ucci".equalsIgnoreCase(p)) {
                return p.toLowerCase(Locale.ROOT);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static void syncBuiltinEngines(Properties prop, List<DetectedEngine> detected) {
        prop.getEngineConfigList().removeIf(ec -> ec != null && ec.getName() != null
                && (ec.getName().startsWith(BUILTIN_PREFIX) || BUILTIN_NAME.equals(ec.getName())));

        for (int i = 0; i < detected.size(); i++) {
            DetectedEngine de = detected.get(i);
            String displayName = buildDisplayName(de, i == 0);
            prop.getEngineConfigList().add(new EngineConfig(displayName,
                    de.file().getAbsolutePath(), de.protocol(), detectDefaultOptions(de.file())));
        }

        if (StringUtils.isEmpty(prop.getEngineName()) || !isSelectedEngineAvailable(prop, prop.getEngineName())) {
            prop.setEngineName(buildDisplayName(detected.get(0), true));
        }

        // 兼容旧名字：仍允许显示为旧内置名称时映射到最优引擎。
        if (BUILTIN_NAME.equals(prop.getEngineName())) {
            prop.setEngineName(buildDisplayName(detected.get(0), true));
        }
    }

    private static String buildDisplayName(DetectedEngine de, boolean best) {
        String base = toChineseEngineName(de.file().getName());
        String arch = toChineseArchTag(de.file().getName());
        String protocol = "uci".equalsIgnoreCase(de.protocol()) ? "UCI" : "UCCI";
        String recommend = best ? " 推荐" : "";
        return BUILTIN_PREFIX + base + " " + arch + " " + protocol + recommend;
    }

    private static String toChineseEngineName(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.startsWith("pikafish")) {
            return "皮卡鱼";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            return fileName.substring(0, dot);
        }
        return fileName;
    }

    private static String toChineseArchTag(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.contains("avx512")) {
            return "AVX512";
        }
        if (lower.contains("avx2")) {
            return "AVX2";
        }
        if (lower.contains("bmi2")) {
            return "BMI2";
        }
        if (lower.contains("sse41") || lower.contains("sse4.1")) {
            return "SSE4.1";
        }
        if (lower.contains("sse2")) {
            return "SSE2";
        }
        return "通用";
    }

    private static LinkedHashMap<String, String> detectDefaultOptions(File engineFile) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        File dir = engineFile.getParentFile();
        if (dir == null || !dir.exists()) {
            return options;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return options;
        }

        File nnue = null;
        for (File f : files) {
            if (f == null || !f.isFile()) {
                continue;
            }
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (n.endsWith(".nnue")) {
                if (nnue == null) {
                    nnue = f;
                }
                if (n.contains("pikafish")) {
                    nnue = f;
                    break;
                }
            }
        }

        if (nnue != null) {
            // 引擎工作目录就是可执行文件所在目录，优先使用文件名，
            // 避免中文安装路径下部分引擎对全路径解析失败。
            options.put("EvalFile", nnue.getName());
        }
        return options;
    }

    private record DetectedEngine(File file, String protocol, int score) {}

    private static boolean isSelectedEngineAvailable(Properties prop, String name) {
        for (EngineConfig ec : prop.getEngineConfigList()) {
            if (name.equals(ec.getName())) {
                if (StringUtils.isEmpty(ec.getPath())) {
                    return false;
                }
                File f = new File(ec.getPath());
                return f.exists() && f.isFile();
            }
        }
        return false;
    }
}
