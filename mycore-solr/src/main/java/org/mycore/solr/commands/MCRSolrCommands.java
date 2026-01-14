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

package org.mycore.solr.commands;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRBasicCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.classification.MCRSolrClassificationUtil;
import org.mycore.solr.index.MCRSolrIndexer;
import org.mycore.solr.search.MCRSolrSearchUtils;

/**
 * Class provides useful solr related commands.
 *
 * @author shermann
 * @author Sebastian Hofmann
 */
@MCRCommandGroup(
    name = "SOLR Index and Search Commands")
public class MCRSolrCommands extends MCRAbstractCommands {

    @MCRCommand(
        syntax = "rebuild solr metadata and content index in core {0}",
        help = "rebuilds metadata and content index in Solr for core with the id {0}",
        order = 110)
    public static void rebuildMetadataAndContentIndex(String coreIDs) {
        List<MCRSolrCore> cores = getCoreList(coreIDs);
        MCRSolrIndexer.rebuildMetadataIndex(cores);
        MCRSolrIndexer.rebuildContentIndex(cores);
        MCRSolrIndexer.optimize(cores);
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index for all objects of type {0} in core {1}",
        help = "rebuilds the metadata index in Solr for all objects of type {0} in core with the id {1}",
        order = 130)
    public static void rebuildMetadataIndexType(String type, String coreIDs) {
        List<MCRSolrCore> cores = getCoreList(coreIDs);
        MCRSolrIndexer.rebuildMetadataIndex(type, cores);
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index for all objects of base {0} in core {1}",
        help = "rebuilds the metadata index in Solr for all objects of base {0} in core with the id {1}",
        order = 130)
    public static void rebuildMetadataIndexBase(String base, String coreIDs) throws Exception {
        List<MCRSolrCore> cores = getCoreList(coreIDs);
        MCRSolrIndexer.rebuildMetadataIndexForObjectBase(base, cores);
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index for object {0} in core {1}",
        help = "rebuilds metadata index in Solr for the given object in core with the id {1}",
        order = 120)
    public static void rebuildMetadataIndexObject(String object, String coreIDs) {
        List<MCRSolrCore> cores = getCoreList(coreIDs);
        MCRSolrIndexer.rebuildMetadataIndex(Stream.of(object).collect(Collectors.toList()), cores);
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index for selected in core {0}",
        help = "rebuilds content index in Solr for selected objects and or derivates in core with the id {0}",
        order = 140)
    public static void rebuildMetadataIndexForSelected(String coreIDs) {
        List<MCRSolrCore> cores = getCoreList(coreIDs);
        List<String> selectedObjects = MCRBasicCommands.getSelectedValues();
        MCRSolrIndexer.rebuildMetadataIndex(selectedObjects, cores);
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index in core {0}",
        help = "rebuilds metadata index in Solr in core with the id {0}",
        order = 150)
    public static void rebuildMetadataIndex(String coreIDs) {
        List<MCRSolrCore> cores = getCoreList(coreIDs);
        MCRSolrIndexer.rebuildMetadataIndex(cores);
    }

    @MCRCommand(
        syntax = "rebuild solr content index for object {0} in core {1}",
        help = "rebuilds content index in Solr for the all derivates of object with the id {0} "
            + "in core with the id {1}",
        order = 160)
    public static void rebuildContentIndexObject(String objectID, String coreIDs) {
        List<MCRSolrCore> cores = getCoreList(coreIDs);
        MCRSolrIndexer.rebuildContentIndex(Stream.of(objectID).collect(Collectors.toList()), cores);
    }

    @MCRCommand(
        syntax = "rebuild solr content index for selected in core {0}",
        help = "rebuilds content index in Solr for alll derivates of selected objects in core with the id {0}",
        order = 170)
    public static void rebuildContentIndexForSelected(String coreIDs) {
        List<MCRSolrCore> cores = getCoreList(coreIDs);
        List<String> selectedObjects = MCRBasicCommands.getSelectedValues();
        MCRSolrIndexer.rebuildContentIndex(selectedObjects, cores);
    }

    @MCRCommand(
        syntax = "rebuild solr content index in core {0}",
        help = "rebuilds content index in Solr in core with the id {0}",
        order = 180)
    public static void rebuildContentIndex(String coreIDs) {
        List<MCRSolrCore> cores = getCoreList(coreIDs);
        MCRSolrIndexer.rebuildContentIndex(cores);
    }

