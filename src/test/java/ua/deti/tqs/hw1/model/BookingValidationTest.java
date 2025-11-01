package ua.deti.tqs.hw1.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingValidationTest {

    private Validator validator;
    private Booking booking;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        // Create valid booking
        booking = new Booking();
        booking.setCitizenName("John Doe");
        booking.setMunicipality("Aveiro");
        booking.setDescription("Old furniture");
        booking.setItemType(Booking.ItemType.FURNITURE);
        booking.setBookingDate(LocalDate.now().plusDays(1));
        booking.setTimeSlot(2);
    }

    // === VALID BOOKING TEST ===

    @Test
    void whenValidBooking_thenNoViolations() {
        Set<ConstraintViolation<Booking>> violations = validator.validate(
            booking
        );
        assertThat(violations).isEmpty();
    }

    // === CITIZEN NAME VALIDATION TESTS ===

    @Test
    void whenInvalidName_thenViolation() {
        // Test various invalid name formats: single word, lowercase start, numbers, special chars, blank
        String[] invalidNames = {
            "John", // single word
            "john Doe", // lowercase start
            "John Doe2", // contains numbers
            "John-Doe", // contains special chars
            "", // blank
        };

        for (String invalidName : invalidNames) {
            booking.setCitizenName(invalidName);
            Set<ConstraintViolation<Booking>> violations = validator.validate(
                booking
            );
            assertThat(violations)
                .as("Should have violations for name: " + invalidName)
                .isNotEmpty();
        }
    }

    @Test
    void whenValidNames_thenNoViolations() {
        // Test valid names with different formats
        String[] validNames = {
            "John Doe", // two words
            "John Michael Doe", // three words
            "Maria Silva", // accents ok in reality, but regex-compliant names
        };

        for (String validName : validNames) {
            booking.setCitizenName(validName);
            Set<ConstraintViolation<Booking>> violations = validator.validate(
                booking
            );
            assertThat(violations)
                .as("Should have no violations for name: " + validName)
                .isEmpty();
        }
    }

    // === DESCRIPTION VALIDATION TESTS ===

    @Test
    void whenInvalidDescription_thenViolation() {
        // Test invalid descriptions: too short, blank
        String[] invalidDescriptions = {
            "TV", // too short (less than 4 chars)
            "AB", // too short
            "", // blank
        };

        for (String invalidDesc : invalidDescriptions) {
            booking.setDescription(invalidDesc);
            Set<ConstraintViolation<Booking>> violations = validator.validate(
                booking
            );
            assertThat(violations)
                .as("Should have violations for description: " + invalidDesc)
                .isNotEmpty();
        }
    }

    @Test
    void whenValidDescription_thenNoViolations() {
        // Test valid descriptions: minimum 4 chars and longer
        String[] validDescriptions = {
            "Test", // exactly 4 chars (minimum)
            "Old refrigerator", // longer
            "Broken washing machine needing repair", // much longer
        };

        for (String validDesc : validDescriptions) {
            booking.setDescription(validDesc);
            Set<ConstraintViolation<Booking>> violations = validator.validate(
                booking
            );
            assertThat(violations)
                .as("Should have no violations for description: " + validDesc)
                .isEmpty();
        }
    }

    // === ITEM TYPE VALIDATION TESTS ===

    @Test
    void whenNullItemType_thenViolation() {
        booking.setItemType(null);
        Set<ConstraintViolation<Booking>> violations = validator.validate(
            booking
        );
        assertThat(violations).isNotEmpty();
    }

    @Test
    void whenValidItemTypes_thenNoViolations() {
        for (Booking.ItemType itemType : Booking.ItemType.values()) {
            booking.setItemType(itemType);
            Set<ConstraintViolation<Booking>> violations = validator.validate(
                booking
            );
            assertThat(violations)
                .as("Should have no violations for itemType: " + itemType)
                .isEmpty();
        }
    }

    // === ITEM TYPE ENUM TESTS ===

    @Test
    void testItemTypeDisplayNames() {
        assertThat(Booking.ItemType.FURNITURE.getDisplayName()).isEqualTo(
            "Furniture"
        );
        assertThat(Booking.ItemType.ELECTRONICS.getDisplayName()).isEqualTo(
            "Electronics"
        );
        assertThat(Booking.ItemType.HAZARDOUS.getDisplayName()).isEqualTo(
            "Hazardous Waste"
        );
        assertThat(Booking.ItemType.GENERAL.getDisplayName()).isEqualTo(
            "General Waste"
        );
    }

    @Test
    void testGetAllItemTypes() {
        assertThat(Booking.ItemType.getAllTypes())
            .hasSize(4)
            .contains(
                Booking.ItemType.FURNITURE,
                Booking.ItemType.ELECTRONICS,
                Booking.ItemType.HAZARDOUS,
                Booking.ItemType.GENERAL
            );
    }

    // === MULTIPLE INVALID FIELDS TEST ===

    @Test
    void whenMultipleInvalidFields_thenMultipleViolations() {
        booking.setCitizenName("john"); // Invalid name
        booking.setDescription("TV"); // Too short
        booking.setItemType(null); // Null

        Set<ConstraintViolation<Booking>> violations = validator.validate(
            booking
        );
        assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
    }
}
