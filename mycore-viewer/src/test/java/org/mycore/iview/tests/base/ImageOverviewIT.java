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
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mycore.iview.tests.ViewerTestBase;
import org.mycore.iview.tests.controller.ControllerUtil;
import org.mycore.iview.tests.controller.ImageOverviewController;
import org.mycore.iview.tests.controller.ImageViewerController;
import org.mycore.iview.tests.controller.ToolBarController;
import org.mycore.iview.tests.image.api.ColorFilter;
import org.mycore.iview.tests.image.api.FilterSelection;
import org.mycore.iview.tests.image.api.Selection;
import org.mycore.iview.tests.model.TestDerivate;

/**
 * @author Sebastian Röher (basti890)
 *
 */
@Category(org.mycore.iview.tests.groups.ImageViewerTests.class)
public class ImageOverviewIT extends ViewerTestBase {

    private static final String RGB_LABEL = "rgb.tiff";

    private static final String RED_LABEL = "r.png";

    private static final String GREEN_LABEL = "g.png";

    private static final String BLUE_LABEL = "b.png";

    private static final int TOLERANCE = 20;

    private static final Logger LOGGER = LogManager.getLogger(ImageOverviewIT.class);

    @Test
    /**
     * Checks if the image overview works
     * @throws IOException
     * @throws InterruptedException
     */
    public void testImageOverview() throws IOException, InterruptedException {
        this.setTestName(getClassname() + "-testImageOverview");
        this.getDriver();
        this.getAppController().openViewer(this.getDriver(), getTestDerivate());

        ImageViewerController controller = this.getViewerController();

        ToolBarController tbController = controller.getToolBarController();
        ImageOverviewController ioController = controller.getImageOverviewController();

        tbController.pressButton(ToolBarController.BUTTON_ID_ZOOM_IN);
        tbController.pressButton(ToolBarController.BUTTON_ID_SIDEBAR_CONTROLL);
        tbController.clickElementById(ImageOverviewController.IMAGE_OVERVIEW_SELECTOR);

        Thread.sleep(500);

        int greenPixelCount = clickImgAndCountColor(ioController, getGreenLabel(), Color.GREEN);
        int redPixelCount = clickImgAndCountColor(ioController, getRedLabel(), Color.RED);

        ioController.scrollSidbar(getDriver(), 250);

        int bluePixelCount = clickImgAndCountColor(ioController, getBlueLabel(), Color.BLUE);

        ioController.scrollSidbar(getDriver(), 250);

        int redPixelCountRGB = clickImgAndCountColor(ioController, getRgbLabel(), Color.RED);
        int greenPixelCountRGB = clickImgAndCountColor(ioController, getRgbLabel(), Color.GREEN);
        int bluePixelCountRGB = clickImgAndCountColor(ioController, getRgbLabel(), Color.BLUE);

        String messagePattern = "There should be less red pixels in the rgb screenshot than in the red ({0} > {1})";
        assertLess(redPixelCount, redPixelCountRGB, messagePattern);

        messagePattern = "There should be less green pixels in the rgb screenshot than in the green ({0} > {1})";
        assertLess(greenPixelCount, greenPixelCountRGB, messagePattern);

        messagePattern = "There should be less blue pixels in the rgb screenshot than in the blue ({0} > {1})";
        assertLess(bluePixelCount, bluePixelCountRGB, messagePattern);

    }

    /**
     * clicks an image via {@link ImageOverviewController#clickImageByOrder(String)} and counts the pixels of the color
     * 
     * @param ioController
     * @param label
     * @param color
     * @return
     * @throws InterruptedException
     */
    private int clickImgAndCountColor(ImageOverviewController ioController, String label, Color color)
        throws InterruptedException {
        ioController.clickImageByOrder(label);
        Thread.sleep(500);

        String message = label + " should be selected (class-attribut 'selected' should be set)!";
        Assert.assertTrue(message, ioController.isImageSelected(label));
        String fileName = String.format("%s-%s-%s-%s-%s", this.getClassname(), label, color.getRed(), color.getBlue(),
            color.getGreen());
        BufferedImage bImage = ControllerUtil.getScreenshot(getDriver(), fileName);
        return getColorCount(bImage, color);
    }

    /**
     * Gets the count of pixel with a specific color
     * @param image 
     * @param color 
     * @return
     */
    private int getColorCount(BufferedImage image, Color color) {
        return new FilterSelection(Selection.fromBufferedImage(image), new ColorFilter(color, false, TOLERANCE))
            .getPixel().size();
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

    /**
     * @return the blueLabel
     */
    public String getBlueLabel() {
        return BLUE_LABEL;
    }

    /**
     * @return the rgbLabel
     */
    public String getRgbLabel() {
        return RGB_LABEL;
    }

}
