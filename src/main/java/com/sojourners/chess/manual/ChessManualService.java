package com.sojourners.chess.manual;

import java.io.File;

/**
 * ChessManualService 接口。
 * 棋谱读写与棋谱结构相关类型。
 */
public interface ChessManualService {
    ChessManual openChessManual(File file);
    void saveChessManual(ChessManual chessManual, File file);
}
