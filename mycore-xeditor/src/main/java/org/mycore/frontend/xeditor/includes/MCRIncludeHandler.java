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

package org.mycore.frontend.xeditor.includes;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.util.IteratorIterable;
import org.mycore.common.MCRUsageException;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Handles xed:include, xed:preload, xed:modify and xed:extend 
 * to include or modify XEditor components by URI and ID.
 *
 * @author Frank L\U00FCtzenkirchen
 */
public class MCRIncludeHandler {

    private static final String CMD_INCLUDE = "include";

    private static final String CMD_REMOVE = "remove";

    private static final String CMD_MODIFY = "modify";

    private static final String ATTR_AFTER = "after";

    private static final String ATTR_BEFORE = "before";

    private static final String ATTR_ID = "id";

    private static final String ATTR_REF = "ref";

    private static final Logger LOGGER = LogManager.getLogger(MCRIncludeHandler.class);

    private MCRIncludeCache includeCache = new MCRIncludeCache();

    /**
     * Preloads editor components from one or more URIs.
     *
     * @param uris a list of URIs to preload, separated by comma
     */
    public void preloadFromURIs(String uris) throws TransformerFactoryConfigurationError {
        for (String uri : uris.split(",")) {
            preloadFromURI(uri);
        }
    }

    private void preloadFromURI(String uri) throws TransformerFactoryConfigurationError {
        if (uri.isBlank()) {
            return;
        }

        LOGGER.debug(() -> "preloading " + uri);

        Element xml;
        try {
            xml = resolveURI(uri.trim());
        } catch (Exception ex) {
            LOGGER.warn(() -> "Exception preloading " + uri, ex);
            return;
        }

        handlePreloadedComponents(xml);
    }

    /**
     * Cache all descendant components that have an @id, handle xed:modify|xed:extend afterwards
     */
    private void handlePreloadedComponents(Element xml) {
        for (Element component : xml.getChildren()) {
            includeCache.put(component);
            handlePreloadedComponents(component);
            handleModify(component);
        }
    }

    private void handleModify(Element element) {
        if (CMD_MODIFY.equals(element.getName())) {
            String refID = element.getAttributeValue(ATTR_REF);
            if (refID == null) {
                throw new MCRUsageException("<xed:modify /> must have a @ref attribute!");
            }

            Element container = includeCache.get(refID);
            if (container == null) {
                LOGGER.warn(() -> "Ignoring xed:modify of " + refID + ", no component with that @id found");
                return;
            }

            container = container.clone();

            String newID = element.getAttributeValue(ATTR_ID);
            if (newID != null) {
                container.setAttribute(ATTR_ID, newID); // extend rather that modify
                LOGGER.debug(() -> "extending " + refID + " to " + newID);
            } else {
                LOGGER.debug(() -> "modifying " + refID);
            }

            for (Element command : element.getChildren()) {
                String commandType = command.getName();
                if (Objects.equals(commandType, CMD_REMOVE)) {
                    handleRemove(container, command);
                } else if (Objects.equals(commandType, CMD_INCLUDE)) {
                    handleInclude(container, command);
                }
            }

            includeCache.put(container);
        }
    }

    /**
     * Handles xed:remove[@ref] to remove an element from xml.
     * 
     * The @ref attribut determines the ID of the element to be removed.
     * The element to be removed must have an @id matching the given @ref.
     * 
     * @param container the container element to remove from, somewhere below
     * @param removeRule the xed:remove element with @ref attribute
     */
    private void handleRemove(Element container, Element removeRule) {
        String id = removeRule.getAttributeValue(ATTR_REF);
        LOGGER.debug(() -> "removing " + id);
        findDescendant(container, id).ifPresent(Element::detach);
    }

    /**
     * Tries to find an element with the given id below the container xml.
     * This can be a matching @id, or a matching xed:include/@ref with that id.
     * 
     * @param container the xml element to search withing
     * @param id the @id of the element to find
     * @return the first matching element with the @id, or null.
     */
    private Optional<Element> findDescendant(Element container, String id) {
        IteratorIterable<Element> descendants = container.getDescendants(new ElementFilter());
        return StreamSupport.stream(descendants.spliterator(), false)
            .filter(e -> hasOrIncludesID(e, id)).findFirst();
    }

    private boolean hasOrIncludesID(Element e, String id) {
        return id.equals(e.getAttributeValue(ATTR_ID))
            || CMD_INCLUDE.equals(e.getName()) && id.equals(e.getAttributeValue(ATTR_REF));
    }

    private void handleInclude(Element container, Element includeRule) {
        boolean modified = handleBeforeAfter(container, includeRule, ATTR_BEFORE, 0, 0);
        if (!modified) {
            includeRule.setAttribute(ATTR_AFTER, includeRule.getAttributeValue(ATTR_AFTER, "*"));
            handleBeforeAfter(container, includeRule, ATTR_AFTER, 1, container.getChildren().size());
        }
    }

    private boolean handleBeforeAfter(Element container, Element includeRule, String attributeName, int offset,
        int defaultPos) {
        String refID = includeRule.getAttributeValue(attributeName);
        if (refID != null) {
            includeRule.removeAttribute(attributeName);

            Element parent = container;
            int pos;

            Optional<Element> neighbor = findDescendant(container, refID);
            if (neighbor.isPresent()) {
                Element n = neighbor.get();
                parent = n.getParentElement();
                List<Element> children = parent.getChildren();
                pos = children.indexOf(n) + offset;
            } else {
                pos = defaultPos;
            }

            LOGGER.debug(
                () -> "including  " + Arrays.toString(includeRule.getAttributes().toArray()) + " at pos {}" + pos);
            parent.getChildren().add(pos, includeRule.clone());
        }
        return refID != null;
    }

    Element resolveID(String id) throws TransformerException {
        LOGGER.debug(() -> "including component " + id);
        return includeCache.get(id);
    }

    Element resolveURI(String uri)
        throws TransformerFactoryConfigurationError {
        Element resolved = includeCache.get(uri);

        if (resolved != null) {
            LOGGER.debug(() -> "uri was cached: " + uri);
            return resolved;
        } else {
            Element xml = MCRURIResolver.instance().resolve(uri);
            includeCache.put(xml);
            return xml;
        }
    }
}
