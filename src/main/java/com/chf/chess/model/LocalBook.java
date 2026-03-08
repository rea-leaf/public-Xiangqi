package com.chf.chess.model;

/**
 * LocalBook 类。
 * 业务数据模型。
 */
public class LocalBook {
    private String path;

    public LocalBook(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
