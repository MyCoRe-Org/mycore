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

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.iview.tests.ViewerTestBase;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * @author Sebastian Röher (basti890)
 */
public class ToolBarController extends WebDriverController {

    public static final String BUTTON_ID_ZOOM_IN = "ZoomInButton";

    public static final String BUTTON_ID_ZOOM_OUT = "ZoomOutButton";

    public static final String BUTTON_ID_ZOOM_WIDTH = "ZoomWidthButton";

    public static final String BUTTON_ID_ZOOM_FIT = "ZoomFitButton";

    public static final String BUTTON_ID_PREV_IMG = "PreviousImageButton";

    public static final String BUTTON_ID_NEXT_IMG = "NextImageButton";

    public static final String BUTTON_ID_SIDEBAR_CONTROLL = "SidebarControllDropdownButton";

    private static final String BUTTON_SELECTOR_PATTERN = ".btn[data-id={0}]";

    private static final String ELEMENT_SELECTOR_PATTERN = "[data-id={0}]";

    private static final String SELECTBOX_SELECTOR = "[data-id=ImageChangeControllGroup] select.dropdown";

    private static final Logger LOGGER = LogManager.getLogger();

    public ToolBarController(WebDriver webdriver) {
        super(webdriver);
    }

    /**
     * clicks the first element with class="btn" and data-id="<b>id</b>"
     *
     * @param id
     */
    public void pressButton(String id) {
        By selector = By
            .cssSelector(new MessageFormat(BUTTON_SELECTOR_PATTERN, Locale.ROOT).format(new String[] { id }));
        WebElement element = this.getDriver().findElement(selector);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Found ''{}'' with selector :''{}''", element.toString(), selector.toString());
        }

        element.click();
    }

    /**
     * clicks the first element with data-id="<b>id</b>"
     *
     * @param id
     */
    public void clickElementById(String id) {
        By selector = By
            .cssSelector(new MessageFormat(ELEMENT_SELECTOR_PATTERN, Locale.ROOT).format(new String[] { id }));

        WebElement element = getNotStaleElement(selector);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Found ''{}'' with selector :''{}''", element.toString(), selector.toString());
        }

        List<WebElement> webElements = getDriver().findElements(selector);
        if (webElements.size() != 1) {
            LOGGER.warn("Multiple Elements found!");
        }

        element.click();
    }

    private WebElement getNotStaleElement(By selector) {
        final int maxTries = 10;
        if (maxTries <= 0) {
            throw new IllegalArgumentException("maxTries should be more than 0! [" + maxTries + "]");
        }

        StaleElementReferenceException lastException = null;
        for (int attempt = 1; attempt <= maxTries; attempt++) {
            try {
                ViewerTestBase.sleep(1000);
            } catch (InterruptedException e) {
            }
            try {
                WebElement element = this.getDriver().findElement(selector);

                if (element.isEnabled()) {
                    return element;
                }
            } catch (StaleElementReferenceException e) {
                lastException = e;
                LOGGER.debug("Stale check failed! [{}]", attempt);
            }
        }

        throw new NoSuchElementException("Failed to retrieve a non-stale element after " + maxTries + " attempts.",
            lastException);
    }

    /**
     * selects the first picture with the label <b>orderLabel</b> in the selectbox
     *
     * @param orderLabel
     */
    public void selectPictureWithOrder(String orderLabel) {
        By selector = By.cssSelector(SELECTBOX_SELECTOR);
        WebElement element = this.getDriver().findElement(selector);
        Select select = new Select(element);
        select.selectByVisibleText(orderLabel);
    }

    /**
     * checks if the image with the <b>orderLabel</b> is selected
     *
     * @param oderLabel
     * @return true if the if the image with the <b>orderLabel</b> is selected
     */
    public boolean isImageSelected(String oderLabel) {
        By selector = By.cssSelector(SELECTBOX_SELECTOR);
        WebElement element = this.getDriver().findElement(selector);
        Select select = new Select(element);
        return select.getFirstSelectedOption() != null
            && select.getFirstSelectedOption().getText().equalsIgnoreCase(oderLabel);
    }

}
