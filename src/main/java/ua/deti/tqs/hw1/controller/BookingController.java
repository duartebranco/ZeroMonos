package ua.deti.tqs.hw1.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ua.deti.tqs.hw1.model.Booking;
import ua.deti.tqs.hw1.service.BookingService;

@RestController
@RequestMapping("/bookings")
@CrossOrigin
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    /**
     * Create a new booking
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Booking booking) {
        try {
            Booking createdBooking = service.create(booking);
            return ResponseEntity.ok(createdBooking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        }
    }

    /**
     * Get booking by token
     */
    @GetMapping("/{token}")
    public ResponseEntity<Booking> get(@PathVariable String token) {
        Booking booking = service.getByToken(token);
        if (booking == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Booking not found"
            );
        }
        return ResponseEntity.ok(booking);
    }

    /**
     * Get all bookings with optional filters
     */
    @GetMapping
    public List<Booking> getAll(
        @RequestParam(required = false) String municipality,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate date
    ) {
        if (municipality != null && date != null) {
            return service.getByDateAndMunicipality(date, municipality);
        }
        if (date != null) {
            return service.getByDate(date);
        }
        if (municipality != null) {
            return service.getByMunicipality(municipality);
        }
        return service.getAll();
    }

    /**
     * Update booking status
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Booking> updateStatus(
        @PathVariable Long id,
        @RequestParam String status
    ) {
        Booking updated = service.updateStatus(id, status);
        if (updated == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Booking not found"
            );
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a booking
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = service.delete(id);
        if (!deleted) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Booking not found"
            );
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Get available time slots for a specific date and municipality
     */
    @GetMapping("/available-slots")
    public ResponseEntity<Map<String, Object>> getAvailableTimeSlots(
        @RequestParam @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate date,
        @RequestParam String municipality
    ) {
        List<Integer> availableSlots = service.getAvailableTimeSlots(
            date,
            municipality
        );
        List<String> formattedSlots = service.getFormattedAvailableTimeSlots(
            date,
            municipality
        );

        return ResponseEntity.ok(
            Map.of(
                "date",
                date.toString(),
                "municipality",
                municipality,
                "availableSlots",
                availableSlots,
                "formattedSlots",
                formattedSlots
            )
        );
    }
}
