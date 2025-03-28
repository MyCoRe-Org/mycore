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

package org.mycore.impex;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.classifications2.utils.MCRClassificationUtils;
import org.mycore.datamodel.common.MCRMarkManager;
import org.mycore.datamodel.common.MCRMarkManager.Operation;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.services.packaging.MCRPackerManager;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.MCRSolrCoreType;
import org.mycore.solr.index.MCRSolrIndexer;
import org.mycore.solr.search.MCRSolrSearchUtils;

@MCRCommandGroup(name = "Transfer Package Commands")
@SuppressWarnings("PMD.ClassNamingConventions")
public class MCRTransferPackageCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(help = "Creates multiple transfer packages which matches the solr query in {0}.",
        syntax = "create transfer package for objects matching {0}")
    public static void create(String query) throws MCRAccessException {
        List<String> ids = MCRSolrSearchUtils.listIDs(MCRSolrCoreManager.getMainSolrClient(), query);
        for (String objectId : ids) {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("packer", "TransferPackage");
            parameters.put("source", objectId);
            MCRPackerManager.startPacking(parameters);
        }
    }

    @MCRCommand(help = "Imports all transfer packages located in the directory {0}.",
        syntax = "import transfer packages from directory {0}")
    public static List<String> importTransferPackagesFromDirectory(String directory) throws Exception {
        Path dir = Paths.get(directory);
        if (!(Files.exists(dir) || Files.isDirectory(dir))) {
            throw new FileNotFoundException(directory + " does not exist or is not a directory.");
        }
        List<String> importStatements = new ArrayList<>();
        try (Stream<Path> stream = Files.find(dir, 1,
            (path, attr) -> String.valueOf(path).endsWith(".tar") && Files.isRegularFile(path))) {
            stream.map(Path::toAbsolutePath).map(Path::toString).forEach(path -> {
                String subCommand = String.format(Locale.ROOT, "import transfer package from tar %s", path);
                importStatements.add(subCommand);
            });
        }
        return importStatements;
    }

    @MCRCommand(help = "Imports a transfer package located at {0}. Where {0} is the absolute path to the tar file. ",
        syntax = "import transfer package from tar {0}")
    public static List<String> importTransferPackageFromTar(String pathToTar) throws Exception {
        return importTransferPackageFromTar(pathToTar, null);
    }

    @MCRCommand(help = "Imports a transfer package located at {0}. Where {0} is the absolute path to the tar file. "
        + "The parameter {1} is optional and can be omitted. You can specify a mycore id where the first object of "
        + "import.xml should be attached.",
        syntax = "import transfer package from tar {0} to {1}")
    public static List<String> importTransferPackageFromTar(String pathToTar, String mycoreTargetId) throws Exception {
        Path tar = Paths.get(pathToTar);
        if (!Files.exists(tar)) {
            throw new FileNotFoundException(tar.toAbsolutePath() + " does not exist.");
        }
        Path targetDirectory = MCRTransferPackageUtil.getTargetDirectory(tar);

        List<String> commands = new ArrayList<>();
        commands.add("_import transfer package untar " + pathToTar);
        if (mycoreTargetId != null) {
            commands.add("_import transfer package from directory " + targetDirectory + " to " + mycoreTargetId);
        } else {
            commands.add("_import transfer package from directory " + targetDirectory);
        }
        commands.add("_import transfer package clean up " + targetDirectory);
        return commands;
    }

    @MCRCommand(syntax = "_import transfer package untar {0}")
    public static void untar(String pathToTar) throws Exception {
        Path tar = Paths.get(pathToTar);
        Path targetDirectory = MCRTransferPackageUtil.getTargetDirectory(tar);
        LOGGER.info("Untar {} to {}...", pathToTar, targetDirectory);
        MCRUtils.untar(tar, targetDirectory);
    }

    @MCRCommand(syntax = "_import transfer package from directory {0}")
    public static List<String> fromDirectory(String sourceDirectory) throws Exception {
        return fromDirectory(sourceDirectory, null);
    }

    @MCRCommand(syntax = "_import transfer package from directory {0} to {1}")
    public static List<String> fromDirectory(String sourceDirectory, String mycoreTargetId) throws Exception {
        LOGGER.info("Import transfer package from {}...", sourceDirectory);
        Path sourcePath = Paths.get(sourceDirectory);
        List<String> commands = new ArrayList<>();

        // load classifications
        List<Path> classificationPaths = MCRTransferPackageUtil.getClassifications(sourcePath);
        for (Path pathToClassification : classificationPaths) {
            commands.add("_import transfer package classification from " + pathToClassification.toAbsolutePath());
        }

        // import objects
        List<String> mcrObjects = MCRTransferPackageUtil.getMCRObjects(sourcePath);
        MCRMarkManager markManager = MCRMarkManager.getInstance();

        if (mycoreTargetId != null && !mcrObjects.isEmpty()) {
            String rootId = mcrObjects.getFirst();
            markManager.mark(MCRObjectID.getInstance(rootId), Operation.IMPORT);
            commands.add(
                "_import transfer package object " + rootId + " from " + sourceDirectory + " to " + mycoreTargetId);
            mcrObjects = mcrObjects.subList(1, mcrObjects.size());
        }
        for (String id : mcrObjects) {
            markManager.mark(MCRObjectID.getInstance(id), Operation.IMPORT);
            commands.add("_import transfer package object " + id + " from " + sourceDirectory);
        }
        return commands;
    }

    @MCRCommand(syntax = "_import transfer package classification from {0}")
    public static void importObject(String pathToClassification) throws Exception {
        MCRClassificationUtils.fromPath(Paths.get(pathToClassification));
    }

    @MCRCommand(syntax = "_import transfer package object {0} from {1}")
    public static List<String> importObject(String objectId, String targetDirectoryPath) throws Exception {
        return importObject(objectId, targetDirectoryPath, null);
    }

    @MCRCommand(syntax = "_import transfer package object {0} from {1} to {2}")
    public static List<String> importObject(String objectId, String targetDirectoryPath, String parentId)
        throws Exception {
        Path targetDirectory = Paths.get(targetDirectoryPath);
        List<String> derivates = MCRTransferPackageUtil.importObjectCLI(targetDirectory, objectId, parentId);
        return derivates.stream()
            .map(derId -> "_import transfer package derivate " + derId + " from " + targetDirectoryPath)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "_import transfer package derivate {0} from {1}")
    public static void importDerivate(String derivateId, String targetDirectoryPath) throws Exception {
        Path targetDirectory = Paths.get(targetDirectoryPath);
        MCRTransferPackageUtil.importDerivate(targetDirectory, derivateId);
    }

    @MCRCommand(syntax = "_import transfer package clean up {0}")
    public static void cleanUp(String targetDirectoryPath) throws Exception {
        Path targetDirectory = Paths.get(targetDirectoryPath);
        // delete mark of imported object
        List<String> mcrObjects = MCRTransferPackageUtil.getMCRObjects(targetDirectory);
        MCRMarkManager markManager = MCRMarkManager.getInstance();
        for (String id : mcrObjects) {
            markManager.remove(MCRObjectID.getInstance(id));
        }
        // index all objects
        MCRSolrIndexer.rebuildMetadataIndex(mcrObjects, MCRSolrCoreManager.getCoresForType(MCRSolrCoreType.MAIN));

        // deleting expanded directory
        LOGGER.info("Deleting expanded tar in {}...", targetDirectoryPath);
        Files.walkFileTree(Paths.get(targetDirectoryPath), new MCRRecursiveDeleter());
    }

}
