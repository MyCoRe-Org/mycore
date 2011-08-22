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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * @author Silvio Hermann (shermann)
 */
public class MCRMetsTree implements Comparator<MCRIMetsSortable> {
    private List<MCRDirectory> dirs;

    private List<MCREntry> entries;

    private String derivate, docType, docTitle;

    public MCRMetsTree(String derivate) {
        dirs = new Vector<MCRDirectory>();
        entries = new Vector<MCREntry>();
        this.derivate = derivate;
        this.docTitle = derivate;
        this.docType = "''";
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

    /**
     * @return the docType
     */
    public String getDocType() {
        return docType;
    }

    /**
     * @param docType
     *            the docType to set
     */
    public void setDocType(String docType) {
        this.docType = docType;
    }

    /**
     * @return the docTitle
     */
    public String getDocTitle() {
        return docTitle;
    }

    /**
     * @param docTitle
     *            the docTitle to set
     */
    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public String asJson() {
        StringBuilder buffer = new StringBuilder();
        MCRIMetsSortable[] elements = getOrderedElements();

        buffer.append("{\"identifier\": \"id\", \"label\": \"name\", \"items\": [\n");
        buffer.append("{\"id\": \"" + this.derivate + "\", \"name\":\"" + this.docTitle + "\", \"structureType\":\"" + this.docType
                + "\", \"type\":\"category\", \"children\":[\n");

        for (int i = 0; i < elements.length; i++) {
            buffer.append(elements[i].asJson());
            if (i < elements.length - 1) {
                buffer.append(",\n");
            }
        }
        buffer.append("]}\n]}");

        return buffer.toString();
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
