/**
 * 
 */
package org.mycore.mets.webtest;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.selenium.drivers.MCRWebdriverWrapper;
import org.mycore.common.selenium.util.MCRBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * @author Sebastian Röher (basti890)
 *
 */
public class PaginationIT extends MetsEditorTestBase {

    private static final String TEST_STRING = "test123";

    @Test
    public void setPagination() {
        WebDriver webDriver = getDriver();
        WebElement row = webDriver.findElement(MCRBy.partialText("perthes_1855_0001.jpg")).findElement(
            By.xpath("ancestor::tr"));
        row.findElement(By.xpath("//button[@title=\"???editPagination???\"]")).click();
        row.findElement(By.xpath("//input")).sendKeys(TEST_STRING);
        row.findElement(By.xpath("//button[@title=\"???paginationChange???\"]")).click();
        Assert.assertNotNull(row.findElement(MCRBy.partialText(TEST_STRING)));
    }

    @Test
    public void abortPagination() {
        MCRWebdriverWrapper webDriver = getDriver();
        WebElement row = webDriver.waitAndFindElement(MCRBy.partialText("perthes_1855_0001.jpg")).findElement(
            By.xpath("ancestor::tr"));
        row.findElement(By.xpath("//button[@title=\"???editPagination???\"]")).click();
        row.findElement(By.xpath("//input")).sendKeys(TEST_STRING);
        row.findElement(By.xpath("//button[@title=\"???paginationAbort???\"]")).click();
        Assert.assertTrue("Pagination should not be set!", row.findElements(MCRBy.partialText(TEST_STRING)).isEmpty());
    }

    @Test
    public void autoPaginationAll() throws InterruptedException {
        MCRWebdriverWrapper webDriver = getDriver();
        webDriver.waitAndFindElement(By.xpath("//button[@title=\"autoPagination\"]")).click();

        // wait for the Pagination-Dialog
        waitForElement(MCRBy.partialText("???paginationValue???"));
        webDriver.waitAndFindElement(MCRBy.partialText("undefined(1)"));
        webDriver.waitAndFindElement(MCRBy.partialText("undefined(34)"));
        webDriver.waitAndFindElement(By.xpath("//input[@type=\"text\"]")).sendKeys("1v");
        Select select = new Select(webDriver.findElement(By.tagName("select")));
        select.selectByVisibleText("???rectoVerso_lowercase???");
        webDriver.waitAndFindElement(By.xpath("//button[contains(text(),\"???paginationChange???\")]")).click();
        Assert.assertNotNull(webDriver.waitAndFindElement(MCRBy.partialText("1v")));
        Assert.assertNotNull(webDriver.waitAndFindElement(MCRBy.partialText("18r")));
    }

    /**
     * Does not work (Actions) not supported
     @Test
     @Ignore
    public void autoPaginationFew() throws InterruptedException {
        WebDriver webDriver = getDriver();
        WebElement row1 = webDriver.findElement(MCRBy.partialText("perthes_1855_0001.jpg")).findElement(
            By.xpath("ancestor::td"));
        Actions shift = new Actions(webDriver);
        WebElement row15 = webDriver.findElement(MCRBy.partialText("perthes_1855_0015.jpg")).findElement(
            By.xpath("ancestor::td"));
        shift.click(row1).click(row1).keyDown(Keys.SHIFT).click(row15).keyUp(Keys.SHIFT).perform();
        webDriver.findElement(By.xpath("//button[@title=\"autoPagination\"]")).click();
        // wait for the Pagination-Dialog
        waitForElement(MCRBy.partialText("???paginationValue???"));
        webDriver.findElement(MCRBy.partialText("undefined(1)"));
        webDriver.findElement(MCRBy.partialText("undefined(15)"));
        if (!webDriver.findElements(MCRBy.partialText("undefined(34)")).isEmpty())
            throw new AssertionError("to much elements for pagination selected!");
        webDriver.findElement(By.xpath("//input[@type=\"text\"]")).sendKeys("1v");
        Select select = new Select(webDriver.findElement(By.tagName("select")));
        select.selectByVisibleText("???rectoVerso_lowercase???");
        webDriver.findElement(By.xpath("//button[contains(text(),\"???paginationChange???\")]")).click();
        webDriver.findElement(MCRBy.partialText("1v"));
        webDriver.findElement(MCRBy.partialText("8v"));
    }
     */
    /* @Test
    public void deletePaginationFew() throws InterruptedException {
        WebDriver webDriver = getDriver();
        autoPaginationFew();
        WebElement row = webDriver.findElement(MCRBy.partialText("1v")).findElement(By.xpath("ancestor::tr"));
        row.findElement(By.xpath("//button[@title=\"???removePagination???\"]")).click();
        if (!webDriver.findElements(MCRBy.partialText("1v")).isEmpty())
            throw new AssertionError("Pagination has not been removed!");
    }*/

    /*
    @Test
    public void revertChange() throws InterruptedException {
        WebDriver webDriver = getDriver();
        deletePaginationFew();
        webDriver.findElement(By.xpath("//button[@title=\"???undo???\"]")).click();
        webDriver.findElement(MCRBy.partialText("1v"));
    }
    */

    //    TODO find a way to drag&drop the elements to test the sortation by hand
    //    @Test
    //    public void sortPagination() throws InterruptedException {
    //        WebDriver webDriver = getDriver();
    //        autoPaginationAll();
    //        WebElement row1 = webDriver.findElement(MyBy.byTextIgnoreCSS("perthes_1855_0001.jpg")).findElement(
    //            By.xpath("ancestor::td"));
    //        Actions move = new Actions(webDriver);
    //        WebElement rowToMove = webDriver.findElement(By.xpath("//tr[@ng-repeat-end][5]/td"));
    //        move.doubleClick(row1).clickAndHold(row1).moveToElement(rowToMove).release(row1).perform();
    //        Thread.sleep(100000);
    //    }

}
