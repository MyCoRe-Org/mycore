/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xpath.NodeSet;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XNodeSetForDOM;
import org.jdom2.JDOMException;
import org.jdom2.output.DOMOutputter;
import org.mycore.common.MCRUsageException;
import org.mycore.common.xml.MCRURIResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles xed:include, xed:preload and xed:modify|xed:extend to include XEditor components by URI and ID.
 *
 * @author Frank L\U00FCtzenkirchen
 */
public class MCRIncludeHandler {

    private static final String ATTR_AFTER = "after";

    private static final String ATTR_BEFORE = "before";

    private static final String ATTR_ID = "id";

    private static final String ATTR_REF = "ref";

    private static final Logger LOGGER = LogManager.getLogger();

    /** Caches preloaded components at application level: resolved only once, used many times (static) */
    private static final Map<String, Element> CACHE_AT_APPLICATION_LEVEL = new ConcurrentHashMap<>();

    /** Caches preloaded components at transformation level, resolved on every reload of editor form page */
    private Map<String, Element> cacheAtTransformationLevel = new HashMap<>();

    /**
     * Preloads editor components from one or more URIs.
     *
     * @param uris a list of URIs to preload, separated by whitespace
     * @param sStatic if true, use static cache on application level,
     *               otherwise reload at each XEditor form transformation
     */
    public void preloadFromURIs(String uris, String sStatic)
        throws TransformerFactoryConfigurationError {
        for (String uri : uris.split(",")) {
            preloadFromURI(uri, sStatic);
        }
    }

    private void preloadFromURI(String uri, String sStatic)
        throws TransformerFactoryConfigurationError {
        if (uri.isBlank()) {
            return;
        }

        LOGGER.debug(() -> "preloading " + uri);

        Element xml;
        try {
            xml = resolve(uri.trim(), sStatic);
        } catch (Exception ex) {
            LOGGER.warn(() -> "Exception preloading " + uri, ex);
            return;
        }

        Map<String, Element> cache = chooseCacheLevel(uri, sStatic);
        handlePreloadedComponents(xml, cache);
    }

    /**
     * Cache all descendant components that have an @id, handle xed:modify|xed:extend afterwards
     */
    private void handlePreloadedComponents(Element xml, Map<String, Element> cache) {
        for (Element component : getChildren(xml)) {
            cacheComponent(cache, component);
            handlePreloadedComponents(component, cache);
            handleModify(cache, component);
        }
    }

    private void cacheComponent(Map<String, Element> cache, Element element) {
        String id = getAttributeIfPresent(element, ATTR_ID);
        if ((id != null) && !id.isEmpty()) {
            LOGGER.debug(() -> "preloaded component " + id);
            cache.put(id, element);
        }
    }

    private void handleModify(Map<String, Element> cache, Element element) {
        if ("modify".equals(element.getLocalName())) {
            String refID = getAttributeIfPresent(element, ATTR_REF);
            if (refID == null) {
                throw new MCRUsageException("<xed:modify /> must have a @ref attribute!");
            }

            Element container = cache.get(refID);
            if (container == null) {
                LOGGER.warn(() -> "Ignoring xed:modify of " + refID + ", no component with that @id found");
                return;
            }

            container = (Element) container.cloneNode(true);

            String newID = getAttributeIfPresent(element, ATTR_ID);
            if (newID != null) {
                container.setAttribute(ATTR_ID, newID); // extend rather than modify
                LOGGER.debug(() -> "extending " + refID + " to " + newID);
            } else {
                LOGGER.debug(() -> "modifying " + refID);
            }

            for (Element command : getChildren(element)) {
                String commandType = command.getLocalName();
                if (Objects.equals(commandType, "remove")) {
                    handleRemove(container, command);
                } else if (Objects.equals(commandType, "include")) {
                    handleInclude(container, command);
                }
            }
            cacheComponent(cache, container);
        }
    }

    private void handleRemove(Element container, Element removeRule) {
        String id = getAttributeIfPresent(removeRule, ATTR_REF);
        LOGGER.debug(() -> "removing " + id);
        findDescendant(container, id).ifPresent(e -> e.getParentNode().removeChild(e));
    }

