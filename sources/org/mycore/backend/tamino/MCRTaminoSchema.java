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


import com.softwareag.tamino.db.api.accessor.TSchemaDefinition3Accessor;

import com.softwareag.tamino.db.api.connection.TConnection;
import com.softwareag.tamino.db.api.connection.TConnectionFactory;

import com.softwareag.tamino.db.api.objectModel.jdom.TJDOMObjectModel;
import com.softwareag.tamino.db.api.objectModel.TXMLObject;

import com.softwareag.tamino.db.api.common.TException;

import java.io.FileNotFoundException;

/**
 * This class insert tamino schema files into tamino database. It is to be called by MCRTaminoManager.java.
 *
 * It is assumed that a Tamino database has been created and is running.
 * The tamino database uri was defined in Mycore property file.
 *
 * The following tasks are to be preformed:
 * - Obtain the connection factory
 * - Obtain the connection to the database
 * - Instantiate an empty TXMLObject instance using the JDOM object model
 * - Read Tamino Schema from a file and construct a TXMLObject
 * - Obtain a SchemaAccessor
 * - Define a new collection and insert a schema in database
 * - Print the tamino schema to stdout and its name
 * - Close the connection
 *
 * @author: lili tan
 * @version 1.0 $ $Date$
 *
 * @param TaminoDATABASE_URI
 *
 * @throws TException
 **/



public class MCRTaminoSchema {
  /**
      * Do insert tamino schema into tamino database
      *
      * @param TaminoDATABASE_URI
      * @param CollectionName
      * @param SchemaFile
      *
      * @throws TException
      **/


  protected static void InsertTaminoSchema (

      /** TaminoDATABASE_URI, the path to connect to tamino database */
      String TaminoDATABASE_URI,

      /** CollectionName, the collection name to be created in tamino server */
      String CollectionName,

      /** SchemaFile, the schema file name to be inserted in tamino server */
      String SchemaFile)
      throws TException  {

         // Obtain the connection factory
         TConnectionFactory connectionFactory = TConnectionFactory.getInstance();

         // and obtain the connection to the database
         TConnection connection = connectionFactory.newConnection(TaminoDATABASE_URI );

         // Instantiate an empty TXMLObject instance using the JDOM object model
         TXMLObject schemaXmlObject = TXMLObject.newInstance( TJDOMObjectModel.getInstance() );

    try {

          System.out.println( "Reading TSD schema from file and insert into database" );

          // Read Tamino Schema from a file and construct a TXMLObject
          schemaXmlObject.readFrom( new java.io.FileReader( SchemaFile) );

          // Obtain a SchemaAccessor
           TSchemaDefinition3Accessor TSD3Accessor =
           connection.newSchemaDefinition3Accessor( TJDOMObjectModel.getInstance() );

           // define a new collection and insert a schema in database
            TSD3Accessor.define( schemaXmlObject );

             // print the tamino schema to stdout
              schemaXmlObject.writeTo(System.out);

            System.out.println( "\nFinished Reading and insert schema: "
                                            +SchemaFile.substring(15)+"!\n" );

                  }
              catch (TException taminoException)  {
                          taminoException.printStackTrace();
              }
              catch (FileNotFoundException filenotfoundException)  {
                          filenotfoundException.printStackTrace();
              }

            // Close the connection
            connection.close();
          }


  public MCRTaminoSchema() {

  }

}


