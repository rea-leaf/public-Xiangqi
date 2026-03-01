package com.sojourners.chess.manual;

import java.io.File;

public interface ChessManualService {
    ChessManual openChessManual(File file);
    void saveChessManual(ChessManual chessManual, File file);
}
