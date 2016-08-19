package org.mycore.iview.tests.base;

import org.junit.experimental.categories.Category;
import org.mycore.iview.tests.model.TestDerivate;

/**
 * @author Sebastian Röher (basti890)
 *
 */
@Category(org.mycore.iview.tests.groups.ImageViewerTests.class)
public class PDFImageSectionIT extends ImageSectionIT {

    @Override
    public TestDerivate getTestDerivate() {
        return BaseTestConstants.PDF_TEST_DERIVATE;
    }

    @Override
    public String getStartImage() {
        return "[4]";
    }
}
