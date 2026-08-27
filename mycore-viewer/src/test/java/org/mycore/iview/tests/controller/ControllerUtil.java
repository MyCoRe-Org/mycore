/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.iview.tests.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.iview.tests.TestProperties;
import org.mycore.iview.tests.ViewerTestBase;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.FluentWait;

public class ControllerUtil {

    public static final String RESULT_FOLDER = "test.result.folder";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Properties TEST_PROPERTIES = TestProperties.getInstance();
    public static final String SCREENSHOT_FOLDER = TEST_PROPERTIES.getProperty(RESULT_FOLDER) + "/screenshots/";

    /**
     * Number of consecutive identical captures that are required before the rendering is considered settled.
     */
    private static final int STABLE_CAPTURES = 2;

    private static final Duration STABLE_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration STABLE_POLLING_INTERVAL = Duration.ofMillis(100);

    /**
     * Waits until the Page is fully loaded
     * 
     * @param driver
     */
    public static void waitForPageReady(WebDriver driver) {
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
    }

    /**
     * gets a screenshot from the browsers content
     * 
     * @param driver
     * @return screenshot
     */
    public static BufferedImage getScreenshot(WebDriver driver, String name) {
        if (!(driver instanceof TakesScreenshot screenshot)) {
            throw new UnsupportedOperationException(
                "Error while taking screenshot! (driver not instanceof TakesScreenshot)");
        }
        try {
            byte[] imageBytes = awaitSettledRendering(driver, screenshot);
            Path pDir = Paths.get(SCREENSHOT_FOLDER);
            Files.createDirectories(pDir);
            Path pFile = pDir.resolve(name + ".png");
            Files.copy(new ByteArrayInputStream(imageBytes), pFile, StandardCopyOption.REPLACE_EXISTING);
            return ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            LOGGER.error("Error while taking or saving screenshot", e);
            throw new UnsupportedOperationException("Error while taking or saving screenshot", e);
        }
    }

    /**
     * Captures the viewport until {@link #STABLE_CAPTURES} consecutive captures are identical and returns the last one.
     * The canvas of the image viewer paints zooming and image changes as an animation, so a capture taken right after
     * the triggering click would still show an intermediate frame. Waiting for the pixels to stop changing replaces
     * the fixed delays this method used to rely on and only costs as much time as the animation really needs.
     *
     * @return the PNG bytes of the settled viewport
     */
    private static byte[] awaitSettledRendering(WebDriver driver, TakesScreenshot screenshot) {
        Capture capture = new Capture(screenshot);
        long start = System.nanoTime();
        try {
            return new FluentWait<>(driver)
                .withTimeout(STABLE_TIMEOUT)
                .pollingEvery(STABLE_POLLING_INTERVAL)
                .ignoring(WebDriverException.class)
                .withMessage("rendering to settle")
                .until(ignored -> capture.captureIfSettled());
        } catch (TimeoutException e) {
            LOGGER.warn("Rendering did not settle within {}, using last capture.", STABLE_TIMEOUT);
            return capture.last();
        } finally {
            ViewerTestBase.addWaitTime(Duration.ofNanos(System.nanoTime() - start));
        }
    }

    private static final class Capture {

        private final TakesScreenshot screenshot;

        private byte[] last;

        private int identical;

        private Capture(TakesScreenshot screenshot) {
            this.screenshot = screenshot;
        }

        /**
         * @return the current capture once it repeated {@link ControllerUtil#STABLE_CAPTURES} times, else {@code null}
         */
        private byte[] captureIfSettled() {
            byte[] current = screenshot.getScreenshotAs(OutputType.BYTES);
            identical = Arrays.equals(last, current) ? identical + 1 : 0;
            last = current;
            return identical >= STABLE_CAPTURES ? current : null;
        }

        private byte[] last() {
            return last;
        }
    }

}
