package com.wida3.listings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record CreateListingRequest(
        @NotBlank String title,
        @NotBlank String city,
        @NotBlank String address,
        @NotBlank String warehouseType,
        @Positive BigDecimal sizeSqm,
        @Positive BigDecimal weeklyPrice,
        List<String> photoUrls) {

    public CreateListingRequest {
        photoUrls = photoUrls == null ? List.of() : photoUrls;
    }
}
