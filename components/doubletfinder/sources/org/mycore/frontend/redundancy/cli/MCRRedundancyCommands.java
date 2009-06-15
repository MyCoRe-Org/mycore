package org.mycore.frontend.redundancy.cli;

import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

public class MCRRedundancyCommands extends MCRAbstractCommands {

    public MCRRedundancyCommands() {
        super();

        // generate the redundancy map
        MCRCommand generateCommand = new MCRCommand("generate redundancy map for type {0} with map generator {1}", "org.mycore.frontend.redundancy.cli.MCRRedundancyGenerateMapCommand.generate String String", "generates a redundancy map for a specific type which is processed by the map generator of the given alias.");
        command.add(generateCommand);

        // clean up the database
        MCRCommand cleanUpRed = new MCRCommand("clean up redundancy in database for type {0}", "org.mycore.frontend.redundancy.cli.MCRRedundancyCleanUpCommand.cleanUp String", "cleans up the redundancy in the database of a specific type.");
        command.add(cleanUpRed);

        MCRCommand prInternalProcessRedundancyObjects = new MCRCommand("internal process redundancy object {0}", "org.mycore.frontend.redundancy.cli.MCRRedundancyCleanUpCommand.processRedundancyObject String", "");
        command.add(prInternalProcessRedundancyObjects);

        MCRCommand prInternalReplaceLinks = new MCRCommand("internal replace links {0} {1} {2}", "org.mycore.frontend.redundancy.cli.MCRRedundancyCleanUpCommand.replaceLinks String String String", "");
        command.add(prInternalReplaceLinks);
        
        MCRCommand prInternalDeleteRedundancyEntry = new MCRCommand("internal delete redundancy object xml entry {0}", "org.mycore.frontend.redundancy.cli.MCRRedundancyCleanUpCommand.deleteRedundancyElementEntry String", "");
        command.add(prInternalDeleteRedundancyEntry);

        MCRCommand prInternalUpdateXMLDocument = new MCRCommand("internal update xml document {0}", "org.mycore.frontend.redundancy.cli.MCRRedundancyCleanUpCommand.updateXMLDocument String", "");
        command.add(prInternalUpdateXMLDocument);
    }
}