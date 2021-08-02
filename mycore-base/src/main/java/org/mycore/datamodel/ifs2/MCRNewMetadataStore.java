package org.mycore.datamodel.ifs2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * New abstract store class.
 * 
 * @author Christoph Neidahl (OPNA2608)
 *
 */
public abstract class MCRNewMetadataStore extends MCRStore {

    private static final Logger LOGGER = LogManager.getLogger();

    protected String configPrefix;

    protected boolean forceXML;

    protected String forceDocType;

    protected String suffix;

    protected Function<String, String> toNativePath;

    protected boolean hasBaseDirectory = true;

    protected boolean hasWorkDirectory = false;

    protected boolean hasSlotLayout = true;

    protected boolean canVerify = false;

    protected Path workDirectory;

    protected static boolean validateSlotLayout(String layout) {
        // Only digits and - are valid characters
        if (!Pattern.matches("[\\d-]+", layout)) {
            LOGGER.error("Slot layout '{}' contains invalid characters", layout);
            return false;
        }

        final StringTokenizer st = new StringTokenizer(layout, "-");
        String layoutToken;
        int tokenInt;
        while (st.countTokens() > 1) {
            layoutToken = st.nextToken();
            // "--" or starting/ending with - makes no sense
            if (layoutToken.length() == 0) {
                LOGGER.error("Slot layout '{}' contains consecutive slot delimiters", layout);
                return false;
            }
            tokenInt = Integer.parseInt(layoutToken);
            // Slot length 0 makes no sense
            if (tokenInt == 0) {
                LOGGER.error("Slot layout '{}' contains slot of length 0", layout);
            }
        }
        LOGGER.info("Slot layout '{}' is valid", layout);
        return true;
    }

    protected static int[] readSlotLayout(String layout) throws MCRConfigurationException {
        if (!validateSlotLayout(layout)) {
            throw new MCRConfigurationException("Slot layout " + layout + " is invalid");
        }

        final StringTokenizer st = new StringTokenizer(layout, "-");
        int[] slots = new int[st.countTokens()];
        int i = 0;
        while (st.countTokens() > 0) {
            slots[i++] = Integer.parseInt(st.nextToken());
        }
        return slots;
    }

    protected static Path convertPath(String path) {
        Path parsedPath = null;
        try {
            URI uri = new URI(path);
            if (uri.getScheme() != null) {
                parsedPath = Paths.get(uri);
            }
        } catch (URISyntaxException e) {
            // not a uri, handle as relative path
        }
        if (parsedPath == null) {
            parsedPath = Paths.get(path);
        }
        return parsedPath;
    }

    protected static String normalizeSeparators(String path) {
        String separator = FileSystems.getDefault().getSeparator();
        Function<String, String> toNativePath;
        if (separator.equals("/")) {
            toNativePath = s -> s;
        } else {
            toNativePath = s -> s.replace("/", separator);
        }
        return toNativePath.apply(path);
    }

    protected static Path getDirectory(String path) {
        Path readPath = convertPath(normalizeSeparators(path));
        try {
            if (Files.exists(readPath)) {
                BasicFileAttributes attrs = Files.readAttributes(readPath, BasicFileAttributes.class);
                if (!attrs.isDirectory()) {
                    throw new MCRConfigurationException("Path '" + readPath.toString() + "' is not a directory");
                }

                if (!Files.isReadable(readPath)) {
                    throw new MCRConfigurationException("Directory '" + readPath.toString() + "' is not readable");
                }
            } else {
                Files.createDirectories(readPath);
            }
        } catch (IOException e) {
            throw new MCRConfigurationException("Failed to get directory", e);
        }
        return readPath;
    }

    protected MCRContent forceContent(MCRContent content) throws MCRUsageException {
        if (this.forceXML) {
            try {
                MCRJDOMContent forcedContent = new MCRJDOMContent(content.asXML());
                forcedContent.setDocType(forceDocType);
                return forcedContent;
            } catch (SAXException | JDOMException | IOException e) {
                throw new MCRUsageException("Failed to convert content to XML", e);
            }
        } else {
            return content;
        }
    }

    @Override
    protected void init(MCRStoreConfig storeConfig) {
        setStoreConfig(storeConfig);
        this.configPrefix = "MCR.IFS2.Store." + storeConfig.getID() + ".";

        Optional<Boolean> shouldForceXML = MCRConfiguration2.getBoolean(configPrefix + "ForceXML");
        if (shouldForceXML.isPresent()) {
            forceXML = shouldForceXML.get();
        } else {
            forceXML = false;
        }
        if (forceXML) {
            forceDocType = MCRConfiguration2.getStringOrThrow(configPrefix + "ForceDocType");
            suffix = ".xml";
        } else {
            forceDocType = null;
            suffix = null;
        }

        if (hasBaseDirectory) {
            baseDirectory = getDirectory(storeConfig.getBaseDir());
        } else {
            baseDirectory = null;
        }

        if (hasWorkDirectory) {
            workDirectory = getDirectory(MCRConfiguration2.getStringOrThrow(configPrefix + "WorkDir"));
        } else {
            workDirectory = null;
        }

        if (hasSlotLayout) {
            this.slotLength = readSlotLayout(storeConfig.getSlotLayout());
        } else {
            this.slotLength = null;
        }
    }
    
