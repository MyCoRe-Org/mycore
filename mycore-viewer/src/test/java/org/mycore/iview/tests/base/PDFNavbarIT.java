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

package org.mycore.iview.tests.base;

import org.mycore.iview.tests.model.TestDerivate;
import org.openqa.selenium.WebDriver;

/**
 * @author Sebastian Röher (basti890)
 *
 */
public class PDFNavbarIT extends NavbarIT {

    protected PDFNavbarIT(WebDriver webDriver) {
        super(webDriver);
    }

    @Override
    public TestDerivate getTestDerivate() {
        return BaseTestConstants.PDF_TEST_DERIVATE;
    }

    @Override
    public String getGreenLabel() {
        return "[2]";
    }

    @Override
    public String getRedLabel() {
        return "[1]";
    }

    @Override
    public String getBlueLabel() {
        return "[3]";
    }

    @Override
    public String getRgbLabel() {
        return "[4]";
    }
}
