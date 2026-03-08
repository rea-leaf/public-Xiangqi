package com.chf.chess.util;

import com.sun.jna.Platform;

import java.io.File;

/**
 * Path 工具类
 */
public class PathUtils {
    private static final String APP_DATA_DIR = "至尊象棋";

    public static String getJarPath() {
        try {
            String path = PathUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            path = java.net.URLDecoder.decode(path, "UTF-8");
            if (Platform.isWindows() && path.startsWith("/")) {
                path = path.substring(1);
            }
            int i = path.lastIndexOf("/");
            if (i >= 0) {
                path = path.substring(0, i + 1);
            }
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getDataPath() {
        String custom = System.getProperty("zhizunxiangqi.data.dir");
        File base;
        if (custom != null && !custom.trim().isEmpty()) {
            base = new File(custom.trim());
        } else if (Platform.isWindows()) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.trim().isEmpty()) {
                base = new File(localAppData, APP_DATA_DIR);
            } else {
                base = new File(System.getProperty("user.home"), APP_DATA_DIR);
            }
        } else {
            base = new File(System.getProperty("user.home"), ".zhizunxiangqi");
        }

        if (!base.exists()) {
            base.mkdirs();
        }
        String path = base.getAbsolutePath();
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
    }

    /**
     * 获取 path 的父目录
     * @param path
     * @return
     */
    public static File getParentDir(String path) {
        return new File(path).getParentFile();
    }

    public static boolean isImage(String path) {
        String[] paths = path.split("\\.");
        String suffix = paths[paths.length - 1].toLowerCase();
        if (suffix.equals("png") || suffix.equals("jpg") || suffix.equals("jpeg") || suffix.equals("bmp")) {
            return true;
        }
        return false;
    }

    public static String getDotExtension(File file) {
        String name = file.getName();
        int idx = name.lastIndexOf('.');
        if (idx > 0 && idx < name.length() - 1) {
            return name.substring(idx + 1);
        }
        return "";
    }
}
