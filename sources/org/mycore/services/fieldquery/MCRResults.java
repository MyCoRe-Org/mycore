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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.jdom.Document;
import org.jdom.Element;

/**
 * This class represents the results of a query performed by MCRSearcher.
 * Searchers add the hits using the addHit() method. Clients can get the hits,
 * sort the entries and do merge/and/or operations on two different result sets.
 * 
 * Searches may add the same hit (hit with the same ID) more than once. If the
 * hit already is contained in the result set, the data of both objects is
 * merged.
 * 
 * @see MCRHit
 * 
 * @author Arne Seifert
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRResults {
    /** The list of MCRHit objects */
    private ArrayList hits = new ArrayList();

    /**
     * A map containing MCRHit IDs used for and/or operations on two different
     * MCRResult objects
     */
    private HashMap map = new HashMap();

    /** If true, this results are already sorted */
    private boolean isSorted = false;

    /** The unique ID of this result set */
    private String id;

    private static Random random = new Random(System.currentTimeMillis());

    /**
     * Creates a new, empty MCRResults.
     */
    public MCRResults() {
        id = Long.toString(random.nextLong(), 36) + Long.toString(System.currentTimeMillis(), 36);
    }

    /**
     * Returns the unique ID of this result set
     * 
     * @return the unique ID of this result set
     */
    public String getID() {
        return id;
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
        String key = hit.getKey();
        MCRHit existing = getHit(key);

        if (existing == null) {
            // This is a new entry with new ID
            hits.add(hit);
            map.put(key, hit);
        } else {
            // Merge data of existing hit with new one with the same ID
            existing.merge(hit);
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
        }
        return null;
    }

    /**
     * Returns the MCRHit with the given key, if it is in this results.
     * 
     * @param key
     *            the key of the hit
     * @return the MCRHit, if it exists
     */
    private MCRHit getHit(String key) {
        if (map.containsKey(key)) {
            return (MCRHit) (map.get(key));
        }
        return null;
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
     * Cuts the result list to the given maximum size, if more hits are present.
     * 
     * @param maxResults
     *            the number of results to be left
     */
    public void cutResults(int maxResults) {
        while ((hits.size() > maxResults) && (maxResults > 0)) {
            MCRHit hit = (MCRHit) (hits.remove(hits.size() - 1));
            map.remove(hit.getKey());
        }
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
     * Sorts this results by the given sort criteria.
     * 
     * @param sortByList
     *            a List of MCRSortBy objects
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
     * Returns a XML element containing hits and their data
     * 
     * @param min
     *            the position of the first hit to include in output
     * @param max
     *            the position of the last hit to include in output
     * @return a 'results' element with attributes 'sorted' and 'numHits' and
     *         hit child elements
     */
    public Element buildXML(int min, int max) {
        Element results = new Element("results", MCRFieldDef.mcrns);
        results.setAttribute("id", getID());
        results.setAttribute("sorted", Boolean.toString(isSorted()));
        results.setAttribute("numHits", String.valueOf(getNumHits()));

        for (int i = min; i <= max; i++)
            results.addContent(((MCRHit) hits.get(i)).buildXML());

        return results;
    }

    /**
     * Returns a XML element containing all hits and their data
     * 
     * @return a 'results' element with attributes 'sorted' and 'numHits' and
     *         hit child elements
     */
    public Element buildXML() {
        return buildXML(0, getNumHits() - 1);
    }

    /**
     * Merges the hits from a remote query to this results
     * 
     * @param doc
     *            the results from the remote query as XML document
     * @param hostAlias
     *            the alias of the host where the hits come from
     * @return the number of hits added
     */
    int merge(Document doc, String hostAlias) {
        Element xml = doc.getRootElement();
        int numHitsBefore = this.getNumHits();
        int numRemoteHits = Integer.parseInt(xml.getAttributeValue("numHits"));

        List hitList = xml.getChildren();
        hits.ensureCapacity(numHitsBefore + numRemoteHits);

        for (Iterator it = hitList.iterator(); it.hasNext();) {
            Element hitElement = (Element) (it.next());
            MCRHit hit = MCRHit.parseXML(hitElement, hostAlias);
            hits.add(hit);
            map.put(hit.getKey(), hit);
        }
        return this.getNumHits() - numHitsBefore;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("---- MCRResults ----");
        sb.append("\nNumHits = ").append(this.getNumHits());
        for (int i = 0; i < hits.size(); i++)
            sb.append(hits.get(i));
        return sb.toString();
    }

    /**
     * Does a logical and of this results hits and other results hits. The hits
     * that are contained in both results are kept, the others are removed from
     * this results list. The data of common hits is combined from both result
     * lists.
     * 
     * @param other
     *            the other result list
     */
    public void and(MCRResults other) {
        // x AND {} is always {}
        if (other.getNumHits() == 0) {
            map.clear();
            hits.clear();
            return;
        }

        int numHits = this.getNumHits();
        for (int i = 0; i < numHits; i++) {
            MCRHit a = this.getHit(i);
            String key = a.getKey();
            MCRHit b = other.getHit(key);

            if (b == null) {
                map.remove(key);
                hits.remove(i--);
                numHits--;
            } else
                a.merge(b);
        }
    }

    /**
     * Adds all hits of another result list that are not yet in this result
     * list. Combines the MCRHit data of both result lists.
     * 
     * @param other
     *            the other result list
     */
    public void or(MCRResults other) {
        int numHits = other.getNumHits();
        for (int i = 0; i < numHits; i++)
            this.addHit(other.getHit(i));
    }
}
