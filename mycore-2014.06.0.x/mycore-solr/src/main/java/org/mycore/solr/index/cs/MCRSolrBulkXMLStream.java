package org.mycore.solr.index.cs;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;

/**
 * A content stream class to index a bunch of xml elements. Use the {@link #getList()}
 * method to add elements.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrBulkXMLStream extends MCRSolrContentStream {

    public List<Element> elementList;

    public MCRSolrBulkXMLStream(String name) {
        super(name, null);
        this.elementList = new ArrayList<>();
    }

    public List<Element> getList() {
        return this.elementList;
    }

    @Override
    public MCRContent getSource() {
        Element objCollector = new Element("mcrObjs");
        for (Element e : this.elementList) {
            e = e.detach();
            objCollector.addContent(e);
        }
        return new MCRJDOMContent(objCollector);
    }

}
