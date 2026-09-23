package com.chess.core;

import com.chess.i18n.I18n;

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
        if (!hasTimer()) return I18n.t("timer.unlimited");
        return (initialMs / 60_000) + "+" + (incrementMs / 1_000);
    }

    public String modeLabel() {
        switch (mode) {
            case "TWO_PLAYER":     return I18n.t("mode.two_player");
            case "AI_EASY":        return I18n.t("mode.ai_easy");
            case "AI_MEDIUM":      return I18n.t("mode.ai_medium");
            case "AI_EXPERT":      return I18n.t("mode.ai_expert");
            case "NETWORK_HOST":   return I18n.t("mode.network_host");
            case "NETWORK_CLIENT": return I18n.t("mode.network_client");
            default:               return mode;
        }
    }
}
