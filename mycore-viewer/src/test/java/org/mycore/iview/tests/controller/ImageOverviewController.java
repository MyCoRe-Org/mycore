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
