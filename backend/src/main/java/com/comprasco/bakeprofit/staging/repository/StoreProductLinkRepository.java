package com.comprasco.bakeprofit.staging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.comprasco.bakeprofit.staging.entity.StoreProductLink;

public interface StoreProductLinkRepository extends JpaRepository<StoreProductLink, Long> {
    java.util.Optional<StoreProductLink> findByStoreIdAndStoreSku(Long storeId, String storeSku);
}
