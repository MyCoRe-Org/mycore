package org.mycore.iview.tests.base;

import org.junit.experimental.categories.Category;
import org.mycore.iview.tests.model.TestDerivate;

/**
 * @author Sebastian Röher (basti890)
 *
 */
@Category(org.mycore.iview.tests.groups.ImageViewerTests.class)
public class PDFNavbarIT extends NavbarIT {

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
