package com.chess.theme;

public class ThemeManager {

    private static final ThemeManager INSTANCE = new ThemeManager();
    private BoardTheme current = Themes.CLASSIC;

    private ThemeManager() {}

    public static ThemeManager get() { return INSTANCE; }

    public BoardTheme current() { return current; }

    public void set(BoardTheme theme) { current = theme; }
}
