package org.mycore.services.fieldquery;

import org.jdom.Element;

public interface MCRQueryConditionVisitor {

    abstract void visitQuery(MCRQueryCondition entry);
    
    abstract void visitType(Element element);

}
