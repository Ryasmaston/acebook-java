package com.makersacademy.acebook.feature;

import com.github.javafaker.Faker;
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
public class SignOutTest {
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
    public void testSignOutButtonShowsWhenSignedIn() {
        authHelper.signUpTestUser(testUserEmail, "TestPA@ssword");
        wait.until(ExpectedConditions.urlContains("/posts"));

        assertTrue(driver.findElement(By.id("signout-button")).isDisplayed());
    }

    @Test
    public void testSigningOutLogsUserOut() {
        authHelper.signUpTestUser(testUserEmail, "TestPA@ssword");
        wait.until(ExpectedConditions.urlContains("/posts"));

        driver.findElement(By.id("signout-button")).click();

        wait.until(ExpectedConditions.urlContains("auth0.com"));

        assertTrue(driver.findElements(By.id("greeting")).isEmpty());
    }
}