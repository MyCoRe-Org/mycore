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
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @author Sebastian Röher (basti890)
 *
 */
public class StructureOverviewController extends SideBarController {

    public static final String CHAPTER_OVERVIEW_SELECTOR = "chapterOverview";

    private static final Logger LOGGER = LogManager.getLogger(StructureOverviewController.class);

    /**
     * @param webdriver
     */
    public StructureOverviewController(WebDriver webdriver) {
        super(webdriver);
    }

    /**
     * builds xpath and calls function {@link SideBarController#clickElementByXpath(String)}
     * @param orderLabel
     */
    public void selectImageByOrder(String orderLabel) {
        String xpath = MessageFormat.format("//li/a[../span[@class=\"childLabel\" and contains(text(),\"{0}\")]"
            + "]|//li/a[contains(text(),\"{0}\")]",
            orderLabel);
        clickElementByXpath(xpath);
    }

    public boolean isImageSelected(String orderLabel) {
        String xPath = MessageFormat.format("//li[./span[@class=\"childLabel\" and contains(text(),\"{0}\")]|"
            + "./a[contains(text(),\"{0}\")]]",
            orderLabel);
        return assertAttributeByXpath(xPath, "data-selected", true);
    }

    /**
     * test all div's with class="childLabel"
     * 
     * @return false if any div with class="childLabel" has "undefined as content else true 
     */
    public boolean hasUndefinedPageLabels() {
        By selector = By.xpath("//span[@class=\"childLabel\"]");
        List<WebElement> element = this.getDriver().findElements(selector);
        for (WebElement webElement : element) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(MessageFormat.format("Found ''{0}'' with selector :''{1}''", element.toString(),
                    selector.toString()));
            }
            String text = webElement.getText();
            if ("undefined".equalsIgnoreCase(text)) {
                return true;
            }
        }
        return false;
    }
}
