package org.mycore.frontend.editor.def;

import java.text.MessageFormat;
import java.util.List;

import org.jdom2.DataConversionException;
import org.jdom2.Element;
import org.mycore.common.MCRConfigurationException;

public class MCRPanelCellTransformer extends MCRTransformerBase {

    private final static int UNDEFINED = Integer.MAX_VALUE;

    private Element panel;

    private int minRow = UNDEFINED;

    private int minCol = UNDEFINED;

    private int maxRow = 0;

    private int maxCol = 0;

    private List<Element> cells;

    private Element[][] grid;

    private Element spannedCell = new Element("spannedCell");

    public void transform(Element panel) throws DataConversionException {
        this.panel = panel;
        this.cells = panel.getChildren("cell");

        findMinMaxCoordinates();
        addSortNrAttribute();
        buildGrid();
        fillGrid();
        buildOutputRows();
    }

    private void buildOutputRows() {
        for (int row = 1; row <= maxRow; row++)
            if (!isEmptyRow(row))
                panel.addContent(buildOutputRow(row));
    }

    private Element buildOutputRow(int row) {
        Element out = new Element("row");
        for (int col = minCol; col <= maxCol; col++) {
            if (isEmptyCol(col) || isSpannedCell(row, col))
                continue;

            out.addContent(buildOutputCell(row, col));
        }
        return out;
    }

    private Element buildOutputCell(int row, int col) {
        Element cell = grid[row][col];
        if (cell == null)
            return new Element("cell");
        else {
            removeCellAttributes(cell);
            return (Element) (cell.detach());
        }
    }

    private boolean isSpannedCell(int row, int col) {
        return grid[row][col] == spannedCell;
    }

    private void removeCellAttributes(Element cell) {
        cell.removeAttribute("row");
        cell.removeAttribute("col");
    }

    private boolean isEmptyRow(int row) {
        for (int col = minCol; col <= maxCol; col++)
            if (grid[row][col] != null)
                return false;
        return true;
    }

    private boolean isEmptyCol(int col) {
        for (int row = minRow; row <= maxRow; row++)
            if (grid[row][col] != null)
                return false;
        return true;
    }

    private void buildGrid() {
        grid = new Element[maxRow + 1][maxCol + 1];
    }

    private void fillGrid() throws DataConversionException {
        for (Element cell : cells) {
            int row = getAttributeValue(cell, "row", 1);
            int col = getAttributeValue(cell, "col", 1);

            testPositionConflict(row, col, cell);
            grid[row][col] = cell;
            fillSpannedCells(cell, row, col);
        }
    }

    private void fillSpannedCells(Element cell, int row, int col) {
        int colspan = getAttributeValue(cell, "colspan", 1);
        for (int i = 1; i < colspan; i++) {
            testPositionConflict(row, col + i, cell);
            grid[row][col + i] = spannedCell;
        }
    }

    private void testPositionConflict(int row, int col, Element newCell) {
        Element existing = grid[row][col];
        if (existing == null)
            return;

        while (existing == spannedCell)
            existing = grid[row][--col];

        String msg = "Already existing " + outputCell(existing) + " is in position conflict with new " + outputCell(newCell);
        System.out.println(msg);
        throw new MCRConfigurationException(msg);
    }

    private String outputCell(Element cell) {
        int row = getAttributeValue(cell, "row", 1);
        int col = getAttributeValue(cell, "col", 1);
        int colspan = getAttributeValue(cell, "colspan", 1);
        return MessageFormat.format("<cell row={0} col={1} colspan={2} /> ", row, col, colspan);
    }

    private void findMinMaxCoordinates() throws DataConversionException {
        for (Element cell : cells) {
            int row = getAttributeValue(cell, "row", 1);
            int col = getAttributeValue(cell, "col", 1);

            int colspan = getAttributeValue(cell, "colspan", 1);
            col += colspan - 1;

            minRow = Math.min(minRow, row);
            minCol = Math.min(minCol, col);
            maxRow = Math.max(maxRow, row);
            maxCol = Math.max(maxCol, col);
        }
    }

    private void addSortNrAttribute() throws DataConversionException {
        int count = 1;
        for (Element cell : cells) {
            int sortNr = getAttributeValue(cell, "sortNr", count++);
            cell.setAttribute("sortNr", Integer.toString(sortNr));
        }
    }

    public int getMinRow() {
        return (minRow == UNDEFINED ? 0 : minRow);
    }

    public int getMinCol() {
        return (minCol == UNDEFINED ? 0 : minCol);
    }

    public int getMaxRow() {
        return maxRow;
    }

    public int getMaxCol() {
        return maxCol;
    }
}
