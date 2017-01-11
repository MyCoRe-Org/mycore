package org.mycore.frontend.xeditor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xpath.NodeSet;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XNodeSetForDOM;
import org.mycore.common.xml.MCRURIResolver;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MCRIncludeHandler {

    private final static Logger LOGGER = LogManager.getLogger(MCRIncludeHandler.class);

    private final static Map<String, Node> includesPerApplication = new ConcurrentHashMap<String, Node>();

    private Map<String, Node> includesPerTransformation = new HashMap<String, Node>();

    public XNodeSet resolve(ExpressionContext context, String uri, String sStatic) throws TransformerException {
        boolean bStatic = "true".equals(sStatic) || includesPerApplication.containsKey(uri);
        Map<String, Node> includes = bStatic ? includesPerApplication : includesPerTransformation;

        LOGGER.debug("Including " + uri + " static=" + bStatic + " cached=" + includes.containsKey(uri));

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
