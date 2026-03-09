package com.chf.chess.openbook;

import com.chf.chess.board.ChessBoard;
import com.chf.chess.config.Properties;
import com.chf.chess.model.BookData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OpenBookManager 类。
 * 开局库查询、聚合与策略相关类型。
 */
public class OpenBookManager {

    private volatile static OpenBookManager instance;

    /** 云库实现（chessdb）。 */
    private OpenBook cloudOpenBook;
    /** 本地库实现列表（按配置顺序）。 */
    private List<OpenBook> localOpenBooks;
    Properties prop;

    private OpenBookManager() {
        this.cloudOpenBook = new CloudOpenBook();
        this.localOpenBooks = new ArrayList<>();
        prop = Properties.getInstance();

        setLocalOpenBooks();
    }

    public synchronized void close() {
        for (OpenBook ob : localOpenBooks) {
            ob.close();
        }
    }

    public synchronized void setLocalOpenBooks() {
        // 重载本地库：先关闭旧连接，再按扩展名创建对应实现。
        close();
        localOpenBooks.clear();
        if (prop.getOpenBookList() == null) {
            return;
        }
        for (String path : prop.getOpenBookList()) {
            if (path == null || path.isBlank()) {
                continue;
            }
            try {
                String fileName = new File(path).getName().toLowerCase(Locale.ROOT);
                if (fileName.endsWith(".xqb")) {
                    localOpenBooks.add(new XqbOpenBook(path));
                } else if (fileName.endsWith(".obk")) {
                    localOpenBooks.add(new BhOpenBook(path));
                } else if (fileName.endsWith(".pfbook")) {
                    localOpenBooks.add(new PfOpenBook(path));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized List<BookData> queryBook(char[][] b, boolean redGo, boolean offManual) {
        // 查询策略：
        // 1) 可选查云库；
        // 2) 未脱谱时查本地库；
        // 3) 按 localBookFirst 合并结果顺序。

        List<BookData> cloudResults = new ArrayList<>();
        if (prop.getUseCloudBook()) {
            String fenCode = ChessBoard.fenCode(b, redGo);
            cloudResults.addAll(cloudOpenBook.query(fenCode, offManual, prop.getMoveRule()));
        }

        List<BookData> localResults = new ArrayList<>();
        if (!offManual) {
            for (OpenBook ob : this.localOpenBooks) {
                localResults.addAll(ob.query(b, redGo, prop.getMoveRule()));
            }
        }

        if (prop.getLocalBookFirst()) {
            localResults.addAll(cloudResults);
            return localResults;
        } else {
            cloudResults.addAll(localResults);
            return cloudResults;
        }
    }

    public static OpenBookManager getInstance() {
        if (instance == null) {
            synchronized (OpenBookManager.class) {
                if (instance == null) {
                    instance = new OpenBookManager();
                }
            }
        }
        return instance;
    }



}
