package org.mycore.frontend.editor.def;

import java.util.StringTokenizer;

import org.jdom2.Element;

public abstract class MCRTransformerTest {

    protected Element buildTestList(int numItems, int rows, int cols) {
        Element list = new Element("list");
        list.setAttribute("type", "radio");

        if (rows > 0)
            list.setAttribute("rows", Integer.toString(rows));
        if (cols > 0)
            list.setAttribute("cols", Integer.toString(cols));

        for (int i = 0; i < numItems; i++)
            list.addContent(new Element("item"));

        return list;
    }

    protected Element buildTestPanel(String cellLayout) {
        Element panel = new Element("panel");
        StringTokenizer st = new StringTokenizer(cellLayout, ",|");
        while (st.hasMoreTokens()) {
            Element cell = new Element("cell");
            panel.addContent(cell);

            String row = setAttribute(cell, "row", st.nextToken());
            String col = setAttribute(cell, "col", st.nextToken());
            setAttribute(cell, "colspan", st.nextToken());

            cell.setAttribute("test", row + "," + col);
        }
        return panel;
    }

    private String setAttribute(Element cell, String attributeName, String value) {
        if (!value.equals("*"))
            cell.setAttribute(attributeName, value);
        return value;
    }

}