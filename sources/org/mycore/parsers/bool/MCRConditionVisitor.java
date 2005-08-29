package org.mycore.parsers.bool;

import org.jdom.Element;

public interface MCRConditionVisitor {

    abstract void visitQuery(MCRCondition entry);
    abstract void visitType(Element element);

}
