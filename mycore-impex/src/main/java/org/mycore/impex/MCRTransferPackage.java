package org.mycore.impex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
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
 * links and derivates (including their files).
 * 
 * @author Silvio Hermann
 * @author Matthias Eichner
 */
public class MCRTransferPackage {

    public static final String INSERT_ORDER_XML_FILENAME = "InsertOrder.xml";

    public static final String CONTENT_PATH = "content/";

    protected MCRObject source;

    /**
     * List of objects including the source, its descendants and all resolved links.
     */
    protected LinkedHashSet<MCRObject> objects;

    /**
     * List of transfer package file containers.
     */
    protected List<MCRTransferPackageFileContainer> fileContainers;

    /**
     * Creates a new transfer package for the given sourceId.
     * 
     * @param sourceId a mycore object identifier
     */
    public MCRTransferPackage(MCRObjectID sourceId) {
        if (sourceId == null) {
            throw new IllegalArgumentException("One does not simply provide 'null' as parameter.");
        }
        if (!MCRMetadataManager.exists(sourceId)) {
            throw new IllegalArgumentException(
                "Requested object '" + sourceId + "' does not exist. Thus a transfer package cannot be created.");
        }
        this.source = MCRMetadataManager.retrieveMCRObject(sourceId);
    }

    /**
     * Builds the transfer package.
     * 
     * @throws MCRException is thrown if some of the referenced objects or derivates couldn't be retrieved
     */
    public void build() throws MCRException {
        this.objects = buildObjects(this.source);
        this.fileContainers = buildFileContainers(this.source);
    }

    /**
     * Creates and returns a set of {@link MCRObject}s referenced by the object given by {@link MCRTransferPackage#getObjectID()} 
     * explicitly and implicitly (by its children). Derivate's are handled different (they are treated in 
     * {@link MCRTransferPackage#createFileContainers()}).
     *
     * In fact all links of type {@link MCRMetaLinkID} will be resolved and the corresponding objects will be added to the list.  
     * 
     * @return a list of objects referenced by the object given by {@link MCRTransferPackage#getObjectID()}
     */
    protected LinkedHashSet<MCRObject> buildObjects(MCRObject object) {
        LinkedHashMap<MCRObjectID, MCRObject> objectMap = new LinkedHashMap<MCRObjectID, MCRObject>();
        resolveChildrenAndLinks(object, objectMap);
        return new LinkedHashSet<>(objectMap.values());
    }

    /**
     * Fills the given objectMap with all children and links of the object. The object
     * itself is also added.
     * 
     * @param object the source object
     * @param objectMap the map which will be created
     */
    protected void resolveChildrenAndLinks(MCRObject object, LinkedHashMap<MCRObjectID, MCRObject> objectMap) {
        // add links
        for (MCRObject entityLink : MCRObjectUtils.getLinkedObjects(this.source)) {
            if (!objectMap.containsKey(entityLink.getId())) {
                objectMap.put(entityLink.getId(), entityLink);
            }
        }
        // add the object to the objectMap
        objectMap.put(object.getId(), object);

        // resolve children
        for (MCRMetaLinkID metaLinkId : object.getStructure().getChildren()) {
            MCRObjectID childId = MCRObjectID.getInstance(metaLinkId.toString());
            if (!MCRMetadataManager.exists(childId)) {
                throw new MCRException(
                    "Requested object '" + childId + "' does not exist. Thus a transfer package cannot be created.");
            }
            MCRObject child = MCRMetadataManager.retrieveMCRObject(childId);
            resolveChildrenAndLinks(child, objectMap);
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
        List<MCRTransferPackageFileContainer> fileContainerList = new ArrayList<MCRTransferPackageFileContainer>();
        MCRObjectUtils.getDescendantsAndSelf(object)
                      .stream()
                      .map(MCRObject::getStructure)
                      .map(MCRObjectStructure::getDerivates)
                      .flatMap(Collection::stream)
                      .map(MCRMetaLinkID::getXLinkHrefID)
                      .peek(derivateId -> {
                          if (!MCRMetadataManager.exists(derivateId)) {
                              throw new MCRException("Requested derivate '" + derivateId
                                  + "' does not exist. Thus a transfer package cannot be created.");
                          }
                      })
                      .map(MCRTransferPackageFileContainer::new)
                      .forEach(fileContainerList::add);
        return fileContainerList;
    }

    /**
     * Generates an xml file, which contains the insert order for the MCRObject you will get by
     * invoking {@link TransferPackage#getObjectHierarchy()}.
     * 
     * @return the objectInsertOrder or null
     */
    public Document getObjectInsertOrderDocument() {
        if (this.objects == null) {
            return null;
        }
        Element importOrder = new Element("ImportOrder");
        int order = 0;
        for (MCRObject object : objects) {
            Element e = new Element("Order");
            e.setText(object.toString());
            e.setAttribute("value", String.valueOf(order++));
            importOrder.addContent(e);
        }
        return new Document(importOrder);
    }

    /**
     * Returns the content for this transfer package. You have to call {@link #build()}
     * before you can retrieve this data.
     * 
     * @return a map where key = filename; value = MCRContent
     */
    public Map<String, MCRContent> getContent() throws IOException {
        Map<String, MCRContent> content = new HashMap<>();
        // objects
        for (MCRObject object : this.objects) {
            String fileName = CONTENT_PATH + object.getId().toString() + ".xml";
            Document xml = object.createXML();
            content.put(fileName, new MCRJDOMContent(xml));
        }
        String insertOrderFileName = CONTENT_PATH + INSERT_ORDER_XML_FILENAME;
        content.put(insertOrderFileName, new MCRJDOMContent(getObjectInsertOrderDocument()));

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
