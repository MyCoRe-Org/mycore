package org.mycore.iview.tests.controller;

import org.mycore.iview.tests.model.TestDerivate;
import org.openqa.selenium.WebDriver;

public abstract class ApplicationController {
    public ApplicationController() {
    }

    public abstract void init();

    public abstract void setUpDerivate(WebDriver webdriver, TestDerivate testDerivate);

    public abstract void shutDownDerivate(WebDriver webdriver, TestDerivate testDerivate);

    public abstract void openViewer(WebDriver webdriver, TestDerivate testDerivate);
}
