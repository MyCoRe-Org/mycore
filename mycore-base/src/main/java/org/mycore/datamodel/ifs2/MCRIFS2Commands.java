package org.mycore.datamodel.ifs2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

@MCRCommandGroup(name = "IFS2 Commands")
public class MCRIFS2Commands extends MCRAbstractCommands {

    public static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "list all current ifs2 stores",
        help = "Lists all currently configured IFS2 MCRStore instances")
    public static void listAllStores() throws IOException {
        initStores();
        final Stream<MCRStore> stores = MCRStoreCenter.instance().getCurrentStores(MCRStore.class);
        stores.map(MCRIFS2Commands::toString)
            .forEach(LOGGER::info);
    }

    @MCRCommand(syntax = "list all current ifs2 file stores",
        help = "Lists all currently configured IFS2 MCRFileStore instances")
    public static void listAllFileStores() {
        initFileStores();
        final Stream<MCRFileStore> stores = MCRStoreCenter.instance().getCurrentStores(MCRFileStore.class);
        stores.map(MCRIFS2Commands::toString)
            .forEach(LOGGER::info);
    }

    private static void initStores() {
        initMetadataStores();
        initFileStores();
    }

    private static void initFileStores() {
        //FileSystems.getFileSystem(schemeURI) will not work in WebCLI
        //Workaround is to go via MCRPath
        final FileSystem defaultFileSystem = MCRPath.getRootPath("ignored").getFileSystem();
        defaultFileSystem.getFileStores()
            .forEach(FileStore::name);
    }

    private static void initMetadataStores() {
        final MCRXMLMetadataManager metadataManager = MCRXMLMetadataManager.instance();
        metadataManager.getObjectBaseIds().forEach(id -> {
            final String[] parts = id.split("_");
            metadataManager.getHighestStoredID(parts[0], parts[1]);
        });
    }

    private static String toString(MCRStore store) {
        final Map<String, String> configMap = MCRConfiguration2.getSubPropertiesMap(
            "MCR.IFS2.Store." + store.getID() + ".");
        final String config = configMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> ", " + e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining());
        return store.getClass().getSimpleName() + "{" +
            "id=" + store.getID() +
            ", baseDirectory=" + store.getBaseDirectory() +
            ", lastID=" + store.getHighestStoredID() +
            config +
            '}';
    }

    @MCRCommand(syntax = "generate ifs2 md5sum files in directory {0}",
        help = "writes md5sum files for every ifs2 file store in directory {0}")
    public static List<String> writeMD5SumsToDirectory(String directory) throws NotDirectoryException {
        Path targetPath = Paths.get(directory);
        if (!Files.isDirectory(targetPath)) {
            throw new NotDirectoryException(targetPath.toString());
        }
        initFileStores();
        return MCRStoreCenter.instance().getCurrentStores(MCRFileStore.class)
            .sorted(Comparator.comparing(MCRStore::getID))
            .map(s -> "generate md5sum file " + targetPath.resolve(s.getID() + ".md5") + " for ifs2 file store "
                + s.getID())
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "generate md5sum file {0} for ifs2 file store {1}",
        help = "writes md5sum file {0} for every file in MCRFileStore with ID {1}")
    public static void writeMD5SumFile(String targetFile, String ifsStoreId) throws IOException {
        initFileStores();
        final MCRStore store = MCRStoreCenter.instance().getStore(ifsStoreId);
        if (!(store instanceof MCRFileStore)) {
            LOGGER.error("Store " + ifsStoreId + " is not found or is not a file store.");
            return;
        }
        Path targetPath = Paths.get(targetFile);
        if (!Files.isDirectory(targetPath.getParent())) {
            throw new NotDirectoryException(targetPath.getParent().toString());
        }
        try (BufferedWriter bw = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE)) {
            final MessageFormat md5FileFormat = new MessageFormat("{0}  {1}\n", Locale.ROOT);
            MCRFileStore fileStore = (MCRFileStore) store;
            fileStore.getStoredIDs()
                .sorted()
                .mapToObj(id -> {
                    //retrieve MCRFileCollection
                    try {
                        return fileStore.retrieve(id);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .flatMap(fc -> {
                    //List every file in FileCollection
                    try {
                        return getAllFiles(fc.getLocalPath(), fc.getMetadata().getRootElement());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .forEach(f -> {
                    //Write single line in md5sum file
                    try {
                        bw.write(md5FileFormat.format(new Object[] { f.md5, f.localPath }));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static Stream<FileInfo> getAllFiles(Path nodePath, Element node)
        throws IOException {
        Function<Element, String> getName = e -> e.getAttributeValue("name");
        try {
            return node.getChildren().stream()
                .sorted(Comparator.comparing(getName))
                .flatMap(n -> {
                    final String fileName = getName.apply(n);
                    if ("dir".equals(n.getName())) {
                        try {
                            return getAllFiles(nodePath.resolve(fileName), n);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    return Stream.of(new FileInfo(nodePath.resolve(fileName), n.getAttributeValue("md5")));
                });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @MCRCommand(syntax = "verify ifs2 versioning metadata store {0}",
        help = "checks versioning metadata store {0} for errors")
    public static void verifyVersioningMetadataStore(String storeId) {
        initMetadataStores();
        MCRVersioningMetadataStore store = MCRStoreCenter.instance().getStore(storeId);
        if (storeId == null) {
            LOGGER.error("MCRVersioningMetadataStore with id " + storeId + " was not found.");
            return;
        }
        store.verify();
    }

    @MCRCommand(syntax = "verify ifs2 versioning metadata stores",
        help = "checks all versioning metadata stores for errors")
    public static List<String> verifyVersioningMetadataStores() {
        initMetadataStores();
        return MCRStoreCenter.instance().getCurrentStores(MCRVersioningMetadataStore.class)
            .map(s -> "verify versioning metadata store " + s.getID())
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "repair metadata for file collection {0,number} in ifs2 file store {1}",
        help = "repairs checksums in file collection {0} of ifs2 file store {1}")
    public static void repairMetaXML(int fileCollection, String storeId) throws IOException {
        initFileStores();
        MCRFileStore store = MCRStoreCenter.instance().getStore(storeId);
        final MCRFileCollection fc = store.retrieve(fileCollection);
        fc.repairMetadata();
    }

    @MCRCommand(syntax = "repair ifs2 metadata for derivate {0}",
        help = "repairs checksums for derivate {0}")
    public static List<String> repairMetaXML(String mcrId) {
        final MCRObjectID derivateId = MCRObjectID.getInstance(mcrId);
        //works for derivate that use ifs2:// URIs
        return List.of("repair metadata for file collection " + derivateId.getNumberAsInteger()
            + " in ifs2 file store IFS2_" + derivateId.getBase());
    }

    private record FileInfo(Path localPath, String md5) {
    }

}
