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

package org.mycore.iview.tests.controller;

import org.openqa.selenium.WebDriver;

public class ImageViewerController extends WebDriverController {

    private ToolBarController toolBarController;

    private SideBarController sideBarController;

    private ImageOverviewController imageOverviewController;

    private StructureOverviewController structureOverviewController;

    public ImageViewerController(WebDriver webdriver) {
        super(webdriver);
        this.toolBarController = new ToolBarController(webdriver);
        this.sideBarController = new SideBarController(webdriver);
        this.imageOverviewController = new ImageOverviewController(webdriver);
        this.structureOverviewController = new StructureOverviewController(webdriver);
    }

    @Override
    public void setDriver(WebDriver driver) {
        super.setDriver(driver);
        getToolBarController().setDriver(driver);
        getSideBarController().setDriver(driver);
        getImageOverviewController().setDriver(driver);
        getStructureOverviewController().setDriver(driver);
    }

    public ToolBarController getToolBarController() {
        return toolBarController;
    }

    public SideBarController getSideBarController() {
        return sideBarController;
    }

    public ImageOverviewController getImageOverviewController() {
        return imageOverviewController;
    }

    public StructureOverviewController getStructureOverviewController() {
        return structureOverviewController;
    }
}
