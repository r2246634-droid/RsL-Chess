package com.chess.core;

import java.util.function.Consumer;

public class GameTimer {

    private volatile long whiteMs;
    private volatile long blackMs;
    private final long incrementMs;

    private volatile boolean running = false;
    private volatile boolean whiteTurn = true;
    private long lastTick;

    private Runnable onWhiteTimeout;
    private Runnable onBlackTimeout;
    private Consumer<GameTimer> onTick;

    private Thread thread;

    public GameTimer(long initialMs, long incrementMs) {
        this.whiteMs    = initialMs;
        this.blackMs    = initialMs;
        this.incrementMs = incrementMs;
    }

    public void start(boolean whitePlaysFirst) {
        this.whiteTurn = whitePlaysFirst;
        this.running   = true;
        this.lastTick  = System.currentTimeMillis();

        thread = new Thread(() -> {
            while (running) {
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                if (!running) break;

                long now     = System.currentTimeMillis();
                long elapsed = now - lastTick;
                lastTick     = now;

                if (whiteTurn) {
                    whiteMs = Math.max(0, whiteMs - elapsed);
                    if (whiteMs == 0) {
                        running = false;
                        tick();
                        if (onWhiteTimeout != null) onWhiteTimeout.run();
                        return;
                    }
                } else {
                    blackMs = Math.max(0, blackMs - elapsed);
                    if (blackMs == 0) {
                        running = false;
                        tick();
                        if (onBlackTimeout != null) onBlackTimeout.run();
                        return;
                    }
                }
                tick();
            }
        }, "game-timer");
        thread.setDaemon(true);
        thread.start();
    }

    // Hamle sonrası çağrılır: inkrement ekle, turu değiştir
    public void switchTurn() {
        if (whiteTurn) whiteMs += incrementMs;
        else           blackMs += incrementMs;
        whiteTurn = !whiteTurn;
        lastTick  = System.currentTimeMillis();
    }

    public void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    public long getWhiteMs() { return whiteMs; }
    public long getBlackMs() { return blackMs; }
    public boolean isWhiteTurn() { return whiteTurn; }

    public String formatWhite() { return format(whiteMs); }
    public String formatBlack() { return format(blackMs); }

    public String format(long ms) {
        if (ms <= 0) return "0:00";
        long min = ms / 60000;
        long sec = (ms % 60000) / 1000;
        return String.format("%d:%02d", min, sec);
    }

    public void setOnWhiteTimeout(Runnable r) { onWhiteTimeout = r; }
    public void setOnBlackTimeout(Runnable r) { onBlackTimeout = r; }
    public void setOnTick(Consumer<GameTimer> c) { onTick = c; }

    private void tick() { if (onTick != null) onTick.accept(this); }
}
