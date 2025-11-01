package ua.deti.tqs.hw1.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.deti.tqs.hw1.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByToken(String token);
    List<Booking> findByMunicipality(String municipality);

    // Check if a specific date/time slot is available (not taken by active booking)
    @Query(
        "SELECT COUNT(b) FROM Booking b WHERE b.bookingDate = :date AND b.timeSlot = :timeSlot AND b.municipality = :municipality AND b.status != 'CANCELLED'"
    )
    long countActiveBookingsForSlot(
        @Param("date") LocalDate date,
        @Param("timeSlot") Integer timeSlot,
        @Param("municipality") String municipality
    );

    // Find active booking for specific date/time/municipality combination
    Optional<Booking> findByBookingDateAndTimeSlotAndMunicipalityAndStatusNot(
        LocalDate bookingDate,
        Integer timeSlot,
        String municipality,
        String status
    );

    // Get all occupied time slots for a specific date and municipality
    @Query(
        "SELECT b.timeSlot FROM Booking b WHERE b.bookingDate = :date AND b.municipality = :municipality AND b.status != 'CANCELLED'"
    )
    List<Integer> findOccupiedTimeSlotsForDate(
        @Param("date") LocalDate date,
        @Param("municipality") String municipality
    );

    // Get all bookings for a specific date
    List<Booking> findByBookingDateOrderByTimeSlot(LocalDate bookingDate);

    // Get all bookings for a specific date and municipality
    List<Booking> findByBookingDateAndMunicipalityOrderByTimeSlot(
        LocalDate bookingDate,
        String municipality
    );
}
