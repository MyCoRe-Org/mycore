package org.mycore.iview.tests.controller;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class ScrollUtil {

    public static void scrollByXpath(WebDriver driver, String xPath, int offestX) {
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("arguments[0].scrollTop = arguments[0].scrollTop + arguments[1];",
            driver.findElement(By.xpath(xPath)), offestX);
    }

}
