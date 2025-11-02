package ua.deti.tqs.hw1.functional;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object Model for the citizen booking/reserve page.
 * Provides methods to interact with the booking form.
 */
public class BookReservePage {

    private WebDriver driver;
    private WebDriverWait wait;

    private final By nameInput = By.id("name");
    private final By municipalityInput = By.id("municipality");
    private final By dateInput = By.id("date");
    private final By timeSlotSelect = By.id("timeSlot");
    private final By itemTypeSelect = By.id("itemType");
    private final By descriptionInput = By.id("description");
    private final By submitButton = By.xpath("//button[@type='submit']");
    private final By feedbackDiv = By.id("feedback");
    private final By checkResultPre = By.id("checkResult");
    private final By clearBtn = By.id("clearBtn");

    public BookReservePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/citizen/reserve");
        wait.until(ExpectedConditions.presenceOfElementLocated(nameInput));
    }

    private void triggerChangeEvent(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
            element
        );
    }

    private void triggerInputEvent(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
            element
        );
    }

    public void fillName(String name) {
        WebElement nameField = wait.until(
            ExpectedConditions.visibilityOfElementLocated(nameInput)
        );
        nameField.clear();
        nameField.sendKeys(name);
    }

    public void fillMunicipality(String municipality) {
        WebElement municipalityField = wait.until(
            ExpectedConditions.visibilityOfElementLocated(municipalityInput)
        );
        municipalityField.clear();
        municipalityField.sendKeys(municipality);
        triggerInputEvent(municipalityField);
        triggerChangeEvent(municipalityField);
    }

    public void fillDate(String date) {
        WebElement dateField = wait.until(
            ExpectedConditions.visibilityOfElementLocated(dateInput)
        );
        dateField.clear();
        dateField.sendKeys(date);
        triggerChangeEvent(dateField);
        // Wait for time slot options to be populated after date selection
        wait.until(ExpectedConditions.elementToBeClickable(By.id("timeSlot")));
    }

    public void selectTimeSlot(int index) {
        WebElement timeSlotElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(timeSlotSelect)
        );
        wait.until(ExpectedConditions.elementToBeClickable(timeSlotElement));

        Select select = new Select(timeSlotElement);
        select.selectByIndex(index + 1); // +1 to skip the "Select a time slot" option
    }

    public void selectItemType(int index) {
        WebElement itemTypeElement = wait.until(
            ExpectedConditions.visibilityOfElementLocated(itemTypeSelect)
        );
        Select select = new Select(itemTypeElement);
        select.selectByIndex(index + 1); // +1 to skip the "Select waste type" option
    }

    public void fillDescription(String description) {
        WebElement descriptionField = wait.until(
            ExpectedConditions.visibilityOfElementLocated(descriptionInput)
        );
        descriptionField.clear();
        descriptionField.sendKeys(description);
    }

    public void submit() {
        WebElement submitBtn = wait.until(
            ExpectedConditions.elementToBeClickable(submitButton)
        );
        submitBtn.click();
    }

    public String getFeedbackMessage() {
        try {
            WebElement feedback = wait.until(
                ExpectedConditions.visibilityOfElementLocated(feedbackDiv)
            );
            return feedback.getText();
        } catch (Exception e) {
            return "";
        }
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

    public void clear() {
        WebElement clearButton = wait.until(
            ExpectedConditions.elementToBeClickable(clearBtn)
        );
        clearButton.click();
    }

    public boolean isTimeSlotEnabled() {
        WebElement timeSlotElement = driver.findElement(timeSlotSelect);
        return timeSlotElement.isEnabled();
    }

    public boolean isFormDisplayed() {
        return wait
            .until(
                ExpectedConditions.presenceOfElementLocated(
                    By.id("bookingForm")
                )
            )
            .isDisplayed();
    }

    /**
     * Extract the token from the booking confirmation JSON response
     * @return the booking token or empty string if not found
     */
    public String extractTokenFromResult() {
        try {
            String resultText = getCheckResultContent();
            if (resultText == null || resultText.isEmpty()) {
                return "";
            }

            // Parse the JSON response to extract token
            int tokenIndex = resultText.indexOf("\"token\"");
            if (tokenIndex == -1) {
                return "";
            }

            // Find the value after "token": "
            int startIndex = resultText.indexOf("\"", tokenIndex + 8);
            if (startIndex == -1) {
                return "";
            }

            int endIndex = resultText.indexOf("\"", startIndex + 1);
            if (endIndex == -1) {
                return "";
            }

            return resultText.substring(startIndex + 1, endIndex);
        } catch (Exception e) {
            return "";
        }
    }
}
