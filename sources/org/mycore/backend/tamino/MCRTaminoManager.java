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

package org.mycore.backend.taminoxmldb;

import org.mycore.common.MCRConfiguration;
import org.apache.log4j.PropertyConfigurator;

import com.softwareag.tamino.db.api.accessor.TSchemaDefinition3Accessor;
import com.softwareag.tamino.db.api.accessor.TXMLObjectAccessor;
import com.softwareag.tamino.db.api.accessor.TSystemAccessor;

import com.softwareag.tamino.db.api.accessor.TAccessorException;
import com.softwareag.tamino.db.api.accessor.TInsertException;

import com.softwareag.tamino.db.api.connection.TConnection;
import com.softwareag.tamino.db.api.connection.TConnectionFactory;
import com.softwareag.tamino.db.api.common.TException;
import com.softwareag.tamino.db.api.common.TAccessFailureMessage;

/**
 * This class organize the tamino schemes files to be inserted into database
 * It is assumed that a Tamino database has been created and is running.
 * The tamino database uri was defined in Mycore property file.
 * The following tasks are to be preformed:
 *  - establish a connection to the Tamino database
 *  - obtain a system accessor, check Server alive or not, and print out system information
 *  - call class "MCRTaminoSchema" to insert tamino schemes for collection "ino:etc", "legalentity", "document", "derivate".
 *
 * @author: lili tan
 * @version 1.0 $ $Date$
 */

public class MCRTaminoManager {
  public MCRTaminoManager()
      {
       }
 public static void main(String[] args) throws Exception  {

     MCRConfiguration config = MCRConfiguration.instance();
     PropertyConfigurator.configure(config.getLoggingProperties());

     /** the database url in Tamino http form that got from properity file  */
     String TaminoDATABASE_URI=config.getString(  "MCR.persistence_taminoxmldb_TaminoDATABASE_URI" , "");

      System.out.println( "\nHere is some systeminformation" );

      // print TaminoDATABASE_URI
      System.out.println( "The Tamino server hosting " +TaminoDATABASE_URI);

     // Obtain the connection factory
      TConnectionFactory connectionFactory = TConnectionFactory.getInstance();
     // and obtain the connection to the database
      TConnection connection = connectionFactory.newConnection(TaminoDATABASE_URI );

      // Obtain a TSystemAccesor
      TSystemAccessor systemaccessor = connection.newSystemAccessor();


       // Check if the connection is available and print out some system information
        if ( !checkServerAndPrintSystemInformation( systemaccessor ) )
        return;

  /**
   * call for insert tamino schema file into tamino database
   *
   * @param TaminoDATABASE_URI
   *
   * @throws TInsertException
   **/

         System.out.println("\n-------------Loading Tamino Schema------------------\n");

         // call for insert tamino schema "xlink.tsd" into tamino database in collection "ino:etc"
         MCRTaminoSchema.InsertTaminoSchema(
                    TaminoDATABASE_URI,"ino:etc","./taminoschema/xlink.tsd");

         // call for insert tamino schema "MyCoReDemoDC_LegalEntity.tsd" into tamino database in collection "legalentity"
         MCRTaminoSchema.InsertTaminoSchema(
                   TaminoDATABASE_URI,"legalentity","./taminoschema/MyCoReDemoDC_LegalEntity.tsd");

        // call for insert tamino schema "MyCoReDemoDC_Document.tsd" into tamino database in collection "document"
        MCRTaminoSchema.InsertTaminoSchema(
                   TaminoDATABASE_URI,"document","./taminoschema/MyCoReDemoDC_Document.tsd");

         // call for insert tamino schema "MyCoReDemoDC_Derivate.tsd" into tamino database in collection "derivate"
         MCRTaminoSchema.InsertTaminoSchema(TaminoDATABASE_URI,
                                            "derivate","./taminoschema/MyCoReDemoDC_Derivate.tsd");
          System.out.println("-------------End of Loading Tamino Schema------------------");
         }


  /**
   * use a system accessor to check if the database is alive and
   * print some system information to stdout
   *
   * @param systemaccessor
   * @throws MTAccessorException
  **/

    protected static boolean checkServerAndPrintSystemInformation(
    TSystemAccessor  systemaccessor)
            throws TAccessorException {
            if (!systemaccessor.isServerAlive()) {
                    return false;
                  }
            else
                  {
                    System.out.println( "server is alive" );
                    System.out.println( "Version: " + systemaccessor.getServerVersion() );
                    System.out.println( "Server API version: " +systemaccessor.getServerAPIVersion());
                    System.out.println( "Tamino API for Java version: " +systemaccessor.getAPIVersion() +"\n" );
                    return true;
                    }
           }
}