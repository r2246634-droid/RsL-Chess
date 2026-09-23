package com.chess.core;

public record GameConfig(String mode, long initialMs, long incrementMs) {

    public static final long UNLIMITED = Long.MAX_VALUE;

    public boolean hasTimer()      { return initialMs != UNLIMITED; }
    public boolean isNetworkMode() { return mode.startsWith("NETWORK_"); }
    public boolean isNetworkHost() { return "NETWORK_HOST".equals(mode); }

    /** Color of the local human player from this machine's perspective. */
    public String localColor() {
        return "NETWORK_CLIENT".equals(mode) ? "BLACK" : "WHITE";
    }

    public String timerLabel() {
        if (!hasTimer()) return "Süresiz";
        return (initialMs / 60_000) + "+" + (incrementMs / 1_000);
    }

    public String modeLabel() {
        switch (mode) {
            case "TWO_PLAYER":     return "2 Kişilik";
            case "AI_EASY":        return "Standart AI";
            case "AI_MEDIUM":      return "Orta Zorluk AI";
            case "AI_EXPERT":      return "Uzman AI";
            case "NETWORK_HOST":   return "Çevrimiçi (Host)";
            case "NETWORK_CLIENT": return "Çevrimiçi (Misafir)";
            default:               return mode;
        }
    }
}
