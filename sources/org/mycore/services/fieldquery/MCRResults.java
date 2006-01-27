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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * This class represents the results of a query performed by MCRSearcher.
 * Searchers add the hits using the addHit() method. Clients can get the hits,
 * sort the entries and do merge/and/or operations on two different result sets.
 * 
 * Searches may add the same hit (hit with the same ID) more than once. If the hit
 * already is contained in the result set, the data of both objects is merged. 
 * 
 * @see MCRHit
 * 
 * @author Arne Seifert
 * @author Frank Lützenkirchen
 */
public class MCRResults {
    /** The list of MCRHit objects */
    private List hits = new ArrayList();

    /** A map containing MCRHit IDs used for and/or operations on two different MCRResult objects */
    private HashMap map = new HashMap();

    /** If true, this results are already sorted */
    private boolean isSorted = false;

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
        String ID = hit.getID();
        if (!map.containsKey(ID)) {
            // This is a new entry with new ID
            hits.add(hit);
            map.put(ID, hit);
        } else {
            // Merge data of existing hit with new one with the same ID
            MCRHit existing = getHit(ID);
            MCRHit merged = MCRHit.merge(hit, existing);
            hits.set(hits.indexOf(existing), merged);
            map.put(ID, merged);
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
        if ((i >= 0) && (i < hits.size())) {
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
        return hits.size();
    }

    /**
     * The searcher must set this to true, if the hits already have been added
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
     * Convenience method to sort results by up to three differend fields.
     * Each parameter may be null, so you can sort by only one or two fields
     * if you like.
     * 
     * @param first the first field to sort by
     * @param second the second field to sort by
     * @param third the third field to sort by
     */
    public void sortBy(MCRSortBy first, MCRSortBy second, MCRSortBy third) {
        List sortByList = new ArrayList();
        if (first != null)
            sortByList.add(first);
        if (second != null)
            sortByList.add(second);
        if (third != null)
            sortByList.add(third);
        sortBy(sortByList);
    }

    /**
     * Sorts this results by the given sort criteria.
     * 
     * @param sortByList a List of MCRSortBy objects
     */
    public void sortBy(final List sortByList) {
        Collections.sort(this.hits, new Comparator() {
            public int compare(Object oa, Object ob) {
                MCRHit a = (MCRHit) oa;
                MCRHit b = (MCRHit) ob;

                int result = 0;

                for (int i = 0; (result == 0) && (i < sortByList.size()); i++) {
                    MCRSortBy sortBy = (MCRSortBy) (sortByList.get(i));
                    result = a.compareTo(sortBy.getField(), b);
                    if (sortBy.getSortOrder() == MCRSortBy.DESCENDING) {
                        result *= -1;
                    }
                }

                return result;
            }
        });

        setSorted(true);
    }

    /**
     * Returns a XML element containing all hits and their data
     * 
     * @return a 'results' element with attributes 'sorted' and 'numHits' and hit child elements
     */
    public Element buildXML() {
        Namespace mcrns = Namespace.getNamespace("mcr", "http://www.mycore.org/");

        Element results = new Element("results", mcrns);
        results.setAttribute("sorted", Boolean.toString(isSorted()));
        results.setAttribute("numHits", String.valueOf(getNumHits()));

        for (int i = 0; i < getNumHits(); i++)
            results.addContent(((MCRHit) hits.get(i)).buildXML());

        return results;
    }

    public String toString() {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(buildXML());
    }

    /**
     * Returns a new MCRResults that only contains those hits that are members
     * of both source MCRResults objects. The compare is based on the ID of the
     * hits. The data of each single hit is merged from both results. 
     * 
     * @param a
     *            the first result list
     * @param b
     *            the other result list
     * @return the new result list
     */
    public static MCRResults and(MCRResults a, MCRResults b) {
        MCRResults res = new MCRResults();

        for (int i = 0; i < a.getNumHits(); i++) {
            MCRHit hitA = a.getHit(i);
            MCRHit hitB = b.getHit(hitA.getID());

            if (hitB != null) {
                res.addHit(MCRHit.merge(hitA, hitB));
            }
        }

        return res;
    }

    /**
     * Returns a new MCRResults that contains those hits that are members of at
     * least one of the source MCRResults objects. The compare is based on the
     * ID of the hits. The data of each single hit is merged from both results.
     * 
     * @param a
     *            the first result list
     * @param b
     *            the other result list
     * @return the new result list
     */
    public static MCRResults or(MCRResults a, MCRResults b) {
        MCRResults res = new MCRResults();

        for (int i = 0; i < a.getNumHits(); i++) {
            MCRHit hitA = a.getHit(i);
            MCRHit hitB = b.getHit(hitA.getID());

            MCRHit hitC = MCRHit.merge(hitA, hitB);
            res.addHit(hitC);
        }

        for (int i = 0; i < b.getNumHits(); i++)
            if (!res.map.containsKey(b.getHit(i).getID())) {
                res.addHit(b.getHit(i));
            }

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

        for (int i = 0; i < a.getNumHits(); i++)
            merged.addHit(a.getHit(i));

        for (int i = 0; i < b.getNumHits(); i++)
            merged.addHit(b.getHit(i));

        return merged;
    }
}
