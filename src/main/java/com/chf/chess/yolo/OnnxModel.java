package com.chf.chess.yolo;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.chf.chess.config.Properties;
import com.chf.chess.util.PathUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

/**
 * OnnxModel 类。
 * ONNX 识别模型与推理逻辑相关类型。
 */
public abstract class OnnxModel {

    public static final double PADDING = 0.8d;

    public final float CONFIDENCE = 0.75f;

    public final int SIZE = 640;

    public static final char[] labels = {'n', 'b', 'a', 'k', 'r', 'c', 'p', 'R', 'N', 'A', 'K', 'B', 'C', 'P', '0'};

    OrtSession session;

    OrtEnvironment env;

    public OnnxModel() {
        try {
            env = OrtEnvironment.getEnvironment();

            OrtSession.SessionOptions opt = new OrtSession.SessionOptions();
            opt.setIntraOpNumThreads(Properties.getInstance().getLinkThreadNum());

            String path = PathUtils.getJarPath() + getModelPath();
            File modelFile = new File(path);
            if (modelFile.exists() && modelFile.isFile()) {
                session = env.createSession(path, opt);
            } else {
                byte[] bytes = readModelFromResource(getModelPath());
                session = env.createSession(bytes, opt);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] readModelFromResource(String modelPath) throws Exception {
        String p = modelPath.replace('\\', '/');
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        InputStream in = OnnxModel.class.getClassLoader().getResourceAsStream(p);
        if (in == null) {
            throw new IllegalStateException("model not found: " + modelPath);
        }
        try (in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return out.toByteArray();
        }
    }

    public abstract String getModelPath();

    public abstract java.awt.Rectangle findBoardPosition(BufferedImage img);

    public abstract boolean findChessBoard(BufferedImage img, char[][] board);

}
