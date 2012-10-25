/**
 * 
 */
package org.mycore.solr.commands;

import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

/**
 * Class provides useful solr related commands.
 * 
 * @author shermann
 *
 */
public class SolrCommands extends MCRAbstractCommands {

    public SolrCommands() {
        super("Solr Commands");
        MCRCommand com = null;

        com = new MCRCommand("rebuild solr metadata index", "org.mycore.solr.index.cs.SolrIndexer.rebuildMetadataIndex",
                "rebuilds solr's metadata index");
        addCommand(com);

        com = new MCRCommand("rebuild solr content index", "org.mycore.solr.index.cs.SolrIndexer.rebuildContentIndex",
                "rebuilds solr's content index");
        addCommand(com);

        com = new MCRCommand("rebuild solr metadata and content index", "org.mycore.solr.index.cs.SolrIndexer.rebuildMetadataAndContentIndex",
                "rebuilds solr's metadata and content index");
        addCommand(com);

        com = new MCRCommand("drop solr index", "org.mycore.solr.index.cs.SolrIndexer.dropIndex", "Deletes an existing index from solr");
        addCommand(com);
    }
}
