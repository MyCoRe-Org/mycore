package org.mycore.iview.tests.controller;

import java.text.MessageFormat;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger LOGGER = LogManager.getLogger(ToolBarController.class);

    public ToolBarController(WebDriver webdriver) {
        super(webdriver);
    }

    /**
     * clicks the first element with class="btn" and data-id="<b>id</b>"
     *
     * @param id
     */
    public void pressButton(String id) {
        By selector = By.cssSelector(MessageFormat.format(BUTTON_SELECTOR_PATTERN, id));
        WebElement element = this.getDriver().findElement(selector);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format("Found ''{0}'' with selector :''{1}''", element.toString(),
                selector.toString()));
        }

        element.click();
    }

    /**
     * clicks the first element with data-id="<b>id</b>"
     *
     * @param id
     */
    public void clickElementById(String id) {
        int trys = 10;

        By selector = By.cssSelector(MessageFormat.format(ELEMENT_SELECTOR_PATTERN, id));

        WebElement element = getNotStaleElement(trys, selector);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format("Found ''{0}'' with selector :''{1}''", element.toString(),
                selector.toString()));
        }

        List<WebElement> webElements = getDriver().findElements(selector);
        if (webElements.size() != 1) {
            LOGGER.warn("Multiple Elements found!");
        }

        element.click();
    }

    // TODO: do this better!
    private WebElement getNotStaleElement(int trys, By selector) {
        if (trys <= 0) {
            throw new IllegalArgumentException("trys should be more then 0! [" + trys + "]");
        }

        Exception sere = null;
        while (--trys > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            try {
                WebElement element = this.getDriver().findElement(selector);

                element.isEnabled();
                return element;
            } catch (StaleElementReferenceException e) {
                sere = e;
                LOGGER.debug("Stale check failed! [" + trys + "]");
            }
        }
        throw new RuntimeException(sere);
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
        return select != null && select.getFirstSelectedOption() != null
            && select.getFirstSelectedOption().getText().equalsIgnoreCase(oderLabel);
    }

}
