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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.mycore.iview.tests.ViewerTestBase;
import org.mycore.iview.tests.controller.ImageOverviewController;
import org.mycore.iview.tests.controller.ImageViewerController;
import org.mycore.iview.tests.controller.SideBarController;
import org.mycore.iview.tests.controller.ToolBarController;
import org.mycore.iview.tests.model.TestDerivate;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebDriver;

public class SideBarIT extends ViewerTestBase {

    private static final Logger LOGGER = LogManager.getLogger();

    protected SideBarIT(WebDriver webDriver) {
        super(webDriver);
    }

    @Test
    public void testSideBarPresent() {
        this.getDriver();
        this.getAppController().openViewer(this.getDriver(), getBaseURL(), getTestDerivate());

        ImageViewerController controller = this.getViewerController();

        ToolBarController tbController = controller.getToolBarController();
        SideBarController sbController = controller.getSideBarController();

        tbController.pressButton(ToolBarController.BUTTON_ID_SIDEBAR_CONTROLL);
        tbController.clickElementById(ImageOverviewController.IMAGE_OVERVIEW_SELECTOR);
        assertTrue(sbController.assertSideBarPresent(), "SideBar should be present!");

        tbController.clickElementByXpath(SideBarController.SIDEBAR_CLOSE_SELECTOR);
        assertFalse(sbController.assertSideBarPresent(), "SideBar should not be present!");
    }

    /**
     * Ignored because https://github.com/mozilla/geckodriver/issues/233
     */
    @Test
    public void testSideBarResize() {
        this.getDriver();
        this.getAppController().openViewer(this.getDriver(), getBaseURL(), getTestDerivate());

        ImageViewerController controller = this.getViewerController();

        ToolBarController tbController = controller.getToolBarController();
        SideBarController sbController = controller.getSideBarController();

        tbController.pressButton(ToolBarController.BUTTON_ID_SIDEBAR_CONTROLL);
        tbController.clickElementById(ImageOverviewController.IMAGE_OVERVIEW_SELECTOR);

        int sbWidthStart = sbController.getSideBarWidth();

        try { // Firefox does not support actions so we just let the test pass.
            sbController.dragAndDropByXpath("//div[contains(@class,\"sidebar\")]/span[@class=\"resizer\"]", 50, 0);
        } catch (UnsupportedCommandException e) {
            LOGGER.warn("Driver does not support Actions", e);
            return;
        }
        int sbWidthEnd = sbController.getSideBarWidth();

        assertLess(sbWidthEnd, sbWidthStart, "Sidebar width schould be increased!");

    }

    @Test
    public void testOverviewLayout() throws InterruptedException {
        this.getDriver();
        this.getAppController().openViewer(this.getDriver(), getBaseURL(), getTestDerivate());

        ImageViewerController controller = this.getViewerController();

        ToolBarController tbController = controller.getToolBarController();
        SideBarController sbController = controller.getSideBarController();

        tbController.pressButton(ToolBarController.BUTTON_ID_SIDEBAR_CONTROLL);
        tbController.clickElementById(ImageOverviewController.IMAGE_OVERVIEW_SELECTOR);

        int before = sbController.countThumbnails();

        try {
            sbController.dragAndDropByXpath("//div[contains(@class,'sidebar')]/span[@class='resizer']", 300, 0);
        } catch (UnsupportedCommandException e) {
            LOGGER.warn("Driver does not support Actions", e);
            return;
        }

        sleep(1000);
        int after = sbController.countThumbnails();

        // this test does not really work, because there are only 4 thumbnails left
        assertEquals(before, after);
    }

    private void assertLess(int moreValue, int lessValue, String messagePattern) {
        String message = new MessageFormat(messagePattern, Locale.ROOT).format(new Object[] { lessValue, moreValue });
        LOGGER.debug(message);
        assertTrue(lessValue < moreValue, message);
    }

    @Override
    public TestDerivate getTestDerivate() {
        return BaseTestConstants.RGB_TEST_DERIVATE;
    }
}
