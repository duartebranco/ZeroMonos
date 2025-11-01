package ua.deti.tqs.hw1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ua.deti.tqs.hw1.model.Booking;
import ua.deti.tqs.hw1.repository.BookingRepository;

/**
 * Unit tests for BookingService with dependency isolation using mocks.
 * Focuses on domain rules: availability, slot management, date validation, status updates.
 */
class BookingServiceTest {

    private BookingRepository repo;
    private BookingService service;
    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(BookingRepository.class);
        service = new BookingService(repo);

        sampleBooking = new Booking();
        sampleBooking.setCitizenName("John Doe");
        sampleBooking.setMunicipality("Aveiro");
        sampleBooking.setDescription("Old refrigerator");
        sampleBooking.setItemType(Booking.ItemType.ELECTRONICS);
        sampleBooking.setBookingDate(LocalDate.now().plusDays(1));
        sampleBooking.setTimeSlot(2);
    }

    // === BOOKING CREATION & DOMAIN RULES TESTS ===

    @Test
    void whenCreateValidBookingWithAvailableSlot_thenSaveSuccessfully() {
        // Arrange: verify slot is available (no active bookings)
        when(
            repo.countActiveBookingsForSlot(any(), anyInt(), anyString())
        ).thenReturn(0L);
        when(repo.save(any(Booking.class))).thenReturn(sampleBooking);

        // Act
        Booking result = service.create(sampleBooking);

        // Assert
        verify(repo).save(sampleBooking);
        assertThat(result).isEqualTo(sampleBooking);
    }

    @Test
    void whenCreateBookingWithPastDate_thenThrowIllegalArgumentException() {
        // Arrange: set date to yesterday
        sampleBooking.setBookingDate(LocalDate.now().minusDays(1));

        // Act & Assert: booking creation should fail with clear message
        assertThatThrownBy(() -> service.create(sampleBooking))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid booking date");

        // Verify repo.save was never called
        verify(repo, never()).save(any());
    }

    @Test
    void whenCreateBookingToOccupiedSlot_thenThrowIllegalArgumentException() {
        // Arrange: slot is already occupied (1 active booking)
        when(
            repo.countActiveBookingsForSlot(any(), anyInt(), anyString())
        ).thenReturn(1L);

        // Act & Assert: booking creation should fail
        assertThatThrownBy(() -> service.create(sampleBooking))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not available");

        // Verify repo.save was never called
        verify(repo, never()).save(any());
    }

    // === BOOKING RETRIEVAL TESTS ===

    @Test
    void whenGetByValidToken_thenReturnBooking() {
        // Arrange
        when(repo.findByToken("abc12345")).thenReturn(
            Optional.of(sampleBooking)
        );

        // Act
        Booking result = service.getByToken("abc12345");

        // Assert
        assertThat(result).isEqualTo(sampleBooking);
        verify(repo).findByToken("abc12345");
    }

    @Test
    void whenGetByInvalidToken_thenReturnNull() {
        // Arrange
        when(repo.findByToken("invalid")).thenReturn(Optional.empty());

        // Act
        Booking result = service.getByToken("invalid");

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void whenGetByMunicipality_thenReturnFilteredBookings() {
        // Arrange
        List<Booking> expectedBookings = List.of(sampleBooking);
        when(repo.findByMunicipality("Aveiro")).thenReturn(expectedBookings);

        // Act
        List<Booking> results = service.getByMunicipality("Aveiro");

        // Assert
        assertThat(results).hasSize(1).contains(sampleBooking);
        verify(repo).findByMunicipality("Aveiro");
    }

    @Test
    void whenGetByDate_thenReturnBookingsForThatDate() {
        // Arrange
        LocalDate testDate = LocalDate.now().plusDays(1);
        List<Booking> expectedBookings = List.of(sampleBooking);
        when(repo.findByBookingDateOrderByTimeSlot(testDate)).thenReturn(
            expectedBookings
        );

        // Act
        List<Booking> results = service.getByDate(testDate);

        // Assert
        assertThat(results).hasSize(1).contains(sampleBooking);
    }

    @Test
    void whenGetByDateAndMunicipality_thenReturnFilteredBookings() {
        // Arrange
        LocalDate testDate = LocalDate.now().plusDays(1);
        List<Booking> expectedBookings = List.of(sampleBooking);
        when(
            repo.findByBookingDateAndMunicipalityOrderByTimeSlot(
                testDate,
                "Aveiro"
            )
        ).thenReturn(expectedBookings);

        // Act
        List<Booking> results = service.getByDateAndMunicipality(
            testDate,
            "Aveiro"
        );

        // Assert
        assertThat(results).hasSize(1);
    }

    // === STATUS UPDATE TESTS ===

    @Test
    void whenUpdateStatusOfExistingBooking_thenUpdateSuccessfully() {
        // Arrange
        when(repo.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(repo.save(any())).thenReturn(sampleBooking);

        // Act
        Booking result = service.updateStatus(1L, "IN_PROGRESS");

        // Assert
        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        verify(repo).save(sampleBooking);
    }

    @Test
    void whenUpdateStatusToCANCELLED_thenStatusChanges() {
        // Arrange: booking exists with RECEIVED status
        sampleBooking.setStatus("RECEIVED");
        when(repo.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(repo.save(any())).thenReturn(sampleBooking);

        // Act
        Booking result = service.updateStatus(1L, "CANCELLED");

        // Assert
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(repo).save(sampleBooking);
    }

    @Test
    void whenUpdateStatusOfNonExistentBooking_thenReturnNull() {
        // Arrange
        when(repo.findById(99L)).thenReturn(Optional.empty());

        // Act
        Booking result = service.updateStatus(99L, "COMPLETED");

        // Assert
        assertThat(result).isNull();
        verify(repo, never()).save(any());
    }

    // === DELETION TESTS ===

    @Test
    void whenDeleteExistingBooking_thenReturnTrueAndDelete() {
        // Arrange
        when(repo.existsById(1L)).thenReturn(true);

        // Act
        boolean deleted = service.delete(1L);

        // Assert
        assertThat(deleted).isTrue();
        verify(repo).deleteById(1L);
    }

    @Test
    void whenDeleteNonExistentBooking_thenReturnFalse() {
        // Arrange
        when(repo.existsById(99L)).thenReturn(false);

        // Act
        boolean deleted = service.delete(99L);

        // Assert
        assertThat(deleted).isFalse();
        verify(repo, never()).deleteById(anyLong());
    }

    // === AVAILABILITY & SLOT MANAGEMENT TESTS ===

    @Test
    void whenCheckAvailableSlot_thenReturnTrue() {
        // Arrange: no active bookings for this slot
        LocalDate testDate = LocalDate.now().plusDays(1);
        when(repo.countActiveBookingsForSlot(testDate, 3, "Aveiro")).thenReturn(
            0L
        );

        // Act
        boolean available = service.isTimeSlotAvailable(testDate, 3, "Aveiro");

        // Assert
        assertThat(available).isTrue();
    }

    @Test
    void whenCheckOccupiedSlot_thenReturnFalse() {
        // Arrange: at least one active booking exists for this slot
        LocalDate testDate = LocalDate.now().plusDays(1);
        when(repo.countActiveBookingsForSlot(testDate, 3, "Aveiro")).thenReturn(
            1L
        );

        // Act
        boolean available = service.isTimeSlotAvailable(testDate, 3, "Aveiro");

        // Assert
        assertThat(available).isFalse();
    }

    @Test
    void whenCheckSlotWithInvalidTimeSlotNumber_thenReturnFalse() {
        // Arrange: time slot is out of valid range (0-8)
        LocalDate testDate = LocalDate.now().plusDays(1);

        // Act & Assert for invalid slots
        assertThat(
            service.isTimeSlotAvailable(testDate, -1, "Aveiro")
        ).isFalse();
        assertThat(
            service.isTimeSlotAvailable(testDate, 9, "Aveiro")
        ).isFalse();
    }

    @Test
    void whenCheckSlotWithNullParameters_thenReturnFalse() {
        // Act & Assert
        assertThat(service.isTimeSlotAvailable(null, 3, "Aveiro")).isFalse();
        assertThat(
            service.isTimeSlotAvailable(
                LocalDate.now().plusDays(1),
                null,
                "Aveiro"
            )
        ).isFalse();
        assertThat(
            service.isTimeSlotAvailable(LocalDate.now().plusDays(1), 3, null)
        ).isFalse();
    }

    @Test
    void whenGetAvailableSlots_thenReturnCorrectList() {
        // Arrange: slots 1 and 3 are occupied
        LocalDate testDate = LocalDate.now().plusDays(1);
        when(repo.findOccupiedTimeSlotsForDate(testDate, "Aveiro")).thenReturn(
            List.of(1, 3)
        );

        // Act
        List<Integer> availableSlots = service.getAvailableTimeSlots(
            testDate,
            "Aveiro"
        );

        // Assert: should return all slots except 1 and 3 (0-8 = 9 total)
        assertThat(availableSlots)
            .hasSize(7)
            .contains(0, 2, 4, 5, 6, 7, 8)
            .doesNotContain(1, 3);
    }

    @Test
    void whenGetAvailableSlotsForPastDate_thenReturnEmptyList() {
        // Arrange: date is in the past
        LocalDate pastDate = LocalDate.now().minusDays(1);

        // Act
        List<Integer> availableSlots = service.getAvailableTimeSlots(
            pastDate,
            "Aveiro"
        );

        // Assert
        assertThat(availableSlots).isEmpty();
    }

    @Test
    void whenGetFormattedAvailableTimeSlots_thenReturnFormattedStrings() {
        // Arrange
        LocalDate testDate = LocalDate.now().plusDays(1);
        when(repo.findOccupiedTimeSlotsForDate(testDate, "Aveiro")).thenReturn(
            List.of()
        );

        // Act
        List<String> formattedSlots = service.getFormattedAvailableTimeSlots(
            testDate,
            "Aveiro"
        );

        // Assert: should format as HH:00 (09:00 to 17:00)
        assertThat(formattedSlots)
            .hasSize(9)
            .contains(
                "09:00",
                "10:00",
                "11:00",
                "12:00",
                "13:00",
                "14:00",
                "15:00",
                "16:00",
                "17:00"
            );
    }

    // === DATE VALIDATION TESTS ===

    @Test
    void whenValidateBookingDateToday_thenReturnTrue() {
        // Act & Assert: today is valid
        assertThat(service.isValidBookingDate(LocalDate.now())).isTrue();
    }

    @Test
    void whenValidateBookingDateInFuture_thenReturnTrue() {
        // Act & Assert: future dates are valid
        assertThat(
            service.isValidBookingDate(LocalDate.now().plusDays(1))
        ).isTrue();
        assertThat(
            service.isValidBookingDate(LocalDate.now().plusDays(30))
        ).isTrue();
    }

    @Test
    void whenValidateBookingDateInPast_thenReturnFalse() {
        // Act & Assert: past dates are invalid
        assertThat(
            service.isValidBookingDate(LocalDate.now().minusDays(1))
        ).isFalse();
    }

    @Test
    void whenValidateNullDate_thenReturnFalse() {
        // Act & Assert
        assertThat(service.isValidBookingDate(null)).isFalse();
    }

    // === FORMAT TIME SLOT TESTS ===

    @Test
    void whenFormatValidTimeSlots_thenReturnCorrectTimeStrings() {
        // Act & Assert
        assertThat(service.formatTimeSlot(0)).isEqualTo("09:00");
        assertThat(service.formatTimeSlot(1)).isEqualTo("10:00");
        assertThat(service.formatTimeSlot(4)).isEqualTo("13:00");
        assertThat(service.formatTimeSlot(8)).isEqualTo("17:00");
    }

    @Test
    void whenFormatInvalidTimeSlots_thenReturnEmptyString() {
        // Act & Assert
        assertThat(service.formatTimeSlot(-1)).isEqualTo("");
        assertThat(service.formatTimeSlot(9)).isEqualTo("");
        assertThat(service.formatTimeSlot(null)).isEqualTo("");
    }

    // === GET ALL BOOKINGS TEST ===

    @Test
    void whenGetAllBookings_thenReturnAllFromRepository() {
        // Arrange
        List<Booking> bookings = List.of(sampleBooking);
        when(repo.findAll()).thenReturn(bookings);

        // Act
        List<Booking> result = service.getAll();

        // Assert
        assertThat(result).hasSize(1).contains(sampleBooking);
        verify(repo).findAll();
    }
}
