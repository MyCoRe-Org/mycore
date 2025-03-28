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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;

/**
 * This class contains several helper methods for {@link MCRObject}.
 *
 * @author Matthias Eichner
 */
public final class MCRObjectUtils {

    private static final XPathExpression<Attribute> META_LINK_HREF;

    private static final XPathExpression<Element> META_CLASS;

    static {
        // META_LINK_HREF
        String linkExp = "./mycoreobject/metadata/*[@class='MCRMetaLinkID']/*/@xlink:href";
        META_LINK_HREF = XPathFactory.instance().compile(linkExp, Filters.attribute(), null,
            MCRConstants.getStandardNamespaces());

        // META_CLASS
        String classExp = "./mycoreobject/metadata/*[@class='MCRMetaClassification']/*";
        META_CLASS = XPathFactory.instance().compile(classExp, Filters.element(), null,
            MCRConstants.getStandardNamespaces());
    }

    private MCRObjectUtils() {
    }

    /**
     * Retrieves a list of all ancestors of the given object. The first entry
     * is the parent object, the last entry is the root node. Returns an empty
     * list if no ancestor is found.
     *
     * @return list of ancestors
     */
    public static List<MCRObject> getAncestors(MCRObject mcrObject) {
        List<MCRObject> ancestorList = new ArrayList<>();
        MCRObject currentAncestor = mcrObject;
        while (currentAncestor.hasParent()) {
            MCRObjectID parentID = currentAncestor.getStructure().getParentID();
            MCRObject parent = MCRMetadataManager.retrieveMCRObject(parentID);
            ancestorList.add(parent);
            currentAncestor = parent;
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
        List<MCRObject> ancestorList = new ArrayList<>();
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
        return ancestorList.isEmpty() ? null : ancestorList.getLast();
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
            .map(MCRMetaLinkID::getXLinkHrefID)
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
        getChildren(mcrObject).forEach(child -> objectList.addAll(getDescendantsAndSelf(child)));
        return objectList;
    }

    /**
     * Returns all derivates connected with this object. This includes derivates which are defined in the
     * structure part and also derivate links.
     *
     * @param mcrObjectID object identifier to get the root node
     * @return set of derivates
     */
    public static List<MCRObjectID> getDerivates(MCRObjectID mcrObjectID) {
        MCRLinkTableManager linkTableManager = MCRLinkTableManager.getInstance();
        Stream<String> derivateStream = linkTableManager
            .getDestinationOf(mcrObjectID, MCRLinkTableManager.ENTRY_TYPE_DERIVATE).stream();
        Stream<String> derivateLinkStream = linkTableManager
            .getDestinationOf(mcrObjectID, MCRLinkTableManager.ENTRY_TYPE_DERIVATE_LINK).stream()
            .map(link -> link.substring(0, link.indexOf('/')));
        return Stream.concat(derivateStream, derivateLinkStream).distinct().map(MCRObjectID::getInstance)
            .collect(Collectors.toList());
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
     * <p>Removes all links of the source object. This includes parent links, children links and metadata links. A list
     * of all updated objects is returned.</p>
     * <p>Be aware that this method does not take care of storing the returned objects.</p>
     *
     * @param sourceId id of the object
     * @return a stream of updated objects where a link of the source was removed
     */
    public static Stream<MCRObject> removeLinks(MCRObjectID sourceId) {
        return MCRLinkTableManager.getInstance().getSourceOf(sourceId).stream().filter(MCRObjectID::isValid)
            .map(MCRObjectID::getInstance).distinct().map(MCRMetadataManager::retrieveMCRObject)
            .flatMap(linkedObject -> removeLink(linkedObject, sourceId) ? Stream.of(linkedObject)
                : Stream.empty());
    }

    /**
     * <p>Removes the <b>linkToRemove</b> in the metadata and the structure part of the <b>source</b> object. Be aware
     * that this can lead to a zombie source object without a parent! Use this method with care!</p>
     *
     * <p>This method does not take care of storing the source object.</p>
     *
     * @param source the source object where the links should be removed from
     * @param linkToRemove the link id to remove
     * @return true if a link was removed (the source object changed)
     */
    public static boolean removeLink(MCRObject source, MCRObjectID linkToRemove) {
        final AtomicBoolean updated = new AtomicBoolean(false);
        // remove parent
        if (source.getParent() != null && source.getParent().equals(linkToRemove)) {
            source.getStructure().removeParent();
            updated.set(true);
        }
        // remove children
        if (source.getStructure().removeChild(linkToRemove)) {
            updated.set(true);
        }
        // remove metadata parts
        List<MCRMetaElement> emptyElements = source.getMetadata().stream()
            .filter(metaElement -> metaElement.getClazz().equals(MCRMetaLinkID.class))
            .flatMap(metaElement -> {
                List<MCRMetaLinkID> linksToRemove = metaElement.stream().map(MCRMetaLinkID.class::cast)
                    .filter(metaLinkID -> metaLinkID.getXLinkHrefID().equals(linkToRemove))
                    .collect(Collectors.toList());
                if (!linksToRemove.isEmpty()) {
                    updated.set(true);
                    linksToRemove.forEach(metaElement::removeMetaObject);
                }
                return metaElement.isEmpty() ? Stream.of(metaElement) : Stream.empty();
            }).collect(Collectors.toList());
        emptyElements.forEach(source.getMetadata()::removeMetadataElement);
        return updated.get();
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

    /**
     * Restores a MyCoRe Object to the selected revision. Please note that children and derivates
     * are not deleted or reverted!
     *
     * @param mcrId the mycore object identifier
     * @param revision The revision to restore to. If this is lower than zero, the last revision is used.
     * @return the new {@link MCRObject}
     *
     * @throws IOException An error occurred while retrieving the revision information. This is most
     *          likely due an svn error.
     * @throws MCRPersistenceException There is no such object with the given id and revision.
     * @throws ClassCastException The returning type must be the same as the type of the restored object
     */
    public static <T extends MCRBase> T restore(MCRObjectID mcrId, String revision) throws IOException,
        MCRPersistenceException {
        @SuppressWarnings("unchecked")
        T mcrBase = (T) (mcrId.getTypeId().equals(MCRDerivate.OBJECT_TYPE) ? new MCRDerivate() : new MCRObject());

        // get content
        MCRXMLMetadataManager xmlMetadataManager = MCRXMLMetadataManager.getInstance();
        MCRContent content = xmlMetadataManager.retrieveContent(mcrId, revision);
        if (content == null) {
            throw new MCRPersistenceException("No such object " + mcrId + " with revision " + revision + ".");
        }

        // store it
        try {
            mcrBase.setFromJDOM(content.asXML());
            if (MCRMetadataManager.exists(mcrId)) {
                // set modified date to now() to force update
                mcrBase.getService().setDate(MCRObjectService.DATE_TYPE_MODIFYDATE, new Date());
                MCRMetadataManager.update(mcrBase);
            } else {
                if (mcrBase instanceof MCRObject object) {
                    MCRMetadataManager.create(object);
                } else {
                    MCRMetadataManager.create((MCRDerivate) mcrBase);
                }
            }
            return mcrBase;
        } catch (Exception exc) {
            throw new MCRException("Unable to get object " + mcrId + " with revision " + revision + ".", exc);
        }
    }

}
