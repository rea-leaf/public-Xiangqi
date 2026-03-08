package com.chf.chess.controller.handle;

import java.util.List;

/**
 * ChessManualCallBack 接口。
 * 棋谱管理相关的回调与处理逻辑。
 */
public interface ChessManualCallBack {
    void browseChessRecord(String fenCode, List<String> moveList, boolean redGo, List<String> nextList);
    void setNextList(List<String> nextList);
    void newChessBoardFromManual(String fenCode);
    void turnOnAnalysisMode();
    void turnOffAnalysisMode();
    void refreshLineChart();
}
