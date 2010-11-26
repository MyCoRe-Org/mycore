package org.mycore.frontend.editor.def;

import static org.junit.Assert.*;

import java.util.List;
import java.util.StringTokenizer;

import org.jdom.Element;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRConfigurationException;
import org.mycore.frontend.editor.def.MCRPanelCellTransformer;

public class MCRPanelCellTransformerTest extends MCRTransformerTest {

    MCRPanelCellTransformer transformer;

    @Before
    public void setup() {
        transformer = new MCRPanelCellTransformer();
    }

    @Test
    public void testEmptyPanel() throws Exception {
        testPanelTransformation("", "", 0, 0, 0, 0);
    }

    @Test
    public void testSingleCell() throws Exception {
        testPanelTransformation("1,1,*", "R|1,1", 1, 1, 1, 1);
    }

    @Test
    public void testComplexLayout() throws Exception {
        String oldStructure = "2,2,*|2,4,*|3,5,*|3,4,*";
        String newStructure = "R|2,2|2,4|-|R|-|3,4|3,5";
        testPanelTransformation(oldStructure, newStructure, 2, 3, 2, 5);
    }

    @Test
    public void testDefaults() throws Exception {
        String oldStructure = "*,2,*|2,4,*|3,5,*|3,*,*";
        String newStructure = "R|-|*,2|-|-|R|-|-|2,4|-|R|3,*|-|-|3,5";
        testPanelTransformation(oldStructure, newStructure, 1, 3, 1, 5);
    }

    @Test
    public void testSpannedCell() throws Exception {
        String oldStructure = "1,1,1|1,2,2|2,2,1|2,3,1";
        String newStructure = "R|1,1|1,2|R|-|2,2|2,3";
        testPanelTransformation(oldStructure, newStructure, 1, 2, 1, 3);
    }

    @Test(expected = MCRConfigurationException.class)
    public void testPositionConflict() throws Exception {
        Element panel = buildTestPanel("2,4,*|2,4,*");
        transformer.transform(panel);
    }

    @Test(expected = MCRConfigurationException.class)
    public void testPositionConflictWithExistingSpannedCell() throws Exception {
        Element panel = buildTestPanel("1,1,3|1,2,*");
        transformer.transform(panel);
    }

    @Test(expected = MCRConfigurationException.class)
    public void testPositionConflictWithNewSpannedCell() throws Exception {
        Element panel = buildTestPanel("1,2,*|1,1,3");
        transformer.transform(panel);
    }

    private void testPanelTransformation(String oldStructure, String newStructure, int minRow, int maxRow, int minCol, int maxCol) throws Exception {
        Element panel = buildTestPanel(oldStructure);
        transformer.transform(panel);
        assertMinMax(minRow, maxRow, minCol, maxCol);
        assertPanelLayout(panel, newStructure);
    }

    private void assertMinMax(int minRow, int maxRow, int minCol, int maxCol) {
        assertEquals(minCol, transformer.getMinCol());
        assertEquals(maxCol, transformer.getMaxCol());
        assertEquals(minRow, transformer.getMinRow());
        assertEquals(maxRow, transformer.getMaxRow());
    }

    private void assertPanelLayout(Element panel, String rowLayout) {
        assertEquals(0, panel.getChildren("cell").size());
        StringTokenizer st = new StringTokenizer(rowLayout, "|");
        for (Element row : (List<Element>) (panel.getChildren("row"))) {
            assertEquals("R", st.nextToken());
            for (Element cell : (List<Element>) (row.getChildren("cell"))) {
                assertEquals(st.nextToken(), cell.getAttributeValue("test", "-"));
            }
        }
        assertFalse(st.hasMoreTokens());
    }
}
