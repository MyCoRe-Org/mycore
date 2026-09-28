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

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.selenium.drivers.MCRWebdriverWrapper;
import org.mycore.common.selenium.util.MCRBy;
import org.openqa.selenium.By;
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
        MCRWebdriverWrapper webDriver = getDriver();
        WebElement row = webDriver.waitAndFindElement(MCRBy.partialText("perthes_1855_0001.jpg")).findElement(
            By.xpath("ancestor::tr"));
        webDriver.waitAndFindElement(By.xpath("//button[@title=\"???editPagination???\"]")).click();
        webDriver.waitAndFindElement(By.xpath("//input")).sendKeys(TEST_STRING);
        webDriver.waitAndFindElement(By.xpath("//button[@title=\"???paginationChange???\"]")).click();
        Assert.assertNotNull(webDriver.waitAndFindElement(MCRBy.partialText(TEST_STRING)));
    }

    @Test
    public void abortPagination() {
        MCRWebdriverWrapper webDriver = getDriver();
        WebElement row = webDriver.waitAndFindElement(MCRBy.partialText("perthes_1855_0001.jpg")).findElement(
            By.xpath("ancestor::tr"));
        webDriver.waitAndFindElement(By.xpath("//button[@title=\"???editPagination???\"]")).click();
        webDriver.waitAndFindElement(By.xpath("//input")).sendKeys(TEST_STRING);
        webDriver.waitAndFindElement(By.xpath("//button[@title=\"???paginationAbort???\"]")).click();
        Assert.assertTrue("Pagination should not be set!", row.findElements(MCRBy.partialText(TEST_STRING)).isEmpty());
    }

    @Test
    public void autoPaginationAll() {
        MCRWebdriverWrapper webDriver = getDriver();
        webDriver.waitAndFindElement(By.xpath("//button[@title=\"autoPagination\"]")).click();

        // wait for the Pagination-Dialog
        waitForElement(MCRBy.partialText("???paginationValue???"));
        webDriver.waitAndFindElement(MCRBy.partialText("undefined(1)"));
        webDriver.waitAndFindElement(MCRBy.partialText("undefined(34)"));
        webDriver.waitAndFindElement(By.xpath("//input[@type=\"text\"]")).sendKeys("1v");
        Select select = new Select(webDriver.waitAndFindElement(By.tagName("select")));
        select.selectByVisibleText("???rectoVerso_lowercase???");
        webDriver.waitAndFindElement(By.xpath("//button[contains(text(),\"???paginationChange???\")]")).click();
        Assert.assertNotNull(webDriver.waitAndFindElement(MCRBy.partialText("1v")));
        Assert.assertNotNull(webDriver.waitAndFindElement(MCRBy.partialText("18r")));
    }

}

