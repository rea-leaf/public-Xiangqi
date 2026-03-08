package com.chf.chess.board;

import com.chf.chess.util.PathUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * CustomBoardRender 类。
 * 棋盘绘制与棋局显示相关类型。
 */
public class CustomBoardRender extends BaseBoardRender {

    private Image bgImage;
    private Image maskImage;
    private Image mask2Image;
    private Map<Character, Image> map;

    public CustomBoardRender(Canvas canvas) {
        super(canvas);

        this.bgImage = loadUiImage("board.png");

        this.maskImage = loadUiImage("mask.png");
        this.mask2Image = loadUiImage("mask2.png");

        map = new HashMap<>();
        map.put('r', loadUiImage("br.png"));
        map.put('n', loadUiImage("bn.png"));
        map.put('b', loadUiImage("bb.png"));
        map.put('a', loadUiImage("ba.png"));
        map.put('k', loadUiImage("bk.png"));
        map.put('c', loadUiImage("bc.png"));
        map.put('p', loadUiImage("bp.png"));

        map.put('R', loadUiImage("rr.png"));
        map.put('N', loadUiImage("rn.png"));
        map.put('B', loadUiImage("rb.png"));
        map.put('A', loadUiImage("ra.png"));
        map.put('K', loadUiImage("rk.png"));
        map.put('C', loadUiImage("rc.png"));
        map.put('P', loadUiImage("rp.png"));
    }

    private Image loadUiImage(String name) {
        String filePath = PathUtils.getJarPath() + "ui/" + name;
        File file = new File(filePath);
        if (file.exists()) {
            return new Image(file.toURI().toString());
        }
        InputStream in = CustomBoardRender.class.getClassLoader().getResourceAsStream("ui/" + name);
        if (in != null) {
            try (in) {
                return new Image(in);
            } catch (Exception ignored) {
            }
        }
        // 兜底返回空图，避免启动期直接抛异常。
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8Xw8AAoMBgQ0S8fEAAAAASUVORK5CYII=");
    }

    @Override
    public void drawBackgroundImage(double width, double height) {
        gc.drawImage(bgImage, 0, 0, width, height);
    }

    @Override
    public void drawCenterText(int pos, int piece, ChessBoard.BoardSize style) {

    }

    @Override
    public void drawBoardLine(int pos, int padding, int piece, boolean isReverse, ChessBoard.BoardSize style) {

    }

    @Override
    public void drawPieces(int pos, int piece, char[][] board, boolean isReverse, ChessBoard.BoardSize style) {
        // 绘制棋子
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                Image img = map.get(board[i][j]);
                if (img != null) {
                    int x = pos + piece * getReverseX(j, isReverse);
                    int y = pos + piece * getReverseY(i, isReverse);
                    drawPieceAtCenter(x, y, piece, board[i][j], style);
                }
            }
        }
    }

    @Override
    protected void drawPieceAtCenter(int centerX, int centerY, int piece, char chess, ChessBoard.BoardSize style) {
        Image img = map.get(chess);
        if (img == null) {
            return;
        }
        int r = (piece - piece / 16) / 2;
        gc.drawImage(img, centerX - r, centerY - r, 2 * r, 2 * r);
    }

    @Override
    public Color getBackgroundColor() {
        int centerX = (int) (bgImage.getWidth() / 2);
        int centerY = (int) (bgImage.getHeight() / 2);
        return this.bgImage.getPixelReader().getColor(centerX, centerY);
    }

    @Override
    public void drawStepRemark(int pos, int piece, int x, int y, boolean isPrevStep, boolean isReverse, ChessBoard.BoardSize style) {

        int r = (piece - piece / 32) / 2;

        x = pos + piece * getReverseX(x, isReverse);
        y = pos + piece * getReverseY(y, isReverse);

        Image img = isPrevStep ? mask2Image : maskImage;
        gc.drawImage(img, x - r, y - r, 2 * r, 2 * r);
    }
}
