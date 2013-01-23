package org.mycore.frontend.editor.def;

import java.util.List;

import org.jdom2.Element;

public class MCREditorTransformer extends MCRTransformerBase {

    public void transform(Element element) throws Exception {
        String name = element.getName();
        String type = element.getAttributeValue("type");

        if ("panel".equals(name))
            new MCRPanelCellTransformer().transform(element);
        else if ("list".equals(name) && ("radio".equals(type) || "checkbox".equals(type)))
            new MCRItemListTransformer().transform(element);

        List<Element> children = element.getChildren();
        for (Element child : children) {
            transform(child);
        }
    }
}