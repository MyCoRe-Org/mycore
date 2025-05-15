package org.mycore.datamodel.metadata;

import java.io.IOException;
import java.net.URI;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;


/**
 * This class extends the MCRObject class and contains additional metadata and structure information which is duplicated
 * from other objects or resources.
 * @see MCRObject
 */
public class MCRExpandedObject extends MCRObject {

    public MCRExpandedObject() throws MCRException {
        super(new MCRExpandedObjectStructure(), new MCRObjectMetadata(), new MCRObjectService(), "");
    }

    public MCRExpandedObject(Document objXML) throws MCRException {
        this();
        setFromJDOM(objXML);
    }
    
    public MCRExpandedObject(URI uri) throws IOException, JDOMException {
        this();
        setFromURI(uri);
    }

    public MCRExpandedObject(MCRExpandedObjectStructure structure, MCRObjectMetadata metadata, MCRObjectService service,
                             String label) {
        super(structure, metadata, service, label);
    }

    @Override
    public MCRExpandedObjectStructure getStructure() {
        return (MCRExpandedObjectStructure) super.getStructure();
    }
}
