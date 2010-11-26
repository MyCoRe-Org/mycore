package org.mycore.frontend.editor.def;

import java.util.List;

import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.junit.Test;
import static org.junit.Assert.*;

public class MCREditorTransformerTest extends MCRTransformerTest {

    @Test
    public void testTransformation() throws Exception {
        Element rootPanel = buildTestPanel("1,1,*|2,1,*");
        Element innerPanel = buildTestPanel("1,1,*");
        Element innerList = buildTestList(2, 0, 0);

        List<Element> cells = rootPanel.getChildren("cell");
        cells.get(0).addContent(innerPanel);
        cells.get(1).addContent(innerList);

        new MCREditorTransformer().transform(rootPanel);

        assertNotNull(XPath.selectSingleNode(rootPanel, "row[1]/cell/panel/row/cell"));
        assertNotNull(XPath.selectSingleNode(rootPanel, "row[2]/cell/list/row/item"));
    }
}
