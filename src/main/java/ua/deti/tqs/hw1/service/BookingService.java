package ua.deti.tqs.hw1.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ua.deti.tqs.hw1.model.Booking;
import ua.deti.tqs.hw1.repository.BookingRepository;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(
        BookingService.class
    );

    private final BookingRepository repo;

    public BookingService(BookingRepository repo) {
        this.repo = repo;
        logger.info("BookingService initialized");
    }

    /**
     * Create a new booking with availability validation.
     */
    public Booking create(Booking booking) {
        logger.info("Creating new booking");

        // Validate booking date
        if (!isValidBookingDate(booking.getBookingDate())) {
            logger.warn("Booking creation failed: Invalid date");
            throw new IllegalArgumentException(
                "Invalid booking date. Date must be today or in the future."
            );
        }

        // Check if time slot is available
        if (
            !isTimeSlotAvailable(
                booking.getBookingDate(),
                booking.getTimeSlot(),
                booking.getMunicipality()
            )
        ) {
            logger.warn("Booking creation failed: Time slot already occupied");
            throw new IllegalArgumentException(
                String.format(
                    "Time slot %s on %s is not available for %s",
                    formatTimeSlot(booking.getTimeSlot()),
                    booking.getBookingDate(),
                    booking.getMunicipality()
                )
            );
        }

        Booking savedBooking = repo.save(booking);
        logger.info(
            "Booking created successfully with token: {}",
            savedBooking.getToken()
        );
        return savedBooking;
    }

    /**
     * Get booking by token.
     */
    public Booking getByToken(String token) {
        return repo.findByToken(token).orElse(null);
    }

    /**
     * Get all bookings.
     */
    public List<Booking> getAll() {
        return repo.findAll();
    }

    /**
     * Get bookings by municipality.
     */
    public List<Booking> getByMunicipality(String municipality) {
        return repo.findByMunicipality(municipality);
    }

    /**
     * Get bookings for a specific date.
     */
    public List<Booking> getByDate(LocalDate date) {
        return repo.findByBookingDateOrderByTimeSlot(date);
    }

    /**
     * Get bookings for a specific date and municipality.
     */
    public List<Booking> getByDateAndMunicipality(
        LocalDate date,
        String municipality
    ) {
        return repo.findByBookingDateAndMunicipalityOrderByTimeSlot(
            date,
            municipality
        );
    }

    /**
     * Update booking status.
     */
    public Booking updateStatus(Long id, String status) {
        logger.info("Updating booking {} status", id);

        return repo
            .findById(id)
            .map(b -> {
                String oldStatus = b.getStatus();
                b.setStatus(status);
                Booking savedBooking = repo.save(b);

                logger.info("Booking {} status changed", id);

                // Log availability change when cancelled
                if (
                    "CANCELLED".equals(status) && !"CANCELLED".equals(oldStatus)
                ) {
                    logger.info(
                        "Time slot is now available (booking {} cancelled)",
                        b.getId()
                    );
                }

                return savedBooking;
            })
            .orElseGet(() -> {
                logger.warn(
                    "Attempted to update non-existent booking with ID: {}",
                    id
                );
                return null;
            });
    }

    /**
     * Delete a booking.
     */
    public boolean delete(Long id) {
        if (repo.existsById(id)) {
            logger.info("Deleting booking with ID: {}", id);
            repo.deleteById(id);
            logger.info("Booking {} deleted successfully", id);
            return true;
        }
        logger.warn("Attempted to delete non-existent booking with ID: {}", id);
        return false;
    }

    // === AVAILABILITY METHODS (previously in AvailabilityService) ===

    /**
     * Check if a specific time slot is available.
     */
    public boolean isTimeSlotAvailable(
        LocalDate date,
        Integer timeSlot,
        String municipality
    ) {
        if (date == null || timeSlot == null || municipality == null) {
            return false;
        }

        if (timeSlot < 0 || timeSlot > 8) {
            return false;
        }

        if (date.isBefore(LocalDate.now())) {
            return false;
        }

        long activeBookings = repo.countActiveBookingsForSlot(
            date,
            timeSlot,
            municipality
        );
        return activeBookings == 0;
    }

    /**
     * Get all available time slots for a specific date and municipality.
     */
    public List<Integer> getAvailableTimeSlots(
        LocalDate date,
        String municipality
    ) {
        if (
            date == null ||
            municipality == null ||
            date.isBefore(LocalDate.now())
        ) {
            return new ArrayList<>();
        }

        List<Integer> occupiedSlots = repo.findOccupiedTimeSlotsForDate(
            date,
            municipality
        );

        return IntStream.range(0, 9)
            .filter(slot -> !occupiedSlots.contains(slot))
            .boxed()
            .toList();
    }

    /**
     * Get formatted available time slots for a date and municipality.
     */
    public List<String> getFormattedAvailableTimeSlots(
        LocalDate date,
        String municipality
    ) {
        return getAvailableTimeSlots(date, municipality)
            .stream()
            .map(this::formatTimeSlot)
            .toList();
    }

    /**
     * Format time slot number to time string (e.g., 0 -> "09:00").
     */
    public String formatTimeSlot(Integer timeSlot) {
        if (timeSlot == null || timeSlot < 0 || timeSlot > 8) {
            return "";
        }
        return String.format("%02d:00", 9 + timeSlot);
    }

    /**
     * Validate if a booking date is allowed (not in the past).
     */
    public boolean isValidBookingDate(LocalDate date) {
        if (date == null) {
            return false;
        }
        return !date.isBefore(LocalDate.now());
    }
}
