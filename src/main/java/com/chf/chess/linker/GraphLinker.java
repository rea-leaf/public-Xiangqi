package com.chf.chess.linker;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * GraphLinker 接口。
 * 连线功能抽象与平台实现相关类型。
 */
public interface GraphLinker {

    void start();

    void stop();

    void getTargetWindowId();

    Rectangle getTargetWindowPosition();

    BufferedImage screenshotByBack(Rectangle windowPos);

    BufferedImage screenshotByFront(Rectangle windowPos);

    void mouseClickByFront(Rectangle windowPos, Point p1, Point p2);

    void mouseClickByBack(Point p1, Point p2);

}
