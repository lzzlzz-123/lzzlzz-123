package com.example.weiboblog.service;

import com.example.weiboblog.domain.Follow;
import com.example.weiboblog.domain.User;
import com.example.weiboblog.dto.UserProfileResponse;
import com.example.weiboblog.exception.BadRequestException;
import com.example.weiboblog.exception.ResourceNotFoundException;
import com.example.weiboblog.repository.FollowRepository;
import com.example.weiboblog.repository.PostRepository;
import com.example.weiboblog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;

    public UserService(UserRepository userRepository,
                       FollowRepository followRepository,
                       PostRepository postRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId, Long viewerId) {
        User user = getById(userId);
        long followerCount = followRepository.countByFolloweeId(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        long postCount = postRepository.countByAuthorId(userId);
        boolean followed = viewerId != null && followRepository.existsByFollowerIdAndFolloweeId(viewerId, userId);
        return UserMapper.toProfile(user, followerCount, followingCount, postCount, followed);
    }

    @Transactional
    public void follow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BadRequestException("Cannot follow yourself");
        }
        User follower = getById(followerId);
        User followee = getById(followeeId);
        if (followRepository.existsByFollowerIdAndFolloweeId(follower.getId(), followee.getId())) {
            return;
        }
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowee(followee);
        followRepository.save(follow);
    }

    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .ifPresent(followRepository::delete);
    }
}
