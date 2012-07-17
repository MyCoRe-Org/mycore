package org.mycore.mets.tools.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sebastian Hofmann
 * Represents a Page or Folder.
 */
public class MCRMETSNode implements Comparable<MCRMETSNode> {

    protected String id, type, path, contentIds, orderLabel, structureType, name;

    protected int order;

    protected boolean hide;
    
    protected List<MCRMETSNode> children;

    public MCRMETSNode() {
        id = "";
        type = "";
        name = "";
        structureType = "";
        path = "";
        contentIds = "";
        orderLabel = "";
        hide = false;
    }

    public boolean isHide() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

    public String getContentIds() {
        return contentIds;
    }

    /**
     * Sets the contentId attribute of the sub <code><mets:div/> </code>
     * @param contentIds the contentId that should be set
     */
    public void setContentIds(String contentIds) {
        this.contentIds = contentIds;
    }

    public String getPath() {
        return path;
    }

    /**
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    /**
     * Sets the type. This is not the TYPE attribute. Its only for METS-Editor.
     * @param type
     */
    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrder() {
        return this.order;
    }

    /**
     * Sets the order of the <code><mets:div/> </code>. The Items will be shown in the order from lower to higher number.
     * @param order the ordner number that should be set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int compareTo(MCRMETSNode o) {
        // hidden files should be displayed on top.
        return this.getOrder() - o.getOrder();
    }

    /**
     * Creates the Children list if not exist.
     * @return The Children list.
     */
    public List<MCRMETSNode> getChildren() {
        if (children == null) {
            children = new ArrayList<MCRMETSNode>();
        }
        return children;
    }

    public String getStructureType() {
        return structureType;
    }

    /**
     * Sets the Structure type e.g. page, stamp or section. 
     * In xml the attribute is TYPE not structure type.
     * @param structureType the structure type
     */
    public void setStructureType(String structureType) {
        this.structureType = structureType;
    }

    /**
     * Creates the children list if not exist and adds a {@link MCRMETSNode}.
     * @param node the {@link MCRMETSNode} that should be added.
     */
    public void addChild(MCRMETSNode node) {
        if (children == null) {
            children = new ArrayList<MCRMETSNode>();
        }
        getChildren().add(node);
    }

}
