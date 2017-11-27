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
import org.mycore.iview.tests.controller.ImageViewerController;
import org.mycore.iview.tests.controller.ToolBarController;
import org.mycore.iview.tests.image.api.ColorFilter;
import org.mycore.iview.tests.image.api.FilterSelection;
import org.mycore.iview.tests.image.api.Selection;
import org.mycore.iview.tests.model.TestDerivate;

@Category(org.mycore.iview.tests.groups.ImageViewerTests.class)
public class ImageSectionIT extends ViewerTestBase {

    private static final String START_IMAGE = "[4] - rgb.tiff";

    private static final int TOLERANCE = 20;

    private static final Logger LOGGER = LogManager.getLogger(ImageSectionIT.class);

    private static final int DELAY_TIME = 3000;

    @Test
    /**
     * Checks if the zoom button works!
     * @throws IOException
     * @throws InterruptedException
     */
    public void testImageZoom() throws IOException, InterruptedException {
        this.setTestName(getClassname() + "-testImageZoom");
        this.getDriver();
        this.getAppController().openViewer(this.getDriver(), getTestDerivate());

        ImageViewerController controller = this.getViewerController();

        ToolBarController tbController = controller.getToolBarController();

        tbController.selectPictureWithOrder(getStartImage());

        tbController.pressButton(ToolBarController.BUTTON_ID_ZOOM_WIDTH);
        Thread.sleep(DELAY_TIME);

        BufferedImage notZoomed = ControllerUtil.getScreenshot(getDriver(), this.getClassname() + "-notZoomed");
        int redPixelCountNotZoomed = getColorCount(notZoomed, Color.RED);
        int greenPixelCountNotZoomed = getColorCount(notZoomed, Color.GREEN);

        //-

        tbController.pressButton(ToolBarController.BUTTON_ID_ZOOM_IN);
        Thread.sleep(DELAY_TIME);

        BufferedImage zoomed = ControllerUtil.getScreenshot(getDriver(), this.getClassname() + "-zoomed");
        int redPixelCountZoomed = getColorCount(zoomed, Color.RED);
        int greenPixelCountZoomed = getColorCount(zoomed, Color.GREEN);
        int bluePixelCountZoomed = getColorCount(zoomed, Color.BLUE);

        String message1Pattern = "There should be less red pixels in the zoomed screenshot than in the not zoomed ({0} > {1})";
        assertLess(redPixelCountNotZoomed, redPixelCountZoomed, message1Pattern);

        String message2Pattern = "There should be less green pixels in the not zoomed screenshot than in the zoomed ({0} < {1})";
        assertLess(greenPixelCountZoomed, greenPixelCountNotZoomed, message2Pattern);

        //-

        tbController.pressButton(ToolBarController.BUTTON_ID_ZOOM_IN);
        Thread.sleep(DELAY_TIME);

        BufferedImage zoomed2 = ControllerUtil.getScreenshot(getDriver(), this.getClassname() + "-zoomed2");
        int greenPixelCountZoomed2 = getColorCount(zoomed2, Color.GREEN);
        int bluePixelCountZoomed2 = getColorCount(zoomed2, Color.BLUE);

        String message3Pattern = "There should be less blue pixels in the zoomed screenshot than in the zoomed2 ({0} > {1})";
        assertLess(bluePixelCountZoomed2, bluePixelCountZoomed, message3Pattern);

        String message4Pattern = "There should be less green pixels in the zoomed2 screenshot than in the zoomed ({0} > {1})";
        assertLess(greenPixelCountZoomed, greenPixelCountZoomed2, message4Pattern);

        //-

        tbController.pressButton(ToolBarController.BUTTON_ID_ZOOM_OUT);
        Thread.sleep(DELAY_TIME);

        BufferedImage zoomed3 = ControllerUtil.getScreenshot(getDriver(), this.getClassname() + "zoomed3");
        int greenPixelCountZoomed3 = getColorCount(zoomed3, Color.GREEN);
        int bluePixelCountZoomed3 = getColorCount(zoomed3, Color.BLUE);
        int redPixelCountZoomed3 = getColorCount(zoomed3, Color.RED);

        String message5Pattern = "There should be less green pixels in the zoomed2 screenshot than in the zoomed3 ({0} > {1})";
        assertLess(greenPixelCountZoomed3, greenPixelCountZoomed2, message5Pattern);

        String message6Pattern = "There should be less blue pixels in the zoomed3 screenshot than in the zoomed2 ({0} > {1})";
        assertLess(bluePixelCountZoomed2, bluePixelCountZoomed3, message6Pattern);

        //-

        tbController.pressButton(ToolBarController.BUTTON_ID_ZOOM_OUT);
        Thread.sleep(DELAY_TIME);

        BufferedImage zoomed4 = ControllerUtil.getScreenshot(getDriver(), this.getClassname() + "zoomed4");
        int greenPixelCountZoomed4 = getColorCount(zoomed4, Color.GREEN);
        int bluePixelCountZoomed4 = getColorCount(zoomed4, Color.BLUE);
        int redPixelCountZoomed4 = getColorCount(zoomed4, Color.RED);

        tbController.pressButton(ToolBarController.BUTTON_ID_ZOOM_OUT);
        Thread.sleep(DELAY_TIME);

        String message7Pattern = "There should be less blue pixels in the zoomed4 screenshot than in the zoomed3 ({0} > {1})";
        assertLess(bluePixelCountZoomed3, bluePixelCountZoomed4, message7Pattern);

        String message8Pattern = "There should be less green pixels in the zoomed4 screenshot than in the zoomed3 ({0} > {1})";
        assertLess(greenPixelCountZoomed3, greenPixelCountZoomed4, message8Pattern);

        String message9Pattern = "There should be less red pixels in the zoomed3 screenshot than in the zoomed4 ({0} > {1})";
        assertLess(redPixelCountZoomed4, redPixelCountZoomed3, message9Pattern);

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
     * @return the startImage
     */
    public String getStartImage() {
        return START_IMAGE;
    }

}