    @MCRCommand(
        syntax = "rebuild solr classification index in core {0}",
        help = "rebuilds classification index in Solr in the core with the id {0}",
        order = 190)
    public static void rebuildClassificationIndex(String coreIDs) {
        List<MCRSolrCore> core = getCoreList(coreIDs);
        MCRSolrClassificationUtil.rebuildIndex(core);
    }

    @MCRCommand(
        syntax = "clear solr index in core {0}",
        help = "deletes all entries from index in Solr in core with the id {0}",
        order = 210)
    public static void dropIndex(String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.dropIndex(core.getClient());
    }

    @MCRCommand(
        syntax = "delete from solr index all objects of type {0} in core {1}",
        help = "deletes all objects of type {0} from index in Solr in core with the id {1}",
        order = 220)
    public static void dropIndexByType(String type, String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.dropIndexByType(type, core.getClient());
    }

    @MCRCommand(
        syntax = "delete from solr index all objects of base {0} in core {1}",
        help = "deletes all objects of base {0} from index in Solr in core with the id {1}",
        order = 220)
    public static void dropIndexByBase(String base, String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.dropIndexByBase(base, core.getClient());
    }

    @MCRCommand(
        syntax = "delete from solr index object {0} in core {1}",
        help = "deletes an object with id {0} from index in Solr in core with the id {1}",
        order = 230)
    public static void deleteByIdFromSolr(String objectID, String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.deleteById(core.getClient(), objectID);
    }

    @MCRCommand(
        syntax = "select objects with solr query {0} in core {1}",
        help = "selects mcr objects with a solr query {0} in core with the id {1}",
        order = 310)
    public static void selectObjectsWithSolrQuery(String query, String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRBasicCommands.setSelectedValues(MCRSolrSearchUtils.listIDs(core.getClient(), query));
    }

    /**
     * This command optimizes the index in Solr in a given core.
     * The operation works like a hard commit and forces all of the index segments
     * to be merged into a single segment first.
     * Depending on the use cases, this operation should be performed infrequently (e.g. nightly)
     * since it is very expensive and involves reading and re-writing the entire index.
     */
    @MCRCommand(
        syntax = "optimize solr index in core {0}",
        help = "optimizes the index in Solr in core with the id {0}. "
            + "The operation works like a hard commit and forces all of the index segments "
            + "to be merged into a single segment first. "
            + "Depending on the use cases, this operation should be performed infrequently (e.g. nightly), "
            + "since it is very expensive and involves reading and re-writing the entire index",
        order = 410)
    public static void optimize(String coreID) {
        List<MCRSolrCore> cores = getCoreList(coreID);
        MCRSolrIndexer.optimize(cores);
    }

    private static MCRSolrCore getCore(String coreID) {
        return MCRSolrCoreManager.get(coreID)
            .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(coreID));
    }

    @MCRCommand(
        syntax = "synchronize solr metadata index for all objects of type {0} in core {1}",
        help = "synchronizes the MyCoRe store and index in Solr in core with the id {1} for objects of type {0}",
        order = 420)
    public static void synchronizeMetadataIndex(String objectType, String coreID) throws Exception {
        List<MCRSolrCore> cores = getCoreList(coreID);
        MCRSolrIndexer.synchronizeMetadataIndex(cores, objectType);
    }

    @MCRCommand(
        syntax = "synchronize solr metadata index for all objects of base {0} in core {1}",
        help = "synchronizes the MyCoRe store and index in Solr in core with the id {1} for objects of base {0}",
        order = 420)
    public static void synchronizeMetadataIndexForObjectBase(String objectBase, String coreIDs) throws Exception {
        List<MCRSolrCore> core = getCoreList(coreIDs);
        MCRSolrIndexer.synchronizeMetadataIndexForObjectBase(core, objectBase);
    }

    @MCRCommand(
        syntax = "synchronize solr metadata index in core {0}",
        help = "synchronizes the MyCoRe store and index in Solr in core with the id {0}",
        order = 430)
    public static void synchronizeMetadataIndex(String coreIDs) throws Exception {
        List<MCRSolrCore> cores = getCoreList(coreIDs);
        MCRSolrIndexer.synchronizeMetadataIndex(cores);
    }

    private static List<MCRSolrCore> getCoreList(String coreIDs) {
        return Stream.of(coreIDs.split("[, ]")).map(MCRSolrCommands::getCore)
            .toList();
    }
}
