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

package org.mycore.mets.webtest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.mycore.common.selenium.MCRSeleniumTestBase;
import org.openqa.selenium.By;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;

public class MetsEditorTestBase extends MCRSeleniumTestBase {

    HttpServer httpServer;

    @Before
    public void setUp() throws IOException {
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

    protected void waitForElement(By locator) {
        this.getDriver().waitAndFindElement(locator);
    }
}
