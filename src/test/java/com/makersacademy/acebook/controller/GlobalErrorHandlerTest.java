package com.makersacademy.acebook.controller;

import com.github.javafaker.Faker;
import com.makersacademy.acebook.feature.AuthHelper;
import com.makersacademy.acebook.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest()
public class GlobalErrorHandlerTest {
    @Autowired
    public UserRepository userRepository;

    private WebDriverWait wait;
    WebDriver driver;
    Faker faker;
    String testUserEmail;
    AuthHelper authHelper;

    @BeforeEach
    public void setup() {
        driver = new ChromeDriver();
        faker = new Faker();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        authHelper = new AuthHelper(driver);
        testUserEmail = faker.name().username() + "@email.com";
    }

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
        driver.quit();
    }

    @Test
    public void testUnknownUrlShowsThe404Page() {
        authHelper.signUpTestUser(testUserEmail, "TestPA@ssword");
        wait.until(ExpectedConditions.urlContains("/posts"));

        driver.get("http://localhost:8081/users/unknownpage");

        String pageText = driver.findElement(By.tagName("body")).getText();
        assertTrue(pageText.contains("You done got 404"));
    }

    @Test
    public void testIllegalStateExceptionShowsErrorPageWithMessage() {
        authHelper.signUpTestUser(testUserEmail, "TestPA@ssword");
        wait.until(ExpectedConditions.urlContains("/posts"));

        userRepository.deleteAll();

        driver.findElement(By.id("new-post-content")).sendKeys("This post will fail");
        driver.findElement(By.id("submit-new-post")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));
        String pageText = driver.findElement(By.tagName("h1")).getText();
        assertTrue(pageText.contains("Something went wrong!"));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("error-message")));
        String message = driver.findElement(By.id("error-message")).getText();
        assertTrue(message.contains("Operation failed - try again later"));
    }
}

