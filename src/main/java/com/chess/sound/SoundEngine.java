package com.chess.sound;

import javax.sound.sampled.*;

public class SoundEngine {

    private static final int SAMPLE_RATE = 44100;
    private static volatile boolean enabled = true;

    public static void setEnabled(boolean e) { enabled = e; }
    public static boolean isEnabled()        { return enabled; }

    public static void playMove()    { async(() -> tone(800, 90)); }
    public static void playCapture() { async(() -> { tone(500, 80); sleep(40); tone(1200, 100); }); }

    public static void playCheck() {
        async(() -> { tone(1050, 130); sleep(90); tone(1050, 130); });
    }

    public static void playCheckmate() {
        async(() -> {
            tone(550, 160); sleep(70);
            tone(750, 160); sleep(70);
            tone(1000, 350);
        });
    }

    // ── İç yardımcılar ───────────────────────────────────────────────────────

    private static void async(Runnable r) {
        if (!enabled) return;
        Thread t = new Thread(r, "sfx");
        t.setDaemon(true);
        t.start();
    }

    private static void tone(double freq, int ms) {
        try {
            byte[] data = generate(freq, ms);
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            if (!AudioSystem.isLineSupported(info)) return;
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt);
            line.start();
            line.write(data, 0, data.length);
            line.drain();
            line.close();
        } catch (Exception ignored) {}
    }

    private static byte[] generate(double freq, int ms) {
        int samples = SAMPLE_RATE * ms / 1000;
        byte[] buf  = new byte[samples * 2];
        int fade    = Math.min(400, samples / 3);
        for (int i = 0; i < samples; i++) {
            double t   = (double) i / SAMPLE_RATE;
            double val = Math.sin(2 * Math.PI * freq * t);
            double env = 1.0;
            if (i < fade) env = (double) i / fade;
            if (i > samples - fade) env = (double) (samples - i) / fade;
            short s        = (short) (val * env * 26000);
            buf[i * 2]     = (byte) (s & 0xFF);
            buf[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return buf;
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
