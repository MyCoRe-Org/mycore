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

package org.mycore.iview.tests.base;

import java.io.IOException;
import java.net.URL;

import org.mycore.iview.tests.model.TestDerivate;

public class BaseTestConstants {
    public static final int TIME_OUT_IN_SECONDS = 30;

    protected static final TestDerivate RGB_TEST_DERIVATE = new TestDerivate() {

        @Override
        public String getStartFile() {
            return "r.png";
        }

        @Override
        public String getName() {
            return "derivate_0000005";
        }

        @Override
        public URL getZipLocation() throws IOException {
            return new URL("http://www.mycore.de/tests/derivate_0000005.zip");
        }

    };

    protected static final TestDerivate PDF_TEST_DERIVATE = new TestDerivate() {

        @Override
        public String getStartFile() {
            return "PDF-Test.pdf";
        }

        @Override
        public String getName() {
            return "derivate_0000004";
        }

        @Override
        public URL getZipLocation() throws IOException {
            return new URL("http://www.mycore.de/tests/PDF-Test.pdf");
        }

    };

}
