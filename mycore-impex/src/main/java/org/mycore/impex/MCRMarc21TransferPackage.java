package org.mycore.impex;

import java.io.IOException;
import java.util.Map;

import org.jdom2.Document;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Same as {@link MCRTransferPackage} but contains a marc21 document additionally.
 * 
 * @see MCRTransferPackage
 * 
 * @author Silvio Hermann
 * @author Matthias Eichner
 */
public class MCRMarc21TransferPackage extends MCRTransferPackage {

    public static final String METADATA_SET_FILE_NAME = "catalogue_md.xml";

    private Document marc21;

    public MCRMarc21TransferPackage(Document marc21, MCRObjectID sourceId) {
        super(sourceId);
        this.marc21 = marc21;
    }

    /**
     * @return the underlying marc21 document
     */
    public Document getDocument() {
        return this.marc21;
    }

    /**
     * @return the filename for the metadata, currently fixed to {@link TransferPackage#METADATA_SET_FILE_NAME}
     */
    public String getMetadataSetFileName() {
        return MCRMarc21TransferPackage.METADATA_SET_FILE_NAME;
    }

    @Override
    public Map<String, MCRContent> getContent() throws IOException {
        Map<String, MCRContent> content = super.getContent();
        if (this.marc21 != null) {
            content.put(getMetadataSetFileName(), new MCRJDOMContent(this.marc21));
        }
        return content;
    }

}
