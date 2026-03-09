package com.chf.chess.board;

import com.chf.chess.util.XiangqiUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;

/**
 * DefaultBoardRender 类。
 * 棋盘绘制与棋局显示相关类型。
 */
public class DefaultBoardRender extends BaseBoardRender {

    private final ChessBoard.BoardStyle boardStyle;
    private Image bgImage;
    private Font font;
    private int fontSize;
    private final Palette palette;

    public DefaultBoardRender(Canvas canvas) {
        this(canvas, ChessBoard.BoardStyle.DEFAULT);
    }

    public DefaultBoardRender(Canvas canvas, ChessBoard.BoardStyle boardStyle) {
        super(canvas);
        this.boardStyle = boardStyle == null ? ChessBoard.BoardStyle.DEFAULT : boardStyle;
        this.palette = Palette.forStyle(this.boardStyle);
        if (this.boardStyle == ChessBoard.BoardStyle.DEFAULT) {
            this.bgImage = new Image(ChessBoard.class.getResourceAsStream("/image/BOARD.JPG"));
        }
    }

    @Override
    public void drawBackgroundImage(double width, double height) {
        if (bgImage != null) {
            gc.drawImage(bgImage, 0, 0, width, height);
            return;
        }
        gc.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, palette.backgroundTop),
                new Stop(0.55, palette.backgroundBottom),
                new Stop(1, palette.backgroundAccent)));
        gc.fillRect(0, 0, width, height);

        gc.setFill(new Color(1, 1, 1, 0.12));
        gc.fillOval(-width * 0.18, -height * 0.12, width * 0.68, height * 0.38);

        gc.setFill(new Color(0, 0, 0, 0.05));
        gc.fillOval(width * 0.48, height * 0.72, width * 0.6, height * 0.22);
    }

    @Override
    public Color getBackgroundColor() {
        if (bgImage == null) {
            return palette.backgroundBottom;
        }
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

        Color color = XiangqiUtils.isRed(chess) ? palette.redPiece : palette.blackPiece;
        gc.setFill(palette.pieceFill);
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

    @Override
    public void drawBoardLine(int pos, int padding, int piece, boolean isReverse, ChessBoard.BoardSize style) {
        Color lineColor = palette.lineColor;
        gc.setStroke(lineColor);
        gc.setLineWidth(getPieceSize(style) / 40d);
        gc.setGlobalAlpha(0.78);
        gc.strokeRect(pos - padding / 2, pos - padding / 2, piece * 8 + padding, piece * 9 + padding);
        gc.setGlobalAlpha(1);
        gc.setLineWidth(getPieceSize(style) / 80d);
        gc.strokeRect(pos, pos, piece * 8, piece * 9);
        for (int i = 1; i < 9; i++) {
            gc.strokeLine(pos, pos + piece * i, pos + piece * 8, pos + piece * i);
        }
        for (int i = 1; i < 8; i++) {
            gc.strokeLine(pos + piece * i, pos, pos + piece * i, pos + piece * 4);
            gc.strokeLine(pos + piece * i, pos + piece * 5, pos + piece * i, pos + piece * 9);
        }
        gc.strokeLine(pos + piece * 3, pos, pos + piece * 5, pos + piece * 2);
        gc.strokeLine(pos + piece * 3, pos + piece * 2, pos + piece * 5, pos);
        gc.strokeLine(pos + piece * 3, pos + piece * 9, pos + piece * 5, pos + piece * 7);
        gc.strokeLine(pos + piece * 3, pos + piece * 7, pos + piece * 5, pos + piece * 9);
        for (int i = 0; i < 9; i += 2) {
            String style1 = i == 0 ? "r" : (i == 8 ? "l" : "lr");
            drawStarPos(pos + piece * i, pos + piece * 3, piece, style1);
            drawStarPos(pos + piece * i, pos + piece * 6, piece, style1);
        }
        for (int i = 1; i < 9; i += 6) {
            drawStarPos(pos + piece * i, pos + piece * 2, piece, "lr");
            drawStarPos(pos + piece * i, pos + piece * 7, piece, "lr");
        }
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

    private void drawStarPos(int x, int y, int w, String style) {
        int offset = w / 16;
        int len = w / 6;
        if (style.contains("l")) {
            gc.strokePolyline(new double[]{x - offset - len, x - offset, x - offset},
                    new double[]{y - offset, y - offset, y - offset - len}, 3);

            gc.strokePolyline(new double[]{x - offset - len, x - offset, x - offset},
                    new double[]{y + offset, y + offset, y + offset + len}, 3);
        }
        if (style.contains("r")) {
            gc.strokePolyline(new double[]{x + offset + len, x + offset, x + offset},
                    new double[]{y - offset, y - offset, y - offset - len}, 3);

            gc.strokePolyline(new double[]{x + offset + len, x + offset, x + offset},
                    new double[]{y + offset, y + offset, y + offset + len}, 3);
        }
    }

    private static class Palette {
        private final Color backgroundTop;
        private final Color backgroundBottom;
        private final Color backgroundAccent;
        private final Color pieceFill;
        private final Color redPiece;
        private final Color blackPiece;
        private final Color lineColor;

        private Palette(Color backgroundTop, Color backgroundBottom, Color backgroundAccent,
                        Color pieceFill, Color redPiece, Color blackPiece, Color lineColor) {
            this.backgroundTop = backgroundTop;
            this.backgroundBottom = backgroundBottom;
            this.backgroundAccent = backgroundAccent;
            this.pieceFill = pieceFill;
            this.redPiece = redPiece;
            this.blackPiece = blackPiece;
            this.lineColor = lineColor;
        }

        private static Palette forStyle(ChessBoard.BoardStyle style) {
            return switch (style) {
                case JADE -> new Palette(
                        Color.web("#F3F8F2"),
                        Color.web("#CFE3D7"),
                        Color.web("#9DC3AE"),
                        Color.web("#FFF8EE"),
                        Color.web("#A62B1F"),
                        Color.web("#1C5C4E"),
                        Color.web("#355D52"));
                case INK -> new Palette(
                        Color.web("#F6F3EA"),
                        Color.web("#D9D1C2"),
                        Color.web("#B4A995"),
                        Color.web("#FBF7EF"),
                        Color.web("#8D251D"),
                        Color.web("#2C3138"),
                        Color.web("#423D35"));
                case LIGHT -> new Palette(
                        Color.web("#FFF9F1"),
                        Color.web("#F2DEC2"),
                        Color.web("#E8CAA4"),
                        Color.web("#FFFDF8"),
                        Color.web("#C0392B"),
                        Color.web("#37515F"),
                        Color.web("#7A5A3B"));
                case AUTUMN -> new Palette(
                        Color.web("#FBF2E1"),
                        Color.web("#EBCB9A"),
                        Color.web("#D8A66D"),
                        Color.web("#FFF8EC"),
                        Color.web("#A63A12"),
                        Color.web("#5A4637"),
                        Color.web("#8B5E34"));
                case CHINESE -> new Palette(
                        Color.web("#F6EFE2"),
                        Color.web("#E1C9A0"),
                        Color.web("#C99758"),
                        Color.web("#FFF6E8"),
                        Color.web("#B4201F"),
                        Color.web("#2E3B2F"),
                        Color.web("#7C4A26"));
                case ANTIQUE -> new Palette(
                        Color.web("#EEE2C8"),
                        Color.web("#C9B089"),
                        Color.web("#A7855C"),
                        Color.web("#FAF1E0"),
                        Color.web("#8D3A23"),
                        Color.web("#4D4034"),
                        Color.web("#6E563F"));
                case PALACE -> new Palette(
                        Color.web("#F7E8C7"),
                        Color.web("#E2BF6B"),
                        Color.web("#BF8B30"),
                        Color.web("#FFF8EA"),
                        Color.web("#B2271E"),
                        Color.web("#5E4630"),
                        Color.web("#8E692F"));
                case CELADON -> new Palette(
                        Color.web("#EFF6F0"),
                        Color.web("#C7DDD1"),
                        Color.web("#91B8A4"),
                        Color.web("#FEFDF8"),
                        Color.web("#A6312D"),
                        Color.web("#46635A"),
                        Color.web("#5D7D72"));
                case LANDSCAPE -> new Palette(
                        Color.web("#EEF3E8"),
                        Color.web("#C7D7BF"),
                        Color.web("#9DB297"),
                        Color.web("#FFFDF5"),
                        Color.web("#A43B2E"),
                        Color.web("#3F5A4C"),
                        Color.web("#59735B"));
                case XUAN_PAPER -> new Palette(
                        Color.web("#FBF8EF"),
                        Color.web("#EEE4C8"),
                        Color.web("#D9C79E"),
                        Color.web("#FFFCF4"),
                        Color.web("#A03428"),
                        Color.web("#3A3833"),
                        Color.web("#8A7A5A"));
                case CINNABAR -> new Palette(
                        Color.web("#F7E3D9"),
                        Color.web("#E2B09E"),
                        Color.web("#CD7C63"),
                        Color.web("#FFF7F1"),
                        Color.web("#B52117"),
                        Color.web("#54463F"),
                        Color.web("#91523C"));
                case EBONY -> new Palette(
                        Color.web("#4B403C"),
                        Color.web("#2F2927"),
                        Color.web("#191615"),
                        Color.web("#F7F0E4"),
                        Color.web("#C13C2F"),
                        Color.web("#E1D1BC"),
                        Color.web("#C6AB87"));
                case BRONZE -> new Palette(
                        Color.web("#E7DEC8"),
                        Color.web("#C5AE77"),
                        Color.web("#8C6B39"),
                        Color.web("#FBF4E7"),
                        Color.web("#A23621"),
                        Color.web("#4F4B43"),
                        Color.web("#7B622C"));
                case PINE_SOOT -> new Palette(
                        Color.web("#F1EEE7"),
                        Color.web("#D5D0C6"),
                        Color.web("#ACA698"),
                        Color.web("#FBF9F2"),
                        Color.web("#8F2F24"),
                        Color.web("#242628"),
                        Color.web("#4B4D4F"));
                case DEFAULT, CUSTOM -> new Palette(
                        Color.WHITE,
                        Color.WHITE,
                        Color.WHITE,
                        Color.WHITE,
                        Color.web("#AD1A02"),
                        Color.web("#167B7F"),
                        Color.BLACK);
            };
        }
    }
}
