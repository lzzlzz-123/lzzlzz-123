package com.example.weiboblog.controller;

import com.example.weiboblog.dto.HomeAdResponse;
import com.example.weiboblog.service.HomeAdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home-ads")
public class HomeAdController {

    private final HomeAdService homeAdService;

    public HomeAdController(HomeAdService homeAdService) {
        this.homeAdService = homeAdService;
    }

    @GetMapping
    public ResponseEntity<List<HomeAdResponse>> getActiveAds() {
        return ResponseEntity.ok(homeAdService.getActiveAds());
    }
}
