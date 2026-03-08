package com.chf.chess.lock;

/**
 * SingleLock 类。
 * 线程协作与同步控制相关类型。
 */
public class SingleLock {
    private volatile boolean isLock = false;

    public synchronized void lock() {
        while (isLock) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isLock = true;
    }

    public synchronized void unlock() {
        isLock = false;
        notify();
    }
}
