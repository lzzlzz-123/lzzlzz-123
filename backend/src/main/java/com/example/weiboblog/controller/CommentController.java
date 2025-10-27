package com.example.weiboblog.controller;

import com.example.weiboblog.dto.CommentCreateRequest;
import com.example.weiboblog.dto.CommentResponse;
import com.example.weiboblog.security.UserPrincipal;
import com.example.weiboblog.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> list(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.listComments(postId));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(@PathVariable Long postId,
                                                  @AuthenticationPrincipal UserPrincipal currentUser,
                                                  @Valid @RequestBody CommentCreateRequest request) {
        return ResponseEntity.ok(commentService.createComment(currentUser.getId(), postId, request));
    }
}
