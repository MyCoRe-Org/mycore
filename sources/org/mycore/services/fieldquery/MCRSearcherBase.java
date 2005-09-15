/*
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
 */

package org.mycore.services.fieldquery;

import org.mycore.common.events.MCREventHandler;
import org.mycore.common.events.MCREventHandlerBase;

public abstract class MCRSearcherBase extends MCREventHandlerBase 
  implements MCREventHandler, MCRSearcher
{
	/** The unique searcher ID for this MCRSearcher implementation */
	protected String ID;

	/** The prefix of all properties in mycore.properties for this searcher */
	protected String prefix;
	
	/**
	 * Initializes the searcher and sets its unique ID.
	 * 
	 * @param ID the non-null unique ID of this searcher instance
	 **/
	public void init(String ID) 
	{
		this.ID = ID;
		this.prefix = "MCR.FieldQuery.Searcher." + ID + ".";
	}

	/**
	 * Returns the unique store ID that was set for this store instance
	 * 
	 * @return the unique store ID that was set for this store instance
	 */
	public String getID() {
		return ID;
	}

	/**
	 * Executes a query and returns the result list
	 * 
	 * @param query the query as JDOM XML element
	 * @return the result list
	 **/
	public abstract MCRResults search( org.jdom.Element query );
}

