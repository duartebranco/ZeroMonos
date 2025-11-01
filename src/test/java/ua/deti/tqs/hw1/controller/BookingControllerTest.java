package ua.deti.tqs.hw1.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ua.deti.tqs.hw1.model.Booking;
import ua.deti.tqs.hw1.repository.BookingRepository;

/**
 * Integration tests for BookingController API endpoints.
 * Tests real HTTP requests using MockMvc with transactional database rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BookingRepository repo;

    private Booking validBooking;

    @BeforeEach
    void setUp() {
        repo.deleteAll();

        validBooking = new Booking();
        validBooking.setCitizenName("John Doe");
        validBooking.setMunicipality("Aveiro");
        validBooking.setDescription("Old refrigerator");
        validBooking.setItemType(Booking.ItemType.ELECTRONICS);
        validBooking.setBookingDate(LocalDate.now().plusDays(1));
        validBooking.setTimeSlot(2);
    }

    // === BOOKING CREATION TESTS ===

    @Test
    void whenPostValidBooking_thenCreateSuccessfully() throws Exception {
        String json = mapper.writeValueAsString(validBooking);

        String response = mvc
            .perform(
                post("/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.citizenName").value("John Doe"))
            .andExpect(jsonPath("$.itemType").value("ELECTRONICS"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Booking saved = mapper.readValue(response, Booking.class);
        assertThat(repo.findByToken(saved.getToken())).isPresent();
    }

    @Test
    void whenPostBookingWithInvalidName_thenReturnBadRequest()
        throws Exception {
        validBooking.setCitizenName("john"); // Single word, lowercase
        String json = mapper.writeValueAsString(validBooking);

        mvc
            .perform(
                post("/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void whenPostBookingWithShortDescription_thenReturnBadRequest()
        throws Exception {
        validBooking.setDescription("TV"); // Too short
        String json = mapper.writeValueAsString(validBooking);

        mvc
            .perform(
                post("/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void whenPostBookingWithPastDate_thenReturnBadRequest() throws Exception {
        validBooking.setBookingDate(LocalDate.now().minusDays(1));
        String json = mapper.writeValueAsString(validBooking);

        mvc
            .perform(
                post("/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void whenPostBookingToOccupiedSlot_thenReturnBadRequest() throws Exception {
        // Create first booking
        repo.save(validBooking);

        // Try to create second booking for same slot
        Booking secondBooking = new Booking();
        secondBooking.setCitizenName("Jane Smith");
        secondBooking.setMunicipality("Aveiro");
        secondBooking.setDescription("Old furniture");
        secondBooking.setItemType(Booking.ItemType.FURNITURE);
        secondBooking.setBookingDate(LocalDate.now().plusDays(1));
        secondBooking.setTimeSlot(2); // Same slot

        String json = mapper.writeValueAsString(secondBooking);

        mvc
            .perform(
                post("/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isBadRequest());
    }

    // === BOOKING RETRIEVAL TESTS ===

    @Test
    void whenGetByValidToken_thenReturnBooking() throws Exception {
        Booking saved = repo.save(validBooking);

        mvc
            .perform(get("/bookings/" + saved.getToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.citizenName").value("John Doe"))
            .andExpect(jsonPath("$.itemType").value("ELECTRONICS"));
    }

    @Test
    void whenGetByInvalidToken_thenReturnNotFound() throws Exception {
        mvc
            .perform(get("/bookings/invalid-token"))
            .andExpect(status().isNotFound());
    }

    @Test
    void whenGetAllBookings_thenReturnList() throws Exception {
        repo.save(validBooking);

        mvc
            .perform(get("/bookings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].citizenName").value("John Doe"));
    }

    @Test
    void whenGetBookingsFilteredByMunicipality_thenReturnFiltered()
        throws Exception {
        repo.save(validBooking);

        // Create booking for different municipality
        Booking portoBooking = new Booking();
        portoBooking.setCitizenName("Maria Silva");
        portoBooking.setMunicipality("Porto");
        portoBooking.setDescription("Old computer");
        portoBooking.setItemType(Booking.ItemType.ELECTRONICS);
        portoBooking.setBookingDate(LocalDate.now().plusDays(1));
        portoBooking.setTimeSlot(3);
        repo.save(portoBooking);

        mvc
            .perform(get("/bookings").param("municipality", "Aveiro"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].citizenName").value("John Doe"));
    }

    // === BOOKING MANAGEMENT TESTS ===

    @Test
    void whenUpdateBookingStatus_thenUpdateSuccessfully() throws Exception {
        Booking saved = repo.save(validBooking);

        mvc
            .perform(
                patch("/bookings/" + saved.getId()).param(
                    "status",
                    "IN_PROGRESS"
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        assertThat(repo.findById(saved.getId()).get().getStatus()).isEqualTo(
            "IN_PROGRESS"
        );
    }

    @Test
    void whenCancelBooking_thenUpdateStatusToCancelled() throws Exception {
        // Arrange: create booking with RECEIVED status
        Booking saved = repo.save(validBooking);
        assertThat(saved.getStatus()).isEqualTo("RECEIVED");

        // Act: update status to CANCELLED
        mvc
            .perform(
                patch("/bookings/" + saved.getId()).param("status", "CANCELLED")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Assert: verify booking is now cancelled in database
        assertThat(repo.findById(saved.getId()).get().getStatus()).isEqualTo(
            "CANCELLED"
        );
    }

    @Test
    void whenDeleteBooking_thenRemoveSuccessfully() throws Exception {
        Booking saved = repo.save(validBooking);

        mvc
            .perform(delete("/bookings/" + saved.getId()))
            .andExpect(status().isNoContent());

        assertThat(repo.existsById(saved.getId())).isFalse();
    }

    // === AVAILABILITY TESTS ===

    @Test
    void whenGetAvailableSlots_thenReturnSlotInfo() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        mvc
            .perform(
                get("/bookings/available-slots")
                    .param("date", tomorrow.toString())
                    .param("municipality", "Aveiro")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.date").value(tomorrow.toString()))
            .andExpect(jsonPath("$.municipality").value("Aveiro"))
            .andExpect(jsonPath("$.availableSlots").isArray());
    }
}
