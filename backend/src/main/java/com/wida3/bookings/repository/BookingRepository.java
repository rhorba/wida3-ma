package com.wida3.bookings.repository;

import com.wida3.bookings.entity.Booking;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    List<Booking> findAllByOrderByCreatedAtDesc();

    @Query("SELECT b FROM Booking b WHERE b.renter.id = :userId OR b.listing.owner.id = :userId "
            + "ORDER BY b.createdAt DESC")
    List<Booking> findByRenterOrListingOwner(@Param("userId") UUID userId);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.listing.id = :listingId "
            + "AND b.status = com.wida3.bookings.entity.BookingStatus.CONFIRMED "
            + "AND b.startDate < :endDate AND b.endDate > :startDate")
    boolean existsOverlappingConfirmed(
            @Param("listingId") UUID listingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
