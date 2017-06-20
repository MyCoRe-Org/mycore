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

    private static final int MAXIMAL_TIME_TO_WAIT_FOR_A_ELEMENT = 10;

    private static final int ONE_SECOND_IN_MILLISECONDS = 1000;

    @Before
    public void setUp() throws Exception {
        this.getDriver().get(BASE_URL + "/classes/META-INF/resources/module/mets/example/mets-editor.html");
    }

    @After
    public void tearDown() throws Exception {
        this.takeScreenshot();
    }

    protected void waitForElement(By byTextIgnoreCSS) throws InterruptedException {
        int maxWait = MAXIMAL_TIME_TO_WAIT_FOR_A_ELEMENT;
        WebDriver webDriver = this.getDriver();
        List<WebElement> elements = new ArrayList<WebElement>();
        while (elements.isEmpty() && maxWait-- > 0) {
            elements = webDriver.findElements(byTextIgnoreCSS);
            Thread.sleep(ONE_SECOND_IN_MILLISECONDS);
        }
        if (elements.isEmpty()) {
            throw new AssertionError("The element to wait for was not found!");
        }
    }
}
