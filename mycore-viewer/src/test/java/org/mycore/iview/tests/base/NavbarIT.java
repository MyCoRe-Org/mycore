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

package org.mycore.iview.tests.base;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mycore.iview.tests.ViewerTestBase;
import org.mycore.iview.tests.controller.ControllerUtil;
import org.mycore.iview.tests.controller.ImageViewerController;
import org.mycore.iview.tests.controller.StructureOverviewController;
import org.mycore.iview.tests.controller.ToolBarController;
import org.mycore.iview.tests.image.api.ColorFilter;
import org.mycore.iview.tests.image.api.FilterSelection;
import org.mycore.iview.tests.image.api.Selection;
import org.mycore.iview.tests.model.TestDerivate;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class NavbarIT extends ViewerTestBase {

    private static final String RGB_LABEL = "[4] - rgb.tiff";

    private static final String RED_LABEL = "[1] - r.png";

    private static final String GREEN_LABEL = "[2] - g.png";

    private static final String BLUE_LABEL = "[3] - b.png";

    private static final int TOLERANCE = 20;

    private static final Logger LOGGER = LogManager.getLogger();

    protected NavbarIT(WebDriver webDriver) {
        super(webDriver);
    }

    @Test
    public void testBasicElementsPresent() {
        this.getAppController().openViewer(this.getDriver(), getBaseURL(), this.getTestDerivate());
        WebDriver driver = this.getDriver();

        Assertions.assertTrue(driver.findElement(By.xpath("//*[@data-id='SidebarControllGroup']")).isDisplayed(),
            "Sidebar button should be present");
    }

    @Test
    public void testActionGroup() {
        this.getAppController().openViewer(this.getDriver(), getBaseURL(), this.getTestDerivate());
        WebDriver driver = this.getDriver();
        Assertions.assertTrue(driver.findElement(By.xpath("//*[@data-id='ShareButton']")).isDisplayed(),
            "Share button should be present");
    }

    @Test
    public void testModificationGroup() {
        this.getAppController().openViewer(this.getDriver(), getBaseURL(), this.getTestDerivate());
        WebDriver driver = this.getDriver();
        Assertions.assertTrue(driver.findElement(By.xpath("//*[@data-id='RotateButton']")).isDisplayed(),
            "RotateButton should be present");
    }

    @Test
    public void testImageChangeGroup() {
        this.getAppController().openViewer(this.getDriver(), getBaseURL(), this.getTestDerivate());
        WebDriver driver = this.getDriver();
        Assertions.assertTrue(driver.findElement(By.xpath("//*[@data-id='PreviousImageButton']")).isDisplayed(),
            "PreviousImageButton should be present");
        Assertions.assertTrue(driver.findElement(By.xpath("//*[@data-id='NextImageButton']")).isDisplayed(),
            "NextImageButton should be present");
    }

    @Test
    public void testZoomGroup() {
        this.getAppController().openViewer(this.getDriver(), getBaseURL(), this.getTestDerivate());
        WebDriver driver = this.getDriver();
        Assertions.assertTrue(driver.findElement(By.xpath("//*[@data-id='ZoomInButton']")).isDisplayed(),
            "ZoomIn button should be present");
        Assertions.assertTrue(driver.findElement(By.xpath("//*[@data-id='ZoomOutButton']")).isDisplayed(),
            "ZoomOut button should be present");
        Assertions.assertTrue(driver.findElement(By.xpath("//*[@data-id='ZoomWidthButton']")).isDisplayed(),
            "ZoomWidth button should be present");
        Assertions.assertTrue(driver.findElement(By.xpath("//*[@data-id='ZoomFitButton']")).isDisplayed(),
            "ZoomFit button should be present");
    }

    @Test
    public void testNavigationPrev() throws InterruptedException {
        this.getAppController().openViewer(this.getDriver(), getBaseURL(), getTestDerivate());

        ImageViewerController controller = this.getViewerController();

        ToolBarController tbController = controller.getToolBarController();

        int redPixelCountRGB = selectImgAndCountColor(tbController, getRgbLabel(), Color.RED);
        int greenPixelCountRGB = selectImgAndCountColor(tbController, getRgbLabel(), Color.GREEN);
        int bluePixelCountRGB = selectImgAndCountColor(tbController, getRgbLabel(), Color.BLUE);
        int bluePixelCount = selectPrevImgAndCountColor(tbController, getBlueLabel(), Color.BLUE);
        int greenPixelCount = selectPrevImgAndCountColor(tbController, getGreenLabel(), Color.GREEN);
        int redPixelCount = selectPrevImgAndCountColor(tbController, getRedLabel(), Color.RED);

        String messagePattern = "There should be less red pixels in the rgb screenshot than in the red ({0} > {1})";
        assertLess(redPixelCount, redPixelCountRGB, messagePattern);

        messagePattern = "There should be less green pixels in the rgb screenshot than in the green ({0} > {1})";
        assertLess(greenPixelCount, greenPixelCountRGB, messagePattern);

        messagePattern = "There should be less blue pixels in the rgb screenshot than in the blue ({0} > {1})";
        assertLess(bluePixelCount, bluePixelCountRGB, messagePattern);
    }

    @Test
    public void testNavigationNext() throws InterruptedException {
        this.getAppController().openViewer(this.getDriver(), getBaseURL(), getTestDerivate());

        ImageViewerController controller = this.getViewerController();

        ToolBarController tbController = controller.getToolBarController();

        int redPixelCountRGB = selectImgAndCountColor(tbController, getRgbLabel(), Color.RED);
        int greenPixelCountRGB = countColor(tbController, getRgbLabel(), Color.GREEN);
        int bluePixelCountRGB = countColor(tbController, getRgbLabel(), Color.BLUE);

        int redPixelCount = selectImgAndCountColor(tbController, getRedLabel(), Color.RED);
        String messagePattern = "There should be less red pixels in the rgb screenshot than in the red ({0} > {1})";
        assertLess(redPixelCount, redPixelCountRGB, messagePattern);

        int greenPixelCount = selectNextImgAndCountColor(tbController, getGreenLabel(), Color.GREEN);
        messagePattern = "There should be less green pixels in the rgb screenshot than in the green ({0} > {1})";
        assertLess(greenPixelCount, greenPixelCountRGB, messagePattern);

        int bluePixelCount = selectNextImgAndCountColor(tbController, getBlueLabel(), Color.BLUE);
        messagePattern = "There should be less blue pixels in the rgb screenshot than in the blue ({0} > {1})";
        assertLess(bluePixelCount, bluePixelCountRGB, messagePattern);
    }

    private int selectPrevImgAndCountColor(ToolBarController tbController, String label, Color color)
        throws InterruptedException {
        tbController.pressButton(ToolBarController.BUTTON_ID_PREV_IMG);
        sleep(500);

        return countColor(tbController, label, color);
    }

    private int selectNextImgAndCountColor(ToolBarController tbController, String label, Color color)
        throws InterruptedException {
        tbController.pressButton(ToolBarController.BUTTON_ID_NEXT_IMG);
        sleep(500);

        return countColor(tbController, label, color);
    }

    private int countColor(ToolBarController tbController, String label, Color color) {
        String message = label + " should be selected!";
        Assertions.assertTrue(tbController.isImageSelected(label), message);
        String fileName = String.format("%s-%s-%s-%s-%s", this.getClassname(), label, color.getRed(), color.getBlue(),
            color.getGreen());
        BufferedImage bImage = ControllerUtil.getScreenshot(getDriver(), fileName);

        return new FilterSelection(Selection.fromBufferedImage(bImage), new ColorFilter(color, false, TOLERANCE))
            .getPixel().size();
    }

    /**
     * selects an image with {@link StructureOverviewController#selectImageByOrder(String)} and counts Pixels of the color
     * 
     * @param tbController
     * @param label
     * @param color
     * @return
     * @throws InterruptedException
     */
    private int selectImgAndCountColor(ToolBarController tbController, String label, Color color)
        throws InterruptedException {
        tbController.selectPictureWithOrder(label);
        sleep(500);

        return countColor(tbController, label, color);
    }

    private void assertLess(int moreValue, int lessValue, String messagePattern) {
        String message = new MessageFormat(messagePattern, Locale.ROOT).format(new Object[] { lessValue, moreValue });
        LOGGER.debug(message);
        Assertions.assertTrue(lessValue < moreValue, message);
    }

    @Override
    public TestDerivate getTestDerivate() {
        return BaseTestConstants.RGB_TEST_DERIVATE;
    }

    /**
     * @return the rgbLabel
     */
    public String getRgbLabel() {
        return RGB_LABEL;
    }

    /**
     * @return the blueLabel
     */
    public String getBlueLabel() {
        return BLUE_LABEL;
    }

    /**
     * @return the greenLabel
     */
    public String getGreenLabel() {
        return GREEN_LABEL;
    }

    /**
     * @return the redLabel
     */
    public String getRedLabel() {
        return RED_LABEL;
    }
}
