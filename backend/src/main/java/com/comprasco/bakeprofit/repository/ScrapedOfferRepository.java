package com.comprasco.bakeprofit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comprasco.bakeprofit.entity.OfferStatus;
import com.comprasco.bakeprofit.entity.ScrapedOffer;

public interface ScrapedOfferRepository extends JpaRepository<ScrapedOffer, Long> {

    List<ScrapedOffer> findByStatus(OfferStatus status);
}