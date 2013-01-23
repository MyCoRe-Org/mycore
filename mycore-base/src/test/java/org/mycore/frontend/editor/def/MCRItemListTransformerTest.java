package org.mycore.frontend.editor.def;

import static org.junit.Assert.*;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.frontend.editor.def.MCRItemListTransformer;

public class MCRItemListTransformerTest extends MCRTransformerTest {

    @Test
    public void testEmptyPanel() throws Exception {
        testTransformation(0, 0, 0, 0, 0);
    }

    @Test
    public void testSingleItem() throws Exception {
        testTransformation(1, 0, 0, 1, 1);
    }

    @Test
    public void testDefaultIsOneRow() throws Exception {
        testTransformation(3, 0, 0, 1, 3);
    }

    @Test
    public void testNumColsGiven() throws Exception {
        testTransformation(8, 0, 3, 3, 3);
    }

    @Test
    public void testNumRowsGiven() throws Exception {
        testTransformation(8, 2, 0, 2, 4);
    }

    @Test
    public void testColBeforeRowPrecedence() throws Exception {
        testTransformation(8, 2, 3, 3, 3);
    }

    @Test
    public void testNumItemsIsMaximumRows() throws Exception {
        testTransformation(3, 4, 0, 3, 1);
    }

    @Test
    public void testNumItemsIsMaximumCols() throws Exception {
        testTransformation(3, 0, 4, 1, 3);
    }

    private void testTransformation(int numItems, int rows, int cols, int rowsCalculated, int colsCalculated) {
        Element list = buildTestList(numItems, rows, cols);

        MCRItemListTransformer transformer = new MCRItemListTransformer();
        transformer.transform(list);
        assertEquals(rowsCalculated, transformer.getNumRows());
        assertEquals(colsCalculated, transformer.getNumCols());

        assertEquals(0, list.getChildren("item").size());
        assertEquals(rowsCalculated, list.getChildren("row").size());

        if (numItems > 0)
            assertEquals(colsCalculated, list.getChild("row").getChildren("item").size());
    }
}
