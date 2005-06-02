/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.backend.hibernate;

import java.sql.Timestamp;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.mycore.services.nbn.MCRNBN;
import org.mycore.services.nbn.MCRNBNManager;

import org.hibernate.*;

import java.util.List;

import org.mycore.backend.hibernate.tables.*;

/**
 * Provides persistency functions for managing NBN URNs, * using tables in SQL for persistent storage.
 */
public class MCRHIBNBNManager implements MCRNBNManager {

    // logger
    static Logger logger = Logger.getLogger(MCRHIBNBNManager.class);

    /**
     * Method MCRSQLNBNManager. Creates a new MCRNBNManager.
     */
    public MCRHIBNBNManager()
    {
    }

    private Session getSession()
    {
        return null;
    }

    /**
     * Method reserveURN. Reserves a NBN for later use. In a later step, that NBN can be
     * 		assigned to a document.
     * @param urn the NBN URN to be reserved.
     */
    public void reserveURN(MCRNBN urn) {
	Date now = new Date();

	Session session = getSession();
	Transaction tx = session.beginTransaction();

	MCRNBNS c = new MCRNBNS(
		urn.getNISSandChecksum(),
		null,
		urn.getAuthor(),
		urn.getComment(),
		new Timestamp(now.getTime()),
		null);

	session.update(c);

	tx.commit();
	session.close();
    }

    /**
     * Method getURN. Gets an URN for a given URL
     * @param url the URL of the given document
     * @return MCRNBN the NBN URN for the given URL, or null
     */
    public MCRNBN getURN(String url)
    {
	Session session = getSession();
        List l = session.createQuery("FROM MCRNBN WHERE URL = " + url).list();
	session.close();
	if(l.size() < 1) return null;
	MCRNBN nbn = new MCRNBN(MCRNBN.getLocalPrefix() + ((MCRNBNS)l.get(0)).getNiss());
	return nbn;
    }

    /**
     * Method setURL. Sets the URL for the NBN URN given. This is the URL that
     * 		the NBN points to. The NBN has to be already reserved.
     * @param urn the NBN URN that represents the URL
     * @param url the URL the NBN points to
     */
    public void setURL(MCRNBN urn, String url) {
	Session session = getSession();
        List l = session.createQuery("FROM MCRNBN WHERE URL = " + urn.getURL()).list();
	if(l.size() < 1) return;
	MCRNBNS n = (MCRNBNS)l.get(0);

	n.setUrl(url);
	session.update(n);
	session.close();
    }

    /**
     * Method getURL. Gets the URL for the NBN URN given. This is the URL that
     * 		the NBN points to. If there is no URL for this NBN, the
     * 		method returns null.
     * @param urn the NBN URN that represents a URL
     * @return String the URL the NBN points to, or null
     */
    public String getURL(MCRNBN urn) {
	Session session = getSession();
        List l = session.createQuery("FROM MCRNBN WHERE URL = " + urn.getURL()).list();
	if(l.size() < 1) return null;
	MCRNBNS n = (MCRNBNS)l.get(0);
	return n.getUrl();
    }

    /**
     * Method getAuthor. Gets the Author for the NBN URN given.
     * @param urn the NBN URN that represents a URL
     * @return String the author
     */
    public String getAuthor(MCRNBN urn) {
	Session session = getSession();
        List l = session.createQuery("FROM MCRNBN WHERE URL = " + urn.getURL()).list();
	if(l.size() < 1) return null;
	MCRNBNS n = (MCRNBNS)l.get(0);
	return n.getAuthor();
    }

    /**
     * Method getComment. Gets the Comment for the NBN URN given.
     * @param urn the NBN URN that represents a URL
     * @return String the Comment
     */
    public String getComment(MCRNBN urn) {
	Session session = getSession();
        List l = session.createQuery("FROM MCRNBN WHERE URL = " + urn.getURL()).list();
	if(l.size() < 1) return null;
	MCRNBNS n = (MCRNBNS)l.get(0);
	return new String(n.getComment());
    }

