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

package mycore.xml;

import java.util.*;
import mycore.common.MCRConfigurationException;
import mycore.common.MCRException;
import mycore.common.MCRConfiguration;
import mycore.xml.MCRQueryResultArray;

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
// The list of hosts from the configuration
private ArrayList remoteAliasList = null;
// The instcnce of configuraion
private MCRConfiguration conf = null;

/**
 * This constructor create the MCRQueryResult class and read the host list
 * from the mycore.property configuration.
 **/
public MCRQueryResult()
  {
  // read host list from configuration
  MCRConfiguration config = MCRConfiguration.instance();
  String hostconf = config.getString("MCR.communication_hostaliases",
    "local");
  remoteAliasList = new ArrayList();
  int i = 0;
  int j = hostconf.length();
  int k = 0;
  while (k!=-1) {
    k = hostconf.indexOf(",",i);
    if (k==-1) {
      remoteAliasList.add(hostconf.substring(i,j)); }
    else {
      remoteAliasList.add(hostconf.substring(i,k)); i = k+1; }
    }
  // get an instance of configuration
  conf = MCRConfiguration.instance();
  }

/**
 * IF it was succesful, the MCRQueryResultArray is filled with answers.
 *
 * @param type                  the MCRObjectID type
 * @param hostlist              a String of host name aliases
 * @param query	                the Query string
 * @return                      the filled MCRQueryResultArray
 * @exception MCRException      general Exception of MyCoRe
 * @exception MCRConfigurationException
 *                              an Exception of MyCoRe Configuration
 **/
public final MCRQueryResultArray setFromQuery(String host, String type,
  String query) throws MCRException, MCRConfigurationException
  {
  System.out.println( "hosts = " + host  );
  System.out.println( "type  = " + type  );
  System.out.println( "query = " + query );

  // check the type
  type  = type.toLowerCase();
  try {
    String test = conf.getString("MCR.type_"+type); }
  catch (MCRConfigurationException e) {
    throw new MCRException("The MCRObjectID type is false for search."); }

  // build host list from host string
  ArrayList hostAliasList = new ArrayList();
  int i = 0;
  int j = host.length();
  int k = 0;
  int n = 0;
  while (n!=-1) {
    n = host.indexOf(",",i);
    String hostname = "";
    if (n==-1) { hostname = host.substring(i,j); }
    else { hostname = host.substring(i,n); i = n+1; }
    if (hostname.equals("local")) {
      k = -1;
      for (int l=0;l<hostAliasList.size();l++) {
        if (((String)hostAliasList.get(l)).equals("local")) {
          k = 0; break; }
        }
      if (k==-1) { hostAliasList.add("local"); }
      }
    else {
      if (hostname.equals("remote")) {
        for (int m=0;m<remoteAliasList.size();m++) {
          k = -1;
          for (int l=0;l<hostAliasList.size();l++) {
            if (((String)hostAliasList.get(l))
              .equals(remoteAliasList.get(m))) { k = 0; break; }
            }
          if (k==-1) { hostAliasList.add(remoteAliasList.get(m)); }
          }
        }
      else {
        k = -1;
        for (int m=0;m<remoteAliasList.size();m++) {
          if (((String)remoteAliasList.get(m)).equals(hostname)) {
            k = 0; break; } }
        if (k==-1) {
          throw new MCRException( "The host name is not in the list."); }
        k = -1;
        for (int l=0;l<hostAliasList.size();l++) {
          if (((String)hostAliasList.get(l)).equals(hostname)) {
            k = 0; break; } }
        if (k==-1) { hostAliasList.add(hostname); }
        }
      }
    }

  // print list for debug
  for (i=0;i<hostAliasList.size();i++) {
    System.out.println("Host : "+hostAliasList.get(i)); }
  System.out.println();

  ThreadGroup threadGroup = new ThreadGroup("threadGroup");
  MCRQueryResultArray result = new MCRQueryResultArray();
  MCRQueryThread[] thr = new MCRQueryThread[hostAliasList.size()];
  boolean threadsRunning = false;
  try {
      for (i=0; i<hostAliasList.size() ;i++){
	  thr[i] = new MCRQueryThread( threadGroup,
				       (String)hostAliasList.get( i ),
				       query,
				       type,
				       result );
          thr[i].start();
	  threadsRunning = true;
      }
      // wait until all threads have finished
      while( threadsRunning ) {
	  threadsRunning = false;
	  for( i = 0; i < thr.length; i++ )
	      if( thr[i].isAlive() )
		  threadsRunning = true;
      }
  }
  catch( Exception e ) {
      throw new MCRException( e.getMessage(), e );
  }
  return result;
  }

}

