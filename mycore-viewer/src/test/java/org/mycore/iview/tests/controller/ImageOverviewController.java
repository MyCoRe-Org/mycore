/**
 * 
 */
package org.mycore.iview.tests.controller;

import java.text.MessageFormat;

import org.openqa.selenium.WebDriver;

/**
 * @author Sebastian Röher (basti890)
 *
 */
public class ImageOverviewController extends SideBarController {

    public static final String IMAGE_OVERVIEW_SELECTOR = "imageOverview";

    /**
     * @param webdriver
     */
    public ImageOverviewController(WebDriver webdriver) {
        super(webdriver);
    }

    /**
     * builds xpath for and calls function {@link SideBarController#clickElementByXpath(String)}
     * @param orderLabel
     */
    public void clickImageByOrder(String orderLabel) {
        clickElementByXpath(MessageFormat.format("//div[@class=\"caption\" and contains(text(),\"{0}\")]", orderLabel));
    }

    /**
     * checks if the Image is selected
     * 
     * @param orderLabel
     * @return true if the div of the Image has the class "selected"
     */
    public boolean isImageSelected(String orderLabel) {
        String xPath = MessageFormat
            .format("//div[./div[@class=\"caption\" and contains(text(),\"{0}\")]]", orderLabel);
        return assertAttributeByXpath(xPath, "class", "selected");
    }
}
