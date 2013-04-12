/*
 * 
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;

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
public class MCRResults implements Iterable<MCRHit> {
    /** The list of MCRHit objects */
    protected ArrayList<MCRHit> hits = new ArrayList<MCRHit>();

    /** The state of the connection */
    private HashMap<String, String> hostconnection = new HashMap<String, String>();

    /**
     * A map containing MCRHit IDs used for and/or operations on two different
     * MCRResult objects
     */
    protected Map<String, MCRHit> map = Collections.synchronizedMap(new HashMap<String, MCRHit>());

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
     * Adds hits to this result.
     * Use {@link #isReadonly()} to verify that this results is not read only.
     * @param hits e.g. other MCRResults instance
     */
    public void addHits(Iterable<MCRHit> hits) {
        for (MCRHit hit : hits) {
            addHit(hit);
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
        if (i >= 0 && i < hits.size()) {
            return hits.get(i);
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
    protected MCRHit getHit(String key) {
        return map.get(key);
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
        while (hits.size() > maxResults && maxResults > 0) {
            MCRHit hit = hits.remove(hits.size() - 1);
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
    public void sortBy(final List<MCRSortBy> sortByList) {
        Collections.sort(hits, new Comparator<MCRHit>() {
            public int compare(MCRHit a, MCRHit b) {
                int result = 0;

                for (int i = 0; result == 0 && i < sortByList.size(); i++) {
                    MCRSortBy sortBy = sortByList.get(i);
                    if (sortBy.getSortOrder()) {
                        result = a.compareTo(sortBy.getField(), b);
                    } else {
                        result = b.compareTo(sortBy.getField(), a);
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
        Element results = new Element("results", MCRConstants.MCR_NAMESPACE);
        results.setAttribute("id", getID());
        results.setAttribute("sorted", Boolean.toString(isSorted()));
        results.setAttribute("numHits", String.valueOf(getNumHits()));

        for (Map.Entry<String, String> entry : hostconnection.entrySet()) {
            Element connection = new Element("hostconnection", MCRConstants.MCR_NAMESPACE);
            connection.setAttribute("host", entry.getKey());
            String msg = entry.getValue();
            if (msg == null) {
                msg = "";
            }
            connection.setAttribute("message", msg);
            if (msg.length() == 0) {
                connection.setAttribute("connection", "true");
            } else {
                connection.setAttribute("connection", "false");
            }
            results.addContent(connection);
        }

        for (int i = min; i <= max; i++) {
            MCRHit hit = getHit(i);
            if (hit != null) {
                results.addContent(hit.buildXML());
            }
        }

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
    protected int merge(Document doc, String hostAlias) {
        Element xml = doc.getRootElement();
        int numHitsBefore = getNumHits();
        int numRemoteHits = Integer.parseInt(xml.getAttributeValue("numHits"));

        List<Element> connectionList = xml.getChildren("hostconnection", MCRConstants.MCR_NAMESPACE);
        for (Element connectionElement : connectionList) {
            String conKey = connectionElement.getAttributeValue("host");
            String conValue = connectionElement.getAttributeValue("message");
            hostconnection.put(conKey, conValue);
        }

        List<Element> hitList = xml.getChildren("hit", MCRConstants.MCR_NAMESPACE);
        hits.ensureCapacity(numHitsBefore + numRemoteHits);
        for (Element hitElement : hitList) {
            MCRHit hit = MCRHit.parseXML(hitElement, hostAlias);
            hits.add(hit);
            map.put(hit.getKey(), hit);
        }
        return getNumHits() - numHitsBefore;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("---- MCRResults ----");
        sb.append("\nNumHits = ").append(getNumHits());
        for (MCRHit hit : hits) {
            sb.append(hit);
        }
        return sb.toString();
    }

    /**
     * Does a logical and of this results hits and other results hits. The hits
     * that are contained in both results are kept, the others are removed from
     * this results list. The data of common hits is combined from both result
     * lists.
     * 
     * @param others
     *            the other result lists
     */
    public static MCRResults intersect(MCRResults... others) {
        //check if result is empty
        for (MCRResults other : others) {
            // x AND {} is always {}
            if (other.getNumHits() == 0) {
                return new MCRResults();
            }
        }
        //result with less hits first to speed up intersect
        Arrays.sort(others, new Comparator<MCRResults>() {

            @Override
            public int compare(MCRResults o1, MCRResults o2) {
                return o1.getNumHits() - o2.getNumHits();
            }
        });
        final MCRResults firstResult = others[0];
        firstResult.fetchAllHits();
        MCRResults totalResult = new MCRResults();
        final List<MCRResults> subResultList = Arrays.asList(others).subList(1, others.length);
        //merge everything together
        for (MCRHit hit : firstResult) {
            boolean complete = true;
            final String key = hit.getKey();
            for (MCRResults other : subResultList) {
                other.fetchAllHits();
                MCRHit otherHit = other.getHit(key);
                if (otherHit == null) {
                    complete = false;
                    break;
                }
                hit.merge(otherHit);
            }
            if (complete) {
                totalResult.addHit(hit);
            }
        }
        return totalResult;
    }

    /**
     * Adds all hits of another result list that are not yet in this result
     * list. Combines the MCRHit data of both result lists.
     * 
     * @param others
     *            the other result lists
     */
    public static MCRResults union(MCRResults... others) {
        MCRResults totalResult = new MCRResults();
        for (MCRResults other : others) {
            if (other != null) {
                other.fetchAllHits();
                for (MCRHit hit : other) {
                    totalResult.addHit(hit);
                }
            }
        }
        return totalResult;
    }

    public Iterator<MCRHit> iterator() {
        return hits.iterator();
    }

    /**
     * Set the state of the connection of a host alias.
     * 
     * @param host
     *            the host alias
     * @param msg
     *            the exception message of the connection or an empty string
     */
    public void setHostConnection(String host, String msg) {
        if (msg == null) {
            msg = "";
        }
        hostconnection.put(host, msg);
    }

    /**
     * @return false if {@link #addHit(MCRHit)} and {@link #merge(Document, String)} are safe operations. 
     * 
     */
    public boolean isReadonly() {
        return false; // default implementation
    }

    public void fetchAllHits() {

    }
}
