/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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

    private static final Logger LOGGER = LogManager.getLogger(MCRIncludeHandler.class);

    private static final Map<String, Node> includesPerApplication = new ConcurrentHashMap<>();

    private Map<String, Node> includesPerTransformation = new HashMap<>();

    public XNodeSet resolve(ExpressionContext context, String uri, String sStatic) throws TransformerException {
        boolean bStatic = "true".equals(sStatic) || includesPerApplication.containsKey(uri);
        Map<String, Node> includes = bStatic ? includesPerApplication : includesPerTransformation;

        LOGGER.debug("Including {} static={} cached={}", uri, bStatic, includes.containsKey(uri));

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
