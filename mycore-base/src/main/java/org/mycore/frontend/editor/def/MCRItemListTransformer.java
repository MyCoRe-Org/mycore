package org.mycore.frontend.editor.def;

import java.util.List;

import org.jdom.Element;

public class MCRItemListTransformer extends MCREditorTransformer {

    private Element list;

    private int num;

    private int rows;

    private int cols;

    public MCRItemListTransformer(Element list) {
        this.list = list;
        calculateRowsCols();
    }

    private void calculateRowsCols() {
        num = list.getChildren("item").size();
        rows = getAttributeValue(list, "rows", 1);
        cols = (int) (Math.ceil((double) num / (double) rows));
        cols = Math.min(cols, getAttributeValue(list, "cols", cols));
        rows = (int) (Math.ceil((double) num / (double) cols));
    }

    public int getNumRows() {
        return rows;
    }

    public int getNumCols() {
        return cols;
    }

    public void transform() {
        List<Element> items = list.getChildren("item");

        for (int row = 1; row <= rows; row++) {
            Element r = new Element("row");
            list.addContent(r);

            for (int col = 1; col <= cols; col++) {
                int pos = (row - 1) * cols + col;
                if (pos <= num)
                    r.addContent(items.remove(0));
            }
        }
    }
}
