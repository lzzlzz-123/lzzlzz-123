package com.example.weiboblog.controller;

import com.example.weiboblog.domain.User;
import com.example.weiboblog.dto.UserProfileResponse;
import com.example.weiboblog.dto.UserSummaryDto;
import com.example.weiboblog.security.UserPrincipal;
import com.example.weiboblog.service.UserMapper;
import com.example.weiboblog.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryDto> currentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userService.getById(currentUser.getId());
        return ResponseEntity.ok(UserMapper.toSummary(user));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> profile(@PathVariable Long userId,
                                                        @AuthenticationPrincipal UserPrincipal currentUser) {
        Long viewerId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(userService.getProfile(userId, viewerId));
    }

    @PostMapping("/{userId}/follow")
    public ResponseEntity<Void> follow(@PathVariable Long userId,
                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        userService.follow(currentUser.getId(), userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable Long userId,
                                         @AuthenticationPrincipal UserPrincipal currentUser) {
        userService.unfollow(currentUser.getId(), userId);
        return ResponseEntity.noContent().build();
    }
}
