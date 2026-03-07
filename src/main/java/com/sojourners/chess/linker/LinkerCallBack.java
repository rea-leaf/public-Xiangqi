package com.sojourners.chess.linker;

/**
 * LinkerCallBack 接口。
 * 连线功能抽象与平台实现相关类型。
 */
public interface LinkerCallBack {

    void linkerInitChessBoard(String fenCode, boolean isReverse);

    char[][] getEngineBoard();

    boolean isThinking();

    boolean isWatchMode();

    void linkerMove(int x1, int y1, int x2, int y2);
}
