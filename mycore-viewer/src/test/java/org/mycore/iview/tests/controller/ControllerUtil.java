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
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

public class ControllerUtil {

    public static final String RESULT_FOLDER = "test.result.folder";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Properties TEST_PROPERTIES = TestProperties.getInstance();
    public static final String SCREENSHOT_FOLDER = TEST_PROPERTIES.getProperty(RESULT_FOLDER) + "/screenshots/";

    /**
     * The canvas of a single viewer. Its page controller publishes {@link #PENDING_RENDER_OPERATIONS} on it, so a
     * second viewer on the same page cannot keep this one from ever looking settled.
     */
    private static final By VIEWER_CANVAS = By.cssSelector(".mycoreViewer .mainView");

    /**
     * Name of the function the viewer publishes on {@link #VIEWER_CANVAS}. Defined as
     * <code>PENDING_RENDER_OPERATIONS</code> by
     * <code>src/main/typescript/modules/base/widgets/canvas/PageController.ts</code>.
     */
    private static final String PENDING_RENDER_OPERATIONS = "mcrPendingRenderOperations";

    private static final Duration SETTLE_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration SETTLE_POLLING_INTERVAL = Duration.ofMillis(100);

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
            byte[] imageBytes = awaitSettledRendering(driver, screenshot, driver.findElement(VIEWER_CANVAS));
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
     * Captures the viewport once the viewer reports that it has nothing left to paint and returns that capture.
     * <p>
     * The canvas is cleared before every redraw and stays blank while image tiles or PDF pages are still loading, and
     * zooming and page changes are painted as an animation. A capture that does not change therefore does not mean
     * that the viewer is done - it may just as well be an intermediate blank or a stale frame. So instead of waiting
     * for a fixed delay this asks the viewer how many render operations are still pending and only accepts a capture
     * that was taken with nothing pending, twice in a row.
     *
     * @return the PNG bytes of the finished viewport
     */
    private static byte[] awaitSettledRendering(WebDriver driver, TakesScreenshot screenshot, WebElement canvas) {
        Capture capture = new Capture((JavascriptExecutor) driver, screenshot, canvas);
        long start = System.nanoTime();
        try {
            return new FluentWait<>(driver)
                .withTimeout(SETTLE_TIMEOUT)
                .pollingEvery(SETTLE_POLLING_INTERVAL)
                .ignoring(StaleElementReferenceException.class)
                .withMessage(capture::describeState)
                .until(ignored -> capture.captureIfSettled());
        } finally {
            ViewerTestBase.addWaitTime(Duration.ofNanos(System.nanoTime() - start));
        }
    }

    private static final class Capture {

        private final JavascriptExecutor javascript;

        private final TakesScreenshot screenshot;

        private final WebElement canvas;

        private byte[] last;

        private Long pending;

        private boolean lastWasSettled;

        private int polls;

        private Capture(JavascriptExecutor javascript, TakesScreenshot screenshot, WebElement canvas) {
            this.javascript = javascript;
            this.screenshot = screenshot;
            this.canvas = canvas;
        }

        private String describeState() {
            return pending == null
                ? "the viewer to publish '" + PENDING_RENDER_OPERATIONS + "' on its canvas"
                : "the viewer to finish painting, " + pending + " operation(s) still pending";
        }

        /**
         * Reads the render state before capturing, so that a redraw scheduled after the read shows up as pending in
         * the next round instead of being missed.
         *
         * @return the current capture if it equals the previous one and both were taken with nothing pending, else
         *         {@code null}
         */
        private byte[] captureIfSettled() {
            pending = readPendingRenderOperations();
            LOGGER.debug("Poll {}: {} render operation(s) pending", ++polls, pending);
            boolean settled = pending != null && pending == 0;
            byte[] current = screenshot.getScreenshotAs(OutputType.BYTES);
            boolean done = settled && lastWasSettled && Arrays.equals(last, current);
            last = current;
            lastWasSettled = settled;
            return done ? current : null;
        }

        /**
         * @return the pending render operations of the canvas, or {@code null} while the viewer has not published
         *         them yet
         */
        private Long readPendingRenderOperations() {
            Object result = javascript.executeScript("const canvas = arguments[0], name = arguments[1];"
                + "return typeof canvas[name] === 'function' ? canvas[name]() : null;", canvas,
                PENDING_RENDER_OPERATIONS);
            if (result == null) {
                return null;
            }
            if (!(result instanceof Number number)) {
                throw new IllegalStateException(
                    PENDING_RENDER_OPERATIONS + " returned " + result + " instead of a number");
            }
            return number.longValue();
        }
    }

}
