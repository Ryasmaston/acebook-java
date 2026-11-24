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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest()
public class UsersControllerTest {
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
    public void testSignedInUserSeesTheirEmail() {
        authHelper.signUpTestUser(testUserEmail, "TestPA@ssword");
        wait.until(ExpectedConditions.urlContains("/posts"));

        String greeting = driver.findElement(By.id("greeting")).getText();
        assertTrue(greeting.contains(testUserEmail));
    }

    @Test
    public void testSigningInAgainDoesNotCreateDuplicateUser() {
        authHelper.signUpTestUser(testUserEmail, "TestPA@ssword");
        wait.until(ExpectedConditions.urlContains("/posts"));

        driver.get("http://localhost:8081/users/after-login");
        wait.until(ExpectedConditions.urlContains("/posts"));

        assertEquals(1, userRepository.count());
    }
}
