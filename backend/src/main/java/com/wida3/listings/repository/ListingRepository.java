package com.wida3.listings.repository;

import com.wida3.listings.entity.Listing;
import com.wida3.listings.entity.ListingStatus;
import com.wida3.listings.entity.WarehouseType;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    // filterByAvailability is a plain Java boolean (not availableFrom/availableUntil IS NULL) because
    // Postgres/pgjdbc can't determine a bind parameter's type when its only appearance in the query
    // is a bare "? IS NULL" check -- it needs a typed comparison elsewhere to infer DATE, which the
    // startDate/endDate comparisons below already provide.
    @Query("SELECT l FROM Listing l WHERE l.status = com.wida3.listings.entity.ListingStatus.ACTIVE "
            + "AND (:city IS NULL OR l.city = :city) "
            + "AND (:warehouseType IS NULL OR l.warehouseType = :warehouseType) "
            + "AND (:minSizeSqm IS NULL OR l.sizeSqm >= :minSizeSqm) "
            + "AND (:maxSizeSqm IS NULL OR l.sizeSqm <= :maxSizeSqm) "
            + "AND (:filterByAvailability = false OR NOT EXISTS ("
            + "  SELECT 1 FROM Booking b WHERE b.listing = l "
            + "  AND b.status = com.wida3.bookings.entity.BookingStatus.CONFIRMED "
            + "  AND b.startDate < :availableUntil AND b.endDate > :availableFrom))")
    List<Listing> search(
            @Param("city") String city,
            @Param("warehouseType") WarehouseType warehouseType,
            @Param("minSizeSqm") BigDecimal minSizeSqm,
            @Param("maxSizeSqm") BigDecimal maxSizeSqm,
            @Param("filterByAvailability") boolean filterByAvailability,
            @Param("availableFrom") LocalDate availableFrom,
            @Param("availableUntil") LocalDate availableUntil);

    List<Listing> findByStatusOrderByCreatedAtAsc(ListingStatus status);

    List<Listing> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);
}
