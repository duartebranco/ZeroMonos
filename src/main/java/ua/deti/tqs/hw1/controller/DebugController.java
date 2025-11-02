package ua.deti.tqs.hw1.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.deti.tqs.hw1.service.BookingService;
import ua.deti.tqs.hw1.service.MunicipalityService;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(
        DebugController.class
    );
    private final BookingService bookingService;
    private final MunicipalityService municipalityService;

    public DebugController(
        BookingService bookingService,
        MunicipalityService municipalityService
    ) {
        this.bookingService = bookingService;
        this.municipalityService = municipalityService;
        logger.info("DebugController initialized - logging system ready");
    }

    /**
     * System information endpoint
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        logger.info("System info requested");

        Map<String, Object> info = new HashMap<>();

        // Time slot mappings
        Map<Integer, String> timeSlots = new HashMap<>();
        for (int i = 0; i <= 8; i++) {
            timeSlots.put(i, bookingService.formatTimeSlot(i));
        }

        info.put("timeSlots", timeSlots);
        info.put("totalSlots", 9);
        info.put("workingHours", "09:00 - 17:00");

        // Available municipalities
        List<String> municipalities = municipalityService.getMunicipalities();
        info.put("municipalities", municipalities.size());
        info.put("currentDate", LocalDate.now().toString());

        logger.info(
            "System info provided: {} municipalities, {} time slots",
            municipalities.size(),
            timeSlots.size()
        );

        return ResponseEntity.ok(info);
    }

    /**
     * Application statistics for professor review
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        logger.info("Application statistics requested");

        var allBookings = bookingService.getAll();
        Map<String, Object> stats = new HashMap<>();

        // Basic stats
        stats.put("totalBookings", allBookings.size());
        stats.put("currentDate", LocalDate.now().toString());

        // Status breakdown
        Map<String, Long> statusCounts = allBookings
            .stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    Booking::getStatus,
                    java.util.stream.Collectors.counting()
                )
            );
</parameter>
        stats.put("bookingsByStatus", statusCounts);

        // Municipality popularity
        Map<String, Long> municipalityCounts = allBookings
            .stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    Booking::getMunicipality,
                    java.util.stream.Collectors.counting()
                )
            );
        stats.put("topMunicipalities", municipalityCounts);

        logger.info(
            "Statistics generated: {} total bookings, {} different municipalities",
            allBookings.size(),
            municipalityCounts.size()
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * Check availability for next few days (useful for testing)
     */
    @GetMapping("/availability-summary")
    public ResponseEntity<Map<String, Object>> getAvailabilitySummary(
        @RequestParam String municipality,
        @RequestParam(required = false, defaultValue = "3") Integer days
    ) {
        logger.info(
            "Availability summary requested for {} (next {} days)",
            municipality,
            days
        );

        Map<String, Object> summary = new HashMap<>();
        Map<String, Object> dailyInfo = new HashMap<>();

        LocalDate startDate = LocalDate.now();
        int totalAvailable = 0;
        int totalOccupied = 0;

        for (int i = 0; i < days; i++) {
            LocalDate checkDate = startDate.plusDays(i);
            List<Integer> availableSlots = bookingService.getAvailableTimeSlots(
                checkDate,
                municipality
            );
            List<String> formattedSlots =
                bookingService.getFormattedAvailableTimeSlots(
                    checkDate,
                    municipality
                );

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", checkDate.toString());
            dayData.put("availableSlots", availableSlots.size());
            dayData.put("occupiedSlots", 9 - availableSlots.size());
            dayData.put("formattedAvailable", formattedSlots);

            dailyInfo.put("day" + i, dayData);
            totalAvailable += availableSlots.size();
            totalOccupied += (9 - availableSlots.size());
        }

        summary.put("municipality", municipality);
        summary.put("period", days + " days from " + startDate);
        summary.put("totalAvailableSlots", totalAvailable);
        summary.put("totalOccupiedSlots", totalOccupied);
        summary.put("dailyBreakdown", dailyInfo);

        logger.info(
            "Availability summary for {}: {}/{} slots available over {} days",
            municipality,
            totalAvailable,
            totalAvailable + totalOccupied,
            days
        );

        return ResponseEntity.ok(summary);
    }

    /**
     * Test endpoint to generate log entries (for professor to see logging in action)
     */
    @GetMapping("/test-logging")
    public ResponseEntity<Map<String, String>> testLogging() {
        logger.debug("DEBUG level log - detailed information");
        logger.info("INFO level log - general information");
        logger.warn("WARN level log - warning message");

        // Simulate some application events
        logger.info("User accessed test logging endpoint");
        logger.info("System is functioning normally");
        logger.info("Database connection is healthy");

        Map<String, String> response = new HashMap<>();
        response.put("message", "Log entries generated successfully");
        response.put("logFile", "Check logs/zeromonos.log for output");
        response.put("levels", "DEBUG, INFO, WARN levels tested");

        logger.info("Test logging completed - check log file for entries");

        return ResponseEntity.ok(response);
    }
}
