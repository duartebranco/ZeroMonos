package ua.deti.tqs.hw1.functional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
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
    private final By tableRows = By.xpath("//table[@id='bookingsTable']//tbody/tr");
    private final By noBookingsMessage = By.xpath("//td[contains(text(), 'No bookings found')]");

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
            ExpectedConditions.visibilityOfElementLocated(municipalitySearchInput)
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
        // Wait for table to update
        wait.until(ExpectedConditions.stalenessOf(driver.findElement(tableRows)));
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(tableRows));
    }

    public void clickRefresh() {
        WebElement refreshBtn = wait.until(
            ExpectedConditions.elementToBeClickable(refreshButton)
        );
        refreshBtn.click();
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(tableRows));
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
            .filter(row -> !row.getText().contains("No bookings found"))
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
            wait.until(ExpectedConditions.visibilityOfElementLocated(noBookingsMessage));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTableDisplayed() {
        try {
            return wait
                .until(ExpectedConditions.visibilityOfElementLocated(bookingsTable))
                .isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getMunicipalitySearchValue() {
        WebElement searchInput = driver.findElement(municipalitySearchInput);
        return searchInput.getAttribute("value");
    }

    public void clearMunicipalitySearch() {
        WebElement searchInput = driver.findElement(municipalitySearchInput);
        searchInput.clear();
    }
}
