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

package mycore.datamodel;

import java.util.*;
import mycore.common.MCRConfigurationException;
import mycore.common.MCRException;
import mycore.datamodel.MCRQueryResultArray;

/**
 * This class is the result list of a XQuery question to the persistence
 * system or remote systems. the result ist transparent over all searched instances of
 * a common instance collection or of a local instance.
 * The method use, depending of the persitence type property,
 * a interface to a query transformer form XQuery to the target system
 * query language.
 *
 * @author Jens Kupferschmidt
 * @author Mathias Zarick
 * @version $Revision$ $Date$
 **/
public class MCRQueryResult
{
private MCRQueryResultArray mcr_result = null;
private String mcr_type = null;
private ArrayList mcr_hostAliases = null;
private String mcr_query = null;

/**
 * This constructor create the MCRQueryResult class with an empty
 * MCRQueryResultArray.
 *
 * @param type the type of the MCRObjectId
 **/
public MCRQueryResult(String type)
  {
  if (type==null) {
    throw new MCRException("The MCRObjectID type is empty."); }
  mcr_type = type.toLowerCase();
  mcr_hostAliases = new ArrayList();
  mcr_query = "";
  }

/**
 * This methode read the properties for the MCRObjectID type and call
 * the coresponding class for a query to the persistence layer. If
 * it was succesful, the MCRQueryResultArray is filled with answers.
 *
 * @param host                  a list of host name aliases
 * @param query	                the Query string
 * @exception MCRException      general Exception of MyCoRe
 * @exception MCRConfigurationException
 *                              an Exception of MyCoRe Configuration
 **/
public final void setFromQuery(ArrayList hostAliases, String query)
  throws MCRException, MCRConfigurationException
  {
  ThreadGroup threadGroup = new ThreadGroup("threadGroup");
  mcr_hostAliases = hostAliases;
  mcr_query = query;
  mcr_result = new MCRQueryResultArray();
  for (int i=0; i<hostAliases.size() ;i++)
    new MCRQueryThread(threadGroup,(String)hostAliases.get(i),
      mcr_query,mcr_type,mcr_result).start();
  // wait until all threads have finished
  do {} while(threadGroup.activeCount() > 0);
  }

/**
 * This methode return the MCRQueryResultArray.
 *
 * @return the MCRQueryResultArray.
 **/
public final MCRQueryResultArray getResultArray()
  { return mcr_result; }

}

