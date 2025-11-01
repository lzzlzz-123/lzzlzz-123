package com.example.weiboblog.service;

import static com.example.weiboblog.service.HeatConstants.TOPIC_MEMBERSHIP_HEAT_BONUS;

import com.example.weiboblog.domain.Topic;
import com.example.weiboblog.domain.TopicMember;
import com.example.weiboblog.domain.User;
import com.example.weiboblog.dto.AdminTopicCreateRequest;
import com.example.weiboblog.dto.TopicCreateRequest;
import com.example.weiboblog.dto.TopicResponse;
import com.example.weiboblog.dto.TopicSummaryDto;
import com.example.weiboblog.exception.BadRequestException;
import com.example.weiboblog.exception.ResourceNotFoundException;
import com.example.weiboblog.repository.TopicMemberRepository;
import com.example.weiboblog.repository.TopicRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicMemberRepository topicMemberRepository;
    private final UserService userService;

    public TopicService(TopicRepository topicRepository,
                        TopicMemberRepository topicMemberRepository,
                        UserService userService) {
        this.topicRepository = topicRepository;
        this.topicMemberRepository = topicMemberRepository;
        this.userService = userService;
    }

    @Transactional
    public TopicResponse createTopic(Long ownerId, TopicCreateRequest request) {
        String trimmedName = request.name().trim();
        if (topicRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BadRequestException("Topic name already exists");
        }
        User owner = userService.getById(ownerId);
        Topic topic = new Topic();
        topic.setName(trimmedName);
        topic.setDescription(normalize(request.description()));
        topic.setOwner(owner);
        Topic saved = topicRepository.save(topic);

        TopicMember member = new TopicMember();
        member.setTopic(saved);
        member.setMember(owner);
        topicMemberRepository.save(member);
        adjustTopicHeat(saved, TOPIC_MEMBERSHIP_HEAT_BONUS);
        return toTopicResponse(saved, ownerId);
    }

    @Transactional
    public TopicResponse createTopicByAdmin(Long adminId, AdminTopicCreateRequest request) {
        String trimmedName = request.name().trim();
        if (topicRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BadRequestException("Topic name already exists");
        }
        User owner = userService.getById(adminId);
        Topic topic = new Topic();
        topic.setName(trimmedName);
        topic.setDescription(normalize(request.description()));
        topic.setOwner(owner);
        topic.setHeat(Math.max(0, request.initialHeat()));
        Topic saved = topicRepository.save(topic);

        TopicMember member = new TopicMember();
        member.setTopic(saved);
        member.setMember(owner);
        topicMemberRepository.save(member);
        adjustTopicHeat(saved, TOPIC_MEMBERSHIP_HEAT_BONUS);
        return toTopicResponse(saved, adminId);
    }

    @Transactional
    public TopicResponse joinTopic(Long userId, Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
        if (!topicMemberRepository.existsByTopicIdAndMemberId(topic.getId(), userId)) {
            TopicMember member = new TopicMember();
            member.setTopic(topic);
            member.setMember(userService.getById(userId));
            topicMemberRepository.save(member);
            adjustTopicHeat(topic, TOPIC_MEMBERSHIP_HEAT_BONUS);
        }
        return toTopicResponse(topic, userId);
    }

    @Transactional
    public void leaveTopic(Long userId, Long topicId) {
        topicMemberRepository.deleteByTopicIdAndMemberId(topicId, userId);
    }

    @Transactional(readOnly = true)
    public TopicResponse getTopic(Long topicId, Long viewerId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
        return toTopicResponse(topic, viewerId);
    }

    @Transactional(readOnly = true)
    public List<TopicSummaryDto> getMyTopics(Long userId) {
        return topicMemberRepository.findByMemberIdOrderByCreatedAtDesc(userId).stream()
                .map(TopicMember::getTopic)
                .filter(Objects::nonNull)
                .distinct()
                .map(topic -> toTopicSummary(topic, userId, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopicSummaryDto> getTopicRankings(int size, Long viewerId) {
        int limit = Math.max(1, Math.min(size, 20));
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("heat"), Sort.Order.desc("updatedAt")));
        Page<Topic> page = topicRepository.findAllByOrderByHeatDescUpdatedAtDesc(pageable);
        return page.getContent().stream()
                .map(topic -> toTopicSummary(topic, viewerId, false))
                .toList();
    }

    @Transactional
    public TopicResponse updateTopicHeat(Long topicId, long heat, Long viewerId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
        long target = Math.max(0, heat);
        if (topic.getHeat() != target) {
            topic.setHeat(target);
        }
        return toTopicResponse(topic, viewerId);
    }

    private TopicResponse toTopicResponse(Topic topic, Long viewerId) {
        boolean joined = viewerId != null && topicMemberRepository.existsByTopicIdAndMemberId(topic.getId(), viewerId);
        long memberCount = topicMemberRepository.countByTopicId(topic.getId());
        return new TopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getHeat(),
                memberCount,
                joined,
                UserMapper.toSummary(topic.getOwner()),
                topic.getCreatedAt()
        );
    }

    private TopicSummaryDto toTopicSummary(Topic topic, Long viewerId, boolean assumeJoined) {
        boolean joined = assumeJoined || (viewerId != null && topicMemberRepository.existsByTopicIdAndMemberId(topic.getId(), viewerId));
        long memberCount = topicMemberRepository.countByTopicId(topic.getId());
        return new TopicSummaryDto(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getHeat(),
                memberCount,
                joined
        );
    }

    private void adjustTopicHeat(Topic topic, long delta) {
        if (delta == 0) {
            return;
        }
        long updated = topic.getHeat() + delta;
        if (updated < 0) {
            updated = 0;
        }
        topic.setHeat(updated);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
