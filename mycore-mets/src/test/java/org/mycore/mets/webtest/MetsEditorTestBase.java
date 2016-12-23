package org.mycore.mets.webtest;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.mycore.common.selenium.MCRSeleniumTestBase;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class MetsEditorTestBase extends MCRSeleniumTestBase {

    public static final String BASE_URL = System.getProperty("BaseUrl", "http://localhost:9301");

    @Before
    public void setUp() throws Exception {
        this.getDriver().get(BASE_URL + "/classes/META-INF/resources/module/mets/example/mets-editor.html");
    }

    @After
    public void tearDown() throws Exception {
        this.takeScreenshot();
    }

    protected void waitForElement(By byTextIgnoreCSS) throws InterruptedException {
        int maxWait = 10;
        WebDriver webDriver = this.getDriver();
        List<WebElement> elements = new ArrayList<WebElement>();
        while (elements.isEmpty() && maxWait-- > 0) {
            elements = webDriver.findElements(byTextIgnoreCSS);
            Thread.sleep(1000);
        }
        if (elements.isEmpty())
            throw new AssertionError("The element to wait for was not found!");
    }
}
