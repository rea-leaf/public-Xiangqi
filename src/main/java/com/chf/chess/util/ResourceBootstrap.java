package com.chf.chess.util;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * ResourceBootstrap 类。
 * 启动时将关键资源从 classpath 同步到运行目录，兼容文件路径加载场景。
 */
public class ResourceBootstrap {

    private static final String[] UI_FILES = new String[]{
            "ui/board.png", "ui/mask.png", "ui/mask2.png", "ui/circle.ico",
            "ui/br.png", "ui/bn.png", "ui/bb.png", "ui/ba.png", "ui/bk.png", "ui/bc.png", "ui/bp.png",
            "ui/rr.png", "ui/rn.png", "ui/rb.png", "ui/ra.png", "ui/rk.png", "ui/rc.png", "ui/rp.png"
    };

    private static final String[] SOUND_FILES = new String[]{
            "sound/click.wav", "sound/move.wav", "sound/capture.wav", "sound/check.wav", "sound/win.wav"
    };

    private static final String[] MODEL_FILES = new String[]{
            "model/yolov11.onnx"
    };

    public static synchronized void prepare() {
        try {
            copyGroup(UI_FILES);
            copyGroup(SOUND_FILES);
            copyGroup(MODEL_FILES);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void copyGroup(String[] files) {
        if (files == null) {
            return;
        }
        for (String p : files) {
            copyIfMissing(p);
        }
    }

    private static void copyIfMissing(String resourcePath) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            return;
        }

        File target = new File(PathUtils.getJarPath(), resourcePath.replace('/', File.separatorChar));
        if (target.exists() && target.isFile() && target.length() > 0) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        InputStream in = ResourceBootstrap.class.getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            return;
        }
        try (in) {
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
