package com.example.weiboblog.repository;

import com.example.weiboblog.domain.TopicMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicMemberRepository extends JpaRepository<TopicMember, Long> {
    boolean existsByTopicIdAndMemberId(Long topicId, Long memberId);
    long countByTopicId(Long topicId);
    List<TopicMember> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    void deleteByTopicIdAndMemberId(Long topicId, Long memberId);
}
