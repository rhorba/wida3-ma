package com.wida3.listings.service;

import com.wida3.auth.entity.User;
import com.wida3.auth.repository.UserRepository;
import com.wida3.listings.dto.CreateListingRequest;
import com.wida3.listings.dto.ListingResponse;
import com.wida3.listings.entity.Listing;
import com.wida3.listings.entity.ListingPhoto;
import com.wida3.listings.entity.WarehouseType;
import com.wida3.listings.exception.InvalidPhotoUrlException;
import com.wida3.listings.exception.InvalidWarehouseTypeException;
import com.wida3.listings.exception.TooManyPhotosException;
import com.wida3.listings.repository.ListingRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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
                photoUrls);
    }
}
