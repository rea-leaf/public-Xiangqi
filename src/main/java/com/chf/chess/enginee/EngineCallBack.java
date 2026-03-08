package com.chf.chess.enginee;


import com.chf.chess.model.BookData;
import com.chf.chess.model.ThinkData;

import java.util.List;

/**
 * 引擎回调
 */
public interface EngineCallBack {

    void bestMove(String first, String second);

    void thinkDetail(ThinkData td);

    void showBookResults(List<BookData> list);
}
