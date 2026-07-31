package com.wida3.listings.controller;

import com.wida3.listings.dto.CreateListingRequest;
import com.wida3.listings.dto.ListingResponse;
import com.wida3.listings.dto.RejectListingRequest;
import com.wida3.listings.dto.UpdateListingRequest;
import com.wida3.listings.service.ListingService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ListingResponse>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String warehouseType,
            @RequestParam(required = false) BigDecimal minSizeSqm,
            @RequestParam(required = false) BigDecimal maxSizeSqm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableUntil) {
        return ResponseEntity.ok(
                listingService.search(city, warehouseType, minSizeSqm, maxSizeSqm, availableFrom, availableUntil));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ResponseEntity<ListingResponse> create(
            @Valid @RequestBody CreateListingRequest request, Authentication authentication) {
        ListingResponse response = listingService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/mine")
    public ResponseEntity<List<ListingResponse>> mine(Authentication authentication) {
        return ResponseEntity.ok(listingService.mine(authentication.getName()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateListingRequest request, Authentication authentication) {
        return ResponseEntity.ok(listingService.update(authentication.getName(), id, request));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ListingResponse> deactivate(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(listingService.deactivate(authentication.getName(), id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<ListingResponse>> pending() {
        return ResponseEntity.ok(listingService.pending());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ListingResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(listingService.approve(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ListingResponse> reject(@PathVariable UUID id, @Valid @RequestBody RejectListingRequest request) {
        return ResponseEntity.ok(listingService.reject(id, request.reason()));
    }
}
