package org.mycore.frontend.editor.def;

import org.jdom.Element;

public interface MCRTransformer {

    public void transform(Element element) throws Exception;
}
