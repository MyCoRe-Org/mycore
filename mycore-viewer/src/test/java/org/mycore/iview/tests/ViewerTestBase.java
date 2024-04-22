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

package org.mycore.iview.tests;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.mycore.common.selenium.MCRSeleniumTestBase;
import org.mycore.iview.tests.controller.ApplicationController;
import org.mycore.iview.tests.controller.ImageViewerController;
import org.mycore.iview.tests.model.TestDerivate;

public abstract class ViewerTestBase extends MCRSeleniumTestBase {

    private static final String APPLICATION_CONTROLLER_PROPERTY_NAME = "test.viewer.applicationController";

    public static final String BASE_URL = "test.application.url.hostName";

    private ImageViewerController viewerController;

    private static ApplicationController appController = null;

    private ApplicationController applicationController;

    @Before
    public void setUp() {
        initController();
        waitForServer(60000);
        getAppController().setUpDerivate(this.getDriver(), getTestDerivate());
    }

    public static boolean waitForServer(long timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout must be greater than 0");
        }
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;

        boolean serverReady = false;

        Logger logger = LogManager.getLogger();
        URI baseURI = URI.create("http://localhost:" + System.getProperty("BaseUrlPort") + "/");

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
