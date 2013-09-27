package org.mycore.frontend.xeditor;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.apache.log4j.Logger;
import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xpath.NodeSet;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XNodeSetForDOM;
import org.mycore.common.xml.MCRURIResolver;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MCRIncludeHandler {

    private final static Logger LOGGER = Logger.getLogger(MCRIncludeHandler.class);

    public Map<String, Node> includes = new HashMap<String, Node>();

    public XNodeSet resolve(ExpressionContext context, String uri) throws TransformerException {
        LOGGER.debug("Including " + uri + ", is already cached? " + includes.containsKey(uri));

        Node node = includes.get(uri);
        if (node == null) {
            Source source = MCRURIResolver.instance().resolve(uri, null);
            if (source instanceof DOMSource)
                node = ((DOMSource) source).getNode();
            else {
                DOMResult result = new DOMResult();
                TransformerFactory.newInstance().newTransformer().transform(source, result);
                node = result.getNode();
            }
            includes.put(uri, node);
        }

        NodeSet nodeSet = new NodeSet();
        nodeSet.addNode(node);
        return new XNodeSetForDOM((NodeList) nodeSet, context.getXPathContext());
    }
}
