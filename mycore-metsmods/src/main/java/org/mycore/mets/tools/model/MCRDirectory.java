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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * This class models a structure/folder within a tree. It may contain {@link MCREntry}s and {@link MCRDirectory}s. 
 * 
 * @author Silvio Hermann (shermann)
 *
 */
public class MCRDirectory implements MCRIMetsSortable, Comparator<MCRIMetsSortable> {
    private String logicalId, label, structureType;

    private List<MCRDirectory> dirs;

    private List<MCREntry> entries;

    int order;

    /**
     * @param logicalId
     * @param label
     * 
     * @param structureType 
     */
    public MCRDirectory(String logicalId, String label, String structureType) {
        this.logicalId = logicalId;
        this.label = label;
        this.structureType = structureType;
        dirs = new Vector<MCRDirectory>();
        entries = new Vector<MCREntry>();
    }

    /**
     * @return the logicalId
     */
    public String getLogicalId() {
        return logicalId;
    }

    /**
     * @param logicalId the logicalId to set
     */
    public void setLogicalId(String id) {
        this.logicalId = id;
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
     * @param e
     */
    public void addEntry(MCREntry e) {
        this.entries.add(e);
    }

    /**
     * @param dir
     */
    public void addDirectory(MCRDirectory dir) {
        this.dirs.add(dir);
    }

    public String toString() {

        return this.getLogicalId() + " (" + order + ")";
    }

    /* (non-Javadoc)
     * @see org.mycore.mets.tools.model.IMetsSortable#asJson()
     */
    public String asJson() {
        StringBuilder buffer = new StringBuilder();

        MCRIMetsSortable[] obj = getOrderedElements();

        buffer.append("\t{ id: '" + label + "', name:'" + label + "', type:'category'" + ", structureType:'" + structureType + "'");
        buffer.append(", children:[\n");

        for (int i = 0; i < obj.length; i++) {
            if (obj[i] instanceof MCREntry) {
                buffer.append(obj[i].asJson());
            }
            if (obj[i] instanceof MCRDirectory) {
                MCRDirectory aDir = (MCRDirectory) obj[i];
                MCRIMetsSortable[] children = aDir.getOrderedElements();

                buffer.append("{ id: '" + aDir.getLabel() + "', name:'" + aDir.getLabel() + "', type:'category'" + ", structureType:'"
                        + aDir.getStructureType() + "'");
                buffer.append(", children:[\n");

                for (int c = 0; c < children.length; c++) {
                    buffer.append(children[c].asJson());
                    if (c < children.length - 1) {
                        buffer.append(",\n");
                    }
                }
                buffer.append("]}\n");
            }
            if (i < obj.length - 1) {
                buffer.append(",\n");
            }
        }
        buffer.append("]}\n");
        String toReturn = buffer.toString();

        return toReturn;
    }

    private MCRIMetsSortable[] getOrderedElements() {
        Vector<MCRIMetsSortable> v = new Vector<MCRIMetsSortable>();
        for (MCRDirectory dir : this.dirs) {
            v.add(dir);
        }

        for (MCREntry e : this.entries) {
            v.add(e);
        }
        MCRIMetsSortable[] obj = v.toArray(new MCRIMetsSortable[0]);
        Arrays.sort(obj, this);
        return obj;
    }

    /* 
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(MCRIMetsSortable arg0, MCRIMetsSortable arg1) {
        if (arg0.getOrder() < arg1.getOrder()) {
            return -1;
        }

        if (arg0.getOrder() > arg1.getOrder()) {
            return 1;
        }

        return 0;
    }
}
