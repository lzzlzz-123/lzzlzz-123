package com.example.weiboblog.repository;

import com.example.weiboblog.domain.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    Optional<Topic> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    Page<Topic> findAllByOrderByHeatDescUpdatedAtDesc(Pageable pageable);
}
