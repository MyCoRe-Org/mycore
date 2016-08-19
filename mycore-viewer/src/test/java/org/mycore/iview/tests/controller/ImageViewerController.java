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
