package ua.deti.tqs.hw1.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        columnNames = { "bookingDate", "timeSlot", "municipality" }
    )
)
public class Booking {

    public enum ItemType {
        FURNITURE("Furniture"),
        ELECTRONICS("Electronics"),
        HAZARDOUS("Hazardous Waste"),
        GENERAL("General Waste");

        private final String displayName;

        ItemType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static List<ItemType> getAllTypes() {
            return Arrays.asList(values());
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token = UUID.randomUUID().toString().substring(0, 8);

    @NotBlank
    @Pattern(
        regexp = "^[A-Z][a-z]*+(?>\\s[A-Z][a-z]*+)+$",
        message = "Name must have at least two words, each starting with uppercase letter, letters only"
    )
    private String citizenName;

    @NotBlank
    private String municipality;

    @NotBlank
    @Size(min = 4, message = "Description must have at least 4 characters")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    @NotNull
    private LocalDate bookingDate;

    @NotNull
    @Min(0)
    @Max(8)
    private Integer timeSlot; // 0=09:00, 1=10:00, ..., 8=17:00

    private String status = "RECEIVED";
    private LocalDateTime createdAt = LocalDateTime.now();

    public Booking() {
        // Default no-arg constructor required by JPA/Hibernate for entity instantiation
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getCitizenName() {
        return citizenName;
    }

    public void setCitizenName(String citizenName) {
        this.citizenName = citizenName;
    }

    public String getMunicipality() {
        return municipality;
    }

    public void setMunicipality(String municipality) {
        this.municipality = municipality;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public Integer getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(Integer timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    // Helper method to get formatted time slot
    public String getFormattedTimeSlot() {
        if (timeSlot == null) return "";
        return String.format("%02d:00", 9 + timeSlot);
    }

    // Helper method to check if booking is active (not cancelled)
    public boolean isActive() {
        return !"CANCELLED".equals(status);
    }
}
