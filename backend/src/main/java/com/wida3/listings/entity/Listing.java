package com.wida3.listings.entity;

import com.wida3.auth.entity.User;
import com.wida3.listings.exception.InvalidListingStateException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "warehouse_type", nullable = false)
    private WarehouseType warehouseType;

    @Column(name = "size_sqm", nullable = false)
    private BigDecimal sizeSqm;

    @Column(name = "weekly_price", nullable = false)
    private BigDecimal weeklyPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.PENDING_APPROVAL;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<ListingPhoto> photos = new ArrayList<>();

    protected Listing() {
    }

    public Listing(
            User owner,
            String title,
            String city,
            String address,
            WarehouseType warehouseType,
            BigDecimal sizeSqm,
            BigDecimal weeklyPrice) {
        this.owner = owner;
        this.title = title;
        this.city = city;
        this.address = address;
        this.warehouseType = warehouseType;
        this.sizeSqm = sizeSqm;
        this.weeklyPrice = weeklyPrice;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addPhoto(ListingPhoto photo) {
        photos.add(photo);
        photo.setListing(this);
    }

    public void update(
            String title,
            String city,
            String address,
            WarehouseType warehouseType,
            BigDecimal sizeSqm,
            BigDecimal weeklyPrice) {
        this.title = title;
        this.city = city;
        this.address = address;
        this.warehouseType = warehouseType;
        this.sizeSqm = sizeSqm;
        this.weeklyPrice = weeklyPrice;
    }

    public void deactivate() {
        if (status == ListingStatus.INACTIVE) {
            throw new InvalidListingStateException(status, "deactivate");
        }
        status = ListingStatus.INACTIVE;
    }

    public void approve() {
        if (status != ListingStatus.PENDING_APPROVAL) {
            throw new InvalidListingStateException(status, "approve");
        }
        status = ListingStatus.ACTIVE;
    }

    public void reject(String reason) {
        if (status != ListingStatus.PENDING_APPROVAL) {
            throw new InvalidListingStateException(status, "reject");
        }
        status = ListingStatus.REJECTED;
        rejectionReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public WarehouseType getWarehouseType() {
        return warehouseType;
    }

    public BigDecimal getSizeSqm() {
        return sizeSqm;
    }

    public BigDecimal getWeeklyPrice() {
        return weeklyPrice;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public List<ListingPhoto> getPhotos() {
        return photos;
    }
}
