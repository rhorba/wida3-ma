package com.wida3.listings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "listing_photos")
public class ListingPhoto {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ListingPhoto() {
    }

    public ListingPhoto(String fileUrl, short sortOrder) {
        this.fileUrl = fileUrl;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    void setListing(Listing listing) {
        this.listing = listing;
    }

    public UUID getId() {
        return id;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public short getSortOrder() {
        return sortOrder;
    }
}