    @Override
    public abstract boolean isEmpty();

    public MCRNewMetadata create(MCRContent content) throws MCRPersistenceException, MCRUsageException {
        return create(getNextFreeID(), content);
    }

    public MCRNewMetadata create(int id, MCRContent content) throws MCRPersistenceException, MCRUsageException {
        MCRNewMetadata metadata = new MCRNewMetadata(this, id);
        metadata.create(content);
        return metadata;
    }

    public MCRNewMetadata retrieve(int id) throws MCRPersistenceException {
        MCRNewMetadata metadata = new MCRNewMetadata(this, id);
        metadata.read();
        return metadata;
    }

    public MCRNewMetadata update(int id, MCRContent content) throws MCRPersistenceException, MCRUsageException {
        MCRNewMetadata metadata = new MCRNewMetadata(this, id);
        metadata.update(content);
        return metadata;
    }

    @Override
    public void delete(final int id) throws MCRPersistenceException, MCRUsageException {
        new MCRNewMetadata(this, id).delete();
    }

    public boolean getForcedXML() {
        return forceXML;
    }

    public String getDocType() {
        return forceDocType;
    }

    @Override
    public boolean exists(final int id) throws MCRPersistenceException {
        return exists(new MCRNewMetadata(this, id));
    }

    public abstract boolean exists(MCRNewMetadata metadata) throws MCRPersistenceException;

    protected abstract void createContent(MCRNewMetadata metadata, MCRContent content)
        throws MCRPersistenceException, MCRUsageException;

    protected abstract MCRContent readContent(MCRNewMetadata metadata)
        throws MCRPersistenceException, MCRUsageException;

    protected abstract void updateContent(MCRNewMetadata metadata, MCRContent content)
        throws MCRPersistenceException, MCRUsageException;

    protected abstract void deleteContent(MCRNewMetadata metadata) throws MCRPersistenceException, MCRUsageException;
    
    @Override
    public abstract int getHighestStoredID() throws MCRPersistenceException;

    @Override
    public abstract Iterator<Integer> listIDs(boolean order) throws MCRPersistenceException;

    public MCRNewMetadataVersion getVersion(int id, String revision)
        throws MCRPersistenceException, MCRUsageException {
        return getVersion(new MCRNewMetadata(this, id, revision));
    }

    public abstract MCRNewMetadataVersion getVersion(MCRNewMetadata metadata)
        throws MCRPersistenceException, MCRUsageException;

    public MCRNewMetadataVersion getVersionFirst(int id) throws MCRPersistenceException, MCRUsageException {
        return getVersionFirst(new MCRNewMetadata(this, id));
    }

    public MCRNewMetadataVersion getVersionFirst(MCRNewMetadata metadata)
        throws MCRPersistenceException, MCRUsageException {
        String firstRevision = getVersions(metadata).sequential().reduce((first, second) -> first).orElseThrow(
            () -> new MCRUsageException("Failed to find first version of " + metadata.getFullID().toString()))
            .getRevision();
        metadata.setRevision(firstRevision);
        return getVersion(metadata);
    }

    public MCRNewMetadataVersion getVersionLast(MCRNewMetadata metadata)
        throws MCRPersistenceException, MCRUsageException {
        String firstRevision = getVersions(metadata).sequential()
            //.filter(v -> v.getType() == MCRAbstractMetadataVersion.UPDATED)
            .reduce((first, second) -> second)
            .orElseThrow(
                () -> new MCRUsageException("Failed to find last version of " + metadata.getFullID().toString()))
            .getRevision();
        metadata.setRevision(firstRevision);
        return getVersion(metadata);
    }

    public Stream<MCRNewMetadataVersion> getVersions(int id) throws MCRPersistenceException, MCRUsageException {
        return getVersions(new MCRNewMetadata(this, id));
    }

    public abstract Stream<MCRNewMetadataVersion> getVersions(MCRNewMetadata metadata)
        throws MCRPersistenceException, MCRUsageException;

    public void verify() throws MCRPersistenceException, MCRUsageException {
        if (!canVerify) {
            throw new MCRUsageException(
                "Store class " + this.getClass().getCanonicalName() + " does not support verification");
        } else {
            doVerification();
        }
    }

    protected abstract void doVerification() throws MCRPersistenceException;

    public Date getModified(int id) throws MCRPersistenceException {
        return getModified(new MCRNewMetadata(this, id));
    }

    public abstract Date getModified(MCRNewMetadata mcrNewMetadata) throws MCRPersistenceException;

    public void setModified(int id, Date date) throws MCRPersistenceException {
        setModified(new MCRNewMetadata(this, id), date);
    }

    public abstract void setModified(MCRNewMetadata metadata, Date date) throws MCRPersistenceException;
}
