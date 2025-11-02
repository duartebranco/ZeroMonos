package ua.deti.tqs.hw1.functional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object Model for the staff bookings page.
 * Provides methods to interact with the bookings table and filters.
 */
public class StaffPage {

    private WebDriver driver;
    private WebDriverWait wait;

    private final By municipalitySearchInput = By.id("searchMunicipality");
    private final By searchButton = By.id("searchBtn");
    private final By refreshButton = By.id("refreshBtn");
    private final By bookingsTable = By.id("bookingsTable");
    private final By tableRows = By.xpath(
        "//table[@id='bookingsTable']//tbody/tr"
    );
    private final By noBookingsMessage = By.xpath(
        "//td[contains(text(), 'No bookings found')]"
    );

    public StaffPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/staff");
        wait.until(ExpectedConditions.presenceOfElementLocated(bookingsTable));
    }

    public void searchByMunicipality(String municipality) {
        WebElement searchInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                municipalitySearchInput
            )
        );
        searchInput.clear();
        searchInput.sendKeys(municipality);
        clickSearch();
    }

    public void clickSearch() {
        WebElement searchBtn = wait.until(
            ExpectedConditions.elementToBeClickable(searchButton)
        );
        searchBtn.click();
        // Wait for table to be updated after search
        wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(tableRows)
        );
    }

    public void clickRefresh() {
        WebElement refreshBtn = wait.until(
            ExpectedConditions.elementToBeClickable(refreshButton)
        );
        refreshBtn.click();
        wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(tableRows)
        );
    }

    public int getBookingCount() {
        try {
            List<WebElement> rows = driver.findElements(tableRows);
            // Filter out the "no bookings found" row if it exists
            return (int) rows
                .stream()
                .filter(row -> !row.getText().contains("No bookings found"))
                .count();
        } catch (Exception e) {
            return 0;
        }
    }

    public List<String> getBookingMunicipalities() {
        return driver
            .findElements(tableRows)
            .stream()
            .filter(
                row ->
                    !row.getText().contains("No bookings found") &&
                    row.isDisplayed()
            )
            .map(row -> {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                return cells.size() > 3 ? cells.get(3).getText() : "";
            })
            .collect(Collectors.toList());
    }

    public List<String> getBookingCitizenNames() {
        return driver
            .findElements(tableRows)
            .stream()
            .filter(row -> !row.getText().contains("No bookings found"))
            .map(row -> {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                return cells.size() > 2 ? cells.get(2).getText() : "";
            })
            .collect(Collectors.toList());
    }

    public List<String> getBookingStatuses() {
        return driver
            .findElements(tableRows)
            .stream()
            .filter(row -> !row.getText().contains("No bookings found"))
            .map(row -> {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                return cells.size() > 7 ? cells.get(7).getText() : "";
            })
            .collect(Collectors.toList());
    }

    public boolean isNoBookingsMessageDisplayed() {
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(noBookingsMessage)
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTableDisplayed() {
        try {
            return wait
                .until(
                    ExpectedConditions.visibilityOfElementLocated(bookingsTable)
                )
                .isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getMunicipalitySearchValue() {
        WebElement searchInput = driver.findElement(municipalitySearchInput);
        return searchInput.getDomProperty("value");
    }

    public void clearMunicipalitySearch() {
        WebElement searchInput = driver.findElement(municipalitySearchInput);
        searchInput.clear();
    }

    /**
     * Get all booking details (as a list of maps with column names as keys)
     * Columns: id, token, citizenName, municipality, itemType, description, date, status
     */
    public List<java.util.Map<String, String>> getAllBookingDetails() {
        return driver
            .findElements(tableRows)
            .stream()
            .filter(
                row ->
                    !row.getText().contains("No bookings found") &&
                    row.isDisplayed()
            )
            .map(row -> {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                java.util.Map<String, String> booking =
                    new java.util.HashMap<>();
                if (cells.size() > 0) booking.put("id", cells.get(0).getText());
                if (cells.size() > 1) booking.put(
                    "token",
                    cells.get(1).getText()
                );
                if (cells.size() > 2) booking.put(
                    "citizenName",
                    cells.get(2).getText()
                );
                if (cells.size() > 3) booking.put(
                    "municipality",
                    cells.get(3).getText()
                );
                if (cells.size() > 4) booking.put(
                    "itemType",
                    cells.get(4).getText()
                );
                if (cells.size() > 5) booking.put(
                    "description",
                    cells.get(5).getText()
                );
                if (cells.size() > 6) booking.put(
                    "date",
                    cells.get(6).getText()
                );
                if (cells.size() > 7) booking.put(
                    "status",
                    cells.get(7).getText()
                );
                return booking;
            })
            .collect(Collectors.toList());
    }

    /**
     * Find a booking that matches all provided criteria
     * @param citizenName the citizen name to match
     * @param municipality the municipality to match
     * @param date the booking date to match (partial match - just the date part)
     * @param description the item description to match
     * @return true if a booking matching all criteria is found
     */
    public boolean findBookingByCriteria(
        String citizenName,
        String municipality,
        String date,
        String description
    ) {
        return getAllBookingDetails()
            .stream()
            .anyMatch(
                booking ->
                    booking
                        .getOrDefault("citizenName", "")
                        .equals(citizenName) &&
                    booking
                        .getOrDefault("municipality", "")
                        .equals(municipality) &&
                    booking.getOrDefault("date", "").startsWith(date) &&
                    booking.getOrDefault("description", "").equals(description)
            );
    }

    /**
     * Find a booking by its token and return the booking ID
     * @param token the booking token to search for
     * @return the booking ID or empty string if not found
     */
    public String findBookingIdByToken(String token) {
        try {
            return getAllBookingDetails()
                .stream()
                .filter(booking ->
                    booking.getOrDefault("token", "").equals(token)
                )
                .map(booking -> booking.get("id"))
                .findFirst()
                .orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if a booking with the given token exists
     * @param token the booking token to search for
     * @return true if a booking with this token exists, false otherwise
     */
    public boolean bookingExistsByToken(String token) {
        try {
            return getAllBookingDetails()
                .stream()
                .anyMatch(booking ->
                    booking.getOrDefault("token", "").equals(token)
                );
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Change the status of a booking by its booking ID
     * @param bookingId the ID of the booking to update
     * @param newStatus the new status (e.g., "ASSIGNED", "IN_PROGRESS", "COMPLETED")
     * @return true if status change was successful, false otherwise
     */
    public boolean changeBookingStatus(String bookingId, String newStatus) {
        try {
            // Find the row for this booking
            List<WebElement> rows = driver.findElements(tableRows);
            WebElement targetRow = null;

            for (WebElement row : rows) {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                if (
                    cells.size() > 0 && cells.get(0).getText().equals(bookingId)
                ) {
                    targetRow = row;
                    break;
                }
            }

            if (targetRow == null) {
                return false;
            }

            // Find the select dropdown in the Actions column (column 8)
            List<WebElement> cells = targetRow.findElements(By.tagName("td"));
            if (cells.size() <= 8) {
                return false;
            }

            WebElement actionCell = cells.get(8);
            WebElement statusSelect = actionCell.findElement(
                By.tagName("select")
            );

            // Select the new status from dropdown
            Select select = new Select(statusSelect);
            select.selectByValue(newStatus);

            // Wait for confirmation dialog to appear and handle it
            try {
                Alert alert = wait.until(ExpectedConditions.alertIsPresent());
                alert.accept();
            } catch (Exception e) {
                // If no alert appears, that's ok - the dialog might be custom
                // Try clicking the confirm button in the custom dialog
                try {
                    WebElement confirmBtn = wait.until(
                        ExpectedConditions.elementToBeClickable(
                            By.xpath(
                                "//button[contains(@class, 'btn') and contains(text(), 'Confirm')]"
                            )
                        )
                    );
                    confirmBtn.click();
                } catch (Exception ex) {
                    // Dialog handling failed, but selection was made
                }
            }

            // Wait for the table to be updated after status change
            wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(tableRows)
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the current status of a booking by its booking ID
     * @param bookingId the ID of the booking
     * @return the status text or empty string if not found
     */
    public String getBookingStatus(String bookingId) {
        try {
            List<WebElement> rows = driver.findElements(tableRows);

            for (WebElement row : rows) {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                if (
                    cells.size() > 7 && cells.get(0).getText().equals(bookingId)
                ) {
                    return cells.get(7).getText();
                }
            }
        } catch (Exception e) {
            // Return empty if error
        }
        return "";
    }
}
