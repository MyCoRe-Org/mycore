package org.mycore.iview.tests.base;

import org.junit.experimental.categories.Category;
import org.mycore.iview.tests.model.TestDerivate;

/**
 * @author Sebastian Röher (basti890)
 *
 */
@Category(org.mycore.iview.tests.groups.ImageViewerTests.class)
public class PDFStructureIT extends StructureOverviewIT {

    @Override
    public TestDerivate getTestDerivate() {
        return BaseTestConstants.PDF_TEST_DERIVATE;
    }

    @Override
    public String getGreenLabel() {
        return "g.png";
    }

    @Override
    public String getRedLabel() {
        return "r.png";
    }

    @Override
    public String getBlueLabel() {
        return "b.png";
    }

    @Override
    public String getRgbLabel() {
        return "rgb.png";
    }
}
