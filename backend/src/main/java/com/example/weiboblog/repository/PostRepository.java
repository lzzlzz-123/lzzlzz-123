package com.example.weiboblog.repository;

import com.example.weiboblog.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByAuthorIdInOrderByCreatedAtDesc(Iterable<Long> authorIds, Pageable pageable);
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Post> findByHeatGreaterThanEqual(long heat, Pageable pageable);
    Page<Post> findByTopicIdOrderByCreatedAtDesc(Long topicId, Pageable pageable);
    long countByAuthorId(Long authorId);
    long countByTopicId(Long topicId);
}
