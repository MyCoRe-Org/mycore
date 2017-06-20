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
