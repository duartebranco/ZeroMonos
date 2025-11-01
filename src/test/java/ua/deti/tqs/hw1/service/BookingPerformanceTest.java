package ua.deti.tqs.hw1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import ua.deti.tqs.hw1.model.Booking;
import ua.deti.tqs.hw1.repository.BookingRepository;

/**
 * Performance tests for BookingService.
 * Tests response times under load and concurrent operations.
 */
class BookingPerformanceTest {

    private BookingRepository repo;
    private BookingService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(BookingRepository.class);
        service = new BookingService(repo);
    }

    /**
     * Test: Creating 100 bookings sequentially should complete within reasonable time.
     * Verifies: No significant performance degradation with batch operations.
     */
    @Test
    @Timeout(5) // Must complete within 5 seconds
    void whenCreateHundredBookings_thenCompleteInReasonableTime() {
        // Arrange: mock repo to return 0 occupied slots
        when(
            repo.countActiveBookingsForSlot(any(), anyInt(), anyString())
        ).thenReturn(0L);

        Booking booking = new Booking();
        booking.setCitizenName("John Doe");
        booking.setMunicipality("Aveiro");
        booking.setDescription("Test item");
        booking.setItemType(Booking.ItemType.ELECTRONICS);
        booking.setBookingDate(LocalDate.now().plusDays(1));

        // Act: create 100 bookings with different time slots
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) {
            booking.setTimeSlot(i % 9); // Cycle through available slots
            when(repo.save(any(Booking.class))).thenReturn(booking);
            service.create(booking);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert: should complete in less than 5 seconds
        assertThat(duration).isLessThan(5000L);
    }

    /**
     * Test: Querying available slots for 50 different dates/municipalities.
     * Verifies: No performance issues with repeated availability queries.
     */
    @Test
    @Timeout(3) // Must complete within 3 seconds
    void whenQueryAvailableSlotsFor50Dates_thenCompleteQuickly() {
        // Arrange: mock repo to return some occupied slots
        when(repo.findOccupiedTimeSlotsForDate(any(), anyString())).thenReturn(
            List.of(1, 3, 5)
        );

        // Act: query available slots for 50 different dates
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 50; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            service.getAvailableTimeSlots(date, "Aveiro");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert: should complete in less than 3 seconds
        assertThat(duration).isLessThan(3000L);
    }

    /**
     * Test: Concurrent availability checks (simulating multiple users checking slots).
     * Verifies: Thread-safety and no race conditions under concurrent load.
     */
    @Test
    @Timeout(5) // Must complete within 5 seconds
    void whenConcurrentAvailabilityChecks_thenHandleThreadSafely()
        throws InterruptedException {
        // Arrange
        when(
            repo.countActiveBookingsForSlot(any(), anyInt(), anyString())
        ).thenReturn(0L);

        LocalDate testDate = LocalDate.now().plusDays(1);
        int numThreads = 10;
        int checksPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads * checksPerThread);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act: simulate 100 concurrent availability checks
        long startTime = System.currentTimeMillis();

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                for (int i = 0; i < checksPerThread; i++) {
                    boolean available = service.isTimeSlotAvailable(
                        testDate,
                        i % 9,
                        "Aveiro"
                    );
                    if (available) {
                        successCount.incrementAndGet();
                    }
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        // Assert
        assertThat(duration).isLessThan(5000L);
        assertThat(successCount.get()).isEqualTo(numThreads * checksPerThread);
    }

    /**
     * Test: Bulk status updates for 50 bookings.
     * Verifies: Performance when updating many booking statuses.
     */
    @Test
    @Timeout(4) // Must complete within 4 seconds
    void whenUpdateStatusFor50Bookings_thenCompleteQuickly() {
        // Arrange: mock repo to handle multiple status updates
        long startTime = System.currentTimeMillis();

        // Act: simulate 50 status update operations
        for (long i = 0; i < 50; i++) {
            Booking booking = new Booking();
            booking.setCitizenName("Test Person");
            booking.setMunicipality("Aveiro");
            booking.setDescription("Test");
            booking.setItemType(Booking.ItemType.ELECTRONICS);
            booking.setBookingDate(LocalDate.now().plusDays(1));
            booking.setTimeSlot(0);
            booking.setStatus("RECEIVED");

            when(repo.findById(i)).thenReturn(java.util.Optional.of(booking));
            when(repo.save(booking)).thenReturn(booking);

            service.updateStatus(i, "IN_PROGRESS");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert: should complete in less than 4 seconds
        assertThat(duration).isLessThan(4000L);
    }

    /**
     * Test: Format 1000 time slots.
     * Verifies: Utility methods have good performance.
     */
    @Test
    @Timeout(2) // Must complete within 2 seconds
    void whenFormatThousandTimeSlots_thenCompleteInstantly() {
        // Act: format 1000 time slots
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            service.formatTimeSlot(i % 9);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert: should complete in less than 2 seconds
        assertThat(duration).isLessThan(2000L);
    }

    /**
     * Test: Retrieve all available slots for multiple dates with mixed occupancy.
     * Verifies: Performance of complex availability calculations.
     */
    @Test
    @Timeout(3) // Must complete within 3 seconds
    void whenGetFormattedSlotsFor30Dates_thenCompleteQuickly() {
        // Arrange: mock different occupancy patterns
        when(repo.findOccupiedTimeSlotsForDate(any(), anyString())).thenReturn(
            List.of(2, 4, 6)
        ); // 3 slots occupied out of 9

        // Act: get formatted slots for 30 different dates
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 30; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            service.getFormattedAvailableTimeSlots(date, "Aveiro");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert: should complete in less than 3 seconds
        assertThat(duration).isLessThan(3000L);
    }
}