    private Optional<Element> findDescendant(Element container, String id) {
        NodeList children = container.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element) {
                if (hasOrIncludesID(element, id)) {
                    return Optional.of(element);
                }
                // Recursive search in child elements
                Optional<Element> descendant = findDescendant(element, id);
                if (descendant.isPresent()) {
                    return descendant;
                }
            }
        }
        return Optional.empty();
    }

    private boolean hasOrIncludesID(Element e, String id) {
        return id.equals(getAttributeIfPresent(e, ATTR_ID))
            || "include".equals(e.getLocalName()) && id.equals(getAttributeIfPresent(e, ATTR_REF));
    }

    private void handleInclude(Element container, Element includeRule) {
        boolean modified = handleBeforeAfter(container, includeRule, ATTR_BEFORE, 0, 0);
        if (!modified) {
            if (!includeRule.hasAttribute(ATTR_AFTER)) {
                includeRule.setAttribute(ATTR_AFTER, "*");
            }
            handleBeforeAfter(container, includeRule, ATTR_AFTER, 1, getChildren(container).size());
        }
    }

    private boolean handleBeforeAfter(Element container, Element includeRule, String attributeName, int offset,
        int defaultPos) {
        String refID = getAttributeIfPresent(includeRule, attributeName);
        if (refID != null) {
            includeRule.removeAttribute(attributeName);

            Element parent = container;
            int pos;

            Optional<Element> neighbor = findDescendant(container, refID);
            List<Element> childElements;
            if (neighbor.isPresent()) {
                Element n = neighbor.get();
                parent = (Element) n.getParentNode();
                childElements = getChildren(parent);
                pos = childElements.indexOf(n) + offset;
            } else {
                pos = defaultPos;
                childElements = getChildren(parent);
            }

            LOGGER.debug(
                () -> {
                    NamedNodeMap attributes = includeRule.getAttributes();
                    return "including " + IntStream.range(0, attributes.getLength())
                        .mapToObj(attributes::item)
                        .map(attr -> attr.getNodeName() + "=\"" + attr.getNodeValue() + "\"")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("") + " at pos " + pos;
                });
            Node newChild = parent.getOwnerDocument().adoptNode(includeRule.cloneNode(true));
            if (pos >= childElements.size()) {
                parent.appendChild(newChild);
            } else {
                parent.insertBefore(newChild, childElements.get(pos));
            }
        }
        return refID != null;
    }

    private List<Element> getChildren(Element parent) {
        return childrenCache.computeIfAbsent(parent, key -> {
            NodeList childNodes = key.getChildNodes();
            List<Element> childElements = new ArrayList<>(childNodes.getLength());
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i) instanceof Element) {
                    childElements.add((Element) childNodes.item(i));
                }
            }
            return childElements;
        });
    }

    private String getAttributeIfPresent(Element element, String attributeName) {
        if (element.hasAttribute(attributeName)) {
            return element.getAttribute(attributeName);
        }
        return null;
    }

    public XNodeSet resolve(ExpressionContext context, String ref) throws TransformerException {
        LOGGER.debug(() -> "including component " + ref);
        Map<String, Element> cache = chooseCacheLevel(ref, Boolean.FALSE.toString());
        Element resolved = cache.get(ref);
        return (resolved == null ? null : asNodeSet(context, resolved));
    }

    public XNodeSet resolve(ExpressionContext context, String uri, String sStatic)
        throws TransformerException {
        LOGGER.debug(() -> "including xml " + uri);

        Element xml = resolve(uri, sStatic);

        try {
            return asNodeSet(context, xml);
        } catch (Exception ex) {
            LOGGER.error(ex);
            throw ex;
        }
    }

    private Element resolve(String uri, String sStatic)
        throws TransformerFactoryConfigurationError, TransformerException {
        Map<String, Element> cache = chooseCacheLevel(uri, sStatic);

        if (cache.containsKey(uri)) {
            LOGGER.debug(() -> "uri was cached: " + uri);
            return cache.get(uri);
        } else {
            org.jdom2.Element xml = MCRURIResolver.obtainInstance().resolve(uri);
            Document domElement = jdom2dom(wrapIfNeeded(xml));
            cache.put(uri, domElement.getDocumentElement());
            return domElement.getDocumentElement();
        }
    }

    private org.jdom2.Document wrapIfNeeded(org.jdom2.Element rootElement) {
        if (rootElement.getDocument() == null) {
            return new org.jdom2.Document(rootElement);
        } else {
            return rootElement.getDocument();
        }
    }

    private Map<String, Element> chooseCacheLevel(String key, String sStatic) {
        if (Objects.equals(sStatic, "true") || CACHE_AT_APPLICATION_LEVEL.containsKey(key)) {
            return CACHE_AT_APPLICATION_LEVEL;
        } else {
            return cacheAtTransformationLevel;
        }
    }

    private Document jdom2dom(org.jdom2.Document document) throws TransformerException {
        DOMOutputter outputter = new DOMOutputter();
        try {
            return outputter.output(document);
        } catch (JDOMException e) {
            throw new TransformerException(e);
        }
    }

    private XNodeSet asNodeSet(ExpressionContext context, Node node) throws TransformerException {
        NodeSet nodeSet = new NodeSet();
        nodeSet.addNode(node);
        XPathContext xpc = context.getXPathContext();
        return new XNodeSetForDOM((NodeList) nodeSet, xpc);
    }
}
