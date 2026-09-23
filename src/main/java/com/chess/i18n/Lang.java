package com.chess.i18n;

/** Oyunun desteklediği arayüz dilleri. Sıralama I18n tablosundaki sütun sırasıyla eşleşir. */
public enum Lang {
    TR("TR", "Türkçe"),
    EN("EN", "English"),
    RU("RU", "Русский");

    private final String code;
    private final String nativeName;

    Lang(String code, String nativeName) {
        this.code = code;
        this.nativeName = nativeName;
    }

    public String code()       { return code; }
    public String nativeName() { return nativeName; }
}
