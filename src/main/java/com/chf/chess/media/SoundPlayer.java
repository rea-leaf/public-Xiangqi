package com.chf.chess.media;

import javafx.scene.media.AudioClip;

import java.io.File;
import java.net.URL;

/**
 * SoundPlayer 类。
 * 音频播放相关类型。
 */
public class SoundPlayer {
    private AudioClip pick;

    private AudioClip move;

    private AudioClip eat;

    private AudioClip check;

    private AudioClip over;

    public SoundPlayer(String pickSound, String moveSound, String eatSound, String checkSound, String overSound) {
        pick = new AudioClip(resolveMediaUri(pickSound));
        move = new AudioClip(resolveMediaUri(moveSound));
        eat = new AudioClip(resolveMediaUri(eatSound));
        check = new AudioClip(resolveMediaUri(checkSound));
        over = new AudioClip(resolveMediaUri(overSound));
    }

    private String resolveMediaUri(String path) {
        File f = new File(path);
        if (f.exists()) {
            return f.toURI().toString();
        }

        String p = path.replace('\\', '/');
        int idx = p.indexOf("/sound/");
        if (idx >= 0) {
            p = p.substring(idx + 1);
        }
        if (!p.startsWith("sound/")) {
            p = "sound/" + new File(path).getName();
        }
        URL url = SoundPlayer.class.getClassLoader().getResource(p);
        if (url != null) {
            return url.toExternalForm();
        }

        // 最后兜底，沿用原路径，便于抛出明确异常。
        return f.toURI().toString();
    }

    public void eat() {
        eat.play();
    }

    public void pick() {
        pick.play();
    }

    public void move() {
        move.play();
    }

    public void check() {
        check.play();
    }

    public void over() {
        over.play();
    }

}
