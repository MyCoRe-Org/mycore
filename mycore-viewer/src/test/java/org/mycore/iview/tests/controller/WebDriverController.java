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
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.selenium.drivers.MCRWebdriverWrapper;
import org.mycore.iview.tests.ViewerTestBase;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * @author Sebastian Röher (basti890)
 *
 */
public class WebDriverController {

    private WebDriver driver;

    private static final Logger LOGGER = LogManager.getLogger();

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
     * Waits for the given condition and returns its result. Since selenium-utils 0.12 the drivers no longer use an
     * implicit wait, so every lookup of an element that the viewer creates asynchronously has to go through an
     * explicit wait.
     */
    protected <R> R waitFor(ExpectedCondition<R> condition) {
        long start = System.nanoTime();
        try {
            return ((MCRWebdriverWrapper) driver).waitFor(condition);
        } finally {
            ViewerTestBase.addWaitTime(Duration.ofNanos(System.nanoTime() - start));
        }
    }

    /**
     * Waits until the given check holds. Use for conditions that are expressed by one of the
     * <code>assert…</code>/<code>is…</code> methods of a controller instead of by a locator.
     */
    protected void waitUntil(String description, BooleanSupplier check) {
        waitFor(new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver ignored) {
                return check.getAsBoolean() ? Boolean.TRUE : null;
            }

            @Override
            public String toString() {
                return description;
            }
        });
    }

    /**
     * Waits until an element matching <b>selector</b> is present and returns it.
     */
    protected WebElement waitAndFindElement(By selector) {
        return waitFor(ExpectedConditions.presenceOfElementLocated(selector));
    }

    /**
     * Waits until an element matching <b>selector</b> is visible and enabled and returns it.
     */
    protected WebElement waitAndFindClickableElement(By selector) {
        return waitFor(ExpectedConditions.elementToBeClickable(selector));
    }

    /**
     * clicks the first element specified by <b>xPath</b>
     *
     * @param xpath
     */
    public void clickElementByXpath(String xpath) {
        By selector = By.xpath(xpath);
        WebElement element = waitAndFindClickableElement(selector);

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
        WebElement element = waitAndFindElement(selector);

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
                LOGGER.debug("Found ''{}'' with selector :''{}''", webElement.toString(), selector.toString());
            }
            Optional<String> attValue = Optional.ofNullable(webElement.getDomProperty(attribute))
                .or(() -> Optional.ofNullable(webElement.getDomAttribute(attribute)));
            if (attValue.isPresent()) {
                return attValue.get().contains(assertion);
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
