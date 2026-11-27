package com.makersacademy.acebook.feature;

import com.github.javafaker.Faker;
import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.PostRepository;
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


@ActiveProfiles("test")
@SpringBootTest()
public class PostTest {
    @Autowired
    public UserRepository userRepository;

    @Autowired
    public PostRepository postRepository;

    private WebDriverWait wait;
    WebDriver driver;
    Faker faker;
    String testUserEmail;
    AuthHelper authHelper;
    Post testPost;
    User testUser;

    //This block will run before each test and allows you to set-up your test data
    /* CHALLENGE: keep expanding/adapting this block to meet the needs of your tests
    /   - when you create your own test suites, work with some new BeforeEach/AfterEach flows as well
        - you can also investigate BeforeAll and AfterAll blocks as well for other types of operations and testing set-ups */
    @BeforeEach
    public void setup() {
        driver = new ChromeDriver();
        faker = new Faker();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        authHelper = new AuthHelper(driver);
        testUserEmail = faker.name().username() + "@email.com";

        //Saving a test user that we will use throughout the tests
        testUser = new User(testUserEmail);
        userRepository.save(testUser);

        //Creating a post linked to this test user
        testPost = new Post();
        testPost.setUser(testUser);
        testPost.setContent("This is a test post");
        postRepository.save(testPost);
    }

    //This block will run after each test
    @AfterEach
    public void tearDown() {
        //Clean up the test data after each test
        postRepository.deleteAll();
        userRepository.deleteAll();

        /*This will close the Selenium Chrome window used for the test.
        * CHALLENGE: What do you think would happen if you omit this line?
        * Can you think of a scenario where omitting it might work in your favour/show you something useful?*/
        driver.close();
    }

    @Test
    public void testIndexPageShowsAllPosts() {
        authHelper.signUpTestUser(testUserEmail, "TestPA@ssword");
        /* Sometimes Selenium takes a little bit of time to load the next page -
        /* wait() gives it a chance to load a specific element before we try to person a test on it. */
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("post-" + testPost.getId())
        ));
        String postText = driver.findElement(By.id("post-" + testPost.getId())).getText();
        assertEquals(postText, testPost.getContent());
    }

    @Test
    public void testUserCanCreateNewPost() {
        authHelper.signUpTestUser(testUserEmail, "TestPA@ssword");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("new-post-content")
        ));

        driver.findElement(By.id("new-post-content")).sendKeys("This is a new test post");
        driver.findElement(By.id("submit-new-post")).click();

        long newPostId = testPost.getId() + 1;

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("post-" + newPostId)
        ));

        String newPostText = driver.findElement(By.id("post-" + newPostId)).getText();
        assertEquals("This is a new test post", newPostText);
    }

    @Test
    public void testCannotPostIfLoggedInUserDoesNotExist() {
        authHelper.signUpTestUser(testUserEmail, "TestPA@ssword");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("new-post-content")
        ));

        postRepository.delete(testPost);
        userRepository.delete(testUser);

        driver.findElement(By.id("new-post-content")).sendKeys("This is a new test post");
        driver.findElement(By.id("submit-new-post")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.id("error-message")
        ));

        String errorMessage = driver.findElement(By.id("error-message")).getText();
        assertEquals("Operation failed - try again later", errorMessage);
    }

    @Test
    public void testUserCanEditPost() {
        authHelper.signUpTestUser(testUserEmail, "TestPA@ssword");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("new-post-content")));
        driver.findElement(By.id("new-post-content")).sendKeys("This is a new test post");
        driver.findElement(By.id("submit-new-post")).click();
        long newPostId = testPost.getId() + 1;
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("post-" + newPostId)));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("edit-" + newPostId))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("new-post-content")));
        driver.findElement(By.id("new-post-content")).sendKeys(", now edited");
        driver.findElement(By.id("submit-new-post")).click();

        wait.until(ExpectedConditions.textToBe(By.id("post-" + newPostId),
                "This is a new test post, now edited"));
        assertEquals("This is a new test post, now edited",
                driver.findElement(By.id("post-" + newPostId)).getText());
    }
  
    @Test
    public void testUserCanDeletePost() {
        authHelper.signUpTestUser(testUserEmail, "TestPA@ssword");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("new-post-content")
        ));

        driver.findElement(By.id("new-post-content")).sendKeys("This is a new test post");
        driver.findElement(By.id("submit-new-post")).click();

        By newPost = By.xpath("//li[contains(., 'This is a new test post')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(newPost));

        long countBefore = postRepository.count();

        driver.findElement(By.xpath(
                "//li[contains(., 'This is a new test post')]/following-sibling::div//form//button")).click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(newPost));

        assertEquals(countBefore - 1, postRepository.count());
    }
  
}

