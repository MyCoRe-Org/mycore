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
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

@MCRCommandGroup(name = "IFS2 Commands")
public class MCRIFS2Commands extends MCRAbstractCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "list all current ifs2 stores",
        help = "Lists all currently configured IFS2 MCRStore instances")
    public static void listAllStores() throws IOException {
        initStores();
        final Stream<MCRStore> stores = MCRStoreCenter.getInstance().getCurrentStores(MCRStore.class);
        stores.map(MCRIFS2Commands::toString)
            .forEach(LOGGER::info);
    }

    @MCRCommand(syntax = "list all current ifs2 file stores",
        help = "Lists all currently configured IFS2 MCRFileStore instances")
    public static void listAllFileStores() {
        initFileStores();
        final Stream<MCRFileStore> stores = MCRStoreCenter.getInstance().getCurrentStores(MCRFileStore.class);
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
        final MCRXMLMetadataManager metadataManager = MCRXMLMetadataManager.getInstance();
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
        return MCRStoreCenter.getInstance().getCurrentStores(MCRFileStore.class)
            .sorted(Comparator.comparing(MCRStore::getID))
            .map(s -> "generate md5sum file " + targetPath.resolve(s.getID() + ".md5") + " for ifs2 file store "
                + s.getID())
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "generate md5sum file {0} for ifs2 file store {1}",
        help = "writes md5sum file {0} for every file in MCRFileStore with ID {1}")
    public static void writeMD5SumFile(String targetFile, String ifsStoreId) throws IOException {
        initFileStores();
        final MCRStore store = MCRStoreCenter.getInstance().getStore(ifsStoreId);
        if (!(store instanceof MCRFileStore)) {
            throw new MCRException("Store " + ifsStoreId + " is not found or is not a file store.");
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
                .mapToObj(fileStore::retrieve)
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
        } catch (UncheckedIOException ignoredUnchecked) {
            throw ignoredUnchecked.getCause();
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
        } catch (UncheckedIOException ignoredUnchecked) {
            throw ignoredUnchecked.getCause();
        }
    }

    @MCRCommand(syntax = "verify ifs2 versioning metadata store {0}",
        help = "checks versioning metadata store {0} for errors")
    public static void verifyVersioningMetadataStore(String storeId) {
        initMetadataStores();
        MCRVersioningMetadataStore store = MCRStoreCenter.getInstance().getStore(storeId);
        if (store == null) {
            throw new MCRException("MCRVersioningMetadataStore with id " + storeId + " was not found.");
        }
        store.verify();
    }

    @MCRCommand(syntax = "verify ifs2 versioning metadata stores",
        help = "checks all versioning metadata stores for errors")
    public static List<String> verifyVersioningMetadataStores() {
        initMetadataStores();
        return MCRStoreCenter.getInstance().getCurrentStores(MCRVersioningMetadataStore.class)
            .map(s -> "verify versioning metadata store " + s.getID())
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "repair metadata for file collection {0,number} in ifs2 file store {1}",
        help = "repairs checksums in file collection {0} of ifs2 file store {1}")
    public static void repairMetaXML(int fileCollection, String storeId) throws IOException {
        initFileStores();
        MCRFileStore store = MCRStoreCenter.getInstance().getStore(storeId);
        if (store == null) {
            throw new MCRException("MCRFileStore with id " + storeId + " was not found.");
        }
        final MCRFileCollection fc = store.retrieve(fileCollection);
        if (fc == null) {
            throw new MCRException("File collection " + fileCollection + " not found in MCRFileStore " + storeId + ".");
        }
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
