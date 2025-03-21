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
 * This class implements code for the inheritance of metadata of linked objects
 * and the linking of derivates onto an MCRObject. These links are described by
 * the <em>MCRMetaLink</em> class. For links to another object, there are
 * "locators" in use only, and the href variable gives the ID of the linked
 * object, while the label and title attributes can be used freely. Subtag name = "
 * &lt;child&gt;" means a child link from a "parent" object (collected in the
 * "children" and "parents" section of the "structure" part, respectively). The
 * child inherits all heritable metadata of the parent. If the parent itself is
 * a child of another parent, the heritable metadata of this "grand parent" is
 * inherited by the child as well. This mechanism recursively traces the full
 * inheritance hierarchy. So if the grand parent itself has a parent, this grand
 * parent parent's heritable metadata will be inherited and so on. Note, that it
 * is impossible to inherit metadata from multiple parents. In cases of multiple
 * inheritance request, an exception is thrown. A child link cannot occur twice
 * from the same object to the same href (preventing from doubled links). Not
 * supported by this class are links from or to a defined place of a document
 * (inner structure and combination of inner and outer structures of the
 * objects). This will possibly be done in a later extension of
 * <em>MCRMetaLink</em> and <em>MCRObjectStructure</em>.
 *
 * @author Mathias Hegner
 * @author Jens Kupferschmidt
 */
public class MCRObjectStructure {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String XML_NAME = "structure";

    public static final String ELEMENT_DERIVATE_OBJECTS = "derobjects";

    private MCRMetaParentID parent;

    private List<MCRObjectID> childrenOrder = new ArrayList<>();

    /**
     * This method clean the data lists parent, children and derivates of this
     * class.
     */
    public void clear() {
        parent = null;
        childrenOrder.clear();
    }

    /**
     * The method returns the parent link.
     *
     * @return MCRMetaLinkID the corresponding link
     */
    public final MCRMetaParentID getParent() {
        return parent;
    }

    /**
     * The method return the parent reference as a MCRObjectID.
     *
     * @return the parent MCRObjectID or null if there is no parent present
     */
    public final MCRObjectID getParentID() {
        if (parent == null) {
            return null;
        }
        return parent.getXLinkHrefID();
    }

    /**
     * This method set the parent value from a given MCRMetaLinkID.
     *
     * @param parent
     *            the MCRMetaLinkID to set
     *
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
     * Removes the parent reference. Use this method with care!
     */
    public final void removeParent() {
        parent = null;
    }



    /**
     * While the preceding methods dealt with the structure's copy in memory
     * only, the following three will affect the operations to or from datastore
     * too. Thereby <em>setFromDOM</em> will read the structure data from an
     * XML input stream (the "structure" entry).
     *
     * @param element the structure node list
     */
    public void setFromDOM(Element element) {
        clear();

        // Stricture parent part
        Element subElement = element.getChild("parents");

        if (subElement != null) {
            parent = new MCRMetaParentID();
            parent.setFromDOM(subElement.getChild("parent"));
        }

        Element childrenOrderElement = element.getChild("childrenOrder");
        if (childrenOrderElement != null) {
            for (Element child : childrenOrderElement.getChildren("child")) {
                String childID = child.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
                if (childID != null) {
                    childrenOrder.add(MCRObjectID.getInstance(childID));
                }
            }
        }
    }

    /**
     * <em>createXML</em> is the inverse of setFromDOM and converts the
     * structure's memory copy into XML.
     *
     * @exception MCRException
     *                if the content of this class is not valid
     * @return the structure XML
     */
    public Element createXML() throws MCRException {
        try {
            validate();
        } catch (MCRException exc) {
            throw new MCRException("The content is not valid.", exc);
        }

        Element elm = new Element(XML_NAME);

        if (parent != null) {
            Element elmm = new Element("parents");
            elmm.setAttribute("class", "MCRMetaLinkID");
            elmm.addContent(parent.createXML());
            elm.addContent(elmm);
        }

        if (!childrenOrder.isEmpty()) {
            Element childrenOrderElement = new Element("childrenOrder");
            for (MCRObjectID child : childrenOrder) {
                Element childElement = new Element("child");
                childElement.setAttribute("href", child.toString(), MCRConstants.XLINK_NAMESPACE);
                childrenOrderElement.addContent(childElement);
            }
            elm.addContent(childrenOrderElement);
        }

        return elm;
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
     * @return a json gson representation of this structure
     */
    public JsonObject createJSON() {
        JsonObject structure = new JsonObject();
        // parent
        Optional.ofNullable(getParent()).ifPresent(link -> structure.add("parent", link.createJSON()));
        return structure;
    }

    /**
     * The method print all informations about this MCRObjectStructure.
     */
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            if (parent != null) {
                parent.debug();
            }
        }
    }

    /**
     * <em>isValid</em> checks whether all of the MCRMetaLink's in the link
     * vectors are valid or not.
     *
     * @return boolean true, if structure is valid
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
     * Validates this MCRObjectStructure. This method throws an exception if:
     *  <ul>
     *  <li>the parent is not null but invalid</li>
     *  </ul>
     *
     * @throws MCRException the MCRObjectStructure is invalid
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
     * Returns the list of children order. This list is used to store the order of the children. The list can contain
     * ObjectIDs which are not children anymore or can miss children which are not in the list. This is because the
     * children order is not updated when the children are changed. The list is used to store the order which was
     * set by the user.
     * @return the list of children order
     */
    public List<MCRObjectID> getChildrenOrder() {
        return childrenOrder;
    }

    /**
     * Sets the list of children order. This list is used to store the order of the children.
     * @param childrenOrder the list of children order
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
