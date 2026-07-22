package com.wida3.listings.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        String title,
        String city,
        String address,
        String warehouseType,
        BigDecimal sizeSqm,
        BigDecimal weeklyPrice,
        String status,
        List<String> photoUrls) {
}
