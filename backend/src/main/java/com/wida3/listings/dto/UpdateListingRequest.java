package com.wida3.listings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateListingRequest(
        @NotBlank String title,
        @NotBlank String city,
        @NotBlank String address,
        @NotBlank String warehouseType,
        @Positive BigDecimal sizeSqm,
        @Positive BigDecimal weeklyPrice) {
}
