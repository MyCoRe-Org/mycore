package org.mycore.impex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFileImportExport;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Contains utility methods for handling transfer packages.
 * 
 * @author Silvio Hermann
 * @author Matthias Eichner
 */
public abstract class MCRTransferPackageUtil {

    private static final Logger LOGGER = LogManager.getLogger(MCRTransferPackageUtil.class);

    /**
     * The default lookup path appendix (the subdirectory in the tar where the metadata resides).
     * With leading and trailing {@link File#separator} 
     */
    public static final String CONTENT_DIRECTORY = "content";

    /**
     * Imports from an unpacked *.tar archive directory.
     * 
     * @param targetDirectory
     *                the directory where the *.tar was unpacked
     * @throws JDOMException
     *                some jdom parsing went wrong
     * @throws IOException
     *                something went wrong while reading from the file system
     * @throws MCRActiveLinkException
     *                if object is created (no real update), see {@link #create(MCRObject)}
     * @throws MCRAccessException 
     *                if write permission is missing or see {@link #create(MCRObject)}
     */
    public static void importFromDirectory(Path targetDirectory)
        throws JDOMException, IOException, MCRActiveLinkException, MCRAccessException {
        List<String> toImport = getMCRObjects(targetDirectory);
        for (String id : toImport) {
            importObject(targetDirectory, id);
        }
    }

    /**
     * Imports a object from the targetDirectory path. Be aware that the object is imported
     * with importMode = true. This means that the {@link MCREventManager#
     * 
     * @param targetDirectory
     *                the directory where the *.tar was unpacked
     * @param objectId
     *                object id to import
     * @throws JDOMException
     *                coulnd't parse the import order xml
     * @throws IOException
     *                when an I/O error prevents a document from being fully parsed
     * @throws MCRActiveLinkException
     *                if object is created (no real update), see {@link #create(MCRObject)}
     * @throws MCRAccessException 
     *                if write permission is missing or see {@link #create(MCRObject)}
     */
    public static void importObject(Path targetDirectory, String objectId)
        throws JDOMException, IOException, MCRActiveLinkException, MCRAccessException {
        SAXBuilder sax = new SAXBuilder();
        Path targetXML = targetDirectory.resolve(CONTENT_DIRECTORY).resolve(objectId + ".xml");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format("Importing {0}", targetXML.toAbsolutePath().toString()));
        }
        Document objXML = sax.build(targetXML.toFile());
        MCRObject mcr = new MCRObject(objXML);

        List<String> derivates = new LinkedList<String>();
        // one must copy the ids before updating the mcr objects
        for (MCRMetaLinkID id : mcr.getStructure().getDerivates()) {
            derivates.add(id.getXLinkHref());
        }
        // delete children & derivate -> will be added later
        mcr.getStructure().clearChildren();
        mcr.getStructure().clearDerivates();
        // update
        MCRMetadataManager.update(mcr);

        // process the saved derivates
        for (String derivateId : derivates) {
            importDerivate(targetDirectory, derivateId);
        }
    }

    private static void importDerivate(Path targetDirectory, String derivateId)
        throws JDOMException, IOException, MCRAccessException {
        SAXBuilder sax = new SAXBuilder();

        Path derivateDirectory = targetDirectory.resolve(CONTENT_DIRECTORY).resolve(derivateId);
        Path derivatePath = derivateDirectory.resolve(derivateId + ".xml");

        LOGGER.info(MessageFormat.format("Importing {0}", derivatePath.toAbsolutePath().toString()));
        MCRDerivate der = new MCRDerivate(sax.build(derivatePath.toFile()));
        MCRMetadataManager.update(der);

        MCRDirectory dir = MCRDirectory.getRootDirectory(der.getId().toString());
        if (dir == null) {
            LOGGER.info("Creating missing " + MCRFilesystemNode.class.getSimpleName() + " " + der.getId().toString());
            final MCRDirectory difs = new MCRDirectory(der.getId().toString());
            der.getDerivate().getInternals().setIFSID(difs.getID());
            MCRMetadataManager.update(der);
        }
        try (Stream<Path> stream = Files.find(derivateDirectory, 5,
            filterDerivateDirectory(derivateId, derivateDirectory))) {
            stream.forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        MCRPath mcrDirectory = MCRPath.getPath(derivateId,
                            derivateDirectory.relativize(path).toString());
                        Files.createDirectory(mcrDirectory);
                    } else {
                        MCRFileImportExport.addFiles(path.toFile(), derivateId.toString());
                    }
                } catch (IOException ioExc) {
                    throw new MCRException(
                        "Unable to add file " + path.toAbsolutePath().toString() + " to derivate " + derivateId, ioExc);
                }
            });
        }
    }

    private static BiPredicate<Path, BasicFileAttributes> filterDerivateDirectory(String derivateId,
        Path derivateDirectory) {
        return (path, attr) -> {
            if (derivateDirectory.equals(path)) {
                return false;
            }
            if (Files.isDirectory(path)) {
                return true;
            }
            if (path.endsWith(".md5")) {
                return false;
            }
            if (path.getFileName().toString().equals(derivateId + ".xml")) {
                return false;
            }
            return true;
        };
    }

    /**
     * Gets the list of mycore object identifiers from the given directory.
     * 
     * @param targetDirectory directory where the *.tar was upacked
     * @return list of object which lies within the directory
     */
    private static List<String> getMCRObjects(Path targetDirectory) throws JDOMException, IOException {
        Path order = targetDirectory.resolve(CONTENT_DIRECTORY).resolve(MCRTransferPackage.INSERT_ORDER_XML_FILENAME);
        Document xml = new SAXBuilder().build(order.toFile());
        List<String> identifiers = new ArrayList<>();
        XPathExpression<Text> xpath = MCRConstants.XPATH_FACTORY.compile("./ImportOrder/Order/text()", Filters.text());
        for (Text v : xpath.evaluate(xml)) {
            identifiers.add(v.getText());
        }
        return identifiers;
    }

}
