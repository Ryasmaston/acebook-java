package com.makersacademy.acebook.feature;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.test.context.ActiveProfiles;
import java.time.Duration;

//This is a helper so that you don't have to rewrite the signup process for each test where you need it
//Helpers are very common when writing full-stack web applications, whether for tests or other parts of the app.
//Observe where it is currently used and how, and see if you can make further use of it in other places.

/* CHALLENGE: If you want to challenge yourself, maybe have a think throughout
 the project as to whether other parts of the code could be abstracted into reusable helpers.
 */

@ActiveProfiles("test")
public class AuthHelper {
    private final WebDriverWait wait;
    private WebDriver driver;

    public AuthHelper(WebDriver driver) {
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        this.driver = driver;
    }

    public void signUpTestUser(String email, String password) {
        driver.get("http://localhost:8081/");
        wait.until(ExpectedConditions.elementToBeClickable(
                By.linkText("Sign up")
        )).click();
        var emailField = wait.until(ExpectedConditions.elementToBeClickable(
                By.name("email")
        ));
        emailField.clear();
        emailField.sendKeys(email);
        var passwordField = wait.until(ExpectedConditions.elementToBeClickable(
                By.name("password")
        ));
        passwordField.clear();
        passwordField.sendKeys(password);
        wait.until(ExpectedConditions.elementToBeClickable(
                By.name("action")
        )).click();
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id("greeting")
            ));
        } catch (TimeoutException e) {
            System.out.println("CURRENT URL: " + driver.getCurrentUrl());
            System.out.println("PAGE TITLE: " + driver.getTitle());
            System.out.println("PAGE SOURCE: " + driver.getPageSource());

            throw e;
        }
    }
}
