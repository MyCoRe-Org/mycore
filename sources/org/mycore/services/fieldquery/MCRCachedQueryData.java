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

import org.jdom.Document;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.parsers.bool.MCRCondition;

/**
 * Manages a cache that contains the data of the most recently used queries.
 * There are {MCR.SearchServlet.CacheSize} most recently used queries 
 * in a global cache. Additionally, the last query of search session is always 
 * stored within the MCRSession.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRCachedQueryData
{
  /** A cache that contains the data of the most recently used queries */
  protected static MCRCache cache;
  
  static
  { 
    int cacheSize = MCRConfiguration.instance().getInt( "MCR.SearchServlet.CacheSize", 50 );
    cache = new MCRCache( cacheSize, "SearchServlet query data cache" ); 
  }
  
  private final static String LAST_QUERY_IN_SESSION = "LastQuery";
  
  public static MCRCachedQueryData getData(String id) {
        // The n most recently used queries are in a global cache
        MCRCachedQueryData data = (MCRCachedQueryData) (cache.get(id));
        if (data == null) {
            // Additionally, the last query of a single session is always there
            data = (MCRCachedQueryData) (MCRSessionMgr.getCurrentSession().get(LAST_QUERY_IN_SESSION));
            // May be the last query in session is the one we are looking for
            if ((data != null) && (data.getResults().getID().equals(id)))
                return data;
        }
        return data; // May be null, if no cached query data found
    }        
  
  /** The query as XML, for later reloading into search mask editor form */
  private Document query; 
  /** The result list */
  private MCRResults results; 
  /** The normalized query condition */
  private MCRCondition condition;
  /** The page last displayed */
  private int page;
  /** The number of hits per page */
  private int numPerPage;
  
  public MCRCachedQueryData( MCRResults results, Document query, MCRCondition condition )
  {
    this.results = results;
    this.query = query;
    this.condition = condition;
    cache.put( results.getID(), this );
    MCRSessionMgr.getCurrentSession().put( LAST_QUERY_IN_SESSION, this );
  }

  public int getNumPerPage()
  {
    return numPerPage;
  }

  public void setNumPerPage( int numPerPage )
  {
    this.numPerPage = numPerPage;
  }

  public int getPage()
  {
    return page;
  }

  public void setPage( int page )
  {
    this.page = page;
  }

  public MCRCondition getCondition()
  {
    return condition;
  }

  public Document getQuery()
  {
    return query;
  }

  public MCRResults getResults()
  {
    return results;
  }
}
