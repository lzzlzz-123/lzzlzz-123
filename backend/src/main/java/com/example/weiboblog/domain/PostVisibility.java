package com.example.weiboblog.domain;

public enum PostVisibility {
    PUBLIC,
    FOLLOWERS_ONLY,
    PRIVATE,
    CUSTOM;

    public static PostVisibility fromPrivacySetting(PrivacySetting setting) {
        if (setting == null) {
            return PUBLIC;
        }
        return switch (setting) {
            case PUBLIC -> PUBLIC;
            case FOLLOWERS_ONLY -> FOLLOWERS_ONLY;
            case PRIVATE -> PRIVATE;
        };
    }
}
