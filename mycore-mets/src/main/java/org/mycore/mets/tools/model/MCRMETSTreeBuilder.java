package org.mycore.mets.tools.model;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.LogicalSubDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.SmLink;
import org.mycore.mets.model.struct.StructLink;
import org.mycore.mets.tools.MCRJSONTools;

/**
 * Builds the {@link MCRMETSTree} from a {@link Mets} Object.
 * @author Sebastian Hofmann
 */
public class MCRMETSTreeBuilder {

    final private static Logger LOGGER = Logger.getLogger(MCRMETSTreeBuilder.class);

    public MCRMETSTreeBuilder(Mets mets) {
        this.mets = mets;
        this.tree = new MCRMETSTree();

        this.idDivMap = new HashMap<String, PhysicalSubDiv>();
        this.idFileMap = new HashMap<String, File>();
        this.idFolderMap = new HashMap<String, MCRMETSNode>();
    }

    private Mets mets;

    private MCRMETSTree tree;

    private Map<String, PhysicalSubDiv> idDivMap;

    private Map<String, File> idFileMap;

    private Map<String, MCRMETSNode> idFolderMap;

    /**
     * Builds the {@link MCRMETSTree} for Metseditor. 
     * @return the builded tree
     */
    public MCRMETSTree buildTree() {
        // Build the ID Div Map
        PhysicalStructMap metsPhysStructMap = (PhysicalStructMap) mets.getStructMap(PhysicalStructMap.TYPE);
        List<PhysicalSubDiv> subDivList = metsPhysStructMap.getDivContainer().getChildren();
        for (PhysicalSubDiv physicalSubDiv : subDivList) {
            idDivMap.put(physicalSubDiv.getId(), physicalSubDiv);
        }

        // Build the ID File Map
        for (FileGrp fileGroups : mets.getFileSec().getFileGroups()) {
            List<File> fileList = fileGroups.getFileList();
            for (File curFile : fileList) {
                String id = curFile.getId();
                idFileMap.put(id, curFile);
            }
        }

        LogicalStructMap logDiv = ((LogicalStructMap) mets.getStructMap(LogicalStructMap.TYPE));
        List<LogicalSubDiv> metsFolderList = logDiv.getDivContainer().getChildren();

        // add root node to tree
        MCRMETSNode metsStructureFolder;
        metsStructureFolder = new MCRMETSNode();
        metsStructureFolder.setId(logDiv.getDivContainer().getId());
        metsStructureFolder.setName(logDiv.getDivContainer().getLabel());
        metsStructureFolder.setType("category");
        metsStructureFolder.setStructureType(logDiv.getDivContainer().getType());
        tree.items.add(metsStructureFolder);
        idFolderMap.put(metsStructureFolder.getId(), metsStructureFolder);

        MCRMETSNode root = metsStructureFolder;
        buildFolderTree(root, metsFolderList);

        StructLink metsStructLink = mets.getStructLink();
        List<SmLink> links = metsStructLink.getSmLinks();

        // build the tree
        for (SmLink smLink : links) {
            linkStructure(smLink);
        }

        addHiddenNodes(root);
        sort(root.getChildren());

        return tree;
    }

    /**
     * Used to sort the Children of a {@link MCRMETSNode}. Works recursive.
     * Uses the {@link Comparable} interface and the order attribute of a {@link MCRMETSNode}
     * @param listToSort the list of Children that should be sorted.
     */
    private void sort(List<MCRMETSNode> listToSort) {
        for (MCRMETSNode mcrmetsStructureNode : listToSort) {
            if (mcrmetsStructureNode != null && mcrmetsStructureNode.children != null) {
                sort(mcrmetsStructureNode.children);
            }
        }
        Collections.sort(listToSort);
    }

    /**
     * Creates the Children and links them to the right Folders.
     * @param metsSmLink the {@link SmLink} wich contains the source and the destination node
     */
    private void linkStructure(SmLink metsSmLink) {
        String physDivToInsert = metsSmLink.getTo();
        PhysicalSubDiv fileDiv = idDivMap.get(physDivToInsert);
        String fileId = fileDiv.getChildren().get(0).getFileId();
        File metsFile = idFileMap.get(fileId);

        String from = metsSmLink.getFrom();

        MCRMETSNode sourceNode = idFolderMap.get(from);

        MCRMETSNode destinationNode = new MCRMETSNode();
        String fileHref = metsFile.getFLocat().getHref();

        String path = MCRJSONTools.stripBracketsAndQuotes(fileHref);
        int index = path.lastIndexOf("/");

        destinationNode.setId(fileId);
        destinationNode.setPath(path);
        destinationNode.setName(path.substring(index == -1 ? 0 : index + 1));
        destinationNode.setStructureType(fileDiv.getType());
        destinationNode.setType("item");
        destinationNode.setContentIds(fileDiv.getContentids());
        destinationNode.setOrder(fileDiv.getOrder());

        if (sourceNode == null) {
            LOGGER.error("Can´t link with " + from);
            return;
        }

        sourceNode.addChild(destinationNode);
        return;
    }

    /**
     * Creates the folders and add them to the root node. Works recursive.
     * @param root the root node to add the folders.
     * @param metsFolderList the folders that should be added to the root node.
     */
    private void buildFolderTree(MCRMETSNode root, List<LogicalSubDiv> metsFolderList) {
        for (LogicalSubDiv metsFolder : metsFolderList) {
            MCRMETSNode metsStructureFolder = new MCRMETSNode();
            metsStructureFolder.setId(UUID.randomUUID().toString());
            metsStructureFolder.setName(metsFolder.getLabel());
            metsStructureFolder.setType("category");
            metsStructureFolder.setStructureType(metsFolder.getType());
            metsStructureFolder.setOrder(metsFolder.getOrder());
            idFolderMap.put(metsFolder.getId(), metsStructureFolder);
            root.addChild(metsStructureFolder);

            if (metsFolder.getChildren().size() != 0) {
                buildFolderTree(metsStructureFolder, metsFolder.getChildren());
            }
        }
    }

    /**
     * Detects the files which are not present in the struct map and adds them to the root node.
     * The hidden elements gets the hide true attribute.
     * @param root the node were the {@link MCRMETSNode} should be added.
     */
    private void addHiddenNodes(MCRMETSNode root) {
        Set<String> fileIds = this.idFileMap.keySet();

        for (String currentFileId : fileIds) {
            if (!idDivMap.containsKey(currentFileId.replace("master_", "phys_")) && !idDivMap.containsKey("phys_" + currentFileId)) {
                LOGGER.info(MessageFormat.format("{0} is a hidden File!", currentFileId));
                // file does not appear in structmap -> hidden file
                File metsFile = this.idFileMap.get(currentFileId);

                MCRMETSNode destinationNode = new MCRMETSNode();
                String fileHref = metsFile.getFLocat().getHref();

                String path = MCRJSONTools.stripBracketsAndQuotes(fileHref);
                int index = path.lastIndexOf("/");

                destinationNode.setId(currentFileId);
                destinationNode.setPath(path);
                destinationNode.setName(path.substring(index == -1 ? 0 : index + 1));
                destinationNode.setStructureType("page");
                destinationNode.setType("item");
                destinationNode.setHide(true);

                root.addChild(destinationNode);
            }
        }
    }
}
