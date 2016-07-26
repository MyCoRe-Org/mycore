package org.mycore.impex;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.services.packaging.MCRPackerManager;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.search.MCRSolrSearchUtils;

@MCRCommandGroup(name = "Transfer Package Commands")
public class MCRTransferPackageCommands {

    @MCRCommand(help = "Creates multiple transfer packages which matches the solr query in {0}.", syntax = "create transfer package for objects matching {0}")
    public static void create(String query) throws Exception {
        List<String> ids = MCRSolrSearchUtils.listIDs(MCRSolrClientFactory.getSolrClient(), query);
        for (String objectId : ids) {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("packer", "TransferPackage");
            parameters.put("source", objectId);
            MCRPackerManager.startPacking(parameters);
        }
    }

    @MCRCommand(help = "Imports all transfer packages located in the directory {0}.", syntax = "import transfer packages from directory {0}")
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

    @MCRCommand(help = "Imports a transfer package located at {0}. Where {0} is the absolute path to the tar file.", syntax = "import transfer package from tar {0}")
    public static void importTransferPackageFromTar(String pathToTar) throws Exception {
        Path tar = Paths.get(pathToTar);
        MCRTransferPackageUtil.importTar(tar);
    }

}
