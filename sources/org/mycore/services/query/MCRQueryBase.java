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
package org.mycore.services.query;

import java.util.*;

import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.mycore.services.query.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * This is the implementation of the MCRQueryInterface is a basic class which
 * the special query classes under the persistence layer should extend. Some
 * methods can be overwrite from this classes.
 *
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 **/
abstract public class MCRQueryBase implements MCRQueryInterface {

// the logger
protected static Logger logger = Logger.getLogger(MCRQueryBase.class.getName());
// defaults
/** The default maximum of resultes */
public final static int MAX_RESULTS = 1000;
/** The node name for a search in all document texts */
public final static String XPATH_ATTRIBUTE_DOCTEXT = "doctext()";
protected static String NL =
  new String((System.getProperties()).getProperty("line.separator"));
// protected data
protected int maxres = 0;
protected int maxresults = 0;
protected MCRConfiguration config = null;
protected ArrayList flags = null;
protected ArrayList subqueries = null;
protected ArrayList andor = null;
protected String root = null;

/**
 * The Constructor
 **/
public MCRQueryBase() {
  config = MCRConfiguration.instance();
  PropertyConfigurator.configure( config.getLoggingProperties() );
  maxres = config.getInt( "MCR.query_max_results", MAX_RESULTS );
  logger.info("The maximum of the results is "+Integer.toString(maxres));
  }


/**
 * This method parses the XPath Query string and return the result as
 * MCRQueryResultArray. If the type is null or empty or maxresults
 * is lower 1 an empty list was returned.
 *
 * @param query                 the XQuery string
 * @param maxresults            the maximum of results
 * @param type                  a list of the MCRObject types seperated by ,
 * @return                      a result list as MCRXMLContainer
 **/
public MCRXMLContainer getResultList( String query, String type,
  int maxresults ) {
  // check the input
  MCRXMLContainer result = new MCRXMLContainer();
  if( (type == null) || ((type = type.trim()).length() == 0) )
    return result;
  if( (maxresults < 1) || (maxresults > maxres) )
    return result;
  this.maxresults = maxresults;
  // separate the query
  flags = new ArrayList();
  subqueries = new ArrayList();
  andor = new ArrayList();
  int i=0, j = query.length(), l, m, n, kon;
  while (i < j) {
    l = query.indexOf("/mycore",i);
    if (l == -1) { break; }
    m = query.indexOf("[",l);
    if (m == -1) { throwQueryEx(); }
    root = query.substring(l,m);
    n = query.indexOf("]",m);
    if (n == -1) { throwQueryEx(); }
    kon = 1;
    for (int o=m+1;o<n;o++) {
      if (query.charAt(o) == '[') { kon++; } }
    for (int o=kon;o>1;o--) {
      n = query.indexOf("]",n+1);
      if (n == -1) { throwQueryEx(); }
      }
    flags.add(Boolean.FALSE);
    subqueries.add(query.substring(m+1,n));
    i = n+1;
    if (i+5 < j) {
      l = query.toLowerCase().indexOf(" and ",i);
      if (l != -1) { andor.add("and"); i = l+5; continue; }
      }
    if (i+4 < j) {
      l = query.toLowerCase().indexOf(" or ",i);
      if (l != -1) { andor.add("or"); i = l+4; continue; }
      }
    andor.add("");
    }
  // debug subqueries
  for (i=0;i<flags.size();i++) {
    logger.debug("Q: "+(String)subqueries.get(i)+" by "+(String)andor.get(i)); }
  logger.debug("R: The root string is "+root);
  // run over all types
  i=0; j = type.length(); l = i;
  String onetype = "";
  while ((l < j) && ( l != -1)) {
    l = type.indexOf(",",i);
    if (l == -1) {
      onetype = type.substring(i,j); }
    else {
      onetype = type.substring(i,l); i = l+1; }
    if( (onetype == null) || ((onetype = onetype.trim()).length() == 0) ) {
      break; }
    logger.debug("T: The separated query type is "+onetype);
    result.importElements((MCRXMLContainer)startQuery(onetype));
    }
  return result;
  }

/**
 * Throw a MCRPersiteceException when the query is corrupt.
 *
 * @exception if this method would called
 **/
protected void throwQueryEx() throws MCRPersistenceException
  { throw new MCRPersistenceException("Error while analyze the query string."); }
/**
 * This method start the Query over one object type and return the
 * result as MCRXMLContainer. This implementation must be overwrite with
 * them form the persitence layer.
 *
 * @param type                  the MCRObject type
 * @return                      a result list as MCRXMLContainer
 **/
abstract protected MCRXMLContainer startQuery( String type );

}

