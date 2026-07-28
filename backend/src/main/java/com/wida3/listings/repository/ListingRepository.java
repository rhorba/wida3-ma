package com.wida3.listings.repository;

import com.wida3.listings.entity.Listing;
import com.wida3.listings.entity.ListingStatus;
import com.wida3.listings.entity.WarehouseType;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<Listing, UUID> {

    /** Serializes concurrent booking attempts against the same listing (see BookingService.create). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Listing l WHERE l.id = :id")
    Optional<Listing> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT l FROM Listing l WHERE l.status = com.wida3.listings.entity.ListingStatus.ACTIVE "
            + "AND (:city IS NULL OR l.city = :city) "
            + "AND (:warehouseType IS NULL OR l.warehouseType = :warehouseType) "
            + "AND (:minSizeSqm IS NULL OR l.sizeSqm >= :minSizeSqm) "
            + "AND (:maxSizeSqm IS NULL OR l.sizeSqm <= :maxSizeSqm)")
    List<Listing> search(
            @Param("city") String city,
            @Param("warehouseType") WarehouseType warehouseType,
            @Param("minSizeSqm") BigDecimal minSizeSqm,
            @Param("maxSizeSqm") BigDecimal maxSizeSqm);

    List<Listing> findByStatusOrderByCreatedAtAsc(ListingStatus status);
}
