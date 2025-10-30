package com.example.weiboblog.service;

import com.example.weiboblog.domain.Comment;
import com.example.weiboblog.domain.Post;
import com.example.weiboblog.domain.User;
import com.example.weiboblog.dto.CommentCreateRequest;
import com.example.weiboblog.dto.CommentResponse;
import com.example.weiboblog.exception.ResourceNotFoundException;
import com.example.weiboblog.repository.CommentRepository;
import com.example.weiboblog.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private static final long COMMENT_HEAT_WEIGHT = 2L;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserService userService;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserService userService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userService = userService;
    }

    @Transactional
    public CommentResponse createComment(Long userId, Long postId, CommentCreateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        User author = userService.getById(userId);
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(request.content());
        Comment saved = commentRepository.save(comment);
        post.setHeat(post.getHeat() + COMMENT_HEAT_WEIGHT);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                UserMapper.toSummary(comment.getAuthor())
        );
    }
}
