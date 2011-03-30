/* $Revision: 3033 $ 
 * $Date: 2010-10-22 13:41:12 +0200 (Fri, 22 Oct 2010) $ 
 * $LastChangedBy: thosch $
 * Copyright 2010 - Thüringer Universitäts- und Landesbibliothek Jena
 *  
 * Mets-Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mets-Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mets-Editor.  If not, see http://www.gnu.org/licenses/.
 */
package org.mycore.mets.tools.model;

/**
 * @author Silvio Hermann (shermann)
 *
 */
public class MCREntry implements MCRIMetsSortable {
    int order;

    private String physicalId, label, itemId, structureType, orderLabel;

    public MCREntry(String itemId, String physical, String label, String structureType) {
        this.physicalId = physical;
        this.label = label;
        this.itemId = itemId;
        this.structureType = structureType;
        this.orderLabel = new String("--");
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order the order to set
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
     * @param physicalId the physicalId to set
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
     * @param label the label to set
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
     * @param structureType the structureType to set
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
     * @param orderLabel the orderLabel to set
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
     * @param itemId the itemId to set
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String toString() {
        return physicalId + " (" + order + ")";
    }

    /* (non-Javadoc)
     * @see org.mycore.mets.tools.model.IMetsSortable#asJson()
     */
    public String asJson() {
        String toReturn = "{ id: '" + itemId + "', name:'" + label + "',orderLabel:'"+ this.orderLabel + "', structureType:'" + structureType + "', type:'item' }";

        return toReturn;
    }
}
