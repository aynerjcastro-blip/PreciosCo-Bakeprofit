package com.comprasco.bakeprofit.staging.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comprasco.bakeprofit.staging.entity.ScrapeTarget;
import com.comprasco.bakeprofit.store.entity.Store;

public interface ScrapeTargetRepository extends JpaRepository<ScrapeTarget, Long> {

    List<ScrapeTarget> findByStoreAndActiveTrue(Store store);
}