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

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    public void setUp() throws InterruptedException {
        waitForServer(60000);
        this.getDriver().get(BASE_URL + "/module/mets/example/mets-editor.html");
    }

    public static boolean waitForServer(long timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout must be greater than 0");
        }
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;

        boolean serverReady = false;

        Logger logger = LogManager.getLogger();
        URI baseURI = URI.create(BASE_URL);

        while (!serverReady && elapsedTime < timeout) {
            serverReady = checkPort(baseURI.getHost(), baseURI.getPort());
            if (!serverReady) {
                logger.info("Waiting for the server to be ready...");
                Thread.yield();
                elapsedTime = System.currentTimeMillis() - startTime;
            }
        }

        return serverReady;
    }

    public static boolean checkPort(String host, int port) {
        Socket socket = null;
        boolean isOpen = false;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 1000);
            isOpen = true;
        } catch (Exception e) {
            isOpen = false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    LogManager.getLogger().warn("Error closing socket", e);
                }
            }
        }

        return isOpen;
    }

    @After
    public void tearDown() {
        this.takeScreenshot();
    }

    protected void waitForElement(By byTextIgnoreCSS) throws InterruptedException {
        int maxWait = MAXIMAL_TIME_TO_WAIT_FOR_A_ELEMENT;
        WebDriver webDriver = this.getDriver();
        List<WebElement> elements = new ArrayList<>();
        while (elements.isEmpty() && maxWait-- > 0) {
            elements = webDriver.findElements(byTextIgnoreCSS);
            Thread.sleep(ONE_SECOND_IN_MILLISECONDS);
        }
        if (elements.isEmpty()) {
            throw new AssertionError("The element to wait for was not found!");
        }
    }
}
