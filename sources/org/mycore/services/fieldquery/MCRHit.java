/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.services.fieldquery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Represents a single result hit of a query. The hit has an ID which is the
 * MCRObjectID of the document that matched the query. The hit may have
 * MCRFieldValue objects set for sorting data or representing hit metadata like
 * score or rank.
 * 
 * If the same hit (hit with same ID) is in different result sets A and B, the
 * data of the hit objects is merged. The hit sort data is copied from one of
 * the hits that contains sort data. There is only on sort data set for each
 * hit. The hit metadata of both hits is preserved and copied from both hits, so
 * there can be multiple metadata sets from different searches for the same hit.
 * 
 * @see MCRResults
 * @author Arne Seifert
 * @author Frank Lützenkirchen
 */
public class MCRHit {
    /** The ID of this object that matched the query */
    private String id;

    /** List of MCRFieldValue objects that are hit metadata */
    private List metaData = new ArrayList();

    /** List of MCRFieldValue objects that are sort data */
    private List sortData = new ArrayList();

    /** Map from field to field value, used for sorting */
    private Map sortValues = new HashMap();

    /**
     * Creates a new result hit with the given object ID
     * 
     * @param id
     *            the ID of the object that matched the query
     */
    public MCRHit(String id) {
        this.id = id;
    }

    /**
     * Returns the ID of the object that matched the query
     * 
     * @return the ID of the object that matched the query
     */
    public String getID() {
        return id;
    }

    /**
     * Adds hit metadata like score or rank
     * 
     * @param value
     *            the value of the metadata field
     */
    public void addMetaData(MCRFieldValue value) {
        metaData.add(value);
        sortValues.put(value.getField(), value.getValue());
    }

    /**
     * Adds data for sorting this hit
     * 
     * @param fieldValue
     *            the value of a sortable search field
     */
    public void addSortData(MCRFieldValue fieldValue) {
        sortData.add(fieldValue);

        String value = fieldValue.getValue();
        MCRFieldDef field = fieldValue.getField();

        // If field is repeated (multiple values for same field):
        // for text fields, combine all values for sorting
        // for dates and numbers, use only first value for sorting

        if (sortValues.containsKey(field)) {
            if ("text name identifier".indexOf(field.getDataType()) >= 0) {
                String oldValue = (String) (sortValues.get(field));
                String newValue = oldValue.concat(" ").concat(value);
                sortValues.put(field, newValue);
            }
        } else {
            sortValues.put(field, value);
        }
    }

    /**
     * Compares this hit with another hit by comparing the value of the given
     * search field. Used for sorting results.
     * 
     * @param field
     *            the field to compare
     * @param other
     *            the other hit to compare with
     * @return 0 if the two hits are equal, a positive value if this hit is
     *         "greater" than the other, a negative value if this hit is
     *         "smaller" than the other
     * 
     * @see MCRResults#sortBy(List)
     */
    int compareTo(MCRFieldDef field, MCRHit other) {
        String va = (String) (this.sortValues.get(field));
        String vb = (String) (other.sortValues.get(field));

        if ((va == null) || (va.trim().length() == 0)) {
            return (((vb == null) || (vb.trim().length() == 0)) ? 0 : (-1));
        } else if ((vb == null) || (vb.trim().length() == 0)) {
            return (((va == null) || (va.trim().length() == 0)) ? 0 : 1);
        } else if ("decimal".equals(field.getDataType())) {
            return (int) ((Double.parseDouble(va) - Double.parseDouble(vb)) * 10.0);
        } else if ("integer".equals(field.getDataType())) {
            return (int) (Long.parseLong(va) - Long.parseLong(vb));
        } else {
            return va.compareTo(vb);
        }
    }

    /**
     * Combines the data of two MCRHit objects with the same ID, but from
     * different searchers result sets by copying the sort data and hit metadata
     * of both objects.
     * 
     * @param a
     *            the first hit from the first searcher
     * @param b
     *            the other hit from the other searcher
     */
    static MCRHit merge(MCRHit a, MCRHit b) {
        // If there is nothing to merge, return existing hit
        if (b == null) {
            return a;
        }
        if (a == null) {
            return b;
        }

        // Copy ID
        MCRHit c = new MCRHit(a.getID());

        // Copy sortData
        c.sortData.addAll(a.sortData.isEmpty() ? b.sortData : a.sortData);
        c.sortValues.putAll(a.sortValues);
        c.sortValues.putAll(b.sortValues);

        // Copy metaData
        c.metaData.addAll(a.metaData);
        if ((a.metaData.size() > 0) && (b.metaData.size() > 0))
            c.metaData.add(null); // used as a delimiter
        c.metaData.addAll(b.metaData);

        return c;
    }

    public String toString() {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(buildXML());
    }

    /**
     * Creates a XML representation of this hit and its sort data and meta data
     * 
     * @return a 'hit' element with attribute 'id', optionally one 'sortData'
     *         child element and multiple 'metaData' child elements
     */
    public Element buildXML() {
        Element eHit = new Element("hit", MCRFieldDef.mcrns);
        eHit.setAttribute(new Attribute("id", this.id));

        if (!sortData.isEmpty()) {
            Element eSort = new Element("sortData", MCRFieldDef.mcrns);
            eHit.addContent(eSort);

            for (int i = 0; i < sortData.size(); i++) {
                MCRFieldValue fv = (MCRFieldValue) (sortData.get(i));
                eSort.addContent(fv.buildXML());
            }
        }

        Element eMeta = null;
        int count = 0;

        for (int i = 0; i < metaData.size(); i++) {
            MCRFieldValue fv = (MCRFieldValue) (metaData.get(i));
            if ((i == 0) || (fv == null)) {
                if ((eMeta != null) && (count == 0))
                    continue;

                eMeta = new Element("metaData", MCRFieldDef.mcrns);
                eHit.addContent(eMeta);
                count = 0;
                if (i > 0)
                    continue;
            }

            eMeta.addContent(fv.buildXML());
            count++;
        }

        return eHit;
    }
}
