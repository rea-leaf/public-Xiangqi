package com.sojourners.chess.openbook;

/**
 * MoveRule 枚举。
 * 开局库查询、聚合与策略相关类型。
 */
public enum MoveRule {

    BEST_SCORE,

    BEST_WINRATE,

    POSITIVE_RANDOM,

    FULL_RANDOM

}
