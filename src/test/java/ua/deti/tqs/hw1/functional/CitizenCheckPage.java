package ua.deti.tqs.hw1.functional;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object Model for the citizen check page.
 * Provides methods to check booking status by token.
 */
public class CitizenCheckPage {

    private WebDriver driver;
    private WebDriverWait wait;

    private final By tokenInput = By.id("token");
    private final By checkButton = By.id("checkBtn");
    private final By checkResultPre = By.id("checkResult");
    private final By cancelButtonContainer = By.id("cancelButtonContainer");
    private final By cancelBookingButton = By.id("cancelBookingBtn");

    public CitizenCheckPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/citizen/check");
        wait.until(ExpectedConditions.presenceOfElementLocated(tokenInput));
    }

    public void fillToken(String token) {
        WebElement tokenField = wait.until(
            ExpectedConditions.visibilityOfElementLocated(tokenInput)
        );
        tokenField.clear();
        tokenField.sendKeys(token);
    }

    public void clickCheck() {
        WebElement checkBtn = wait.until(
            ExpectedConditions.elementToBeClickable(checkButton)
        );
        checkBtn.click();
    }

    public String getCheckResultContent() {
        try {
            WebElement result = wait.until(
                ExpectedConditions.visibilityOfElementLocated(checkResultPre)
            );
            return result.getText();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isCancelButtonVisible() {
        try {
            return wait
                .until(
                    ExpectedConditions.visibilityOfElementLocated(
                        cancelButtonContainer
                    )
                )
                .isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isPageDisplayed() {
        try {
            return wait
                .until(
                    ExpectedConditions.visibilityOfElementLocated(tokenInput)
                )
                .isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getTokenInputValue() {
        WebElement token = driver.findElement(tokenInput);
        return token.getDomProperty("value");
    }

    public void clickCancelBooking() {
        try {
            WebElement cancelBtn = wait.until(
                ExpectedConditions.elementToBeClickable(cancelBookingButton)
            );
            cancelBtn.click();
        } catch (Exception e) {
            // If cancel button not found, try clicking through the container
            WebElement container = driver.findElement(cancelButtonContainer);
            WebElement button = container.findElement(By.tagName("button"));
            button.click();
        }
    }

    public void confirmCancellation() {
        try {
            // Find the confirm button in the overlay/dialog
            // The confirmation dialog creates buttons with class "btn"
            // We need to find the one that says "Confirm"
            wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.xpath("//button[contains(text(), 'Confirm')]")
                )
            );
            WebElement confirmBtn = driver.findElement(
                By.xpath("//button[contains(text(), 'Confirm')]")
            );
            confirmBtn.click();
        } catch (Exception e) {
            throw new RuntimeException(
                "Could not find or click confirm button: " + e.getMessage()
            );
        }
    }

    public String getCheckResultContentAfterCancel() {
        try {
            // Wait for the result to be updated with the cancelled status
            WebElement result = wait.until(
                ExpectedConditions.presenceOfElementLocated(checkResultPre)
            );
            String content = result.getText();
            // Wait until the content contains CANCELLED status
            wait.until(
                ExpectedConditions.textToBePresentInElement(result, "CANCELLED")
            );
            return content;
        } catch (Exception e) {
            return "";
        }
    }
}
