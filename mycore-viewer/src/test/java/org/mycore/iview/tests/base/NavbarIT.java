/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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

@Category(org.mycore.iview.tests.groups.ImageViewerTests.class)
public class NavbarIT extends ViewerTestBase {

    private static final String RGB_LABEL = "[4] - rgb.tiff";

    private static final String RED_LABEL = "[1] - r.png";

    private static final String GREEN_LABEL = "[2] - g.png";

    private static final String BLUE_LABEL = "[3] - b.png";

    private static final int TOLERANCE = 20;

    private static final Logger LOGGER = LogManager.getLogger(NavbarIT.class);

    @Test
    public void testBasicElementsPresent() throws Exception {
        this.setTestName(getClassname() + "-testBasicElementsPresent");
        this.getAppController().openViewer(this.getDriver(), this.getTestDerivate());
        WebDriver driver = this.getDriver();

        Assert.assertTrue("Sidebar button should be present",
            driver.findElement(By.xpath("//*[@data-id='SidebarControllGroup']")).isDisplayed());
    }

    @Test
    public void testActionGroup() {
        this.setTestName(getClassname() + "-testActionElementsPresent");
        this.getAppController().openViewer(this.getDriver(), this.getTestDerivate());
        WebDriver driver = this.getDriver();
        Assert.assertTrue("Share button should be present", driver.findElement(By.xpath("//*[@data-id='ShareButton']"))
            .isDisplayed());
    }

    @Test
    public void testModificationGroup() {
        this.setTestName(getClassname() + "-testModifactionElementsPresent");
        this.getAppController().openViewer(this.getDriver(), this.getTestDerivate());
        WebDriver driver = this.getDriver();
        Assert.assertTrue("RotateButton should be present", driver
            .findElement(By.xpath("//*[@data-id='RotateButton']")).isDisplayed());
    }

    @Test
    public void testImageChangeGroup() {
        this.setTestName(getClassname() + "-testImageChangePresent");
        this.getAppController().openViewer(this.getDriver(), this.getTestDerivate());
        WebDriver driver = this.getDriver();
        Assert.assertTrue("PreviousImageButton should be present",
            driver.findElement(By.xpath("//*[@data-id='PreviousImageButton']")).isDisplayed());
        Assert.assertTrue("NextImageButton should be present",
            driver.findElement(By.xpath("//*[@data-id='NextImageButton']")).isDisplayed());
    }

    @Test
    public void testZoomGroup() {
        this.setTestName(getClassname() + "-testZoomElementsPresent");
        this.getAppController().openViewer(this.getDriver(), this.getTestDerivate());
        WebDriver driver = this.getDriver();
        Assert.assertTrue("ZoomIn button should be present",
            driver.findElement(By.xpath("//*[@data-id='ZoomInButton']")).isDisplayed());
        Assert.assertTrue("ZoomOut button should be present",
            driver.findElement(By.xpath("//*[@data-id='ZoomOutButton']")).isDisplayed());
        Assert.assertTrue("ZoomWidth button should be present",
            driver.findElement(By.xpath("//*[@data-id='ZoomWidthButton']")).isDisplayed());
        Assert.assertTrue("ZoomFit button should be present",
            driver.findElement(By.xpath("//*[@data-id='ZoomFitButton']")).isDisplayed());
    }

    @Test
    public void testNavigationPrev() throws InterruptedException {
        this.setTestName(getClassname() + "-testStructureOverview");
        this.getDriver();
        this.getAppController().openViewer(this.getDriver(), getTestDerivate());

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
        this.setTestName(getClassname() + "-testStructureOverview");
        this.getDriver();
        this.getAppController().openViewer(this.getDriver(), getTestDerivate());

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
        Thread.sleep(500);

        return countColor(tbController, label, color);
    }

    private int selectNextImgAndCountColor(ToolBarController tbController, String label, Color color)
        throws InterruptedException {
        tbController.pressButton(ToolBarController.BUTTON_ID_NEXT_IMG);
        Thread.sleep(500);

        return countColor(tbController, label, color);
    }

    private int countColor(ToolBarController tbController, String label, Color color) {
        String message = color + " schould be selected (class-attribut 'selected' should be set)!";
        Assert.assertTrue(message, tbController.isImageSelected(label));
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
        Thread.sleep(500);

        return countColor(tbController, label, color);
    }

    private void assertLess(int moreValue, int lessValue, String messagePattern) {
        String message = MessageFormat.format(messagePattern, lessValue, moreValue);
        LOGGER.debug(message);
        Assert.assertTrue(message, lessValue < moreValue);
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
