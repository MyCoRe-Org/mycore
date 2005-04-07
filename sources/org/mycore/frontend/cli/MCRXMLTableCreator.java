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

package org.mycore.frontend.cli;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.services.query.MCRQueryInterface;

/**
 * This main program reads all data from the persitence store XML:DB or CM8
 * and put it in the new SQL Table. The class is a temporary migration tool.
 * <br />
 * Start the program with<br />
 *   java org.mycore.frontend.cli.MCRXMLTableCreator <type>
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRXMLTableCreator
{
// The configuration
static MCRConfiguration conf = null;
static MCRXMLTableManager xmltable = null;

public static void main(String argv[]) throws Exception
  {
  System.out.println(
    "Migration of the XML datas to a new table\n"+
    "=========================================\n\n"
    );
  // read the configuration
  conf = MCRConfiguration.instance();

  // check the argument
  if (argv.length != 1) {
    System.out.println("Usage java org.mycore.frontend.cli.MCRXMLTableCreator <type>\n");
    System.exit(0);
    }
  if (!conf.getBoolean("MCR.type_"+argv[0],false)) {
    System.out.println("The type "+argv[0]+" is false.\n");
    System.exit(0);
    }
  
  // initalize XML table
  xmltable = MCRXMLTableManager.instance();

  // get all data from the old store
  String persist_type = conf.getString( "MCR.XMLStore.Type" );
  String proppers = "MCR.persistence_"+persist_type.toLowerCase()+
    "_query_name";
  MCRQueryInterface mcr_queryint = (MCRQueryInterface)conf
    .getInstanceOf(proppers);
  MCRXMLContainer mcr_result = new MCRXMLContainer();
  String mcr_query = "/mycoreobject[@ID like '*']";
  if (argv[0].equals("derivate")) {
    mcr_query = "/mycorederivate[@ID like '*']"; }
  int maxres = conf.getInt( "MCR.query_max_results", 10000 );
  mcr_result.importElements(mcr_queryint.getResultList(mcr_query,argv[0],
    maxres));

  // store in the new table
  for (int i=0;i<mcr_result.size();i++) {
    System.out.println("ID = "+mcr_result.getId(i));
    MCRObjectID mcrid = new MCRObjectID(mcr_result.getId(i));
    Element elm = (Element)mcr_result.getXML(i).detach();
    Document doc = new Document(elm);
    xmltable.create(mcrid,doc);
    }

  System.out.println("Ready.\n\n");
  }
}

