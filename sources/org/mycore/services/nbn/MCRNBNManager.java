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

package org.mycore.services.nbn;

import java.util.*;
import org.mycore.common.*;

/**
 * Provides persistency functions for managing NBN URNs.
 * This is still work in progress. 
 *
 * @author Frank Lützenkirchen
 * @author Werner Greßhoff
 * @version $Revision$ $Date$
 */
public interface MCRNBNManager
{
	
	/**
	 * Reserves a NBN for later use. In a later step, that NBN can be
	 * assigned to a document.
	 *
	 * @param urn the NBN URN to be reserved. 
	 **/ 
	public void reserveURN( MCRNBN urn );
	
	/**
	 * Gets an URN for a given URL
	 *
	 * @param url the URL of the given document
	 * @return the NBN URN fot the given URL, or null
	 **/ 
	public MCRNBN getURN(String url);
	
	/**
	 * Sets the URL for the NBN URN given. This is the URL that
	 * the NBN points to. The NBN has to be already reserved.
	 *
	 * @param urn the NBN URN that represents the URL
	 * @param url the URL the NBN points to
	 **/
	public void setURL(MCRNBN urn, String url);
	
	/**
	 * Gets the URL for the NBN URN given. This is the URL that
	 * the NBN points to. If there is no URL for this NBN, the 
	 * method returns null.
	 *
	 * @param urn the NBN URN that represents a URL
	 * @return the URL the NBN points to, or null
	 **/
	public String getURL(MCRNBN urn);
	
	/**
	 * Method getAuthor. Gets the Author for the NBN URN given.
	 * @param urn the NBN URN that represents a URL
	 * @return String the author
	 */
	public String getAuthor(MCRNBN urn);

	/**
	 * Method getComment. Gets the Comment for the NBN URN given.
	 * @param urn the NBN URN that represents a URL
	 * @return String the Comment
	 */
	public String getComment(MCRNBN urn);
	
	/**
	 * Method getDate. Gets the timestamp for the NBN
	 * @param urn the NBN
	 * @return GregorianCalendar the date
	 */
	public GregorianCalendar getDate(MCRNBN urn);

	/**
	 * Removes a stored NBN URN from the persistent datastore.
	 *
	 * @param urn the NBN URN that should be removed
	 **/
	public void removeURN(MCRNBN urn);
	
	/**
	 * Returns all URNs that match the given pattern. The pattern
	 * may be null to select all stored URNs, or may be a pattern
	 * containing '*' or '?' wildcard characters.
	 *
	 * @param pattern the pattern the URNs should match, or null
	 * @return a Map containing the matched URNs as keys, and their URLs as values
	 **/
	public Map listURNs(String pattern);
	
	/**
	 * Method listReservedURNs. Returns all URNs that are reserved for later use with a document.
	 * @return a Set containing the URNs
	 */
	public Set listReservedURNs();
	
	/**
	 * Gets the document id for a given urn.
	 * @param urn the urn to get the document id for
	 * @return the Miless/MyCoRe document id
	 */
	public String getDocumentId(MCRNBN urn);

	/**
	 * Sets the document id for the NBN URN given.
	 *
	 * @param urn the NBN URN that represents the URL
	 * @param documentId the document id the NBN points to
	 **/
	public void setDocumentId(MCRNBN urn, String documentId);
	
	/**
	 * Finds the urn for a given document id
	 * @param documentId the document id
	 * @return the nbn or null
	 */
	public MCRNBN getNBNByDocumentId(String documentId);
	
}
