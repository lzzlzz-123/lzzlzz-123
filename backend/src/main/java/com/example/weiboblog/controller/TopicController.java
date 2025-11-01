package com.example.weiboblog.controller;

import com.example.weiboblog.dto.PagedResponse;
import com.example.weiboblog.dto.PostResponse;
import com.example.weiboblog.dto.TopicCreateRequest;
import com.example.weiboblog.dto.TopicResponse;
import com.example.weiboblog.dto.TopicSummaryDto;
import com.example.weiboblog.security.UserPrincipal;
import com.example.weiboblog.service.PostService;
import com.example.weiboblog.service.TopicService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;
    private final PostService postService;

    public TopicController(TopicService topicService, PostService postService) {
        this.topicService = topicService;
        this.postService = postService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TopicResponse> createTopic(@AuthenticationPrincipal UserPrincipal currentUser,
                                                     @Valid @RequestBody TopicCreateRequest request) {
        return ResponseEntity.ok(topicService.createTopic(currentUser.getId(), request));
    }

    @PostMapping("/{topicId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TopicResponse> joinTopic(@PathVariable Long topicId,
                                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(topicService.joinTopic(currentUser.getId(), topicId));
    }

    @DeleteMapping("/{topicId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> leaveTopic(@PathVariable Long topicId,
                                           @AuthenticationPrincipal UserPrincipal currentUser) {
        topicService.leaveTopic(currentUser.getId(), topicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TopicSummaryDto>> myTopics(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(topicService.getMyTopics(currentUser.getId()));
    }

    @GetMapping("/rankings")
    public ResponseEntity<List<TopicSummaryDto>> rankings(@AuthenticationPrincipal UserPrincipal currentUser,
                                                          @RequestParam(defaultValue = "20") int size) {
        Long viewerId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(topicService.getTopicRankings(size, viewerId));
    }

    @GetMapping("/{topicId}")
    public ResponseEntity<TopicResponse> getTopic(@PathVariable Long topicId,
                                                  @AuthenticationPrincipal UserPrincipal currentUser) {
        Long viewerId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(topicService.getTopic(topicId, viewerId));
    }

    @GetMapping("/{topicId}/posts")
    public ResponseEntity<PagedResponse<PostResponse>> getTopicPosts(@PathVariable Long topicId,
                                                                     @AuthenticationPrincipal UserPrincipal currentUser,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        Long viewerId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(postService.getTopicFeed(topicId, page, size, viewerId));
    }
}
