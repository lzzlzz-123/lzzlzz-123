package com.example.weiboblog.service;

public final class HeatConstants {

    public static final long HOTSPOT_THRESHOLD = 10L;
    public static final long LIKE_HEAT_WEIGHT = 1L;
    public static final long COMMENT_HEAT_WEIGHT = 2L;
    public static final long TOPIC_POST_HEAT_BONUS = 3L;
    public static final long TOPIC_MEMBERSHIP_HEAT_BONUS = 1L;

    private HeatConstants() {
    }
}
