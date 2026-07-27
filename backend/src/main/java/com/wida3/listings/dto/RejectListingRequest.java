package com.wida3.listings.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectListingRequest(@NotBlank String reason) {
}
