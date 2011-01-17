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
 * @author Silvio Hermann (shermann)
 *
 */
public class MetsTree implements Comparator<IMetsSortable> {
    private List<Directory> dirs;

    private List<Entry> entries;

    private String derivate, docType,docTitle;

    public MetsTree(String derivate) {
        dirs = new Vector<Directory>();
        entries = new Vector<Entry>();
        this.derivate = derivate;
        this.docTitle = derivate;
        this.docType = "''";
    }

    /**
     * @param e
     */
    public void addEntry(Entry e) {
        this.entries.add(e);
    }

    /**
     * @param dir
     */
    public void addDirectory(Directory dir) {
        this.dirs.add(dir);
    }

    /**
     * @return the docType
     */
    public String getDocType() {
        return docType;
    }

    /**
     * @param docType the docType to set
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
     * @param docTitle the docTitle to set
     */
    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public String asJson() {
        StringBuilder buffer = new StringBuilder();
        IMetsSortable[] elements = getOrderedElements();

        buffer.append("{identifier: 'id',label: 'name',items: [\n");
        buffer.append("{id: '" + this.derivate + "', name:'" + this.docTitle +"', structureType:'" + this.docType + "',children:[\n");

        for (int i = 0; i < elements.length; i++) {
            buffer.append(elements[i].asJson());
            if (i < elements.length - 1) {
                buffer.append(",\n");
            }
        }
        buffer.append("]}\n]}");

        return buffer.toString();
    }

    private IMetsSortable[] getOrderedElements() {
        Vector<IMetsSortable> v = new Vector<IMetsSortable>();
        for (Directory dir : this.dirs) {
            v.add(dir);
        }

        for (Entry e : this.entries) {
            v.add(e);
        }
        IMetsSortable[] obj = v.toArray(new IMetsSortable[0]);
        Arrays.sort(obj, this);
        return obj;
    }

    public int compare(IMetsSortable arg0, IMetsSortable arg1) {
        if (arg0.getOrder() < arg1.getOrder()) {
            return -1;
        }

        if (arg0.getOrder() > arg1.getOrder()) {
            return 1;
        }

        return 0;
    }
}
