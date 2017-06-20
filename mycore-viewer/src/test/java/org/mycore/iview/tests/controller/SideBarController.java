package org.mycore.iview.tests.controller;

import java.text.MessageFormat;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SideBarController extends WebDriverController {

    public static final String SIDEBAR_CLOSE_SELECTOR = "//button[@class=\"close\"]";

    private static final Logger LOGGER = LogManager.getLogger(SideBarController.class);

    public SideBarController(WebDriver webdriver) {
        super(webdriver);
    }

    /**
     * checks the width of the sidebar
     * @param width
     * @return
     */
    public boolean assertSideBarWidth(int width) {
        return getSideBarWidth() == width;
    }

    /**
     * checks the width of the sidebar-div (default is 300px)
     * @return true if the width is greater than 2px (2px cause the width of the border will be counted too)
     */
    public boolean assertSideBarPresent() {
        return getSideBarWidth() > 2;
    }

    /**
     * @return the width of the sidebar
     */
    public int getSideBarWidth() {
        By selector = By.xpath("//div[contains(@class,\"sidebar\")]");
        WebElement element = getDriver().findElement(selector);
        if (element == null) {
            LOGGER.error(MessageFormat.format("No element with xpath: '{0}' found!", selector.toString()));
            throw new NoSuchElementException();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format("Found ''{0}'' with selector :''{1}''", element.toString(),
                selector.toString()));
        }

        return element.getSize().getWidth();
    }

    /**
     * counts the div's, that contain 'thumbnail' in their class-attribute
     * 
     * @return number of thumbnail-div's
     */
    public int countThumbnails() {
        By selector = By.xpath("//div[contains(@class,\"thumbnail\")]/div[@class='imgSpacer']");
        List<WebElement> elements = getDriver().findElements(selector);

        return elements.size();
    }

    /**
     * scrolls <b>offestX</b> pixels vertically in the sidebar
     * 
     * @param driver
     * @param offestX
     */
    public void scrollSidbar(WebDriver driver, int offestX) {
        ScrollUtil.scrollByXpath(driver,
            "//div[contains(@class,\"sidebar\")]/div[./div[contains(@class,\"thumbnail\")]]", 250);
    }

}
