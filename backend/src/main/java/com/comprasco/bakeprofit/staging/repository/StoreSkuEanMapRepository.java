package com.comprasco.bakeprofit.staging.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.comprasco.bakeprofit.staging.entity.StoreSkuEanMap;

public interface StoreSkuEanMapRepository extends JpaRepository<StoreSkuEanMap, Long> {
    Optional<StoreSkuEanMap> findByStoreIdAndStoreSku(Long storeId, String storeSku);
}
