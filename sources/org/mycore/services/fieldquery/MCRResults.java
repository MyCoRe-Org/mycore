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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 * This class represents the results of a query performed by MCRSearcher.
 * Searchers add the hits using the addHit() method and finally call
 * setCompleted() when all hits have been added. Clients can get the hits, sort
 * the entries and do merge/and/or operations on two different result sets.
 * 
 * @see MCRSearcher
 * 
 * @author Arne Seifert
 * @author Frank Lützenkirchen
 */
public class MCRResults {
    /** The number of hits * */
    private int numHits = 0;

    /** The list of MCRHit objects * */
    private List hits = new LinkedList();

    /** A map containing MCRHit IDs used for and/or operations * */
    private HashMap map = new HashMap();

    /** If true, this results are already sorted * */
    private boolean isSorted = false;

    /** If true, all hits are read from the backend * */
    private boolean isComplete = false;

    /**
     * Creates a new, empty MCRResults.
     */
    public MCRResults() {
    }

    /**
     * Adds a hit. If there is already a hit with the same ID, the sort data and
     * meta data of both hits are merged and the merged hit replaces the
     * existing hit.
     * 
     * @param hit
     *            the MCRHit to add
     */
    public void addHit(MCRHit hit) {
        if (isComplete()) {
            throw new IllegalStateException();
        }

        if (!map.containsKey(hit.getID())) {
            // This is a new entry with new ID
            hits.add(hit);
            map.put(hit.getID(), null);
        } else {
            // Merge data of existing hit with new one with the same ID
            MCRHit existing = getHit(hit.getID());
            MCRHit merged = MCRHit.buildMergedHitData(hit, existing);
            hits.remove(existing);
            hits.add(merged);
        }
    }

    /**
     * Gets a single MCRHit. As long as isSorted() returns false, the order of
     * the hits is natural order.
     * 
     * @param i
     *            the position of the hit.
     * @return the hit at this position, or null if position is out of bounds
     */
    public MCRHit getHit(int i) {
        if ((i > 0) && (i < hits.size())) {
            return (MCRHit) hits.get(i);
        } else {
            return null;
        }
    }

    /**
     * Returns the MCRHit with the given ID, if it is in this results.
     * 
     * @param ID
     *            the ID of the hit
     * @return the MCRHit, if it exists
     */
    public MCRHit getHit(String ID) {
        if (map.containsKey(ID)) {
            return (MCRHit) (map.get(ID));
        } else {
            return null;
        }
    }

    /**
     * Returns the number of hits currently in this results
     * 
     * @return the number of hits
     */
    public int getNumHits() {
        return (isComplete ? numHits : hits.size());
    }

    /**
     * When the searcher added all hits, it MUST call this method to indicate
     * that all hits have been added.
     */
    public void setComplete() {
        isComplete = true;
        numHits = hits.size();
    }

    /**
     * If true, all hits have been added by the searcher and the result list is
     * complete.
     * 
     * @return true, if result list is complete
     */
    public boolean isComplete() {
        return isComplete;
    }

    /**
     * The searcher should set this to true, if the hits already have been added
     * in sorted order.
     * 
     * @param value
     *            true, if sorted, false otherwise
     */
    public void setSorted(boolean value) {
        isSorted = value;
    }

    /**
     * Returns true if this result list is currently sorted
     * 
     * @return true if this result list is currently sorted
     */
    public boolean isSorted() {
        return isSorted;
    }

    /**
     * Sorts the hits using the given list of fields
     * 
     * @param fields
     *            a list of MCRSearchField objects that are used as sort
     *            criteria
     */
    public void sort(final List fields) {
        Collections.sort(this.hits, new Comparator() {
            public int compare(Object oa, Object ob) {
                MCRHit a = (MCRHit) oa;
                MCRHit b = (MCRHit) ob;

                int result = 0;

                for (int i = 0; (result == 0) && (i < fields.size()); i++) {
                    MCRSearchField field = (MCRSearchField) (fields.get(i));
                    String va = a.getSortData().getProperty(field.getName());
                    String vb = b.getSortData().getProperty(field.getName());

                    if ((va == null) || (va.trim().length() == 0)) {
                        result = (((vb == null) || (vb.trim().length() == 0)) ? 0 : (-1));
                    } else if ((vb == null) || (vb.trim().length() == 0)) {
                        result = (((va == null) || (va.trim().length() == 0)) ? 0 : 1);
                    } else if ("decimal".equals(field.getDataType())) {
                        result = (int) ((Double.parseDouble(va) - Double.parseDouble(vb)) * 10.0);
                    } else if ("integer".equals(field.getDataType())) {
                        result = (int) (Long.parseLong(va) - Long.parseLong(vb));
                    } else {
                        result = va.compareTo(vb);
                    }

                    if (field.getSortOrder() == MCRSearchField.DESCENDING) {
                        result *= -1;
                    }
                }

                return result;
            }
        });

        setSorted(true);
    }

    /**
     * Returns a XML document containing all hits and their data
     * 
     * @return a JDOM document of this results
     */
    public Document buildXML() {
        Document doc = new Document();
        Element root = new Element("mcrresults");
        doc.setRootElement(root);
        root.setAttribute("sorted", Boolean.toString(isSorted()));

        for (int i = 0; i < getNumHits(); i++)
            root.addContent(((MCRHit) hits.get(i)).buildXML());

        return doc;
    }

    public String toString() {
        XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());

        return out.outputString(buildXML());
    }

    /**
     * Returns a new MCRResults that only contains those hits that are members
     * of both source MCRResults objects. The compare is based on the ID of the
     * hits.
     * 
     * @param a
     *            the first result list
     * @param b
     *            the other result list
     * @return the new result list
     */
    public static MCRResults and(MCRResults a, MCRResults b) {
        MCRResults res = new MCRResults();

        for (int i = 0; i < a.numHits; i++) {
            MCRHit hitA = a.getHit(i);
            MCRHit hitB = b.getHit(hitA.getID());

            if (hitB != null) {
                res.addHit(MCRHit.buildMergedHitData(hitA, hitB));
            }
        }

        res.setComplete();

        return res;
    }

    /**
     * Returns a new MCRResults that contains those hits that are members of at
     * least one of the source MCRResults objects. The compare is based on the
     * ID of the hits.
     * 
     * @param a
     *            the first result list
     * @param b
     *            the other result list
     * @return the new result list
     */
    public static MCRResults or(MCRResults a, MCRResults b) {
        MCRResults res = new MCRResults();

        for (int i = 0; i < a.numHits; i++) {
            MCRHit hitA = a.getHit(i);
            MCRHit hitB = b.getHit(hitA.getID());

            MCRHit hitC = MCRHit.buildMergedHitData(hitA, hitB);
            res.addHit(hitC);
        }

        for (int i = 0; i < b.numHits; i++)
            if (!res.map.containsKey(b.getHit(i).getID())) {
                res.addHit(b.getHit(i));
            }

        res.setComplete();

        return res;
    }

    /**
     * Returns a new MCRResults that contains all hits of both source MCRResults
     * objects. No compare is done, it is assumed that the two lists do not have
     * common members.
     * 
     * @param a
     *            the first result list
     * @param b
     *            the other result list
     * @return the new result list
     */
    public static MCRResults merge(MCRResults a, MCRResults b) {
        MCRResults merged = new MCRResults();

        for (int i = 0; i < a.numHits; i++)
            merged.addHit(a.getHit(i));

        for (int i = 0; i < b.numHits; i++)
            merged.addHit(b.getHit(i));

        merged.setComplete();

        return merged;
    }
}
