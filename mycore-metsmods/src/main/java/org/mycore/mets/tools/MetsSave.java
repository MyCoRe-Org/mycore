package org.mycore.mets.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRDerivate;

/**
 * @author shermann
 */
public class MetsSave {

    private final static Logger LOGGER = Logger.getLogger(MetsSave.class);

    /**
     * Saves the content of the given document to file and then adds the file to
     * the derivate with the given id. The name of the file depends on property
     * 'MCR.Mets.Filename'. If this property has not been set 'mets.xml' is used
     * as a default filename.
     * 
     * @param document
     * @param derivateId
     */
    public static void saveMets(Document document, String derivateId) {
        File tmp = null;
        /* save temporary file */
        try {
            tmp = save(document);
        } catch (Exception ex) {
            LOGGER.error("Could not save file on disk.", ex);
            return;
        }
        // add the file to the existing derivate in ifs
        String fileName = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");
        LOGGER.info("Storing file content from \"" + tmp.getName() + "\" to derivate \"" + derivateId + "\"");
        MCRFile uploadFile = new MCRFile(fileName, new MCRDerivate().receiveDirectoryFromIFS(derivateId));
        uploadFile.setContentFrom(tmp);

        // delete temporary file
        if (!tmp.delete()) {
            LOGGER.warn("Temporary file \"" + tmp.getName() + "\"could not be deleted ");
        }
    }

    /**
     * Saves the given document to disk, pattern is as follows <br/>
     * <br/>
     * <code>File.createTempFile("mets", ".xml");</code>
     * 
     * @return the reference to the mets file
     */
    public static File save(Document mets) throws Exception {
        File file = File.createTempFile(UUID.randomUUID().toString(), ".xml");
        FileOutputStream stream = new FileOutputStream(file);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(mets, stream);
        stream.close();
        return file;
    }
}
