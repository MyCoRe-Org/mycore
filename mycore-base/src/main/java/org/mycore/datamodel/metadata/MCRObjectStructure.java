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

package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;

import com.google.gson.JsonObject;

/**
 * Represents the {@code <structure>} section of a MyCoRe object.
 * <p>
 * This class stores the optional parent reference and the persisted
 * order of child objects. It can populate this state from XML and serialize it
 * back to XML or JSON.
 * <p>
 * The children order is a user-defined ordering hint. It is stored separately
 * from the actual child links and may therefore contain stale or incomplete
 * references if the child set changes independently.
 * <p>
 * Expanded structure data such as child and derivate links is handled by
 * {@link MCRExpandedObjectStructure}.
 *
 * @author Mathias Hegner
 * @author Jens Kupferschmidt
 */
public class MCRObjectStructure {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String XML_NAME = "structure";

    public static final String ELEMENT_DERIVATE_OBJECTS = "derobjects";
    public static final String PARENTS_ELEMENT_NAME = "parents";
    public static final String PARENT_ELEMENT_NAME = "parent";
    public static final String CHILDREN_ORDER_ELEMENT_NAME = "childrenOrder";
    public static final String CHILD_ELEMENT_NAME = "child";

    private MCRMetaParentID parent;

    private List<MCRObjectID> childrenOrder = new ArrayList<>();

    /**
     * Resets this structure to an empty state.
     */
    public void clear() {
        parent = null;
        childrenOrder.clear();
    }

    /**
     * Returns the parent link.
     *
     * @return the parent link, or {@code null} if no parent is set
     */
    public final MCRMetaParentID getParent() {
        return parent;
    }

    /**
     * Returns the parent object ID.
     *
     * @return the parent object ID, or {@code null} if no parent is set
     */
    public final MCRObjectID getParentID() {
        if (parent == null) {
            return null;
        }
        return parent.getXLinkHrefID();
    }

    /**
     * Sets the parent link.
     *
     * @param parent the parent link to set
     */
    public final void setParent(MCRMetaParentID parent) {
        this.parent = parent;
    }

    public final void setParent(MCRObjectID parentID) {
        setParent(parentID.toString());
    }

    public final void setParent(String parentID) {
        parent = new MCRMetaParentID(MCRObjectID.getInstance(parentID));
    }

    /**
     * Removes the parent reference.
     */
    public final void removeParent() {
        parent = null;
    }

    /**
     * Populates this structure from a {@code <structure>} XML element.
     *
     * @param element the structure element to read
     */
    public void setFromDOM(Element element) {
        clear();

        // Stricture parent part
        Element subElement = element.getChild(PARENTS_ELEMENT_NAME);

        if (subElement != null) {
            parent = new MCRMetaParentID();
            parent.setFromDOM(subElement.getChild(PARENT_ELEMENT_NAME));
        }

        Element childrenOrderElement = element.getChild(CHILDREN_ORDER_ELEMENT_NAME);
        if (childrenOrderElement != null) {
            for (Element child : childrenOrderElement.getChildren(CHILD_ELEMENT_NAME)) {
                String childID = child.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
                if (childID != null) {
                    childrenOrder.add(MCRObjectID.getInstance(childID));
                }
            }
        }
    }

    /**
     * Serializes this structure to a {@code <structure>} XML element.
     *
     * @return the serialized structure element
     * @throws MCRException if this structure is not valid
     */
    public Element createXML() throws MCRException {
        try {
            validate();
        } catch (MCRException exc) {
            throw new MCRException("The content is not valid.", exc);
        }

        Element structure = new Element(XML_NAME);

        if (parent != null) {
            Element parents = new Element(PARENTS_ELEMENT_NAME);
            parents.setAttribute("class", MCRMetaLinkID.class.getSimpleName());
            parents.addContent(parent.createXML());
            structure.addContent(parents);
        }

        if (!childrenOrder.isEmpty()) {
            Element childrenOrderElement = new Element(CHILDREN_ORDER_ELEMENT_NAME);
            childrenOrderElement.setAttribute("class", MCRMetaLinkID.class.getSimpleName());
            for (MCRObjectID child : childrenOrder) {
                Element childElement = new Element(CHILD_ELEMENT_NAME);
                childElement.setAttribute("href", child.toString(), MCRConstants.XLINK_NAMESPACE);
                childElement.setAttribute("type", "locator", MCRConstants.XLINK_NAMESPACE);
                childrenOrderElement.addContent(childElement);
            }
            structure.addContent(childrenOrderElement);
        }

        return structure;
    }

    /**
     * Creates the JSON representation of this structure.
     *
     * <pre>
     *   {
     *     parent: {@link MCRMetaLinkID#createJSON()}
     *   }
     * </pre>
     *
     * @return a JSON representation of this structure
     */
    public JsonObject createJSON() {
        JsonObject structure = new JsonObject();
        // parent
        Optional.ofNullable(getParent()).ifPresent(link -> structure.add(PARENT_ELEMENT_NAME, link.createJSON()));
        return structure;
    }

    /**
     * Logs the current structure state for debugging.
     */
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            if (parent != null) {
                parent.debug();
            }
        }
    }

    /**
     * Checks whether this structure is valid.
     *
     * @return {@code true} if this structure is valid, otherwise {@code false}
     */
    public final boolean isValid() {
        try {
            validate();
            return true;
        } catch (MCRException exc) {
            LOGGER.warn("The <structure> part of a <mycoreobject> is invalid.", exc);
        }
        return false;
    }

    /**
     * Validates this structure.
     *
     * @throws MCRException if the configured parent link is invalid
     */
    public void validate() throws MCRException {
        if (parent != null) {
            try {
                parent.validate();
            } catch (Exception exc) {
                throw new MCRException("The link to the parent '" + parent.getXLinkHref() + "' is invalid.", exc);
            }
        }
    }

    /**
     * Returns the persisted child order.
     *
     * <p>This list stores the order set by the user. It is not synchronized
     * automatically with the current child links and may therefore contain IDs
     * that are no longer children or omit children that were added later.</p>
     *
     * @return the persisted child order
     */
    public List<MCRObjectID> getChildrenOrder() {
        return childrenOrder;
    }

    /**
     * Sets the persisted child order.
     *
     * @param childrenOrder the child order to persist
     */
    public void setChildrenOrder(List<MCRObjectID> childrenOrder) {
        this.childrenOrder = childrenOrder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(childrenOrder, parent);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MCRObjectStructure other = (MCRObjectStructure) obj;
        return Objects.equals(childrenOrder, other.childrenOrder)
            && Objects.equals(parent, other.parent);
    }

}
