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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.MCRException;
import org.mycore.common.MCRConfiguration;

/**
 * This class manages all handles of file upload as implementation of 
 * the MCRUploadHandlerInterface.
 *
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRUploadHandlerManager
  {
  /** The manager singleton */
  protected static MCRUploadHandlerManager singleton;

  /** The table of MCRUploadHandlerInterfaces */
  private static Hashtable upl = null;

  /** The class name of the handler from the configuration */
  String handlename = null;

  /** The random generator */
  private Random generator = null;
 
  /** The logger */
  private static Logger logger=Logger.getLogger("org.mycore.frontend.fileupload");

  /**
   * Builds the manager singleton.
   **/
  protected MCRUploadHandlerManager ()
    {
    MCRConfiguration config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());
    generator = new Random();
    upl = new Hashtable();
    handlename = config.getString("MCR.Editor.FileUpload.Handler",
      "org.mycore.frontend.fileupload.MCRUploadHandler");
    }

  /**
   * Returns the manager singleton.
   **/
  public static MCRUploadHandlerManager instance()
    throws MCRException
    {
    if( singleton == null ) singleton = new MCRUploadHandlerManager();
    return singleton;
    }

  /**
   * The method return the logger for org.mycore.backend.cm8 .
   *
   * @return the logger.
   **/
  static final Logger getLogger()
    { return logger; }

  /**
   * The method return a random handle ID for the file upload.
   *
   * @return a random ID as String
   **/
  public final String getRandomID()
    {
    return String.valueOf( generator.nextInt( 10000000 ) + 10000000 ) + 
      upl.size();
    }
  
  /**
   * The method return a new instance of the file upload handler
   * 
   * @return a new instance of a file upload handler as MCRUploadHandlerInterface implementation
   **/
  public final MCRUploadHandlerInterface getNewHandle() throws MCRException
    {
    Object obj = null;
    try {
      obj = Class.forName(handlename).newInstance(); }
    catch (ClassNotFoundException e) {
      throw new MCRException(handlename+" ClassNotFoundException"); }
    catch (IllegalAccessException e) {
      throw new MCRException(handlename+" IllegalAccessException"); }
    catch (InstantiationException e) {
      throw new MCRException(handlename+" InstantiationException"); }
    ((MCRUploadHandlerInterface)obj).setLogger(logger);
    String uploadID = getRandomID();
    ((MCRUploadHandlerInterface)obj).setId(uploadID);
    return ((MCRUploadHandlerInterface)obj);
    }

  /**
   * The method store a new handel of file upload and return the ID of them.
   *
   * @param handle the instance of MCRUploadHandlerInterface
   * @return the ID of them as String
   **/
  public final String register(MCRUploadHandlerInterface handle)
    {
    String uploadID = ((MCRUploadHandlerInterface)handle).getId();
    if (uploadID.length()==0) { 
      uploadID = getRandomID();
      ((MCRUploadHandlerInterface)handle).setId(uploadID);
      }
    upl.put(uploadID,handle);
    logger.debug(handlename+" with ID "+uploadID+" rgistered.");
    return uploadID;
    }

  /**
   * The method return a handle of a given ID.
   *
   * @param the handle ID
   * @return a file upload handle as instance of MCRUploadHandlerInterface
   **/
  public final MCRUploadHandlerInterface getHandle(String uploadID)
    { return (MCRUploadHandlerInterface)upl.get(uploadID); }

  /**
   * The method unregister the hanlde for the given ID.
   *
   * @param the handle ID
   **/
  public void unregister( String uploadID )
    { 
    upl.remove( uploadID ); 
    logger.debug(handlename+" with ID "+uploadID+" unrgistered.");
    }
}
