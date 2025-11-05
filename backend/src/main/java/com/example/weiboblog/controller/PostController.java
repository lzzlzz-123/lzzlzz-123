package com.example.weiboblog.controller;

import com.example.weiboblog.dto.PagedResponse;
import com.example.weiboblog.dto.PostCreateRequest;
import com.example.weiboblog.dto.PostResponse;
import com.example.weiboblog.dto.PostUpdateRequest;
import com.example.weiboblog.security.UserPrincipal;
import com.example.weiboblog.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/feed")
    public ResponseEntity<PagedResponse<PostResponse>> globalFeed(@AuthenticationPrincipal UserPrincipal currentUser,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        Long viewerId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(postService.getGlobalFeed(page, size, viewerId));
    }

    @GetMapping("/hotspot")
    public ResponseEntity<PagedResponse<PostResponse>> hotspotFeed(@AuthenticationPrincipal UserPrincipal currentUser,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        Long viewerId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(postService.getHotspotFeed(page, size, viewerId));
    }

    @GetMapping("/hotspot/ranking")
    public ResponseEntity<List<PostResponse>> hotspotRanking(@AuthenticationPrincipal UserPrincipal currentUser,
                                                              @RequestParam(defaultValue = "20") int size) {
        Long viewerId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(postService.getHotspotRanking(size, viewerId));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<PostResponse>> recommendations(@AuthenticationPrincipal UserPrincipal currentUser,
                                                               @RequestParam(defaultValue = "3") int size,
                                                               @RequestParam(value = "exclude", required = false) List<Long> exclude) {
        Long viewerId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(postService.getRecommendations(size, viewerId, exclude));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResponse<PostResponse>> userFeed(@PathVariable Long userId,
                                                                 @AuthenticationPrincipal UserPrincipal currentUser,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        Long viewerId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(postService.getUserFeed(userId, page, size, viewerId));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long postId,
                                                 @AuthenticationPrincipal UserPrincipal currentUser) {
        Long viewerId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(postService.getPost(postId, viewerId));
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@AuthenticationPrincipal UserPrincipal currentUser,
                                                    @Valid @RequestBody PostCreateRequest request) {
        return ResponseEntity.ok(postService.createPost(currentUser.getId(), request));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long postId,
                                                    @AuthenticationPrincipal UserPrincipal currentUser,
                                                    @Valid @RequestBody PostUpdateRequest request) {
        return ResponseEntity.ok(postService.updatePost(currentUser.getId(), postId, request));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal UserPrincipal currentUser) {
        postService.deletePost(currentUser.getId(), postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long postId,
                                         @AuthenticationPrincipal UserPrincipal currentUser) {
        postService.likePost(currentUser.getId(), postId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<Void> unlikePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal UserPrincipal currentUser) {
        postService.unlikePost(currentUser.getId(), postId);
        return ResponseEntity.noContent().build();
    }
}
