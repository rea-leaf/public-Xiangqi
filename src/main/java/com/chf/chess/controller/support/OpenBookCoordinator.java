package com.chf.chess.controller.support;

import com.chf.chess.model.BookData;
import com.chf.chess.openbook.OpenBookManager;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * 开局库协调层，统一处理查询刷新。
 */
public final class OpenBookCoordinator {

    public void refresh(boolean licensed, boolean enabled, char[][] board, boolean redGo,
                        IntSupplier offManualStepsSupplier, int currentPly,
                        Consumer<List<BookData>> resultsConsumer, Runnable clearAction) {
        if (!licensed) {
            clearAction.run();
            return;
        }
        if (!enabled) {
            clearAction.run();
            return;
        }

        Thread.startVirtualThread(() -> {
            List<BookData> results = OpenBookManager.getInstance().queryBook(
                    board,
                    redGo,
                    currentPly / 2 >= offManualStepsSupplier.getAsInt()
            );
            resultsConsumer.accept(results);
        });
    }
}