    /**
     * Method getDate. Gets the timestamp for the NBN
     * @param urn the NBN
     * @return GregorianCalendar the date
     */
    public GregorianCalendar getDate(MCRNBN urn) {
	Session session = getSession();
        List l = session.createQuery("FROM MCRNBN WHERE URL = " + urn.getURL()).list();
	if(l.size() < 1) return null;
	MCRNBNS n = (MCRNBNS)l.get(0);
        GregorianCalendar greg = new GregorianCalendar();
	greg.setTime(n.getDate());
	return greg;
    }

    /**
     * Method removeURN. Removes a stored NBN URN from the persistent datastore.
     * @param urn the NBN URN that should be removed
     */
    public void removeURN(MCRNBN urn) {
	Session session = getSession();
	Transaction tx = session.beginTransaction();

	session.delete("select from MCRNBN where NISS = "+urn.getNISSandChecksum());

	tx.commit();
	session.close();
    }

    /**
     * Method listReservedURNs. Returns all URNs that are reserved for later use with a document.
     * @return a Set containing the URNs
     */
    public Set listReservedURNs() {
	Session session = getSession();
        List l = session.createQuery("FROM MCRNBN where URL = NULL").list();
	Set results = new HashSet();
	int t;
	for(t=0;t<l.size();t++) {
	    MCRNBNS n = (MCRNBNS)l.get(t);
	    results.add(n.getUrl());
	}
	return results;
    }

    /**
     * Method getDocumentId. Gets the document id for the NBN
     * @param urn the NBN
     * @return String the document id
     */
    public String getDocumentId(MCRNBN urn) {
	Session session = getSession();
	Transaction tx = session.beginTransaction();

        List l = session.createQuery("from MCRNBN where NISS = "+urn.getNISSandChecksum()).list();
        if(l.size() < 1)
            return null;
        MCRNBNS n = (MCRNBNS)l.get(0);

	tx.commit();
	session.close();

	return n.getDocumentid();
    }

    /**
     * Sets the document id for the NBN URN given.
     *
     * @param urn the NBN URN that represents the URL
     * @param documentId the document id the NBN points to
     **/
    public void setDocumentId(MCRNBN urn, String documentId) {

	Session session = getSession();
        List l = session.createQuery("FROM MCRNBN WHERE NISS = " + urn.getNISSandChecksum()).list();
	if(l.size() < 1) return;
	MCRNBNS n = (MCRNBNS)l.get(0);

	n.setDocumentid(documentId);

	session.update(n);
	session.close();
    }

    /**
     * Finds the urn for a given document id
     * @param documentId the document id
     * @return the nbn or null
     */
    public MCRNBN getNBNByDocumentId(String documentId) {
	Session session = getSession();
        List l = session.createQuery("FROM MCRNBN WHERE DOCUMENTID = " + documentId).list();
	if(l.size() < 1) return null;
	MCRNBNS n = (MCRNBNS)l.get(0);
	session.close();
	return new MCRNBN(MCRNBN.getLocalPrefix() + n.getNiss());
    }

    /**
     * Method listURNs. Returns all URNs that match the given pattern. The pattern
     * 		may be null to select all stored URNs, or may be a pattern
     * 		containing '*' or '?' wildcard characters.
     * @param pattern the pattern the URNs should match, or null
     * @return a Map containing the matched URNs as keys, and their URLs as values
     */
    public Map listURNs(String pattern) {
	Map results = new HashMap();
	Session session = getSession();
        List l;

	if (pattern != null) {
	    String sqlPattern = pattern.replace('?', '_').replace('*', '%');
	    l = session.createQuery("FROM MCRNBN WHERE NISS like " + sqlPattern).list();
	} else {
	    l = session.createQuery("FROM MCRNBN").list();
	}
	int t;
	for(t=0;t<l.size();t++) {
	    MCRNBNS n = (MCRNBNS)l.get(t);
	    results.put(n.getNiss(), n.getUrl());
	}
	session.close();
	return results;
    }
}
