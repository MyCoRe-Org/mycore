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
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.iview.tests.TestProperties;
import org.mycore.iview.tests.ViewerTestBase;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class ControllerUtil {

    public static final String RESULT_FOLDER = "test.result.folder";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Properties TEST_PROPERTIES = TestProperties.getInstance();
    public static final String SCREENSHOT_FOLDER = TEST_PROPERTIES.getProperty(RESULT_FOLDER) + "/screenshots/";

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
            ViewerTestBase.sleep(1000);
            ByteArrayInputStream input = new ByteArrayInputStream(screenshot.getScreenshotAs(OutputType.BYTES));
            byte[] imageBytes = input.readAllBytes();
            Path pDir = Paths.get(SCREENSHOT_FOLDER);
            Files.createDirectories(pDir);
            Path pFile = pDir.resolve(name + ".png");
            Files.copy(new ByteArrayInputStream(imageBytes), pFile, StandardCopyOption.REPLACE_EXISTING);
            return ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            LOGGER.error("Error while taking or saving screenshot", e);
            throw new UnsupportedOperationException("Error while taking or saving screenshot", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve the interrupted status
            LOGGER.error("Screenshot capture interrupted", e);
            throw new MCRException("Screenshot capture interrupted", e);
        }
    }

}
