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
    public void setUp() throws Exception {
        initController();
        getAppController().setUpDerivate(this.getDriver(), getTestDerivate());
    }

    @After
    public void tearDown() throws Exception {
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
