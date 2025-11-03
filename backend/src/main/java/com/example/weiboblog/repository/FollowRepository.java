package com.example.weiboblog.repository;

import com.example.weiboblog.domain.Follow;
import com.example.weiboblog.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    Optional<Follow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    long countByFollowerId(Long followerId);
    long countByFolloweeId(Long followeeId);

    @Query("select f.followee.id from Follow f where f.follower.id = :followerId")
    List<Long> findFolloweeIds(Long followerId);

    @Query("select f.follower from Follow f where f.followee.id = :userId")
    List<User> findFollowers(Long userId);

    @Query("select f.followee from Follow f where f.follower.id = :userId")
    List<User> findFollowees(Long userId);
}
