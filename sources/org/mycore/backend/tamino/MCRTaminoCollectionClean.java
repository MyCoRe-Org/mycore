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

package org.mycore.backend.tamino;

import org.apache.log4j.PropertyConfigurator;
import org.mycore.common.MCRConfiguration;

import com.softwareag.tamino.db.api.accessor.TSchemaDefinition3Accessor;
import com.softwareag.tamino.db.api.connection.TConnection;
import com.softwareag.tamino.db.api.connection.TConnectionFactory;
import com.softwareag.tamino.db.api.objectModel.jdom.TJDOMObjectModel;
import com.softwareag.tamino.db.api.common.TException;

/**
    *
    * This class remove tamino collection from tamino database.
    *
    * Both of the schemes and contents in the collection are to be removed.
    *
    * It is assumed that a Tamino database has been created and is running.
    * It is assumed that tamino database uri was given in Mycore property file.
    *
    * The following tasks are to be preformed:
    * - Get TaminoDATABASE_URI from Mycore property file
    * - Obtain the connection factory
    * - Obtain the connection to the database
    * - Obtain a SchemaAccessor
    * - Clean collection "derivate"
    * - Clean collection "document"
    * - Clean collection "legalentity"
    * - Clean collection "ino:etc"
    * - Close the connection
    *
    * @author: lili tan
    * @version 1.0 $ $Date$
    *
    * @param TaminoDATABASE_URI
    *
    * @throws TException
    **/

public class MCRTaminoCollectionClean {
  public MCRTaminoCollectionClean()
      {
      }
 public static void main(String[] args) throws Exception  {

   MCRConfiguration config = MCRConfiguration.instance();
  PropertyConfigurator.configure(config.getLoggingProperties());

    /** the TaminoDATABASE_URI is defined in properity file  */
   String TaminoDATABASE_URI=config.getString(  "MCR.persistence_taminoxmldb_TaminoDATABASE_URI" , "");


         // Obtain the connection factory
         TConnectionFactory connectionFactory = TConnectionFactory.getInstance();
          // obtain the connection to the database
         TConnection connection = connectionFactory.newConnection(TaminoDATABASE_URI );
        try {
             // Obtain a SchemaAccessor
            TSchemaDefinition3Accessor TSD3Accessor =
           connection.newSchemaDefinition3Accessor( TJDOMObjectModel.getInstance() );

           // clean collection "derivate"
            TSD3Accessor.undefine("derivate",null);

             // clean collection "document"
            TSD3Accessor.undefine("document", null);

             // clean collection "legalentity"
            TSD3Accessor.undefine("legalentity", null);

             // clean collection "ino:etc"
             TSD3Accessor.undefine("ino:etc",null);

             }
           catch (TException taminoException)  {
               taminoException.printStackTrace();
                }

              // Close the connection
              connection.close();
            }
    }
