package com.example.weiboblog.service;

import com.example.weiboblog.domain.Post;
import com.example.weiboblog.domain.PostVisibility;
import com.example.weiboblog.domain.PrivacySetting;
import com.example.weiboblog.domain.User;
import com.example.weiboblog.exception.ForbiddenException;
import com.example.weiboblog.repository.FollowRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class PrivacyService {

    private final FollowRepository followRepository;

    public PrivacyService(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    public Set<Long> loadFolloweeIds(Long viewerId) {
        if (viewerId == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(followRepository.findFolloweeIds(viewerId));
    }

    public boolean canViewUser(User owner, Long viewerId, Set<Long> followeeIds) {
        if (owner == null) {
            return false;
        }
        Long ownerId = owner.getId();
        if (ownerId == null) {
            return false;
        }
        if (Objects.equals(ownerId, viewerId)) {
            return true;
        }
        PrivacySetting setting = owner.getPrivacySetting();
        if (setting == null || setting == PrivacySetting.PUBLIC) {
            return true;
        }
        if (viewerId == null) {
            return false;
        }
        Set<Long> effectiveFollowees = followeeIds != null ? followeeIds : loadFolloweeIds(viewerId);
        if (setting == PrivacySetting.FOLLOWERS_ONLY) {
            return effectiveFollowees.contains(ownerId);
        }
        return false;
    }

    public boolean canViewPost(Post post, Long viewerId) {
        return canViewPost(post, viewerId, null);
    }

    public boolean canViewPost(Post post, Long viewerId, Set<Long> followeeIds) {
        if (post == null) {
            return false;
        }
        User owner = post.getAuthor();
        if (owner == null) {
            return false;
        }
        Long ownerId = owner.getId();
        if (Objects.equals(ownerId, viewerId)) {
            return true;
        }
        Set<Long> effectiveFollowees = followeeIds != null ? followeeIds : loadFolloweeIds(viewerId);
        if (!canViewUser(owner, viewerId, effectiveFollowees)) {
            return false;
        }
        PostVisibility visibility = post.getVisibility();
        if (visibility == null || visibility == PostVisibility.PUBLIC) {
            return true;
        }
        if (viewerId == null) {
            return false;
        }
        return switch (visibility) {
            case PUBLIC -> true;
            case FOLLOWERS_ONLY -> effectiveFollowees.contains(ownerId);
            case PRIVATE -> false;
            case CUSTOM -> post.getAllowedUserIds() != null && post.getAllowedUserIds().contains(viewerId);
        };
    }

    public void assertCanViewPost(Post post, Long viewerId) {
        assertCanViewPost(post, viewerId, null);
    }

    public void assertCanViewPost(Post post, Long viewerId, Set<Long> followeeIds) {
        if (!canViewPost(post, viewerId, followeeIds)) {
            throw new ForbiddenException("该内容仅向授权用户开放");
        }
    }

    public void assertCanInteractWithPost(Post post, Long viewerId) {
        assertCanInteractWithPost(post, viewerId, null);
    }

    public void assertCanInteractWithPost(Post post, Long viewerId, Set<Long> followeeIds) {
        if (viewerId == null) {
            throw new ForbiddenException("请先登录后再执行此操作");
        }
        assertCanViewPost(post, viewerId, followeeIds);
    }
}
