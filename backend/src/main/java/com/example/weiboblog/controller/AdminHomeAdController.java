package com.example.weiboblog.controller;

import com.example.weiboblog.dto.HomeAdRequest;
import com.example.weiboblog.dto.HomeAdResponse;
import com.example.weiboblog.service.HomeAdService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/home-ads")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHomeAdController {

    private final HomeAdService homeAdService;

    public AdminHomeAdController(HomeAdService homeAdService) {
        this.homeAdService = homeAdService;
    }

    @GetMapping
    public ResponseEntity<List<HomeAdResponse>> listAds() {
        return ResponseEntity.ok(homeAdService.getAllAds());
    }

    @PostMapping
    public ResponseEntity<HomeAdResponse> createAd(@Valid @RequestBody HomeAdRequest request) {
        return ResponseEntity.ok(homeAdService.create(request));
    }

    @PutMapping("/{adId}")
    public ResponseEntity<HomeAdResponse> updateAd(@PathVariable Long adId,
                                                   @Valid @RequestBody HomeAdRequest request) {
        return ResponseEntity.ok(homeAdService.update(adId, request));
    }

    @DeleteMapping("/{adId}")
    public ResponseEntity<Void> deleteAd(@PathVariable Long adId) {
        homeAdService.delete(adId);
        return ResponseEntity.noContent().build();
    }
}
