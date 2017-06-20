package org.mycore.iview.tests.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.iview.tests.TestProperties;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class ControllerUtil {

    private static final Logger LOGGER = LogManager.getLogger(ControllerUtil.class);

    private static final Properties TEST_PROPERTIES = TestProperties.getInstance();

    public static final String RESULT_FOLDER = "test.result.folder";

    public static final String SCREENSHOT_FOLDER = TEST_PROPERTIES.getProperty(RESULT_FOLDER) + "/screenshots/";

    /**
     * Waits until the Page is fully loaded
     * 
     * @param driver
     */
    public static void waitForPageReady(WebDriver driver) {
        driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);
        //WebDriverWait webDriverWait = new WebDriverWait(driver, 10);
        //webDriverWait.until(new Predicate<WebDriver>() {
        //    @Override
        //    public boolean apply(WebDriver webDriver) {
        //        JavascriptExecutor js = (JavascriptExecutor) webDriver;
        //        return js.executeScript("return document.readyState").equals("complete");
        //    }
        //});
    }

    /**
     * gets a screenshot from the browsers content
     * 
     * @param driver
     * @return screenshot
     */
    public static BufferedImage getScreenshot(WebDriver driver, String name) {
        if (driver instanceof TakesScreenshot) {
            try {
                Thread.sleep(1000);
                ByteArrayInputStream input = new ByteArrayInputStream(((TakesScreenshot) driver)
                    .getScreenshotAs(OutputType.BYTES));
                byte[] imageBytes = IOUtils.toByteArray(input);
                new File(SCREENSHOT_FOLDER).mkdirs();
                IOUtils.copy(new ByteArrayInputStream(imageBytes),
                    new FileOutputStream(new File(SCREENSHOT_FOLDER + name + ".png")));
                return ImageIO.read(new ByteArrayInputStream(imageBytes));
            } catch (IOException e) {
                LOGGER.error("Error while taking screenshot!");
                throw new UnsupportedOperationException("Error while taking screenshot!", e);
            } catch (InterruptedException e) {
                LOGGER.error("Error while taking screenshot!");
                throw new RuntimeException("Error while taking screenshot!", e);
            }
        } else {
            throw new UnsupportedOperationException(
                "Error while taking screenshot! (driver not instanceof TakesScreenshot)");
        }
    }

}
