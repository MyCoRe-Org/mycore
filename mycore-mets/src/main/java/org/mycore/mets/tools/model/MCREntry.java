/*
 * $Revision$ $Date$
 * $LastChangedBy$ Copyright 2010 - Thüringer Universitäts- und
 * Landesbibliothek Jena
 * 
 * Mets-Editor is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Mets-Editor is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Mets-Editor. If not, see http://www.gnu.org/licenses/.
 */
package org.mycore.mets.tools.model;

/**
 * @author Silvio Hermann (shermann)
 */
public class MCREntry implements MCRIMetsSortable {
    int order;

    private String physicalId, label, itemId, structureType, orderLabel, path;

    /**
     * @param itemId
     *            the id of the item, to be used in the dijit tree at the client
     *            side
     * @param label
     *            the label of the page, currently not supported, neither by the
     *            dfg viewer or iview2
     * @param path
     *            the path to the file (the href attribute in the
     *            fileGrp/file/Flocat element)
     * @param physicalId
     *            the physical id of the file (see
     *            mets/structMap[@TYPE='PHYSICAL']/div/div/fptr/@ID)
     * @param structureType
     *            the structure type of the entry, usually <cdoe>"page"</code>
     */
    public MCREntry(String itemId, String label, String path, String physicalId, String structureType) {
        this.itemId = itemId;
        this.path = path;
        this.physicalId = physicalId;
        this.label = label;
        this.structureType = structureType;
        this.orderLabel = new String("");
    }

    /**
     * @param itemId
     *            the id of the item, to be used in the dijit tree at the client
     *            side
     * @param label
     *            the label of the page, currently not supported, neither by the
     *            dfg viewer or iview2
     * @param orderLabel
     *            the order label of the entry, e.g. one of I, II, III, IV or
     *            1r, 2v, 3r
     * @param path
     *            the path to the file (the href attribute in the
     *            fileGrp/file/Flocat element)
     * @param physicalId
     *            the physical id of the file (see
     *            mets/structMap[@TYPE='PHYSICAL']/div/div/fptr/@ID)
     * @param structureType
     *            the structure type of the entry, usually <cdoe>"page"</code>
     */
    public MCREntry(String itemId, String label, String orderLabel, String path, String physicalId, String structureType) {
        this(itemId, label, path, physicalId, structureType);
        this.orderLabel = orderLabel;
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order
     *            the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * @return the physicalId
     */
    public String getPhysicalId() {
        return physicalId;
    }

    /**
     * @param physicalId
     *            the physicalId to set
     */
    public void setPhysicalId(String id) {
        this.physicalId = id;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label
     *            the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the structureType
     */
    public String getStructureType() {
        return structureType;
    }

    /**
     * @param structureType
     *            the structureType to set
     */
    public void setStructureType(String structureType) {
        this.structureType = structureType;
    }

    /**
     * @return the orderLabel
     */
    public String getOrderLabel() {
        return orderLabel;
    }

    /**
     * @param orderLabel
     *            the orderLabel to set
     */
    public void setOrderLabel(String orderLabel) {
        this.orderLabel = orderLabel;
    }

    /**
     * @return the itemId
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * @param itemId
     *            the itemId to set
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String toString() {
        return physicalId + " (" + order + ")";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.mets.tools.model.IMetsSortable#asJson()
     */
    public String asJson() {
        String toReturn = "{ \"id\": \"" + itemId + "\", \"path\": \"" + this.path + "\", \"name\":\"" + this.label
                + "\", \"orderLabel\":\"" + this.orderLabel + "\", \"structureType\":\"" + this.structureType + "\", \"type\":\"item\" }";

        return toReturn;
    }
}
