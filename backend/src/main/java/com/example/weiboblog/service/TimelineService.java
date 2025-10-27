package com.example.weiboblog.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class TimelineService {

    private static final String GLOBAL_TIMELINE_KEY = "timeline:global";
    private static final String USER_TIMELINE_PREFIX = "timeline:user:";
    private static final long MAX_TIMELINE_LENGTH = 1000L;

    private final RedisTemplate<String, Object> redisTemplate;

    public TimelineService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cachePost(Long postId, Long authorId, OffsetDateTime createdAt) {
        long score = createdAt.toInstant().toEpochMilli();
        redisTemplate.opsForZSet().add(GLOBAL_TIMELINE_KEY, postId, score);
        trimTimeline(GLOBAL_TIMELINE_KEY);

        String authorKey = USER_TIMELINE_PREFIX + authorId;
        redisTemplate.opsForZSet().add(authorKey, postId, score);
        trimTimeline(authorKey);
    }

    public List<Long> loadGlobalTimelineIds(int page, int size) {
        return loadTimelineIds(GLOBAL_TIMELINE_KEY, page, size);
    }

    public List<Long> loadUserTimelineIds(Long userId, int page, int size) {
        String key = USER_TIMELINE_PREFIX + userId;
        return loadTimelineIds(key, page, size);
    }

    private List<Long> loadTimelineIds(String key, int page, int size) {
        long start = (long) page * size;
        long end = start + size - 1;
        Set<Object> range = redisTemplate.opsForZSet().reverseRange(key, start, end);
        if (range == null || range.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>(range.size());
        for (Object value : range) {
            if (Objects.nonNull(value)) {
                if (value instanceof Number number) {
                    ids.add(number.longValue());
                } else {
                    ids.add(Long.parseLong(value.toString()));
                }
            }
        }
        return ids;
    }

    private void trimTimeline(String key) {
        Long size = redisTemplate.opsForZSet().zCard(key);
        if (size != null && size > MAX_TIMELINE_LENGTH) {
            redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_TIMELINE_LENGTH - 1);
        }
    }
}
