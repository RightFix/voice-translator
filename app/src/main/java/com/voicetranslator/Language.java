package com.voicetranslator;

public class Language {
    private final String name;
    private final String code;
    private final String locale;

    public Language(String name, String code) {
        this.name = name;
        this.code = code;
        this.locale = code + "-" + code.toUpperCase();
    }

    public Language(String name, String code, String locale) {
        this.name = name;
        this.code = code;
        this.locale = locale;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getLocale() {
        return locale;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Language language = (Language) obj;
        return code.equals(language.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}