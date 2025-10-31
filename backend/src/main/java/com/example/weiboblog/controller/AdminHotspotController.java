package com.example.weiboblog.controller;

import com.example.weiboblog.dto.HotspotPostCreateRequest;
import com.example.weiboblog.dto.PostHeatUpdateRequest;
import com.example.weiboblog.dto.PostResponse;
import com.example.weiboblog.security.UserPrincipal;
import com.example.weiboblog.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/hotspot")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHotspotController {

    private final PostService postService;

    public AdminHotspotController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/posts")
    public ResponseEntity<PostResponse> createHotspotPost(@AuthenticationPrincipal UserPrincipal currentUser,
                                                          @Valid @RequestBody HotspotPostCreateRequest request) {
        return ResponseEntity.ok(postService.createHotspotPost(currentUser.getId(), request));
    }

    @PutMapping("/posts/{postId}/heat")
    public ResponseEntity<PostResponse> updatePostHeat(@PathVariable Long postId,
                                                       @Valid @RequestBody PostHeatUpdateRequest request,
                                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(postService.updateHeat(postId, request.heat(), currentUser.getId()));
    }
}
