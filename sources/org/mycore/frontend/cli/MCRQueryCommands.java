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

package mycore.commandline;

import java.io.*;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import mycore.common.*;
import mycore.datamodel.MCRQueryResult;
import mycore.datamodel.MCRQueryResultArray;

/**
 * This class implements the query command to start a query to a local
 * Library Server or to any remote Library Server in a configuration
 * list or to a dedicated named remote Library Server.
 * The result was presided as a text output.
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @author Mathias Zarick
 * @version $Revision$ $Date$
 **/
final public class MCRQueryCommands
{
  /** Executes a local query */
  public static void queryLocal( String type, String query )
    throws Exception
  { query( "local", type, query ); }

  /** Executes a remote query */
  public static void queryRemote( String type, String query )
    throws Exception
  { query( "remote", type, query ); }

 /**
  * The query command
  *
  * @param host  either "local", "remote" or hostname
  * @param type  the result type, "document" of "legalentity"
  * @param query the query expression
  **/
  public static void query( String host, String type, String query )
    throws Exception
  {
    if (host==null) host = "local";
    if (type==null) { return; }
    type  = type .toLowerCase();
    if (query==null) query = "";

    System.out.println( "Host(s)  : " + host  );
    System.out.println( "Type     : " + type  );
    System.out.println( "Query    : " + query );
    System.out.println();

    MCRQueryResult result = new MCRQueryResult();
    MCRQueryResultArray resarray = result.setFromQuery(host,type,query);

    printResult(resarray, type);

    System.out.println("Ready.");
    System.out.println();
  }

/**
 * This methode transform the XML result collection to an ASCII output.
 *
 * @param results the XML result collection
 **/
private static final void printResult(MCRQueryResultArray results,
  String type)
  throws Exception
  {
  // Configuration
  MCRConfiguration config = MCRConfiguration.instance();
  String applpath = config.getString("MCR.appl_path");
  String xslfile = applpath + "/stylesheets/mcr_results-PlainText-"+
    type.toLowerCase()+".xsl";
  TransformerFactory transfakt = TransformerFactory.newInstance();
  // Indexlist
  byte [] mcrxmlall = results.exportAllToByteArray();
  Transformer trans =
    transfakt.newTransformer(new StreamSource(xslfile));
  StreamResult sr = new StreamResult((OutputStream) System.out); 
  trans.transform(new StreamSource(new ByteArrayInputStream(mcrxmlall)),sr);
  System.out.println();
  }

}

