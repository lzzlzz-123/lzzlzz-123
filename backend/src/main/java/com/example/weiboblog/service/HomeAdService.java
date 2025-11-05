package com.example.weiboblog.service;

import com.example.weiboblog.domain.HomeAd;
import com.example.weiboblog.dto.HomeAdRequest;
import com.example.weiboblog.dto.HomeAdResponse;
import com.example.weiboblog.exception.BadRequestException;
import com.example.weiboblog.exception.ResourceNotFoundException;
import com.example.weiboblog.repository.HomeAdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class HomeAdService {

    private static final int MAX_DISPLAY_ORDER = 10_000;

    private final HomeAdRepository homeAdRepository;

    public HomeAdService(HomeAdRepository homeAdRepository) {
        this.homeAdRepository = homeAdRepository;
    }

    @Transactional(readOnly = true)
    public List<HomeAdResponse> getActiveAds() {
        return homeAdRepository.findByActiveTrueOrderByDisplayOrderAscUpdatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomeAdResponse> getAllAds() {
        return homeAdRepository.findAllByOrderByDisplayOrderAscUpdatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HomeAdResponse create(HomeAdRequest request) {
        HomeAd ad = new HomeAd();
        applyRequest(ad, request);
        return toResponse(homeAdRepository.save(ad));
    }

    @Transactional
    public HomeAdResponse update(Long id, HomeAdRequest request) {
        HomeAd ad = homeAdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("广告不存在: " + id));
        applyRequest(ad, request);
        return toResponse(ad);
    }

    @Transactional
    public void delete(Long id) {
        HomeAd ad = homeAdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("广告不存在: " + id));
        homeAdRepository.delete(ad);
    }

    private void applyRequest(HomeAd ad, HomeAdRequest request) {
        String title = safeTrim(request.title());
        String imageUrl = safeTrim(request.imageUrl());
        String targetUrl = safeTrim(request.targetUrl());
        if (title.isEmpty()) {
            throw new BadRequestException("广告标题不能为空");
        }
        if (imageUrl.isEmpty()) {
            throw new BadRequestException("广告图片地址不能为空");
        }
        if (targetUrl.isEmpty()) {
            throw new BadRequestException("广告跳转链接不能为空");
        }
        ad.setTitle(title);
        ad.setImageUrl(imageUrl);
        ad.setTargetUrl(targetUrl);
        ad.setDisplayOrder(normalizeDisplayOrder(request.displayOrder()));
        ad.setActive(resolveActive(request.active()));
    }

    private int normalizeDisplayOrder(Integer value) {
        if (value == null) {
            return 0;
        }
        int normalized = Math.max(0, value);
        if (normalized > MAX_DISPLAY_ORDER) {
            normalized = MAX_DISPLAY_ORDER;
        }
        return normalized;
    }

    private boolean resolveActive(Boolean active) {
        return active == null || Boolean.TRUE.equals(active);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private HomeAdResponse toResponse(HomeAd ad) {
        Objects.requireNonNull(ad, "广告不能为空");
        return new HomeAdResponse(
                ad.getId(),
                ad.getTitle(),
                ad.getImageUrl(),
                ad.getTargetUrl(),
                ad.getDisplayOrder(),
                ad.isActive(),
                ad.getCreatedAt(),
                ad.getUpdatedAt()
        );
    }
}
