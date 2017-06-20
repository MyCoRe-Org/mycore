package org.mycore.impex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.classifications2.utils.MCRClassificationUtils;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.xml.sax.SAXParseException;

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
     * Imports a *.tar transport package from the given path.
     * 
     * @param pathToTar
     *                path to the *.tar archive
     * @throws IOException
     *                some file system stuff went wrong
     * @throws MCRActiveLinkException
     *                if object is created (no real update), see {@link MCRMetadataManager#create(MCRObject)}
     * @throws MCRAccessException 
     *                if write permission is missing or see {@link MCRMetadataManager#create(MCRObject)}
     * @throws JDOMException
     *                some jdom parsing went wrong
     * @throws URISyntaxException 
     * @throws SAXParseException 
     * @throws MCRException 
     */
    public static void importTar(Path pathToTar) throws IOException, MCRActiveLinkException, MCRAccessException,
        JDOMException, MCRException, SAXParseException, URISyntaxException {
        if (!Files.exists(pathToTar)) {
            throw new FileNotFoundException(pathToTar.toAbsolutePath().toString() + " does not exist.");
        }
        Path targetDirectory = getTargetDirectory(pathToTar);
        MCRUtils.untar(pathToTar, targetDirectory);

        // import the data from the extracted tar
        MCRTransferPackageUtil.importFromDirectory(targetDirectory);

        // delete the extracted files, but keep the tar
        LOGGER.info("Deleting expanded tar in " + targetDirectory);
        Files.walkFileTree(targetDirectory, MCRRecursiveDeleter.instance());
    }

    /**
     * Returns the path where the *.tar will be unzipped.
     * 
     * @param pathToTar the path to the tar.
     * @return path to the directory
     */
    public static Path getTargetDirectory(Path pathToTar) {
        String fileName = pathToTar.getFileName().toString();
        return pathToTar.getParent().resolve(fileName.substring(0, fileName.lastIndexOf(".")));
    }

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
     *                if object is created (no real update), see {@link MCRMetadataManager#create(MCRObject)}
     * @throws MCRAccessException 
     *                if write permission is missing or see {@link MCRMetadataManager#create(MCRObject)}
     * @throws URISyntaxException 
     * @throws SAXParseException 
     * @throws MCRException 
     */
    public static void importFromDirectory(Path targetDirectory) throws JDOMException, IOException,
        MCRActiveLinkException, MCRAccessException, MCRException, SAXParseException, URISyntaxException {
        // import classifications
        for (Path pathToClassification : getClassifications(targetDirectory)) {
            MCRClassificationUtils.fromPath(pathToClassification);
        }

        // import objects & derivates
        List<String> objectImportList = getMCRObjects(targetDirectory);
        for (String id : objectImportList) {
            importObject(targetDirectory, id);
        }
    }

    /**
     * Returns a list containing all paths to the classifications which should be imported.
     * 
     * @param targetDirectory
     *                the directory where the *.tar was unpacked
     * @return list of paths
     * @throws IOException
     *                if an I/O error is thrown when accessing one of the files
     */
    public static List<Path> getClassifications(Path targetDirectory) throws IOException {
        List<Path> classificationPaths = new ArrayList<>();
        Path classPath = targetDirectory.resolve(MCRTransferPackage.CLASS_PATH);
        if (Files.exists(classPath)) {
            try (Stream<Path> stream = Files.find(classPath, 2, filterClassifications())) {
                stream.forEach(classificationPaths::add);
            }
        }
        return classificationPaths;
    }

    /**
     * Imports a object from the targetDirectory path.
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
     *                if object is created (no real update), see {@link MCRMetadataManager#create(MCRObject)}
     * @throws MCRAccessException 
     *                if write permission is missing or see {@link MCRMetadataManager#create(MCRObject)}
     */
    public static void importObject(Path targetDirectory, String objectId)
        throws JDOMException, IOException, MCRActiveLinkException, MCRAccessException {
        // import object
        List<String> derivates = importObjectCLI(targetDirectory, objectId);
        // process the saved derivates
        for (String derivateId : derivates) {
            importDerivate(targetDirectory, derivateId);
        }
    }

    /**
     * Same as {@link #importObject(Path, String)} but returns a list of derivates which
     * should be imported afterwards.
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
     *                if object is created (no real update), see {@link MCRMetadataManager#create(MCRObject)}
     * @throws MCRAccessException 
     *                if write permission is missing or see {@link MCRMetadataManager#create(MCRObject)}
     */
    public static List<String> importObjectCLI(Path targetDirectory, String objectId)
        throws JDOMException, IOException, MCRActiveLinkException, MCRAccessException {
        SAXBuilder sax = new SAXBuilder();
        Path targetXML = targetDirectory.resolve(CONTENT_DIRECTORY).resolve(objectId + ".xml");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format("Importing {0}", targetXML.toAbsolutePath().toString()));
        }
        Document objXML = sax.build(targetXML.toFile());
        MCRObject mcr = new MCRObject(objXML);
        mcr.setImportMode(true);

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
        // return list of derivates
        return derivates;
    }

    /**
     * Imports a derivate from the given target directory path.
     * 
     * @param targetDirectory path to the extracted *.tar archive
     * @param derivateId the derivate to import
     * @throws JDOMException derivate xml couldn't be read
     * @throws IOException some file system stuff went wrong
     * @throws MCRAccessException you do not have the permissions to do the import
     */
    public static void importDerivate(Path targetDirectory, String derivateId)
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
                String targetPath = derivateDirectory.relativize(path).toString();
                try (InputStream in = Files.newInputStream(path)) {
                    Files.copy(in, MCRPath.getPath(derivateId, targetPath), StandardCopyOption.REPLACE_EXISTING);
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
            if (Files.isDirectory(path)) {
                return false;
            }
            if (path.toString().endsWith(".md5")) {
                return false;
            }
            return true;
        };
    }

    private static BiPredicate<Path, BasicFileAttributes> filterClassifications() {
        return (path, attr) -> {
            if (Files.isDirectory(path)) {
                return false;
            }
            return path.toString().endsWith(".xml");
        };
    }

    /**
     * Gets the list of mycore object identifiers from the given directory.
     * 
     * @param targetDirectory directory where the *.tar was unpacked
     * @return list of object which lies within the directory
     */
    public static List<String> getMCRObjects(Path targetDirectory) throws JDOMException, IOException {
        Path order = targetDirectory.resolve(MCRTransferPackage.IMPORT_CONFIG_FILENAME);
        Document xml = new SAXBuilder().build(order.toFile());
        Element config = xml.getRootElement();
        XPathExpression<Text> exp = MCRConstants.XPATH_FACTORY.compile("order/object/text()", Filters.text());
        return exp.evaluate(config).stream().map(Text::getText).collect(Collectors.toList());
    }

}
