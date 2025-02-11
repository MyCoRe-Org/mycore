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

package org.mycore.iview.tests;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.mycore.common.selenium.MCRSeleniumTestBase;
import org.mycore.iview.tests.controller.ApplicationController;
import org.mycore.iview.tests.controller.ImageViewerController;
import org.mycore.iview.tests.model.TestDerivate;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;

public abstract class ViewerTestBase extends MCRSeleniumTestBase {

    private static final String APPLICATION_CONTROLLER_PROPERTY_NAME = "test.viewer.applicationController";

    public static final String BASE_URL = "test.application.url.hostName";

    private ImageViewerController viewerController;

    private static ApplicationController appController;

    private ApplicationController applicationController;

    HttpServer httpServer;

    private static final ThreadLocal<AtomicLong> WAIT_TIME = ThreadLocal.withInitial(AtomicLong::new);

    @AfterClass
    public static void printWaitTime() {
        LogManager.getLogger().info("Total wait time: {}", Duration.ofMillis(WAIT_TIME.get().get()));
        WAIT_TIME.remove();
    }

    public static void sleep(long millis) throws InterruptedException {
        WAIT_TIME.get().addAndGet(millis);
        Thread.sleep(millis);
    }

    @Before
    public void setUp() throws InterruptedException {
        InetSocketAddress serverAddress = new InetSocketAddress(0);
        Path baseDir = Path.of("target").toAbsolutePath();
        httpServer = SimpleFileServer.createFileServer(serverAddress, baseDir, SimpleFileServer.OutputLevel.INFO);
        httpServer.start();

        String baseURL = getBaseURL();
        LogManager.getLogger().info("Server online: " + baseURL);

        initController();
        getAppController().setUpDerivate(this.getDriver(), getBaseURL(), getTestDerivate());
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

    @AfterClass
    public static void tearDownClass() {
        appController.shutDownDerivate(driver, null);
        driver.quit();
    }

    public String getClassname() {
        return getClass().getName();
    }

    public void initController() {
        this.setViewerController(new ImageViewerController(this.getDriver()));

        if (appController == null) {
            ApplicationController applicationController = getApplicationControllerInstance();
            this.setAppController(applicationController);
        }
    }

    private ApplicationController getApplicationControllerInstance() {
        if (applicationController == null) {
            String applicationControllerClassName = TestProperties.getInstance().getProperty(
                APPLICATION_CONTROLLER_PROPERTY_NAME);
            applicationController = TestUtil.instantiate(applicationControllerClassName,
                ApplicationController.class);
            applicationController.init();
        }

        return applicationController;
    }

    public abstract TestDerivate getTestDerivate();

    public ApplicationController getAppController() {
        return appController;
    }

    private void setAppController(ApplicationController appController) {
        ViewerTestBase.appController = appController;
    }

    public ImageViewerController getViewerController() {
        return viewerController;
    }

    private void setViewerController(ImageViewerController viewerController) {
        this.viewerController = viewerController;
    }
}
