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

package org.mycore.frontend.servlets;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.commons.fileupload.*;
import org.jdom.*;

import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.frontend.servlets.*;
import org.mycore.frontend.editor2.*;
import org.mycore.user.*;

/**
 * This class is the superclass of servlets which checks the MCREditorServlet
 * output XML and store the XML in a file or if an error was occured start the
 * editor again.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */

public class MCRCheckNewFileServlet extends MCRServlet
{
// The configuration
protected static MCRConfiguration CONFIG;
protected static Logger logger=Logger.getLogger(MCRCheckNewFileServlet.class);
String NL = System.getProperty("file.separator");

/** Initialisation of the servlet */
public void init()
  {
  MCRConfiguration.instance().reload(true);
  CONFIG = MCRConfiguration.instance();
  PropertyConfigurator.configure(CONFIG.getLoggingProperties());
  }

/**
 * The method check the privileg of this action.
 *
 * @param privs the ArrayList  of privilegs
 * @return true if the privileg exist, else return false
 **/
public final boolean hasPrivileg(ArrayList privs, String type)
  {
  if (!privs.contains("create-"+type)) return false;
  return true;
  }

/**
 * This method overrides doGetPost of MCRServlet.<br />
 */
public void doGetPost(MCRServletJob job) throws Exception
  {
  // read the XML data
  MCREditorSubmission sub = (MCREditorSubmission)
    (job.getRequest().getAttribute("MCREditorSubmission"));
  org.jdom.Document indoc = sub.getXML();
  List files = sub.getFiles();

  // read the parameter
  MCRRequestParameters parms;
  if( sub == null )
    parms = new MCRRequestParameters(job.getRequest());
  else
    parms = sub.getParameters();
  String se_mcrid = parms.getParameter( "mcrid" );
  String re_mcrid = parms.getParameter( "remcrid" );
  String type = parms.getParameter( "type" );
  String step = parms.getParameter( "step" );
  logger.debug("XSL.target.param.0 = "+se_mcrid);
  logger.debug("XSL.target.param.1 = "+type);
  logger.debug("XSL.target.param.2 = "+step);
  logger.debug("XSL.target.param.3 = "+re_mcrid);

  // get the MCRSession object for the current thread from the session manager.
  MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
  String userid = mcrSession.getCurrentUserID();
  //userid = "administrator";
  logger.debug("Curren user for edit check = "+userid);
  ArrayList privs = MCRUserMgr.instance().retrieveAllPrivsOfTheUser(userid);
  if (!hasPrivileg(privs,type)) {
    String pagedir = CONFIG.getString( "MCR.editor_page_dir","" );
    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+pagedir+"editor_error_user.xml"));
    return;
    }
  String mylang = mcrSession.getCurrentLanguage();
  logger.info("LANG = "+mylang);

  // prepare the derivate MCRObjectID
  MCRObjectID ID = new MCRObjectID(re_mcrid);
  MCRObjectID DD = new MCRObjectID(se_mcrid);
  String workdir = CONFIG.getString("MCR.editor_"+ID.getTypeId()+"_directory","/");
  String dirname = workdir+NL+se_mcrid;

  // save the files
  File dir = new File(dirname);
  ArrayList ffname = new ArrayList();
  String mainfile = "";
  for (int i=0;i<files.size();i++) {
    FileItem item = (FileItem)(files.get(i));
    String fname = item.getName().trim();
    int j = 0;
    int l = fname.length();
    while (j<l) {
      int k = fname.indexOf("\\",j);
      if (k == -1) {
        k = fname.indexOf("/",j);
        if (k == -1) {
          fname = fname.substring(j,l); break; }
        else { j = k+1; }
        }
      else { j = k+1; }
      }
    fname.replace(' ','_');
    ffname.add(fname);
    File fout = new File(dirname,fname);
    FileOutputStream fouts = new FileOutputStream(fout);
    MCRUtils.copyStream(item.getInputStream(),fouts);
    fouts.close();
    logger.info("Data object stored under "+fout.getName());
    }
  if ((mainfile.length()==0) && (ffname.size() > 0)) { 
    mainfile = (String)ffname.get(0);
    }
  
  // add the mainfile entry
  MCRDerivate der = new MCRDerivate();
  der.setFromURI(dirname+".xml");
  if (der.getDerivate().getInternals().getMainDoc().equals("#####")) {
    der.getDerivate().getInternals().setMainDoc(mainfile);
    byte [] outxml = MCRUtils.getByteArray(der.createXML());
    try {
      FileOutputStream out = new FileOutputStream(dirname+".xml");
      out.write(outxml);
      out.flush();
      }
    catch (IOException ex) {
      logger.error( ex.getMessage() );
      logger.error( "Exception while store to file "+dirname+".xml");
      }
    }

  String pagedir = CONFIG.getString( "MCR.editor_page_dir","" );
  StringBuffer sb = new StringBuffer(pagedir);
  sb.append("editor_").append(ID.getTypeId()).append("_editor.xml");
  job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+sb.toString()));
  }

/**
 * A method to handle IO errors.
 *
 * @param jab the MCRServletJob
 * @param lang the current language
 **/
protected void errorHandlerIO(MCRServletJob job, String lang)
  throws Exception
  {
  job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+"editor_error_store.xml"));
  }

}
