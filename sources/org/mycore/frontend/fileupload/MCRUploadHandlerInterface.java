/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * Copyright (C) 2000 University of Essen, Germany
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
 * along with this program, normally in the file documentation/license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.frontend.fileupload;

import java.util.*;
import java.io.*;

/**
 * This is an interface for a file upload handler.
 *
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public interface MCRUploadHandlerInterface
  {

  /**
   * The method set the logger.
   *
   * @param logger the log4j logger
   **/
  public void setLogger(org.apache.log4j.Logger logger);

  /**
   * set the data of MCRUploadHandler for MyCoRe
   * @param docId document to which derivate belongs
   * @param derId derivate used to add files, if id="0" a new derivate is create
d
   * @param mode  "append"  add files to derivate, replace old files
   *              "replace" add files to derivate, delete old files
   *              "create"  add files to new derivate
   * @param url    when MCRUploadApplet is finished this url will be shown
   **/
  public void set( String docId, String derId, String mode, String url  );

  /**
   * Get the ID of the handler
   *
   * @return the handler ID
   **/
  public String getId();

  /**
   * Set the ID of the handler
   *
   * @param id the handler ID as String
   **/
  public void setId(String id);

  /**
   * The method return the redirect URL they should use after finish the applet.
   *
   * @return the redirect URL
   **/
  public String getRedirectURL();
  
  public String startUpload( )  throws Exception;
  
  public boolean acceptFile( String path, String checksum ) throws Exception;
  
  public String receiveFile(String path, InputStream in ) throws Exception;
  
  public void finishUpload( )  throws Exception;
  
}
