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

package org.mycore.mets.webtest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.mycore.common.selenium.MCRSeleniumTestBase;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;

public class MetsEditorTestBase extends MCRSeleniumTestBase {

    private static final int MAX_ITERATIONS_TO_WAIT_FOR_A_ELEMENT = 100;

    private static final int WAIT_FOR_ELEMENT_ITERATION_IN_MS = 100;

    HttpServer httpServer;

    @Before
    public void setUp() throws InterruptedException, IOException {
        InetSocketAddress serverAddress = new InetSocketAddress(0);
        Path baseDir = Path.of("target", "classes", "META-INF", "resources").toAbsolutePath();
        httpServer = SimpleFileServer.createFileServer(serverAddress, baseDir, SimpleFileServer.OutputLevel.INFO);
        httpServer.start();
        String baseURL = getBaseURL();
        LogManager.getLogger().info("Server online: " + baseURL);
        this.getDriver().get(baseURL + "/module/mets/example/mets-editor.html");
    }

    protected String getBaseURL() {
        return "http://localhost:" + httpServer.getAddress().getPort();
    }

    @After
    public void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        this.takeScreenshot();
    }

    protected void waitForElement(By byTextIgnoreCSS) throws InterruptedException {
        int maxWait = MAX_ITERATIONS_TO_WAIT_FOR_A_ELEMENT;
        WebDriver webDriver = this.getDriver();
        List<WebElement> elements = new ArrayList<>(webDriver.findElements(byTextIgnoreCSS));
        while (elements.isEmpty() && maxWait-- > 0) {
            Thread.sleep(WAIT_FOR_ELEMENT_ITERATION_IN_MS);
            elements = webDriver.findElements(byTextIgnoreCSS);
        }
        if (elements.isEmpty()) {
            throw new AssertionError("The element to wait for was not found!");
        }
    }
}
