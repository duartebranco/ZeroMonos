package ua.deti.tqs.hw1.functional;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
class FunctionalSeleniumTest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private String baseUrl;

    private BookReservePage bookReservePage;
    private StaffPage staffPage;
    private CitizenCheckPage citizenCheckPage;

    @BeforeEach
    void setUp() {
        // Setup WebDriver Manager for Firefox
        WebDriverManager.firefoxdriver().setup();

        // Configure Firefox options
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless");

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
     * BDD Test 1: Citizen successfully books a collection slot (Happy Path)
     *
     * Given: A citizen is on the booking page
     * When: The citizen fills the form with valid data and submits
     * Then: A success message should be displayed with booking confirmation
     */
    @Test
    @DisplayName("TC1: Citizen can successfully book a collection slot")
    void testCitizenBooksSlotSuccessfully() {
        // Given
        bookReservePage.navigateTo(baseUrl);
        assertThat(bookReservePage.isFormDisplayed())
            .as("Booking form should be displayed")
            .isTrue();

        // When
        bookReservePage.fillName("Maria Silva");
        bookReservePage.fillMunicipality("Aveiro");
        bookReservePage.fillDate(LocalDate.now().plusDays(1).toString());

        // Wait for time slot to be enabled after date selection
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));

        assertThat(bookReservePage.isTimeSlotEnabled())
            .as("Time slot should be enabled after date is filled")
            .isTrue();

        bookReservePage.selectTimeSlot(0); // First available slot
        bookReservePage.selectItemType(0); // First item type
        bookReservePage.fillDescription("Old refrigerator 200L");
        bookReservePage.submit();

        // Then
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String result = bookReservePage.getCheckResultContent();
        assertThat(result)
            .as("Booking confirmation or token should be displayed")
            .isNotEmpty()
            .doesNotContain("No results");
    }

    /**
     * BDD Test 2: Staff can view all bookings on the staff page
     *
     * Given: A staff member navigates to the staff page
     * When: The page loads
     * Then: The bookings table should be displayed
     */
    @Test
    @DisplayName("TC2: Staff page displays bookings table")
    void testStaffPageDisplaysBookings() {
        // Given & When
        staffPage.navigateTo(baseUrl);

        // Then
        assertThat(staffPage.isTableDisplayed())
            .as("Bookings table should be displayed on staff page")
            .isTrue();

        int bookingCount = staffPage.getBookingCount();
        assertThat(bookingCount)
            .as("Booking count should be non-negative")
            .isGreaterThanOrEqualTo(1);
    }

    /**
     * BDD Test 3: Staff can filter bookings by municipality
     *
     * Given: A staff member is on the staff page with bookings displayed
     * When: The staff member searches for a specific municipality
     * Then: Only bookings from that municipality should be shown (or empty result)
     */
    ///@Test
    @DisplayName("TC3: Staff can filter bookings by municipality")
    void testStaffFiltersBookingsByMunicipality() {
        // Given
        staffPage.navigateTo(baseUrl);
        int totalCount = staffPage.getBookingCount();

        // When
        staffPage.searchByMunicipality("Aveiro");

        // Then
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(staffPage.isTableDisplayed())
            .as("Table should still be displayed after filtering")
            .isTrue();

        var municipalities = staffPage.getBookingMunicipalities();
        assertThat(municipalities)
            .as(
                "All displayed municipalities should be Aveiro (if any bookings shown)"
            )
            .allMatch(m -> m.isEmpty() || m.equals("Aveiro"));
    }

    /**
     * BDD Test 4: Citizen can check booking status by token
     *
     * Given: A citizen is on the check page
     * When: The citizen enters a token and clicks check
     * Then: The page should display booking details or a not-found message
     */
    ///@Test
    @DisplayName("TC4: Citizen can check booking status")
    void testCitizenChecksBookingStatus() {
        // Given
        citizenCheckPage.navigateTo(baseUrl);
        assertThat(citizenCheckPage.isPageDisplayed())
            .as("Citizen check page should be displayed")
            .isTrue();

        // When
        citizenCheckPage.fillToken("INVALID-TOKEN-12345");
        citizenCheckPage.clickCheck();

        // Then
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String result = citizenCheckPage.getCheckResultContent();
        assertThat(result)
            .as("Result should be displayed (either booking data or not found)")
            .isNotEmpty();
    }

    /**
     * BDD Test 5: Staff can see citizen names and booking statuses
     *
     * Given: A staff member is on the staff page
     * When: Bookings are displayed
     * Then: Citizen names and statuses should be visible in the table
     */
    @Test
    @DisplayName("TC5: Staff can view citizen details and booking statuses")
    void testStaffSeesCitizenDetailsAndStatuses() {
        // Given
        staffPage.navigateTo(baseUrl);

        // When
        var citizenNames = staffPage.getBookingCitizenNames();
        var statuses = staffPage.getBookingStatuses();

        // Then
        assertThat(citizenNames)
            .as("Citizen names should be retrievable")
            .isNotNull();

        assertThat(statuses)
            .as("Booking statuses should be retrievable")
            .isNotNull();

        if (staffPage.getBookingCount() > 0) {
            assertThat(citizenNames)
                .as("Should have citizen names if bookings exist")
                .isNotEmpty();
            assertThat(statuses)
                .as("Should have statuses if bookings exist")
                .isNotEmpty();
        }
    }
}
