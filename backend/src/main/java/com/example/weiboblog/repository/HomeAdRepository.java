package com.example.weiboblog.repository;

import com.example.weiboblog.domain.HomeAd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeAdRepository extends JpaRepository<HomeAd, Long> {
    List<HomeAd> findByActiveTrueOrderByDisplayOrderAscUpdatedAtDesc();
    List<HomeAd> findAllByOrderByDisplayOrderAscUpdatedAtDesc();
}
