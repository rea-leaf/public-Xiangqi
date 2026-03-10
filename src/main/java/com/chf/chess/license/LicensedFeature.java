package com.chf.chess.license;

/**
 * 可授权控制的功能项。
 */
public enum LicensedFeature {
    ENGINE("引擎对弈", "引擎加载、执红执黑和引擎相关设置"),
    ANALYSIS("局面分析", "无限分析、变招切换和分析面板"),
    OPENING_BOOK("开局库", "开局库查询和开局库设置"),
    LOCAL_BOOK("本地开局库管理", "导入、维护本地开局库"),
    LINK("连线功能", "图像识别、连线和自动点击"),
    MOVE_VOICE("语音报招", "走棋语音播报"),
    MANUAL_SCORE("棋谱打分", "棋谱打分与评分同步");

    private final String displayName;
    private final String description;

    LicensedFeature(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
