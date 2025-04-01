package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRXlink;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class MCRExpandedObjectStructure extends MCRObjectStructure {

    private static final Logger LOGGER = LogManager.getLogger();

    private final List<MCRMetaEnrichedLinkID> derivates;
    private final List<MCRMetaLinkID> children;

    public MCRExpandedObjectStructure() {
        derivates = new ArrayList<>();
        children = new ArrayList<>();

    }

    @Override
    public void clear() {
        super.clear();
        children.clear();
        derivates.clear();
    }

    /**
     * This method clean the data lists children of this class.
     */
    public void clearChildren() {
        children.clear();
    }

    /**
     * This method clean the data lists derivate of this class.
     */
    public final void clearDerivates() {
        derivates.clear();
    }

    /**
     * The method appends a child ID to the child link list if and only if it is
     * not already contained in the list, preventing from doubly-linked objects.
     * If the link could be added a "true" will be returned, otherwise "false".
     *
     * @param child
     *            the MCRMetaLinkID of the child
     * @return boolean true, if successfully done
     */
    public final boolean addChild(MCRMetaLinkID child) {
        for (MCRMetaLinkID c : children) {
            if (c.getXLinkHrefID().equals(child.getXLinkHrefID())) {
                return false;
            }
        }
        children.add(child);

        return true;
    }

    /**
     * removes a child link to another object.
     *  If the link was found a "true" will be returned, otherwise
     * "false".
     *
     * @param href
     *            the MCRObjectID of the child
     * @return boolean true, if successfully completed
     */
    public final boolean removeChild(MCRObjectID href) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Remove child ID {}", href);
        }
        return removeMetaLink(getChildren(), href);
    }

    /**
     * Checks if the child is in the children vector.
     *
     * @param childId child to check
     */
    public final boolean containsChild(MCRObjectID childId) {
        return getChildren().stream().map(MCRMetaLinkID::getXLinkHrefID).anyMatch(childId::equals);
    }

    /**
     * removes a derivate link.
     * If the link was found a "true" will be returned, otherwise
     * "false".
     *
     * @param href
     *            the MCRObjectID of the child
     * @return boolean true, if successfully completed
     */
    public final boolean removeDerivate(MCRObjectID href) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Remove derivate ID {}", href);
        }
        return removeMetaLink(getDerivates(), href);
    }

    /**
     * Removes a MCRMetaLinkID instance by it MCRObjectID.
     */
    private boolean removeMetaLink(List<? extends MCRMetaLinkID> list, MCRObjectID href) {
        final List<MCRMetaLink> toRemove = list.stream()
            .filter(ml -> ml.getXLinkHrefID().equals(href))
            .collect(Collectors.toList());
        return list.removeAll(toRemove);
    }

    /**
     * Returns all children in this structure
     * */
    public final List<MCRMetaLinkID> getChildren() {
        return children;
    }

    /**
     * <em>addDerivate</em> methode append the given derivate link data to the
     * derivate vector. If the link could be added a "true" will be returned,
     * otherwise "false".
     *
     * @param derivate
     *            the link to be added as MCRMetaLinkID
     */
    public final boolean addDerivate(MCRMetaEnrichedLinkID derivate) {
        MCRObjectID href = derivate.getXLinkHrefID();
        if (containsDerivate(href)) {
            return false;
        }
        if (!MCRMetadataManager.exists(href)) {
            LOGGER.warn("Cannot find derivate {}, will add it anyway.", href);
        }
        derivates.add(derivate);
        derivates.sort(Comparator.comparingInt(MCRMetaEnrichedLinkID::getOrder));
        return true;
    }

    /**
     * Adds or updates the derivate link. Returns true if the derivate is added
     * or updated. Returns false when nothing is done.
     *
     * @param derivateLink the link to add or update
     * @return true when the structure is changed
     */
    public final boolean addOrUpdateDerivate(MCRMetaEnrichedLinkID derivateLink) {
        if (derivateLink == null) {
            return false;
        }
        MCRObjectID derivateId = derivateLink.getXLinkHrefID();
        MCRMetaLinkID oldLink = getDerivateLink(derivateId);
        if (derivateLink.equals(oldLink)) {
            return false;
        }
        if (oldLink != null) {
            removeDerivate(oldLink.getXLinkHrefID());
        }
        return addDerivate(derivateLink);
    }

    /**
     * Checks if the derivate is in the derivate vector.
     *
     * @param derivateId derivate to check
     */
    public final boolean containsDerivate(MCRObjectID derivateId) {
        return getDerivateLink(derivateId) != null;
    }

    /**
     * Returns the derivate link by id or null.
     */
    public final MCRMetaEnrichedLinkID getDerivateLink(MCRObjectID derivateId) {
        return getDerivates().stream()
            .filter(derivate -> derivate.getXLinkHrefID().equals(derivateId))
            .findAny()
            .orElse(null);
    }

    /**
     * @return a list with all related derivate ids encapsulated within a {@link MCRMetaLinkID}
     * */
    public List<MCRMetaEnrichedLinkID> getDerivates() {
        return this.derivates;
    }

    @Override
    public void setFromDOM(Element element) {
        super.setFromDOM(element);
        Element subElement = element.getChild("children");

        if (subElement != null) {
            List<Element> childList = subElement.getChildren();

            for (Element linkElement : childList) {
                MCRMetaLinkID link = new MCRMetaLinkID();
                link.setFromDOM(linkElement);
                children.add(link);
            }
        }

        // Structure derivate part
        subElement = element.getChild(ELEMENT_DERIVATE_OBJECTS);

        if (subElement != null) {
            List<Element> derobjectList = subElement.getChildren();

            for (Element derElement : derobjectList) {
                addDerivate(MCRMetaEnrichedLinkIDFactory.obtainInstance().fromDom(derElement));
            }
        }
    }

    @Override
    public Element createXML() throws MCRException {
        Element root = super.createXML();

        if (!children.isEmpty()) {
            Element elmm = new Element("children");
            elmm.setAttribute("class", "MCRMetaLinkID");
            for (MCRMetaLinkID child : getChildren()) {
                elmm.addContent(child.createXML());
            }
            root.addContent(elmm);
        }

        if (!derivates.isEmpty()) {
            Element elmm = new Element(ELEMENT_DERIVATE_OBJECTS);
            elmm.setAttribute("class", "MCRMetaEnrichedLinkID");
            for (MCRMetaLinkID derivate : getDerivates()) {
                elmm.addContent(derivate.createXML());
            }
            root.addContent(elmm);
        }

        return root;
    }

    /**
     * Creates the JSON representation of this structure.
     *
     * <pre>
     *   {
     *     parent: {@link MCRMetaLinkID#createJSON()},
     *     children: [
     *      {@link MCRMetaLinkID#createJSON()}
     *      ...
     *     ],
     *     derivates: [
     *       {@link MCRMetaLinkID#createJSON()}
     *        ...
     *     ]
     *   }
     * </pre>
     *
     * @return a json gson representation of this structure
     */
    @Override
    public JsonObject createJSON() {
        JsonObject structure = super.createJSON();
        // children
        JsonArray children = new JsonArray();
        getChildren().forEach(child -> children.add(child.createJSON()));
        structure.add("children", children);
        // derivates
        JsonArray derivates = new JsonArray();
        getDerivates().forEach(derivate -> derivates.add(derivate.createJSON()));
        structure.add("derivates", derivates);
        return structure;
    }

    @Override
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            for (MCRMetaLinkID linkID : derivates) {
                linkID.debug();
            }
            super.debug();
            for (MCRMetaLinkID linkID : children) {
                linkID.debug();
            }
        }
    }

    /**
     * Validates this MCRObjectStructure. This method throws an exception if:
     *  <ul>
     *  <li>the parent is not null but invalid</li>
     *  <li>one of the children is invalid</li>
     *  <li>one of the derivates is invalid</li>
     *  </ul>
     *
     * @throws MCRException the MCRObjectStructure is invalid
     */
    @Override
    public void validate() throws MCRException {
        for (MCRMetaLinkID child : getChildren()) {
            try {
                child.validate();
            } catch (Exception exc) {
                throw new MCRException("The link to the children '" + child.getXLinkHref() + "' is invalid.", exc);
            }
        }

        super.validate();

        for (MCRMetaLinkID derivate : getDerivates()) {
            try {
                derivate.validate();
            } catch (Exception exc) {
                throw new MCRException("The link to the derivate '" + derivate.getXLinkHref() + "' is invalid.", exc);
            }
            if (!derivate.getXLinkType().equals(MCRXlink.TYPE_LOCATOR)) {
                throw new MCRException("The xlink:type of the derivate link '" + derivate.getXLinkHref()
                    + "' has to be 'locator' and not '" + derivate.getXLinkType() + "'.");
            }

            String typeId = derivate.getXLinkHrefID().getTypeId();
            if (!typeId.equals(MCRDerivate.OBJECT_TYPE)) {
                throw new MCRException("The derivate link '" + derivate.getXLinkHref()
                    + "' is invalid. The _type_ has to be 'derivate' and not '" + typeId + "'.");
            }
        }
    }
}
