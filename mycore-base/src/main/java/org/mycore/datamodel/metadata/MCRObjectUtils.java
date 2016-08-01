package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * This class contains several helper methods for {@link MCRObject}.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRObjectUtils {

    private static XPathExpression<Attribute> META_LINK_HREF;

    private static XPathExpression<Element> META_CLASS;

    static {
        List<Namespace> ns = MCRConstants.getStandardNamespaces();

        // META_LINK_HREF
        String linkExp = "./mycoreobject/metadata/*[@class='MCRMetaLinkID']/*/@xlink:href";
        META_LINK_HREF = XPathFactory.instance().compile(linkExp, Filters.attribute(), null, ns);

        // META_CLASS
        String classExp = "./mycoreobject/metadata/*[@class='MCRMetaClassification']/*";
        META_CLASS = XPathFactory.instance().compile(classExp, Filters.element(), null, ns);
    }

    /**
     * Retrieves a list of all ancestors of the given object. The first entry
     * is the parent object, the last entry is the root node. Returns an empty
     * list if no ancestor is found.
     * 
     * @return list of ancestors
     */
    public static List<MCRObject> getAncestors(MCRObject mcrObject) {
        List<MCRObject> ancestorList = new ArrayList<MCRObject>();
        while (mcrObject.hasParent()) {
            MCRObjectID parentID = mcrObject.getStructure().getParentID();
            MCRObject parent = MCRMetadataManager.retrieveMCRObject(parentID);
            ancestorList.add(parent);
            mcrObject = parent;
        }
        return ancestorList;
    }

    /**
     * Returns a list of all ancestors and the object itself. The first entry
     * is the object itself, the last entry is the root node. Returns a list
     * with one entry if no ancestor is found.
     * 
     * @return list of ancestors
     */
    public static List<MCRObject> getAncestorsAndSelf(MCRObject mcrObject) {
        List<MCRObject> ancestorList = new ArrayList<MCRObject>();
        ancestorList.add(mcrObject);
        ancestorList.addAll(getAncestors(mcrObject));
        return ancestorList;
    }

    /**
     * Returns the root ancestor of the given object. If the object has
     * no parent null is returned.
     * 
     * @param mcrObject object to get the root node
     * @return root <code>MCRObject</code>
     */
    public static MCRObject getRoot(MCRObject mcrObject) {
        List<MCRObject> ancestorList = getAncestors(mcrObject);
        return ancestorList.isEmpty() ? null : ancestorList.get(ancestorList.size() - 1);
    }

    /**
     * Returns all children of the given object. If the object has no
     * children, an empty list is returned.
     * 
     * @param mcrObject the mycore object
     * @return list of all children
     */
    public static List<MCRObject> getChildren(MCRObject mcrObject) {
        return mcrObject.getStructure()
                        .getChildren()
                        .stream()
                        .map(link -> link.getXLinkHrefID())
                        .map(MCRMetadataManager::retrieveMCRObject)
                        .collect(Collectors.toList());
    }

    /**
     * Returns a list of all descendants and the object itself. For more information
     * see {@link MCRObjectUtils#getDescendants}.
     * 
     * @return list of all descendants and the object itself
     */
    public static List<MCRObject> getDescendantsAndSelf(MCRObject mcrObject) {
        List<MCRObject> objectList = getDescendants(mcrObject);
        objectList.add(mcrObject);
        return objectList;
    }

    /**
     * Returns a list of all descendants of the given object. Be aware that
     * there is no specific order. The list is empty if the object has no
     * children.
     * 
     * @return list of all descendants 
     */
    public static List<MCRObject> getDescendants(MCRObject mcrObject) {
        List<MCRObject> objectList = new ArrayList<>();
        getChildren(mcrObject).forEach(child -> {
            objectList.addAll(getDescendantsAndSelf(child));
        });
        return objectList;
    }

    /**
     * Returns a list of {@link MCRObject}s which are linked in the given object
     * by an {@link MCRMetaLinkID}. This does not return any {@link MCRObjectStructure}
     * links or any {@link MCRMetaDerivateLink}s.
     * 
     * @param object the object where to get the entitylinks from
     * @return a list of linked objects
     * @throws MCRPersistenceException one of the linked objects does not exists
     */
    public static List<MCRObject> getLinkedObjects(MCRObject object) throws MCRPersistenceException {
        Stream<String> stream = META_LINK_HREF.evaluate(object.createXML()).stream().map(Attribute::getValue);
        return stream.map(MCRObjectID::getInstance).peek(id -> {
            if (!MCRMetadataManager.exists(id)) {
                throw new MCRPersistenceException("MCRObject " + id + " is linked with (part of the metadata) "
                    + object.getId() + " but does not exist.");
            }
        }).map(MCRMetadataManager::retrieveMCRObject).collect(Collectors.toList());
    }

    /**
     * Returns a list of {@link MCRCategoryID}s which are used in the given object.
     * 
     * @param object the object where to get the categories from
     * @return a list of linked categories
     */
    public static List<MCRCategoryID> getCategories(MCRObject object) {
        Stream<Element> stream = META_CLASS.evaluate(object.createXML()).stream();
        return stream.map((e) -> {
            String classId = e.getAttributeValue("classid");
            String categId = e.getAttributeValue("categid");
            return new MCRCategoryID(classId, categId);
        }).distinct().collect(Collectors.toList());
    }

}
