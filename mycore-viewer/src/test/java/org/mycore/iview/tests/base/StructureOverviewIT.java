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
import org.mycore.iview.tests.controller.StructureOverviewController;
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
public class StructureOverviewIT extends ViewerTestBase {

    private static final String RGB_LABEL = "rgb.tiff";

    private static final String RED_LABEL = "r.png";

    private static final String GREEN_LABEL = "g.png";

    private static final String BLUE_LABEL = "b.png";

    private static final int TOLERANCE = 20;

    private static final Logger LOGGER = LogManager.getLogger(StructureOverviewIT.class);

    @Test
    /**
     * Checks if the structure Overview loaded from mets.xml works!
     * @throws IOException
     * @throws InterruptedException
     */
    public void testStructureOverview() throws IOException, InterruptedException {
        this.setTestName(getClassname() + "-testStructureOverview");
        this.getDriver();
        this.getAppController().openViewer(this.getDriver(), getTestDerivate());

        ImageViewerController controller = this.getViewerController();

        ToolBarController tbController = controller.getToolBarController();
        StructureOverviewController soController = controller.getStructureOverviewController();

        tbController.pressButton(ToolBarController.BUTTON_ID_SIDEBAR_CONTROLL);
        Thread.sleep(1000);
        tbController.clickElementById(StructureOverviewController.CHAPTER_OVERVIEW_SELECTOR);

        int greenPixelCount = selectImgAndCountColor(soController, getGreenLabel(), Color.GREEN);
        int redPixelCount = selectImgAndCountColor(soController, getRedLabel(), Color.RED);
        int bluePixelCount = selectImgAndCountColor(soController, getBlueLabel(), Color.BLUE);
        int redPixelCountRGB = selectImgAndCountColor(soController, getRgbLabel(), Color.RED);
        int greenPixelCountRGB = selectImgAndCountColor(soController, getRgbLabel(), Color.GREEN);
        int bluePixelCountRGB = selectImgAndCountColor(soController, getRgbLabel(), Color.BLUE);

        String messagePattern = "There should be less red pixels in the rgb screenshot than in the red ({0} > {1})";
        assertLess(redPixelCount, redPixelCountRGB, messagePattern);

        messagePattern = "There should be less green pixels in the rgb screenshot than in the red ({0} > {1})";
        assertLess(greenPixelCount, greenPixelCountRGB, messagePattern);

        messagePattern = "There should be less blue pixels in the rgb screenshot than in the red ({0} > {1})";
        assertLess(bluePixelCount, bluePixelCountRGB, messagePattern);

        Assert.assertFalse("There should´nt be any 'undefined' page labels.", soController.hasUndefinedPageLabels());
    }

    /**
     * selects an image with {@link StructureOverviewController#selectImageByOrder(String)} and counts Pixels of the color
     * 
     * @param soController
     * @param label
     * @param color
     * @return
     * @throws InterruptedException
     */
    private int selectImgAndCountColor(StructureOverviewController soController, String label, Color color)
        throws InterruptedException {
        soController.selectImageByOrder(label);
        Thread.sleep(500);

        String message = color.toString() + " schould be selected (class-attribut 'selected' should be set)!";
        Assert.assertTrue(message, soController.isImageSelected(label));
        String fileName = String.format("%s-%s-%s", this.getClassname(), label, color);
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
