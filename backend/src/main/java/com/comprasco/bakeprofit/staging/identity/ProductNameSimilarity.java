package com.comprasco.bakeprofit.staging.identity;

public interface ProductNameSimilarity {
    double score(String offerName, String productName);
}
