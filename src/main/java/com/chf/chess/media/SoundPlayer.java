package com.chf.chess.media;

import javafx.scene.media.AudioClip;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
 * SoundPlayer 类。
 * 音频播放相关类型。
 */
public class SoundPlayer {
    private static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT).contains("win");

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

    public void speakMove(String moveText) {
        if (!WINDOWS || moveText == null || moveText.isBlank()) {
            return;
        }
        String text = normalizeMoveSpeechForStandardMandarin(moveText);
        Thread.startVirtualThread(() -> {
            try {
                String script = "Add-Type -AssemblyName System.Speech;"
                        + "$s=New-Object System.Speech.Synthesis.SpeechSynthesizer;"
                        + "$s.Volume=100;"
                        + "$voice=$s.GetInstalledVoices() | ForEach-Object { $_.VoiceInfo } | "
                        + "Where-Object { $_.Culture.Name -like 'zh*' -and $_.Gender -eq 'Female' } | "
                        + "Select-Object -First 1;"
                        + "if ($voice -eq $null) { "
                        + "$voice=$s.GetInstalledVoices() | ForEach-Object { $_.VoiceInfo } | "
                        + "Where-Object { $_.Gender -eq 'Female' } | Select-Object -First 1"
                        + " };"
                        + "if ($voice -ne $null) { $s.SelectVoice($voice.Name) };"
                        + "$ssml='<speak version=\"1.0\" xml:lang=\"zh-CN\"><prosody rate=\"-8%\" pitch=\"+12%\">"
                        + escapeSsml(text)
                        + "</prosody></speak>';"
                        + "$s.SpeakSsml($ssml)";
                String encoded = Base64.getEncoder()
                        .encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
                Process process = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive",
                        "-EncodedCommand", encoded)
                        .redirectErrorStream(true)
                        .start();
                process.waitFor();
            } catch (Exception ignored) {
            }
        });
    }

    private String escapeSsml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String normalizeMoveSpeechForStandardMandarin(String moveText) {
        return moveText.replaceAll("\\s+", "")
                .replace("将", "匠")
                .replace("相", "像")
                .replace("车", "居")
                .replace("炮", "泡")
                .replace("卒", "足");
    }

    private String normalizeMoveSpeech(String moveText) {
        return moveText.replaceAll("\\s+", "")
                // 象棋术语里“车”读 ju，这里只调整播报文本，不影响界面显示。
                .replace("车", "居");
    }

}
