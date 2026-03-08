package com.chf.chess.board;

import com.chf.chess.util.XiangqiUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * DefaultBoardRender 类。
 * 棋盘绘制与棋局显示相关类型。
 */
public class DefaultBoardRender extends BaseBoardRender {

    private Image bgImage;
    private Font font;
    private int fontSize;

    public DefaultBoardRender(Canvas canvas) {
        super(canvas);
        this.bgImage = new Image(ChessBoard.class.getResourceAsStream("/image/BOARD.JPG"));
    }

    @Override
    public void drawBackgroundImage(double width, double height) {
        gc.drawImage(bgImage, 0, 0, width, height);
    }

    @Override
    public Color getBackgroundColor() {
        int centerX = (int) (bgImage.getWidth() / 2);
        int centerY = (int) (bgImage.getHeight() / 2);
        return this.bgImage.getPixelReader().getColor(centerX, centerY);
    }


    @Override
    public void drawPieces(int pos, int piece, char[][] board, boolean isReverse, ChessBoard.BoardSize style) {
        if (font == null || fontSize != getFontSize(style)) {
            fontSize = getFontSize(style);
            font = Font.loadFont(getClass().getResourceAsStream("/font/chessman.ttf"), fontSize);
        }
        // 绘制棋子
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (XiangqiUtils.map.get(board[i][j]) != null) {
                    int x = pos + piece * getReverseX(j, isReverse);
                    int y = pos + piece * getReverseY(i, isReverse);
                    drawPieceAtCenter(x, y, piece, board[i][j], style);
                }
            }
        }
    }

    @Override
    protected void drawPieceAtCenter(int centerX, int centerY, int piece, char chess, ChessBoard.BoardSize style) {
        String word = XiangqiUtils.map.get(chess);
        if (word == null) {
            return;
        }
        if (font == null || fontSize != getFontSize(style)) {
            fontSize = getFontSize(style);
            font = Font.loadFont(getClass().getResourceAsStream("/font/chessman.ttf"), fontSize);
        }

        int r = (piece - piece / 10) / 2;
        double bW = getPieceBw(style);
        double sW = getPieceSw(style);

        Color color = Color.web(XiangqiUtils.isRed(chess) ? "#AD1A02" : "#167B7F");
        gc.setFill(Color.WHITE);
        gc.fillOval(centerX - r, centerY - r, 2 * r, 2 * r);
        gc.setStroke(color);
        gc.setLineWidth(bW);
        gc.strokeOval(centerX - r, centerY - r, 2 * r, 2 * r);
        gc.setLineWidth(sW);
        gc.strokeOval(centerX - r + bW * 1.8, centerY - r + bW * 1.8, 2 * (r - bW * 1.8), 2 * (r - bW * 1.8));
        gc.setFill(color);
        gc.setFont(font);
        gc.fillText(word, centerX - fontSize / 2, centerY + fontSize / 2 - fontSize / 5.5);
    }

    /**
     * 棋子外圈线条宽度
     * @return
     */
    private double getPieceBw(ChessBoard.BoardSize style) {
        return getPieceSize(style) /  16d;
    }

    /**
     * 棋子内圈线条宽度
     * @return
     */
    private double getPieceSw(ChessBoard.BoardSize style) {
        return getPieceBw(style) / 4d;
    }

    private int getFontSize(ChessBoard.BoardSize style) {
        return getPieceSize(style) / 2;
    }
}
