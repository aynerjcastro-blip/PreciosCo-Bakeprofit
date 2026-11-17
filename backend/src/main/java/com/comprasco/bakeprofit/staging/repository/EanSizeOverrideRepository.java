package com.comprasco.bakeprofit.staging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.comprasco.bakeprofit.staging.entity.EanSizeOverride;

public interface EanSizeOverrideRepository extends JpaRepository<EanSizeOverride, String> {
}
