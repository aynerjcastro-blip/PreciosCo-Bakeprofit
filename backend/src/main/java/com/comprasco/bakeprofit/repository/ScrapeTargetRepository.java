package com.comprasco.bakeprofit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comprasco.bakeprofit.entity.ScrapeTarget;
import com.comprasco.bakeprofit.entity.Store;

public interface ScrapeTargetRepository extends JpaRepository<ScrapeTarget, Long> {

    List<ScrapeTarget> findByStoreAndActiveTrue(Store store);
}