package com.emmanuelgabe.portfolio.entity;

public enum IconType {
    FONT_AWESOME("Font Awesome"),
    CUSTOM_SVG("Custom SVG");

    private final String displayName;

    IconType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
