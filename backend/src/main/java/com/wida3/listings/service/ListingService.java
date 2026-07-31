package com.wida3.listings.service;

import com.wida3.auth.entity.User;
import com.wida3.auth.repository.UserRepository;
import com.wida3.listings.dto.CreateListingRequest;
import com.wida3.listings.dto.ListingResponse;
import com.wida3.listings.dto.UpdateListingRequest;
import com.wida3.listings.entity.Listing;
import com.wida3.listings.entity.ListingPhoto;
import com.wida3.listings.entity.ListingStatus;
import com.wida3.listings.entity.WarehouseType;
import com.wida3.listings.exception.InvalidPhotoUrlException;
import com.wida3.listings.exception.InvalidWarehouseTypeException;
import com.wida3.listings.exception.ListingNotFoundException;
import com.wida3.listings.exception.TooManyPhotosException;
import com.wida3.listings.repository.ListingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final int maxPhotosPerListing;

    public ListingService(
            ListingRepository listingRepository,
            UserRepository userRepository,
            @Value("${app.file-storage.max-photos-per-listing}") int maxPhotosPerListing) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.maxPhotosPerListing = maxPhotosPerListing;
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> search(
            String city,
            String warehouseType,
            BigDecimal minSizeSqm,
            BigDecimal maxSizeSqm,
            LocalDate availableFrom,
            LocalDate availableUntil) {
        WarehouseType type = warehouseType == null ? null : parseWarehouseType(warehouseType);
        // Only apply the availability filter when both ends of the range are given and ordered --
        // an incomplete or inverted range is treated as "no availability filter" rather than an error,
        // matching how the other search filters are already silently optional.
        boolean filterByAvailability =
                availableFrom != null && availableUntil != null && availableFrom.isBefore(availableUntil);
        // When the filter is off, Postgres still needs non-null values bound for these positions
        // (see ListingRepository.search) -- any dates work since the NOT EXISTS branch is unreachable.
        LocalDate from = filterByAvailability ? availableFrom : LocalDate.EPOCH;
        LocalDate until = filterByAvailability ? availableUntil : LocalDate.EPOCH;
        return listingRepository.search(city, type, minSizeSqm, maxSizeSqm, filterByAvailability, from, until).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ListingResponse create(String ownerEmail, CreateListingRequest request) {
        if (request.photoUrls().size() > maxPhotosPerListing) {
            throw new TooManyPhotosException(maxPhotosPerListing);
        }
        for (String url : request.photoUrls()) {
            if (!url.startsWith("/uploads/")) {
                throw new InvalidPhotoUrlException(url);
            }
        }

        WarehouseType warehouseType = parseWarehouseType(request.warehouseType());

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + ownerEmail));

        Listing listing = new Listing(
                owner,
                request.title(),
                request.city(),
                request.address(),
                warehouseType,
                request.sizeSqm(),
                request.weeklyPrice());

        short sortOrder = 0;
        for (String url : request.photoUrls()) {
            listing.addPhoto(new ListingPhoto(url, sortOrder++));
        }

        listingRepository.save(listing);
        return toResponse(listing);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> mine(String ownerEmail) {
        return listingRepository.findByOwnerEmailOrderByCreatedAtDesc(ownerEmail).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> pending() {
        return listingRepository.findByStatusOrderByCreatedAtAsc(ListingStatus.PENDING_APPROVAL).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ListingResponse approve(UUID id) {
        Listing listing = findOrThrow(id);
        listing.approve();
        return toResponse(listing);
    }

    @Transactional
    public ListingResponse reject(UUID id, String reason) {
        Listing listing = findOrThrow(id);
        listing.reject(reason);
        return toResponse(listing);
    }

    @Transactional
    public ListingResponse update(String ownerEmail, UUID id, UpdateListingRequest request) {
        Listing listing = findOrThrow(id);
        requireOwner(listing, ownerEmail);
        WarehouseType warehouseType = parseWarehouseType(request.warehouseType());
        listing.update(
                request.title(), request.city(), request.address(), warehouseType, request.sizeSqm(), request.weeklyPrice());
        return toResponse(listing);
    }

    @Transactional
    public ListingResponse deactivate(String ownerEmail, UUID id) {
        Listing listing = findOrThrow(id);
        requireOwner(listing, ownerEmail);
        listing.deactivate();
        return toResponse(listing);
    }

    private void requireOwner(Listing listing, String requesterEmail) {
        if (!listing.getOwner().getEmail().equals(requesterEmail)) {
            throw new AccessDeniedException("You do not have permission to modify this listing");
        }
    }

    private Listing findOrThrow(UUID id) {
        return listingRepository.findById(id).orElseThrow(() -> new ListingNotFoundException(id));
    }

    private WarehouseType parseWarehouseType(String value) {
        try {
            return WarehouseType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidWarehouseTypeException(value);
        }
    }

    private ListingResponse toResponse(Listing listing) {
        List<String> photoUrls = listing.getPhotos().stream().map(ListingPhoto::getFileUrl).toList();
        return new ListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getCity(),
                listing.getAddress(),
                listing.getWarehouseType().name(),
                listing.getSizeSqm(),
                listing.getWeeklyPrice(),
                listing.getStatus().name(),
                photoUrls,
                listing.getRejectionReason());
    }
}
