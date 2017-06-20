package org.mycore.impex;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.classifications2.utils.MCRClassificationUtils;
import org.mycore.datamodel.common.MCRMarkManager;
import org.mycore.datamodel.common.MCRMarkManager.Operation;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.services.packaging.MCRPackerManager;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.index.MCRSolrIndexer;
import org.mycore.solr.search.MCRSolrSearchUtils;

@MCRCommandGroup(name = "Transfer Package Commands")
public class MCRTransferPackageCommands {

    private static final Logger LOGGER = LogManager.getLogger(MCRTransferPackageCommands.class);

    @MCRCommand(help = "Creates multiple transfer packages which matches the solr query in {0}.",
        syntax = "create transfer package for objects matching {0}")
    public static void create(String query) throws Exception {
        List<String> ids = MCRSolrSearchUtils.listIDs(MCRSolrClientFactory.getSolrClient(), query);
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
        List<String> importStatements = new LinkedList<String>();
        try (Stream<Path> stream = Files.find(dir, 0, (path, attr) -> {
            return String.valueOf(path).endsWith(".tar") && Files.isRegularFile(path);
        })) {
            stream.map(Path::toAbsolutePath).map(Path::toString).forEach(path -> {
                String subCommand = MessageFormat.format("import transfer package from tar {0}", path);
                importStatements.add(subCommand);
            });
        }
        return importStatements;
    }

    @MCRCommand(help = "Imports a transfer package located at {0}. Where {0} is the absolute path to the tar file.",
        syntax = "import transfer package from tar {0}")
    public static List<String> importTransferPackageFromTar(String pathToTar) throws Exception {
        Path tar = Paths.get(pathToTar);
        if (!Files.exists(tar)) {
            throw new FileNotFoundException(tar.toAbsolutePath().toString() + " does not exist.");
        }
        Path targetDirectory = MCRTransferPackageUtil.getTargetDirectory(tar);

        List<String> commands = new ArrayList<>();
        commands.add("_import transfer package untar " + pathToTar);
        commands.add("_import transfer package from directory " + targetDirectory);
        commands.add("_import transfer package clean up " + targetDirectory);
        return commands;
    }

    @MCRCommand(syntax = "_import transfer package untar {0}")
    public static void _untar(String pathToTar) throws Exception {
        Path tar = Paths.get(pathToTar);
        Path targetDirectory = MCRTransferPackageUtil.getTargetDirectory(tar);
        LOGGER.info("Untar " + pathToTar + " to " + targetDirectory + "...");
        MCRUtils.untar(tar, targetDirectory);
    }

    @MCRCommand(syntax = "_import transfer package from directory {0}")
    public static List<String> _fromDirectory(String targetDirectoryPath) throws Exception {
        LOGGER.info("Import transfer package from " + targetDirectoryPath + "...");
        Path targetDirectory = Paths.get(targetDirectoryPath);
        List<String> commands = new ArrayList<>();

        // load classifications
        List<Path> classificationPaths = MCRTransferPackageUtil.getClassifications(targetDirectory);
        for (Path pathToClassification : classificationPaths) {
            commands.add(
                "_import transfer package classification from " + pathToClassification.toAbsolutePath().toString());
        }

        // import objects
        List<String> mcrObjects = MCRTransferPackageUtil.getMCRObjects(targetDirectory);
        MCRMarkManager markManager = MCRMarkManager.instance();
        for (String id : mcrObjects) {
            markManager.mark(MCRObjectID.getInstance(id), Operation.IMPORT);
            commands.add("_import transfer package object " + id + " from " + targetDirectoryPath);
        }
        return commands;
    }

    @MCRCommand(syntax = "_import transfer package classification from {0}")
    public static void _importObject(String pathToClassification) throws Exception {
        MCRClassificationUtils.fromPath(Paths.get(pathToClassification));
    }

    @MCRCommand(syntax = "_import transfer package object {0} from {1}")
    public static List<String> _importObject(String objectId, String targetDirectoryPath) throws Exception {
        Path targetDirectory = Paths.get(targetDirectoryPath);
        List<String> derivates = MCRTransferPackageUtil.importObjectCLI(targetDirectory, objectId);
        return derivates.stream().map(derId -> {
            return "_import transfer package derivate " + derId + " from " + targetDirectoryPath;
        }).collect(Collectors.toList());
    }

    @MCRCommand(syntax = "_import transfer package derivate {0} from {1}")
    public static void _importDerivate(String derivateId, String targetDirectoryPath) throws Exception {
        Path targetDirectory = Paths.get(targetDirectoryPath);
        MCRTransferPackageUtil.importDerivate(targetDirectory, derivateId);
    }

    @MCRCommand(syntax = "_import transfer package clean up {0}")
    public static void _cleanUp(String targetDirectoryPath) throws Exception {
        Path targetDirectory = Paths.get(targetDirectoryPath);
        // delete mark of imported object
        List<String> mcrObjects = MCRTransferPackageUtil.getMCRObjects(targetDirectory);
        MCRMarkManager markManager = MCRMarkManager.instance();
        for (String id : mcrObjects) {
            markManager.remove(MCRObjectID.getInstance(id));
        }
        // index all objects
        MCRSolrIndexer.rebuildMetadataIndex(mcrObjects);

        // deleting expanded directory
        LOGGER.info("Deleting expanded tar in " + targetDirectoryPath + "...");
        Files.walkFileTree(Paths.get(targetDirectoryPath), MCRRecursiveDeleter.instance());
    }

}
