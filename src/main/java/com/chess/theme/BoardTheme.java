package com.chess.theme;

import javafx.scene.paint.Color;

public record BoardTheme(
    String id,
    String displayName,
    PieceStyle pieceStyle,
    Color lightSquare,
    Color darkSquare,
    Color selectionColor,
    Color moveHintColor,
    Color captureRingColor,
    Color checkRingColor,
    Color whitePieceColor,
    Color blackPieceColor,
    Color backgroundColor,
    Color panelColor,
    Color panelBorderColor,
    Color textPrimary,
    Color textSecondary,
    Color accentColor
) {
    public enum PieceStyle { SHADOWED, CLEAN, GLOWING, NEON }
}
