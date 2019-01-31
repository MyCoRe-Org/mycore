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
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * @author Sebastian Röher (basti890)
 *
 */
public class WebDriverController {

    private WebDriver driver;

    private static final Logger LOGGER = LogManager.getLogger(WebDriverController.class);

    /**
     * @param driver
     */
    public WebDriverController(WebDriver driver) {
        this.driver = driver;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * clicks the first element specified by <b>xPath</b>
     * 
     * @param xpath
     */
    public void clickElementByXpath(String xpath) {
        By selector = By.xpath(xpath);
        WebElement element = getDriver().findElement(selector);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Found ''{}'' with selector :''{}''", element.toString(), selector.toString());
        }

        element.click();
    }

    /**
     * clicks on the first element specified by the <b>xPath</b> and drags it <b>offestX</b> pixels horizontal and <b>offsetY</b> pixels vertical
     * 
     * @param xPath
     * @param offsetX
     * @param offsetY
     */
    public void dragAndDropByXpath(String xPath, int offsetX, int offsetY) {
        By selector = By.xpath(xPath);
        WebElement element = getDriver().findElement(selector);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Found ''{}'' with selector :''{}''", element.toString(), selector.toString());
        }

        Actions builder = new Actions(getDriver());
        builder.dragAndDropBy(element, offsetX, offsetY).perform();
    }

    /**
     * compares the Elements <b>attribute</b>-value to the <b>assertion</b>
     * 
     * @param attribute
     * @param assertion
     * @param xPath
     * 
     * @return true if the <b>attribute</b> has <b>assertion</b> as value
     */
    public boolean assertAttributeByXpath(String xPath, String attribute, boolean assertion) {
        return assertAttributeByXpath(xPath, attribute, Boolean.toString(assertion).toLowerCase());
    }

    /**
     * compares the Elements <b>attribute</b>-value to the <b>assertion</b>
     * 
     * @param attribute
     * @param assertion
     * @param xPath
     * 
     * @return true if the <b>attribute</b> has <b>assertion</b> as value
     */
    public boolean assertAttributeByXpath(String xPath, String attribute, String assertion) {
        By selector = By.xpath(xPath);
        List<WebElement> element = getDriver().findElements(selector);
        for (WebElement webElement : element) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Found ''{0}'' with selector :''{1}''", webElement.toString(), selector.toString());
            }
            if (webElement.getAttribute(attribute) != null) {
                return webElement.getAttribute(attribute).contains(assertion);
            }
        }
        LOGGER.error("Element {} or Attribute '{}' not fot found!", xPath, attribute);
        return false;
    }

    /**
     * checks if there is any Element in the dom got with the <b>attribute</b> that contains the <b>value</b>
     * 
     * @param attribute
     * @param value
     * @return true if an element is found
     */
    public boolean assertElementByAttributePresent(String attribute, String value) {
        By selector = By.xpath(
            new MessageFormat("//*[contains(@{0},\"{1}\")]", Locale.ROOT).format(new Object[] { attribute, value }));
        List<WebElement> elements = getDriver().findElements(selector);
        if (elements.isEmpty()) {
            LOGGER.error("No element with attribute '{}' and value '{}' found!", attribute, value);
            return false;
        }
        for (WebElement webElement : elements) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Found ''{}'' with selector :''{}''", webElement.toString(), selector.toString());
            }
        }
        return true;
    }

}
