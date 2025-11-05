package com.example.weiboblog.service;

import static com.example.weiboblog.service.HeatConstants.*;

import com.example.weiboblog.domain.Post;
import com.example.weiboblog.domain.PostLike;
import com.example.weiboblog.domain.PostVisibility;
import com.example.weiboblog.domain.Topic;
import com.example.weiboblog.domain.User;
import com.example.weiboblog.dto.HotspotPostCreateRequest;
import com.example.weiboblog.dto.PagedResponse;
import com.example.weiboblog.dto.PostCreateRequest;
import com.example.weiboblog.dto.PostResponse;
import com.example.weiboblog.dto.PostUpdateRequest;
import com.example.weiboblog.dto.TopicLightDto;
import com.example.weiboblog.dto.UserSummaryDto;
import com.example.weiboblog.exception.BadRequestException;
import com.example.weiboblog.exception.ForbiddenException;
import com.example.weiboblog.exception.ResourceNotFoundException;
import com.example.weiboblog.repository.CommentRepository;
import com.example.weiboblog.repository.PostLikeRepository;
import com.example.weiboblog.repository.PostRepository;
import com.example.weiboblog.repository.TopicMemberRepository;
import com.example.weiboblog.repository.TopicRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final TopicRepository topicRepository;
    private final TopicMemberRepository topicMemberRepository;
    private final TimelineService timelineService;
    private final UserService userService;
    private final PrivacyService privacyService;

    private static final int MAX_RECOMMENDATION_SIZE = 6;
    private static final int MAX_EXCLUDE_SIZE = 200;

    public PostService(PostRepository postRepository,
                       PostLikeRepository postLikeRepository,
                       CommentRepository commentRepository,
                       TopicRepository topicRepository,
                       TopicMemberRepository topicMemberRepository,
                       TimelineService timelineService,
                       UserService userService,
                       PrivacyService privacyService) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository = commentRepository;
        this.topicRepository = topicRepository;
        this.topicMemberRepository = topicMemberRepository;
        this.timelineService = timelineService;
        this.userService = userService;
        this.privacyService = privacyService;
    }

    @Transactional
    public PostResponse createPost(Long authorId, PostCreateRequest request) {
        return createPostInternal(authorId, request, true);
    }

    @Transactional
    public PostResponse createHotspotPost(Long adminId, HotspotPostCreateRequest request) {
        PostResponse created = createPostInternal(
                adminId,
                new PostCreateRequest(request.content(), request.mediaUrls(), request.topicId()),
                false
        );
        if (request.initialHeat() <= 0) {
            return created;
        }
        return updateHeat(created.id(), request.initialHeat(), adminId);
    }

    @Transactional
    public PostResponse updatePost(Long authorId, Long postId, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        if (!Objects.equals(post.getAuthor().getId(), authorId)) {
            throw new ForbiddenException("无权修改该动态");
        }
        post.setContent(request.content());
        applyVisibility(post, post.getAuthor(), request.visibility(), request.allowedUserIds());
        return toPostResponse(post, authorId);
    }

    @Transactional
    public void deletePost(Long authorId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        if (!Objects.equals(post.getAuthor().getId(), authorId)) {
            throw new ForbiddenException("无权删除该动态");
        }
        postRepository.delete(post);
        timelineService.removePost(postId, authorId);
        Topic topic = post.getTopic();
        if (topic != null) {
            adjustTopicHeat(topic, -TOPIC_POST_HEAT_BONUS);
        }
    }

    private PostResponse createPostInternal(Long authorId, PostCreateRequest request, boolean requireTopicMembership) {
        if (request.mediaUrls() != null && request.mediaUrls().stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("Media URLs cannot be null");
        }
        User author = userService.getById(authorId);
        Post post = new Post();
        post.setAuthor(author);
        post.setContent(request.content());
        post.setMediaUrls(request.mediaUrls() == null ? new ArrayList<>() : new ArrayList<>(request.mediaUrls()));

        Topic topic = null;
        if (request.topicId() != null) {
            topic = topicRepository.findById(request.topicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + request.topicId()));
            if (requireTopicMembership && !topicMemberRepository.existsByTopicIdAndMemberId(topic.getId(), authorId)) {
                throw new BadRequestException("Join the topic before posting");
            }
            post.setTopic(topic);
        }

        applyVisibility(post, author, request.visibility(), request.allowedUserIds());

        Post saved = postRepository.save(post);
        timelineService.cachePost(saved.getId(), author.getId(), saved.getCreatedAt());
        if (topic != null) {
            adjustTopicHeat(topic, TOPIC_POST_HEAT_BONUS);
        }
        return toPostResponse(saved, authorId);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId, Long viewerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        privacyService.assertCanViewPost(post, viewerId);
        return toPostResponse(post, viewerId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getGlobalFeed(int page, int size, Long viewerId) {
        Set<Long> followeeIds = viewerId != null ? privacyService.loadFolloweeIds(viewerId) : Collections.emptySet();
        List<Long> cachedIds = timelineService.loadGlobalTimelineIds(page, size);
        if (cachedIds.isEmpty()) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);
            List<PostResponse> content = postPage.getContent().stream()
                    .filter(post -> privacyService.canViewPost(post, viewerId, followeeIds))
                    .map(post -> toPostResponse(post, viewerId))
                    .toList();
            return new PagedResponse<>(content, page, size, postPage.getTotalElements(), postPage.getTotalPages(), postPage.isLast());
        }
        List<PostResponse> responses = postsFromIds(cachedIds, viewerId, followeeIds);
        long total = Math.max(postRepository.count(), cachedIds.size());
        int totalPages = (int) Math.ceil((double) total / size);
        boolean last = page + 1 >= totalPages;
        return new PagedResponse<>(responses, page, size, total, totalPages, last);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getUserFeed(Long userId, int page, int size, Long viewerId) {
        User owner = userService.getById(userId);
        Set<Long> followeeIds = viewerId != null ? privacyService.loadFolloweeIds(viewerId) : Collections.emptySet();
        if (!privacyService.canViewUser(owner, viewerId, followeeIds)) {
            throw new ForbiddenException("该用户仅向授权用户开放动态");
        }
        List<Long> cachedIds = timelineService.loadUserTimelineIds(userId, page, size);
        if (cachedIds.isEmpty()) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Post> postPage = postRepository.findAllByAuthorIdInOrderByCreatedAtDesc(List.of(userId), pageable);
            List<PostResponse> content = postPage.getContent().stream()
                    .filter(post -> privacyService.canViewPost(post, viewerId, followeeIds))
                    .map(post -> toPostResponse(post, viewerId))
                    .toList();
            return new PagedResponse<>(content, page, size, postPage.getTotalElements(), postPage.getTotalPages(), postPage.isLast());
        }
        List<PostResponse> responses = postsFromIds(cachedIds, viewerId, followeeIds);
        long total = postRepository.countByAuthorId(userId);
        int totalPages = (int) Math.ceil((double) Math.max(total, cachedIds.size()) / size);
        boolean last = page + 1 >= totalPages;
        return new PagedResponse<>(responses, page, size, total, totalPages, last);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getHotspotFeed(int page, int size, Long viewerId) {
        Set<Long> followeeIds = viewerId != null ? privacyService.loadFolloweeIds(viewerId) : Collections.emptySet();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("heat"), Sort.Order.desc("updatedAt")));
        Page<Post> postPage = postRepository.findByHeatGreaterThanEqual(HOTSPOT_THRESHOLD, pageable);
        List<PostResponse> content = postPage.getContent().stream()
                .filter(post -> privacyService.canViewPost(post, viewerId, followeeIds))
                .map(post -> toPostResponse(post, viewerId))
                .toList();
        return new PagedResponse<>(content, page, size, postPage.getTotalElements(), postPage.getTotalPages(), postPage.isLast());
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getTopicFeed(Long topicId, int page, int size, Long viewerId) {
        topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
        Set<Long> followeeIds = viewerId != null ? privacyService.loadFolloweeIds(viewerId) : Collections.emptySet();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = postRepository.findByTopicIdOrderByCreatedAtDesc(topicId, pageable);
        List<PostResponse> content = postPage.getContent().stream()
                .filter(post -> privacyService.canViewPost(post, viewerId, followeeIds))
                .map(post -> toPostResponse(post, viewerId))
                .toList();
        return new PagedResponse<>(content, page, size, postPage.getTotalElements(), postPage.getTotalPages(), postPage.isLast());
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getHotspotRanking(int size, Long viewerId) {
        int limit = Math.max(1, Math.min(size, 20));
        Set<Long> followeeIds = viewerId != null ? privacyService.loadFolloweeIds(viewerId) : Collections.emptySet();
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("heat"), Sort.Order.desc("updatedAt")));
        Page<Post> postPage = postRepository.findByHeatGreaterThanEqual(HOTSPOT_THRESHOLD, pageable);
        return postPage.getContent().stream()
                .filter(post -> privacyService.canViewPost(post, viewerId, followeeIds))
                .map(post -> toPostResponse(post, viewerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getRecommendations(int size, Long viewerId, List<Long> excludedIds) {
        int limit = Math.max(1, Math.min(size, MAX_RECOMMENDATION_SIZE));
        Set<Long> followeeIds = viewerId != null ? privacyService.loadFolloweeIds(viewerId) : Collections.emptySet();
        LinkedHashSet<Long> seen = sanitizeExcludeIds(excludedIds);
        List<PostResponse> recommendations = new ArrayList<>(limit);
        int candidateSize = Math.max(limit * 4, 60);

        collectRecommendations(recommendations, seen, followeeIds, viewerId,
                PageRequest.of(0, candidateSize, Sort.by(Sort.Order.desc("heat"), Sort.Order.desc("updatedAt"))),
                limit);

        if (recommendations.size() < limit) {
            collectRecommendations(recommendations, seen, followeeIds, viewerId,
                    PageRequest.of(0, candidateSize, Sort.by(Sort.Order.desc("createdAt"))),
                    limit);
        }

        if (recommendations.size() < limit) {
            collectRecommendations(recommendations, seen, followeeIds, viewerId,
                    PageRequest.of(0, candidateSize, Sort.by(Sort.Order.desc("id"))),
                    limit);
        }

        if (recommendations.size() > limit) {
            return List.copyOf(recommendations.subList(0, limit));
        }
        return List.copyOf(recommendations);
    }

    @Transactional
    public PostResponse updateHeat(Long postId, long heat, Long viewerId) {
        long targetHeat = Math.max(0, heat);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        long current = post.getHeat();
        if (current != targetHeat) {
            post.setHeat(targetHeat);
            adjustTopicHeat(post.getTopic(), targetHeat - current);
        }
        return toPostResponse(post, viewerId);
    }

    @Transactional
    public void likePost(Long userId, Long postId) {
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            return;
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        privacyService.assertCanInteractWithPost(post, userId);
        User user = userService.getById(userId);
        PostLike like = new PostLike();
        like.setPost(post);
        like.setUser(user);
        postLikeRepository.save(like);
        adjustHeat(post, LIKE_HEAT_WEIGHT);
    }

    @Transactional
    public void unlikePost(Long userId, Long postId) {
        postLikeRepository.findByPostIdAndUserId(postId, userId)
                .ifPresent(like -> {
                    Post post = like.getPost();
                    privacyService.assertCanInteractWithPost(post, userId);
                    postLikeRepository.delete(like);
                    adjustHeat(post, -LIKE_HEAT_WEIGHT);
                });
    }

    private void collectRecommendations(List<PostResponse> sink,
                                        LinkedHashSet<Long> seen,
                                        Set<Long> followeeIds,
                                        Long viewerId,
                                        Pageable pageable,
                                        int limit) {
        if (sink.size() >= limit) {
            return;
        }
        Page<Post> page = postRepository.findAll(pageable);
        for (Post post : page.getContent()) {
            if (post == null || post.getId() == null) {
                continue;
            }
            if (sink.size() >= limit) {
                break;
            }
            Long postId = post.getId();
            if (seen.contains(postId)) {
                continue;
            }
            if (viewerId != null) {
                User author = post.getAuthor();
                if (author != null && Objects.equals(author.getId(), viewerId)) {
                    continue;
                }
            }
            if (!privacyService.canViewPost(post, viewerId, followeeIds)) {
                continue;
            }
            sink.add(toPostResponse(post, viewerId));
            rememberSeen(seen, postId);
        }
    }

    private void rememberSeen(LinkedHashSet<Long> seen, Long id) {
        if (id == null || seen.contains(id)) {
            return;
        }
        if (seen.size() >= MAX_EXCLUDE_SIZE) {
            Iterator<Long> iterator = seen.iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        seen.add(id);
    }

    private LinkedHashSet<Long> sanitizeExcludeIds(List<Long> values) {
        LinkedHashSet<Long> sanitized = new LinkedHashSet<>();
        if (values == null) {
            return sanitized;
        }
        for (Long value : values) {
            if (value == null) {
                continue;
            }
            sanitized.add(value);
            if (sanitized.size() >= MAX_EXCLUDE_SIZE) {
                break;
            }
        }
        return sanitized;
    }

    private void applyVisibility(Post post, User author, PostVisibility requestedVisibility, List<Long> allowedUserIds) {
        PostVisibility effective = requestedVisibility != null ? requestedVisibility : PostVisibility.fromPrivacySetting(author.getPrivacySetting());
        post.setVisibility(effective);
        if (effective == PostVisibility.CUSTOM) {
            if (allowedUserIds == null || allowedUserIds.isEmpty()) {
                throw new BadRequestException("请至少选择一位可见用户");
            }
            Set<Long> sanitized = allowedUserIds.stream()
                    .filter(Objects::nonNull)
                    .map(Long::valueOf)
                    .collect(Collectors.toCollection(HashSet::new));
            if (sanitized.isEmpty()) {
                throw new BadRequestException("请至少选择一位可见用户");
            }
            if (sanitized.size() > 20) {
                throw new BadRequestException("最多只能指定20位可见用户");
            }
            post.setAllowedUserIds(new HashSet<>(sanitized));
        } else {
            post.setAllowedUserIds(new HashSet<>());
        }
    }

    private List<PostResponse> postsFromIds(List<Long> ids, Long viewerId, Set<Long> followeeIds) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Post> posts = postRepository.findAllById(ids);
        Map<Long, Post> byId = posts.stream().collect(Collectors.toMap(Post::getId, Function.identity()));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .filter(post -> privacyService.canViewPost(post, viewerId, followeeIds))
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .map(post -> toPostResponse(post, viewerId))
                .toList();
    }

    private PostResponse toPostResponse(Post post, Long viewerId) {
        long likeCount = postLikeRepository.countByPostId(post.getId());
        long commentCount = commentRepository.countByPostId(post.getId());
        boolean liked = viewerId != null && postLikeRepository.existsByPostIdAndUserId(post.getId(), viewerId);
        UserSummaryDto author = UserMapper.toSummary(post.getAuthor());
        List<String> media = post.getMediaUrls() == null ? List.of() : List.copyOf(post.getMediaUrls());
        Topic topic = post.getTopic();
        TopicLightDto topicDto = topic == null ? null : new TopicLightDto(topic.getId(), topic.getName());
        boolean ownedByCurrentUser = viewerId != null && Objects.equals(post.getAuthor().getId(), viewerId);
        List<Long> allowedUserIds = ownedByCurrentUser
                ? (post.getAllowedUserIds() == null ? List.of() : new ArrayList<>(post.getAllowedUserIds()))
                : List.of();
        return new PostResponse(
                post.getId(),
                post.getContent(),
                media,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                author,
                topicDto,
                likeCount,
                commentCount,
                post.getHeat(),
                post.getHeat() >= HOTSPOT_THRESHOLD,
                liked,
                ownedByCurrentUser,
                post.getVisibility(),
                allowedUserIds
        );
    }

    private void adjustHeat(Post post, long delta) {
        long current = post.getHeat();
        long updated = current + delta;
        if (updated < 0) {
            updated = 0;
        }
        long appliedDelta = updated - current;
        if (appliedDelta == 0) {
            return;
        }
        post.setHeat(updated);
        adjustTopicHeat(post.getTopic(), appliedDelta);
    }

    private void adjustTopicHeat(Topic topic, long delta) {
        if (topic == null || delta == 0) {
            return;
        }
        long updated = topic.getHeat() + delta;
        if (updated < 0) {
            updated = 0;
        }
        topic.setHeat(updated);
    }
}
