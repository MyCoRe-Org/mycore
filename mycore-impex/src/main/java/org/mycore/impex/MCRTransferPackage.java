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

package org.mycore.impex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRClassificationUtils;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectStructure;
import org.mycore.datamodel.metadata.MCRObjectUtils;

/**
 * Basic transfer package containing a {@link MCRObject}, all its descendants,
 * links, derivates (including their files) and referenced classifications.
 * <p>
 * To build a transfer package call {@link #build()}, this initializes
 * all required objects and checks if they are valid. Call {@link #getContent()}
 * to retrieve the content afterwards.
 * </p>
 * 
 * @author Silvio Hermann
 * @author Matthias Eichner
 */
public class MCRTransferPackage {

    public static final String IMPORT_CONFIG_FILENAME = "import.xml";

    public static final String CONTENT_PATH = "content/";

    public static final String CLASS_PATH = CONTENT_PATH + "classifications/";

    protected MCRObject source;

    /**
     * Set of objects including the source, its descendants and all resolved links.
     * Its linked because the import order matters.
     */
    protected LinkedHashSet<MCRObject> objects;

    /**
     * List of transfer package file containers.
     */
    protected List<MCRTransferPackageFileContainer> fileContainers;

    /**
     * Set of classifications.
     */
    protected Set<String> classifications;

    public MCRTransferPackage(MCRObject source) {
        this.source = source;
    }

    /**
     * Builds the transfer package.
     * 
     * @throws MCRUsageException is thrown if some of the referenced objects or derivates couldn't be retrieved
     * @throws MCRAccessException if the Users doesn't have the rights to create this transfer package
     */
    public void build() throws MCRUsageException, MCRAccessException {
        LinkedHashMap<MCRObjectID, MCRObject> objectMap = new LinkedHashMap<>();
        Set<MCRCategoryID> categories = new HashSet<>();
        resolveChildrenAndLinks(source, objectMap, categories);

        this.objects = new LinkedHashSet<>(objectMap.values());
        this.classifications = categories.stream().map(MCRCategoryID::getRootID).distinct().collect(Collectors.toSet());
        this.fileContainers = buildFileContainers(source);
    }

    /**
     * Fills the given objectMap with all children and links of the object. The object
     * itself is also added.
     * 
     * @param object the source object
     * @param objectMap the map which will be created
     */
    protected void resolveChildrenAndLinks(MCRObject object, LinkedHashMap<MCRObjectID, MCRObject> objectMap,
        Set<MCRCategoryID> categories) {
        // add links
        for (MCRObject entityLink : MCRObjectUtils.getLinkedObjects(object)) {
            if (!objectMap.containsKey(entityLink.getId())) {
                objectMap.put(entityLink.getId(), entityLink);
            }
        }
        // add classifications
        categories.addAll(MCRObjectUtils.getCategories(object));

        // add the object to the objectMap
        objectMap.put(object.getId(), object);

        // resolve children
        for (MCRMetaLinkID metaLinkId : object.getStructure().getChildren()) {
            MCRObjectID childId = MCRObjectID.getInstance(metaLinkId.toString());
            if (!MCRMetadataManager.exists(childId)) {
                throw new MCRUsageException(
                    "Requested object '" + childId + "' does not exist. Thus a transfer package cannot be created.");
            }
            MCRObject child = MCRMetadataManager.retrieveMCRObject(childId);
            resolveChildrenAndLinks(child, objectMap, categories);
        }
    }

    /**
     * Builds a list of {@link MCRTransferPackageFileContainer} for all derivate's of the
     * given object and all its descendants.
     * 
     * <p>TODO: derivates of linked objects are not added</p>
     * 
     * @param object the object
     * @return list of transfer packages file container
     */
    protected List<MCRTransferPackageFileContainer> buildFileContainers(MCRObject object) {
        List<MCRTransferPackageFileContainer> fileContainerList = new ArrayList<>();
        MCRObjectUtils.getDescendantsAndSelf(object)
            .stream()
            .map(MCRObject::getStructure)
            .map(MCRObjectStructure::getDerivates)
            .flatMap(Collection::stream)
            .map(MCRMetaLinkID::getXLinkHrefID)
            .peek(derivateId -> {
                if (!MCRMetadataManager.exists(derivateId)) {
                    throw new MCRUsageException("Requested derivate '" + derivateId
                        + "' does not exist. Thus a transfer package cannot be created.");
                }
            })
            .map(MCRTransferPackageFileContainer::new)
            .forEach(fileContainerList::add);
        return fileContainerList;
    }

    /**
     * Generates an xml file, which contains import configuration.
     * 
     * @return import configuration document    
     */
    public Document buildImportConfiguration() {
        Element configElement = new Element("config");
        Element orderElement = new Element("order");
        for (MCRObject object : objects) {
            Element e = new Element("object");
            e.setText(object.toString());
            orderElement.addContent(e);
        }
        configElement.addContent(orderElement);
        return new Document(configElement);
    }

    /**
     * Returns the content for this transfer package. You have to call {@link #build()}
     * before you can retrieve this data.
     * 
     * @return a map where key = filename; value = MCRContent
     */
    public Map<String, MCRContent> getContent() throws IOException {
        Map<String, MCRContent> content = new HashMap<>();
        // config
        content.put(IMPORT_CONFIG_FILENAME, new MCRJDOMContent(buildImportConfiguration()));

        // objects
        for (MCRObject object : this.objects) {
            String fileName = CONTENT_PATH + object.getId() + ".xml";
            Document xml = object.createXML();
            content.put(fileName, new MCRJDOMContent(xml));
        }

        // file containers
        for (MCRTransferPackageFileContainer fc : this.fileContainers) {
            // derivate
            MCRObjectID derivateId = fc.getDerivateId();
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateId);
            Document derivateXML = derivate.createXML();
            String folder = CONTENT_PATH + fc.getName();
            String derivateFileName = folder + "/" + fc.getName() + ".xml";
            content.put(derivateFileName, new MCRJDOMContent(derivateXML));

            // files of derivate
            for (MCRFile file : fc.getFiles()) {
                content.put(folder + file.getAbsolutePath(), file.getContent());
            }
        }

        // classifications
        for (String classId : this.classifications) {
            Document classification = MCRClassificationUtils.asDocument(classId);
            String path = CLASS_PATH + classId + ".xml";
            content.put(path, new MCRJDOMContent(classification));
        }
        return content;
    }

    /**
     * Returns the source of this transfer package.
     * 
     * @return the source
     */
    public MCRObject getSource() {
        return source;
    }

    @Override
    public String toString() {
        return this.source.getId().toString();
    }

}
