package ua.deti.tqs.hw1.functional;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

/**
 * Functional/End-to-End tests using Selenium WebDriver with Firefox.
 * Tests the main user workflows for booking management system:
 * - Citizens booking collection slots
 * - Staff viewing and filtering bookings
 * - Citizens checking booking status
 *
 * These tests are BDD-style with clear Given-When-Then structure.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@DisplayName("Functional Tests - Selenium/BDD")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FunctionalSeleniumTest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private String baseUrl;

    private BookReservePage bookReservePage;
    private StaffPage staffPage;
    private CitizenCheckPage citizenCheckPage;

    // Store booking details from first test to verify in later tests (static to share across test methods)
    private static Map<String, String> bookingAveiro;
    private static Map<String, String> bookingIlhavo;

    @BeforeEach
    void setUp() {
        // Setup WebDriver Manager for Firefox
        WebDriverManager.firefoxdriver().setup();

        // Configure Firefox options
        FirefoxOptions options = new FirefoxOptions();
        // options.addArguments("--headless");

        driver = new FirefoxDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        baseUrl = "http://localhost:" + port;

        // Initialize page objects
        bookReservePage = new BookReservePage(driver);
        staffPage = new StaffPage(driver);
        citizenCheckPage = new CitizenCheckPage(driver);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * BDD Test 1: Citizen successfully books two collection slots in different municipalities
     *
     * Given: A citizen is on the booking page
     * When: The citizen fills the form with valid data and submits two bookings
     * Then: Both success messages should be displayed with booking confirmations
     */
    @Test
    @Order(1)
    @DisplayName("TC1: Citizen can successfully book two collection slots")
    void testCitizenBooksSlotSuccessfully() {
        // Given
        bookReservePage.navigateTo(baseUrl);
        assertThat(bookReservePage.isFormDisplayed())
            .as("Booking form should be displayed")
            .isTrue();

        // Common booking details
        String citizenName = "Maria Silva";
        LocalDate bookingDate = LocalDate.now().plusDays(2);
        String description = "Old refrigerator 200L";

        // === FIRST BOOKING: Aveiro ===
        String municipalityAveiro = "Aveiro";

        bookReservePage.fillName(citizenName);
        bookReservePage.fillMunicipality(municipalityAveiro);
        bookReservePage.fillDate(bookingDate.toString());

        // Wait for time slot to be enabled after date selection
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(bookReservePage.isTimeSlotEnabled())
            .as("Time slot should be enabled after date is filled")
            .isTrue();

        bookReservePage.selectTimeSlot(0); // First available slot
        bookReservePage.selectItemType(0); // First item type
        bookReservePage.fillDescription(description);
        bookReservePage.submit();

        // Wait for booking to be processed
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String resultAveiro = bookReservePage.getCheckResultContent();
        assertThat(resultAveiro)
            .as("First booking confirmation should be displayed")
            .isNotEmpty()
            .doesNotContain("No results");

        // Extract and store first booking details
        String tokenAveiro = bookReservePage.extractTokenFromResult();
        assertThat(tokenAveiro)
            .as("Token for Aveiro booking should be extracted")
            .isNotEmpty();

        bookingAveiro = new HashMap<>();
        bookingAveiro.put("token", tokenAveiro);
        bookingAveiro.put("citizenName", citizenName);
        bookingAveiro.put("municipality", municipalityAveiro);
        bookingAveiro.put("date", bookingDate.toString());
        bookingAveiro.put("description", description);

        // === SECOND BOOKING: Ílhavo ===
        bookReservePage.clear();
        bookReservePage.navigateTo(baseUrl);

        String municipalityIlhavo = "Ílhavo";

        bookReservePage.fillName(citizenName);
        bookReservePage.fillMunicipality(municipalityIlhavo);
        bookReservePage.fillDate(bookingDate.toString());

        // Wait for time slot to be enabled
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(bookReservePage.isTimeSlotEnabled())
            .as("Time slot should be enabled for second booking")
            .isTrue();

        bookReservePage.selectTimeSlot(0);
        bookReservePage.selectItemType(0);
        bookReservePage.fillDescription(description);
        bookReservePage.submit();

        // Wait for booking to be processed
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String resultIlhavo = bookReservePage.getCheckResultContent();
        assertThat(resultIlhavo)
            .as("Second booking confirmation should be displayed")
            .isNotEmpty()
            .doesNotContain("No results");

        // Extract and store second booking details
        String tokenIlhavo = bookReservePage.extractTokenFromResult();
        assertThat(tokenIlhavo)
            .as("Token for Ílhavo booking should be extracted")
            .isNotEmpty();

        bookingIlhavo = new HashMap<>();
        bookingIlhavo.put("token", tokenIlhavo);
        bookingIlhavo.put("citizenName", citizenName);
        bookingIlhavo.put("municipality", municipalityIlhavo);
        bookingIlhavo.put("date", bookingDate.toString());
        bookingIlhavo.put("description", description);
    }

    /**
     * BDD Test 2: Staff can view the Aveiro booking and change its status
     *
     * Given: A staff member navigates to the staff page after TC1 bookings
     * When: The staff member finds the Aveiro booking by token and changes its status
     * Then: The booking should exist and its status should be updated to ASSIGNED
     */
    @Test
    @Order(2)
    @DisplayName("TC2: Staff can find booking by token and change its status")
    void testStaffPageDisplaysBookingsFromTC1() {
        // Verify booking details from TC1 were stored
        assertThat(bookingAveiro)
            .as("Aveiro booking details from TC1 should be available")
            .isNotNull()
            .isNotEmpty();

        String token = bookingAveiro.get("token");
        assertThat(token)
            .as("Token from Aveiro booking should be available")
            .isNotEmpty();

        // Given & When
        staffPage.navigateTo(baseUrl);

        // Then - Verify the table is displayed
        assertThat(staffPage.isTableDisplayed())
            .as("Bookings table should be displayed on staff page")
            .isTrue();

        // Verify the Aveiro booking exists by its token
        boolean bookingExists = staffPage.bookingExistsByToken(token);
        assertThat(bookingExists)
            .as("The Aveiro booking should exist in staff page by token")
            .isTrue();

        // Get the booking ID using the token
        String bookingId = staffPage.findBookingIdByToken(token);
        assertThat(bookingId)
            .as("Booking ID should be found using the token")
            .isNotEmpty();

        // Change the status of the booking to ASSIGNED
        String newStatus = "ASSIGNED";
        boolean statusChanged = staffPage.changeBookingStatus(
            bookingId,
            newStatus
        );

        assertThat(statusChanged)
            .as("Status should be successfully changed to " + newStatus)
            .isTrue();

        // Verify the status was actually updated
        String updatedStatus = staffPage.getBookingStatus(bookingId);
        assertThat(updatedStatus)
            .as("Booking status should be updated to " + newStatus)
            .contains(newStatus);
    }

    /**
     * BDD Test 3: Staff can filter bookings by municipality
     *
     * Given: Staff page with two bookings from TC1 (Aveiro and Ílhavo)
     * When: The staff member filters bookings by Aveiro municipality
     * Then: Only the Aveiro booking should be shown with the same token
     */
    @Test
    @Order(3)
    @DisplayName("TC3: Staff can filter bookings by municipality")
    void testStaffFiltersBookingsByMunicipality() {
        // Verify we have both bookings from TC1
        assertThat(bookingAveiro)
            .as("Aveiro booking details from TC1 should be available")
            .isNotNull()
            .isNotEmpty();

        assertThat(bookingIlhavo)
            .as("Ílhavo booking details from TC1 should be available")
            .isNotNull()
            .isNotEmpty();

        String avciroToken = bookingAveiro.get("token");
        assertThat(avciroToken)
            .as("Token from Aveiro booking should be available")
            .isNotEmpty();

        // Navigate to staff page - should see both bookings (Aveiro and Ílhavo)
        staffPage.navigateTo(baseUrl);

        assertThat(staffPage.isTableDisplayed())
            .as("Bookings table should be displayed on staff page")
            .isTrue();

        int totalBookingCount = staffPage.getBookingCount();
        assertThat(totalBookingCount)
            .as("At least 2 bookings should exist (one Aveiro, one Ílhavo)")
            .isGreaterThanOrEqualTo(2);

        // Filter by Aveiro municipality
        staffPage.searchByMunicipality("Aveiro");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify only Aveiro bookings are shown
        var municipalities = staffPage.getBookingMunicipalities();
        assertThat(municipalities)
            .as("All displayed municipalities should be Aveiro after filtering")
            .isNotEmpty()
            .allMatch(m -> m.equals("Aveiro"));

        // Verify the filtered booking is the Aveiro one from TC1 by checking token
        var filteredBookings = staffPage.getAllBookingDetails();
        boolean tokenFound = filteredBookings
            .stream()
            .anyMatch(booking -> booking.get("token").equals(avciroToken));

        assertThat(tokenFound)
            .as(
                "The booking with token from TC1 Aveiro should be present in filtered results"
            )
            .isTrue();
    }

    /**
     * BDD Test 4: Citizen can check booking status and cancel a booking
     *
     * Given: A citizen is on the check page with a valid booking token
     * When: The citizen enters the token and checks booking, then cancels it
     * Then: The booking details should be displayed and then the booking should be cancelled
     */
    @Test
    @Order(4)
    @DisplayName("TC4: Citizen can check booking status and cancel it")
    void testCitizenChecksBookingStatus() {
        // Verify we have the Ílhavo booking token from TC1
        assertThat(bookingIlhavo)
            .as("Ílhavo booking details from TC1 should be available")
            .isNotNull()
            .isNotEmpty();

        String ilhavoToken = bookingIlhavo.get("token");
        assertThat(ilhavoToken)
            .as("Token from Ílhavo booking should be available")
            .isNotEmpty();

        // Given
        citizenCheckPage.navigateTo(baseUrl);
        assertThat(citizenCheckPage.isPageDisplayed())
            .as("Citizen check page should be displayed")
            .isTrue();

        // When - Check booking by token
        citizenCheckPage.fillToken(ilhavoToken);
        citizenCheckPage.clickCheck();

        // Then - Verify booking details are displayed
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String result = citizenCheckPage.getCheckResultContent();
        assertThat(result)
            .as("Booking details should be displayed")
            .isNotEmpty()
            .doesNotContain("not found")
            .doesNotContain("not exist");

        // Verify the result contains the booking token
        assertThat(result)
            .as("Booking result should contain the token we searched for")
            .contains(ilhavoToken);

        // Now cancel the booking
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        citizenCheckPage.clickCancelBooking();

        // Wait for cancellation popup to appear
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Confirm the cancellation in the popup
        citizenCheckPage.confirmCancellation();

        // Wait for cancellation to complete
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify the booking status is now CANCELLED
        String cancelledResult =
            citizenCheckPage.getCheckResultContentAfterCancel();
        assertThat(cancelledResult)
            .as(
                "Booking result should show CANCELLED status after confirmation"
            )
            .contains("CANCELLED");

        // Verify the cancel button is hidden after cancellation
        assertThat(citizenCheckPage.isCancelButtonVisible())
            .as("Cancel button should be hidden after successful cancellation")
            .isFalse();
    }
}
