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

package org.mycore.datamodel.common;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.OperationNotSupportedException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class manage all accesses to the link table database. This database
 * holds all informations about links between MCRObjects/MCRClassifications.
 *
 * @author Jens Kupferschmidt
 */
public final class MCRLinkTableManager {
    /** The list of entry types */
    public static final MCRLinkType ENTRY_TYPE_CHILD = MCRLinkType.CHILD;

    public static final MCRLinkType ENTRY_TYPE_DERIVATE = MCRLinkType.DERIVATE;

    public static final MCRLinkType ENTRY_TYPE_DERIVATE_LINK = MCRLinkType.DERIVATE_LINK;

    public static final MCRLinkType ENTRY_TYPE_PARENT = MCRLinkType.PARENT;

    public static final MCRLinkType ENTRY_TYPE_REFERENCE = MCRLinkType.REFERENCE;

    public static final String LINK_PROVIDER_CONFIG_PREFIX = "MCR.Persistence.LinkProvider.Impl.";

    /** The link table manager singleton */
    private static final MCRLinkTableManager SINGLETON_INSTANCE = new MCRLinkTableManager();

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCRLinkTableInterface linkTableInstance;

    private Map<String, MCRBaseLinkProvider> linkProviders;

    /**
     * The constructor of this class.
     */
    private MCRLinkTableManager() {
        // Load the persistence class

        linkTableInstance = MCRConfiguration2.getInstanceOfOrThrow(
            MCRLinkTableInterface.class, "MCR.Persistence.LinkTable.Store.Class");

        linkProviders = MCRConfiguration2.getInstances(MCRBaseLinkProvider.class, LINK_PROVIDER_CONFIG_PREFIX)
            .entrySet()
            .stream()
            .map(e -> {
                try {
                    return new SimpleImmutableEntry<>(e.getKey(), e.getValue().call());
                } catch (Exception ex) {
                    throw new MCRException(
                        "Error while initializing the " + MCRBaseLinkProvider.class.getSimpleName() + " " + e.getKey(),
                        ex);
                }
            }).collect(Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));
    }

    /**
     * Returns the link table manager singleton.
     *
     * @return Returns a MCRLinkTableManager instance.
     */
    public static synchronized MCRLinkTableManager getInstance() {
        return SINGLETON_INSTANCE;
    }
    
    /**
     * The method add a reference link pair.
     *
     * @param from
     *            the source of the reference as MCRObjectID
     * @param to
     *            the target of the reference as MCRObjectID
     * @param type
     *            the type of the reference as String
     * @param attr
     *            the optional attribute of the reference as String
     */
    public void addReferenceLink(MCRObjectID from, MCRObjectID to, MCRLinkType type, String attr) {
        addReferenceLink(from.toString(), to.toString(), type, attr);
    }

    /**
     * The method add a reference link pair.
     *
     * @param from
     *            the source of the reference as String
     * @param to
     *            the target of the reference as String
     * @param type
     *            the type of the reference as String
     * @param attr
     *            the optional attribute of the reference as String
     */
    public void addReferenceLink(String from, String to, MCRLinkType type, String attr) {
        String fromTrimmed = MCRUtils.filterTrimmedNotEmpty(from).orElse(null);
        if (fromTrimmed == null) {
            LOGGER.warn("The from value of a reference link is false, the link was not added to the link table");
            return;
        }

        String toTrimmed = MCRUtils.filterTrimmedNotEmpty(to).orElse(null);
        if (toTrimmed == null) {
            LOGGER.warn("The to value of a reference link is false, the link was not added to the link table");
            return;
        }


        String attrTrimmed = MCRUtils.filterTrimmedNotEmpty(attr).orElse("");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Link in table {} add for {}<-->{} with {} and {}",
                type.toString(), fromTrimmed, toTrimmed, type.toString(), attrTrimmed);
        }

        try {
            linkTableInstance.create(fromTrimmed, toTrimmed, type.toString(), attrTrimmed);
        } catch (Exception e) {
            LOGGER.warn("An error occured while adding a dataset from the reference link table, adding not succesful.",
                e);
        }
    }

    /**
     * The method delete a reference link.
     *
     * @param from
     *            the source of the reference as MCRObjectID
     */
    public void deleteReferenceLink(MCRObjectID from) {
        deleteReferenceLink(from.toString());
    }

    /**
     * The method delete a reference link.
     *
     * @param from
     *            the source of the reference as String
     */
    public void deleteReferenceLink(String from) {
        String fromTrimmed = MCRUtils.filterTrimmedNotEmpty(from).orElse(null);
        if (fromTrimmed == null) {
            LOGGER
                .warn("The from value of a reference link is false, the link was " + "not deleted from the link table");
            return;
        }

        try {
            linkTableInstance.delete(fromTrimmed, null, null);
        } catch (Exception e) {
            LOGGER.warn(() -> "An error occured while deleting a dataset from the" + fromTrimmed
                + " reference link table, deleting could be not succesful.", e);
        }
    }

    /**
     * The method delete a reference link pair for the given type to the store.
     *
     * @param from
     *            the source of the reference as String
     * @param to
     *            the target of the reference as String
     * @param type
     *            the type of the reference as String
     */
    public void deleteReferenceLink(String from, String to, String type) {
        String fromTrimmed = MCRUtils.filterTrimmedNotEmpty(from).orElse(null);
        if (fromTrimmed == null) {
            LOGGER
                .warn("The from value of a reference link is false, the link was " + "not deleted from the link table");
            return;
        }
        try {
            linkTableInstance.delete(fromTrimmed, to, type);
        } catch (Exception e) {
            LOGGER.warn("An error occured while deleting a dataset from the"
                + " reference link table, deleting is not succesful.", e);
        }
    }

    /**
     * The method count the reference links for a given target MCRobjectID.
     *
     * @param to
     *            the object ID as MCRObjectID, they was referenced
     * @return the number of references
     */
    public int countReferenceLinkTo(MCRObjectID to) {
        return countReferenceLinkTo(to.toString());
    }

    /**
     * The method count the reference links for a given target object ID.
     *
     * @param to
     *            the object ID as String, they was referenced
     * @return the number of references
     */
    public int countReferenceLinkTo(String to) {
        String toTrimmed = MCRUtils.filterTrimmedNotEmpty(to).orElse(null);
        if (toTrimmed == null) {
            LOGGER.warn("The to value of a reference link is false, the link was " + "not added to the link table");

            return 0;
        }

        try {
            return linkTableInstance.countTo(null, toTrimmed, null, null);
        } catch (Exception e) {
            LOGGER.warn(() -> "An error occured while searching for references of " + toTrimmed + ".", e);
        }

        return 0;
    }

    /**
     * counts the reference links for a given to object ID.
     *
     * @param types
     *            Array of document type slected by the mcrfrom content
     * @param restriction
     *            a first part of the to ID as String, it can be null
     * @return the number of references
     */
    public int countReferenceLinkTo(String to, String[] types, String restriction) {
        Optional<String> myTo = MCRUtils.filterTrimmedNotEmpty(to);
        if (!myTo.isPresent()) {
            LOGGER.warn("The to value of a reference link is false, the link was " + "not added to the link table");
            return 0;
        }

        try {
            if (types != null && types.length > 0) {
                return Stream.of(types).mapToInt(type -> linkTableInstance.countTo(null, myTo.get(), type, restriction))
                    .sum();
            }
            return linkTableInstance.countTo(null, myTo.get(), null, restriction);
        } catch (Exception e) {
            LOGGER.warn(() -> "An error occured while searching for references of " + to + ".", e);
            return 0;
        }
    }

    /**
     * The method count the number of references to a category of a
     * classification without sub ID's and returns it as a Map
     *
     * @param classid
     *            the classification ID as MCRObjectID
     *
     * @return a Map with key=categID and value=counted number of references
     */
    public Map<String, Number> countReferenceCategory(String classid) {
        return linkTableInstance.getCountedMapOfMCRTO(classid);
    }

    /**
     * The method count the number of references to a category of a
     * classification.
     *
     * @param classid
     *            the classification ID as String
     * @param categid
     *            the category ID as String
     * @return the number of references
     */
    public int countReferenceCategory(String classid, String categid) {
        return countReferenceLinkTo(classid + "##" + categid, null, null);
    }

    /**
     * Returns a List of all link sources of <code>to</code>
     *
     * @param to
     *            The MCRObjectID to referenced.
     * @return List of Strings (Source-IDs)
     */
    public Collection<String> getSourceOf(MCRObjectID to) {
        return getSourceOf(to.toString());
    }

    /**
     * Returns a List of all link sources of <code>to</code>
     *
     * @param to
     *            The ID to referenced.
     * @return List of Strings (Source-IDs)
     */
    public Collection<String> getSourceOf(String to) {
        if (to == null || to.length() == 0) {
            LOGGER.warn("The to value of a reference link is false, the link was not found in the link table");
            return Collections.emptyList();
        }

        try {
            return linkTableInstance.getSourcesOf(to, null);
        } catch (Exception e) {
            LOGGER.warn(() -> "An error occured while searching for references to " + to + ".", e);
            return Collections.emptyList();
        }
    }

    /**
     * Returns a List of all link sources of <code>to</code> and a special
     * <code>type</code>
     *
     * @param to
     *            Destination-ID
     * @param type
     *            link reference type
     * @return List of Strings (Source-IDs)
     */
    public Collection<String> getSourceOf(MCRObjectID to, MCRLinkType type) {
        return getSourceOf(to.toString(), type);
    }

    /**
     * Returns a List of all link sources of <code>to</code> and a special
     * <code>type</code>
     *
     * @param to
     *            Destination-ID
     * @param type
     *            link reference type
     * @return List of Strings (Source-IDs)
     */
    public Collection<String> getSourceOf(String to, MCRLinkType type) {
        if (to == null || to.length() == 0) {
            LOGGER.warn("The to value of a reference link is false, the link was not found in the link table");
            return Collections.emptyList();
        }

        try {
            return linkTableInstance.getSourcesOf(to, type.toString());
        } catch (Exception e) {
            LOGGER.warn(() -> "An error occured while searching for references to " + to + " with " + type + ".", e);
            return Collections.emptyList();
        }
    }

    /**
     * The method return a list of all source ID's of the refernce target to
     * with the given type.
     *
     * @param to
     *            the refernce target to
     * @param type
     *            type of the refernce
     * @return a list of ID's
     */
    public Collection<String> getSourceOf(String[] to, String type) {
        if (to == null || to.length == 0) {
            LOGGER.warn("The to value of a reference link is false, the link was not found in the link table");
            return Collections.emptyList();
        }
        Collection<String> ll = new ArrayList<>();
        try {
            for (String singleTo : to) {
                ll.addAll(linkTableInstance.getSourcesOf(singleTo, type));
            }
            return ll;
        } catch (Exception e) {
            LOGGER.warn(() -> "An error occured while searching for references to " + Arrays.toString(to) + ".", e);
            return ll;
        }
    }

    /**
     * Returns a List of all link destinations of <code>from</code>
     *
     * @param from
     *            Source-ID
     * @return List of Strings (Destination-IDs)
     */
    public Collection<String> getDestinationOf(MCRObjectID from) {
        return getDestinationOf(from.toString(), null);
    }

    /**
     * Returns a List of all link destinations of <code>from</code>
     *
     * @param from
     *            Source-ID
     * @return List of Strings (Destination-IDs)
     */
    public Collection<String> getDestinationOf(String from) {
        return getDestinationOf(from, null);
    }

    /**
     * Returns a List of all link destinations of <code>from</code> and a
     * special <code>type</code>
     *
     * @param from
     *            Source-ID
     * @param type
     *            link reference type
     * @return List of Strings (Destination-IDs)
     */
    public Collection<String> getDestinationOf(MCRObjectID from, MCRLinkType type) {
        return getDestinationOf(from.toString(), type);
    }

    /**
     * Returns a List of all link destination of <code>from</code> and a
     * special <code>type</code>
     *
     * @param from
     *            Source-ID
     * @param type
     *            Link reference type, this can be null. Current types are
     *            classid, child, parent, reference and derivate.
     * @return List of Strings (Destination-IDs)
     */
    public Collection<String> getDestinationOf(String from, MCRLinkType type) {
        if (from == null || from.length() == 0) {
            LOGGER.warn("The from value of a reference link is false, the link was not found in the link table");
            return Collections.emptyList();
        }

        try {
            return linkTableInstance.getDestinationsOf(from, type != null ? type.toString() : null);
        } catch (Exception e) {
            LOGGER.warn(() -> "An error occured while searching for references from " + from + ".", e);
            return Collections.emptyList();
        }
    }


    /**
     * Removes all references of this object.
     *
     * @param id the object where all references should be removed
     */
    public void delete(MCRObjectID id) {
        deleteReferenceLink(id);
        MCRCategLinkReference reference = new MCRCategLinkReference(id);
        MCRCategLinkServiceFactory.obtainInstance().deleteLink(reference);
    }

    /**
     * Updates all references of this object. Old ones will be removed and new links will be created.
     *
     * @param id the mycore object identifier
     */
    public void update(MCRObjectID id) {
        delete(id);
        create(MCRMetadataManager.retrieve(id));
    }



    /**
     * Creates all references for the given object. You should call {@link #delete(MCRObjectID)} before using this
     * @param obj the object to create the references
     */
    public void update(MCRBase obj) {
        delete(obj.getId());
        create(obj);
    }

    public void create(MCRBase obj) {
        Collection<MCRCategoryID> categoryList = getCategories(obj);

        MCRCategLinkReference objectReference = new MCRCategLinkReference(obj.getId());
        MCRCategLinkServiceFactory.obtainInstance().setLinks(objectReference, categoryList);

        Collection<MCRLinkReference> links = getLinks(obj);
        links.forEach(link -> {
            addReferenceLink(link.from, link.to, link.type, link.attr);
        });
    }

    public Collection<MCRCategoryID> getCategories(MCRBase obj) {
        MCRBaseLinkProvider blp = getLinkProvider(obj);
        try {
            return blp.getCategories(obj);
        } catch (OperationNotSupportedException e) {
            throwMCRConfigurationException(obj.getId(), e, blp);
        }
        return List.of();
    }

    public Collection<MCRLinkReference> getLinks(MCRBase obj) {
        MCRBaseLinkProvider blp = getLinkProvider(obj);
        try {
            return blp.getLinks(obj);
        } catch (OperationNotSupportedException e) {
            throwMCRConfigurationException(obj.getId(), e, blp);
        }
        return List.of();
    }

    private static void throwMCRConfigurationException(MCRObjectID id,
        OperationNotSupportedException cause,
        MCRBaseLinkProvider blp) {
        throw new MCRConfigurationException(
            "The object " + id + " is not supported by the link provider " + blp.toString(), cause);
    }

    private MCRBaseLinkProvider getLinkProvider(MCRBase der) {
        String type = der.getId().getTypeId();
        return linkProviders.containsKey(type) ? linkProviders.get(type) : linkProviders.get("Default");
    }

    public record MCRLinkReference(MCRObjectID from, MCRObjectID to, MCRLinkType type, String attr) {
    }
}
