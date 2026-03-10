package com.zhizun.licenseadmin.license;

public enum LicensedFeature {
    ENGINE("引擎对弈"),
    ANALYSIS("局面分析"),
    OPENING_BOOK("开局库"),
    LOCAL_BOOK("本地开局库管理"),
    LINK("连线功能"),
    MOVE_VOICE("语音报招"),
    MANUAL_SCORE("棋谱打分");

    private final String displayName;

    LicensedFeature(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
