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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

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

  /** The table of handle ID's */
  private ArrayList ids = null;

  /** The table of MCRUploadHandlerInterfaces */
  private ArrayList upl = null;

  /** The tabel of the date values */
  private ArrayList dat = null;

  /** The class name of the handler from the configuration */
  String handlename = null;

  /** The random generator */
  private Random generator = null;
 
  /** The logger */
  private static Logger logger=Logger.getLogger("org.mycore.frontend.fileupload");

  /** The configuration */
  private  MCRConfiguration config = null;

  /**
   * Builds the manager singleton.
   **/
  protected MCRUploadHandlerManager ()
    {
    config = MCRConfiguration.instance();
    generator = new Random();
    ids = new ArrayList();
    upl = new ArrayList();
    dat = new ArrayList();
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
   * The method return a new instance of the file upload handler. The
   * used class is the default class from the configuration.
   * 
   * @return a new instance of a file upload handler as MCRUploadHandlerInterface implementation
   **/
  public final MCRUploadHandlerInterface getNewHandle() throws MCRException
    { return getNewHandle(handlename); }

  /**
   * The method return a new instance of the file upload handler for the
   * given handler class name.
   * 
   * @return a new instance of a file upload handler as MCRUploadHandlerInterface implementation
   **/
  public final MCRUploadHandlerInterface getNewHandle( String name ) throws MCRException
    {
    Object obj = null;
    try {
      obj = Class.forName(name).newInstance(); }
    catch (ClassNotFoundException e) {
      throw new MCRException(name+" ClassNotFoundException"); }
    catch (IllegalAccessException e) {
      throw new MCRException(name+" IllegalAccessException"); }
    catch (InstantiationException e) {
      throw new MCRException(name+" InstantiationException"); }
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
    checkRegister();
    String uploadID = ((MCRUploadHandlerInterface)handle).getId();
    if (uploadID.length()==0) { 
      uploadID = getRandomID();
      ((MCRUploadHandlerInterface)handle).setId(uploadID);
      }
    ids.add(uploadID);
    upl.add(handle);
    dat.add(new GregorianCalendar());
    logger.debug(handlename+" with ID "+uploadID+" rgistered.");
    return uploadID;
    }

  /**
   * The method check all registered handler and remove all they are older
   * than 24 hours.
   **/
  private final void checkRegister()
    {
    GregorianCalendar today = new GregorianCalendar();
    int day = today.get(Calendar.DAY_OF_YEAR);
    int year = today.get(Calendar.YEAR);
    int j = dat.size();
    for (int i=0;i<j;i++) {
      GregorianCalendar test = (GregorianCalendar)dat.get(i);
      int testday = test.get(Calendar.DAY_OF_YEAR);
      int testyear = test.get(Calendar.YEAR);
      if (testyear < year) {
        unregister(i); j--; continue; }
      if (testday < day-1) {
        unregister(i); j--; continue; }
      }
    }

  /**
   * The method return a handle of a given ID.
   *
   * @param the handle ID
   * @return a file upload handle as instance of MCRUploadHandlerInterface
   **/
  public final MCRUploadHandlerInterface getHandle(String uploadID)
    { return (MCRUploadHandlerInterface)upl.get(ids.indexOf(uploadID)); }

  /**
   * The method unregister the hanlde for the given ID.
   *
   * @param the handle ID
   **/
  public final void unregister( String uploadID )
    { 
    int i = ids.indexOf(uploadID);
    unregister(i);
    }

  /**
   * The method unregister the hanlde for the given ID.
   *
   * @param the handle ID
   **/
  private final void unregister( int i )
    { 
    String uploadID = (String)ids.get(i);
    ids.remove(i); 
    upl.remove(i); 
    dat.remove(i); 
    logger.debug(handlename+" with ID "+uploadID+" unrgistered.");
    }
}
