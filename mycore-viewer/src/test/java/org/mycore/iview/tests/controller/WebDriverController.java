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
            LOGGER.debug(MessageFormat.format("Found ''{0}'' with selector :''{1}''", element.toString(),
                selector.toString()));
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
            LOGGER.debug(MessageFormat.format("Found ''{0}'' with selector :''{1}''", element.toString(),
                selector.toString()));
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
                LOGGER.debug(MessageFormat.format("Found ''{0}'' with selector :''{1}''", webElement.toString(),
                    selector.toString()));
            }
            if (webElement.getAttribute(attribute) != null) {
                return webElement.getAttribute(attribute).contains(assertion);
            }
        }
        LOGGER.error(MessageFormat.format("Element {0} or Attribute '{1}' not fot found!", xPath, attribute));
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
        By selector = By.xpath(MessageFormat.format("//*[contains(@{0},\"{1}\")]", attribute, value));
        List<WebElement> elements = getDriver().findElements(selector);
        if (elements.isEmpty()) {
            LOGGER.error(MessageFormat.format("No element with attribute '{0}' and value '{1}' found!", attribute,
                value));
            return false;
        }
        for (WebElement webElement : elements) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(MessageFormat.format("Found ''{0}'' with selector :''{1}''", webElement.toString(),
                    selector.toString()));
            }
        }
        return true;
    }

}
