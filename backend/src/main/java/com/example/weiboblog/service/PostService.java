package com.example.weiboblog.service;

import com.example.weiboblog.domain.Post;
import com.example.weiboblog.domain.PostLike;
import com.example.weiboblog.domain.User;
import com.example.weiboblog.dto.PagedResponse;
import com.example.weiboblog.dto.PostCreateRequest;
import com.example.weiboblog.dto.PostResponse;
import com.example.weiboblog.dto.UserSummaryDto;
import com.example.weiboblog.exception.BadRequestException;
import com.example.weiboblog.exception.ResourceNotFoundException;
import com.example.weiboblog.repository.CommentRepository;
import com.example.weiboblog.repository.PostLikeRepository;
import com.example.weiboblog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final long HOTSPOT_THRESHOLD = 10L;
    private static final long LIKE_HEAT_WEIGHT = 1L;
    private static final long COMMENT_HEAT_WEIGHT = 2L;

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final TimelineService timelineService;
    private final UserService userService;

    public PostService(PostRepository postRepository,
                       PostLikeRepository postLikeRepository,
                       CommentRepository commentRepository,
                       TimelineService timelineService,
                       UserService userService) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository = commentRepository;
        this.timelineService = timelineService;
        this.userService = userService;
    }

    @Transactional
    public PostResponse createPost(Long authorId, PostCreateRequest request) {
        if (request.mediaUrls() != null && request.mediaUrls().stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("Media URLs cannot be null");
        }
        User author = userService.getById(authorId);
        Post post = new Post();
        post.setAuthor(author);
        post.setContent(request.content());
        post.setMediaUrls(request.mediaUrls() == null ? new ArrayList<>() : new ArrayList<>(request.mediaUrls()));
        post.setHeat(0L);
        Post saved = postRepository.save(post);
        timelineService.cachePost(saved.getId(), author.getId(), saved.getCreatedAt());
        return toPostResponse(saved, authorId);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId, Long viewerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        return toPostResponse(post, viewerId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getGlobalFeed(int page, int size, Long viewerId) {
        List<Long> cachedIds = timelineService.loadGlobalTimelineIds(page, size);
        if (cachedIds.isEmpty()) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);
            List<PostResponse> content = postPage.getContent().stream()
                    .map(post -> toPostResponse(post, viewerId))
                    .toList();
            return new PagedResponse<>(content, page, size, postPage.getTotalElements(), postPage.getTotalPages(), postPage.isLast());
        }
        List<PostResponse> responses = postsFromIds(cachedIds, viewerId);
        long total = Math.max(postRepository.count(), cachedIds.size());
        int totalPages = (int) Math.ceil((double) total / size);
        boolean last = page + 1 >= totalPages;
        return new PagedResponse<>(responses, page, size, total, totalPages, last);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getUserFeed(Long userId, int page, int size, Long viewerId) {
        List<Long> cachedIds = timelineService.loadUserTimelineIds(userId, page, size);
        if (cachedIds.isEmpty()) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Post> postPage = postRepository.findAllByAuthorIdInOrderByCreatedAtDesc(List.of(userId), pageable);
            List<PostResponse> content = postPage.getContent().stream()
                    .map(post -> toPostResponse(post, viewerId))
                    .toList();
            return new PagedResponse<>(content, page, size, postPage.getTotalElements(), postPage.getTotalPages(), postPage.isLast());
        }
        List<PostResponse> responses = postsFromIds(cachedIds, viewerId);
        long total = postRepository.countByAuthorId(userId);
        int totalPages = (int) Math.ceil((double) Math.max(total, cachedIds.size()) / size);
        boolean last = page + 1 >= totalPages;
        return new PagedResponse<>(responses, page, size, total, totalPages, last);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getHotspotFeed(int page, int size, Long viewerId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("heat"), Sort.Order.desc("updatedAt")));
        Page<Post> postPage = postRepository.findByHeatGreaterThanEqual(HOTSPOT_THRESHOLD, pageable);
        List<PostResponse> content = postPage.getContent().stream()
                .map(post -> toPostResponse(post, viewerId))
                .toList();
        return new PagedResponse<>(content, page, size, postPage.getTotalElements(), postPage.getTotalPages(), postPage.isLast());
    }

    @Transactional
    public void likePost(Long userId, Long postId) {
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            return;
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
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
                    postLikeRepository.delete(like);
                    adjustHeat(post, -LIKE_HEAT_WEIGHT);
                });
    }

    private List<PostResponse> postsFromIds(List<Long> ids, Long viewerId) {
        List<Post> posts = postRepository.findAllById(ids);
        Map<Long, Post> byId = posts.stream().collect(Collectors.toMap(Post::getId, Function.identity()));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
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
        return new PostResponse(
                post.getId(),
                post.getContent(),
                media,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                author,
                likeCount,
                commentCount,
                post.getHeat(),
                post.getHeat() >= HOTSPOT_THRESHOLD,
                liked
        );
    }

    private void adjustHeat(Post post, long delta) {
        long updated = post.getHeat() + delta;
        if (updated < 0) {
            updated = 0;
        }
        post.setHeat(updated);
    }
}
