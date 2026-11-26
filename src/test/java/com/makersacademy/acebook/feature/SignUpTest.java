package com.makersacademy.acebook.feature;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

@ActiveProfiles("test")
@SpringBootTest()
public class SignUpTest {
    WebDriver driver;
    Faker faker;

    @BeforeEach
    public void setup() {
        driver = new ChromeDriver();
        faker = new Faker();
    }

    @AfterEach
    public void tearDown() {
        driver.close();
    }

    @Test
    public void successfulSignUpAlsoLogsInUser() {
        String email = faker.name().username() + "@email.com";

        driver.get("http://localhost:8081/");
        driver.findElement(By.linkText("Sign up")).click();
        driver.findElement(By.name("email")).sendKeys(email);
        driver.findElement(By.name("password")).sendKeys("P@55qw0rd");
        driver.findElement(By.name("action")).click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("greeting")));
        String greetingText = driver.findElement(By.id("greeting")).getText();
        assertEquals("Signed in as " + email, greetingText);
    }
}
