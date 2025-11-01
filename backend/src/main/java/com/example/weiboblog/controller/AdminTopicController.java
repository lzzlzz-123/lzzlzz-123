package com.example.weiboblog.controller;

import com.example.weiboblog.dto.AdminTopicCreateRequest;
import com.example.weiboblog.dto.TopicHeatUpdateRequest;
import com.example.weiboblog.dto.TopicResponse;
import com.example.weiboblog.security.UserPrincipal;
import com.example.weiboblog.service.TopicService;
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
@RequestMapping("/api/admin/topics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTopicController {

    private final TopicService topicService;

    public AdminTopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @PostMapping
    public ResponseEntity<TopicResponse> createHotTopic(@AuthenticationPrincipal UserPrincipal currentUser,
                                                        @Valid @RequestBody AdminTopicCreateRequest request) {
        return ResponseEntity.ok(topicService.createTopicByAdmin(currentUser.getId(), request));
    }

    @PutMapping("/{topicId}/heat")
    public ResponseEntity<TopicResponse> updateTopicHeat(@PathVariable Long topicId,
                                                         @Valid @RequestBody TopicHeatUpdateRequest request,
                                                         @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(topicService.updateTopicHeat(topicId, request.heat(), currentUser.getId()));
    }
}
