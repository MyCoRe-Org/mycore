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

// package
package org.mycore.frontend.servlets;

// Imported java classes
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.frontend.workflow.MCRWorkflowManager;
import org.mycore.user.MCRUserMgr;

/**
 * This class contains a servlet as extention of MCRServlet.
 * The Servlet read the object metadata files from the workflow, transform
 * them and analyze also the appended derivates. It return a XML file
 * with the following content.<p />
 * &gt;mcr_workflow type="..." step="..."&lt;<br />
 * &gt;item ID="..."&lt;<br />
 * &gt;label&lt;Die 99 582 am Lokschuppen in Schönheide&gt;/label&lt;<br />
 * &gt;data&lt;Jens Kupferschmidt&gt;/data&lt;<br />
 * &gt;data&lt;2004-06-08&gt;/data&lt;<br />
 * &gt;derivate ID="..."&lt;<br />
 * &gt;file size="..." main="true|false" &lt;...&gt;/file&lt;<br />
 * &gt;/derivate&lt;<br />
 * &gt;/item&lt;<br />
 * &gt;/mcr_workflow&lt;<br />
 * Call this servlet with <b>.../servlets/MCRListWorkflowServlet/XSL.Style=xml&type=...&step=...</b>
 *
 * @author  Jens Kupferschmidt
 * @version $Revision$ $Date$
 */

public class MCRListWorkflowServlet extends MCRServlet
{
// The logger
private static Logger LOGGER=Logger.getLogger(MCRListWorkflowServlet.class.getName());

// The workflow manager
private static MCRWorkflowManager WFM = null;

// The file slash
private static String SLASH = System.getProperty("file.separator");;

/** Initialisation of the servlet */
public void init() throws MCRConfigurationException {
    super.init();
    WFM = MCRWorkflowManager.instance();
}

/** 
 * This method overrides doGetPost of MCRServlet and put the generated DOM
 * in the LayoutServlet.<br />
 * Input parameter are<br />
 * <ul>
 * <li>type - the MyCoRe type</li>
 * <li>step - the workflow step</li>
 * </ul> 
 * @param job an instance of MCRServletJob
 */
public void doGetPost(MCRServletJob job) throws Exception
  {
  // get the type
  String type = getProperty(job.getRequest(),"type").trim();
  LOGGER.debug( "MCRListWorkflowServlet : type = " + type );
  // get the step
  String step = getProperty(job.getRequest(),"step").trim();
  LOGGER.debug( "MCRListWorkflowServlet : step = " + step );
  
  // check the privileg
  boolean haspriv = false;
  MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
  String userid = mcrSession.getCurrentUserID();
  //userid = "administrator";
  LOGGER.debug("Curren user for list workflow = "+userid);
  ArrayList privs = MCRUserMgr.instance().retrieveAllPrivsOfTheUser(userid);
  if (privs.contains("create-"+type)) { haspriv = true; }

  // read directory
  ArrayList workfiles = new ArrayList();
  ArrayList derifiles = new ArrayList();
  if (haspriv) { 
    workfiles = WFM.getAllObjectFileNames(type);
    derifiles = WFM.getAllDerivateFileNames(type);
    }

  // create a XML JDOM tree with master tag mcr_workflow
  // prepare the transformer stylesheet
  String xslfile = "mycoreobject-" + type + "-to-workflow.xsl";
  InputStream in = MCRListWorkflowServlet.class.getResourceAsStream("/"+xslfile); 
  if( in == null ) {
    throw new MCRConfigurationException("Can't read stylesheet " + xslfile ); }
  org.jdom.transform.XSLTransformer trans = null;
  org.jdom.Document xslstylesheet = (new org.jdom.input.SAXBuilder()).build(in);
  try {
    trans = new org.jdom.transform.XSLTransformer (xslstylesheet); }
  catch( Exception ex ) {
    throw new MCRConfigurationException("Can't initialize transformer.",ex ); }
  LOGGER.debug(xslfile+" readed.");

  // build the frame of mcr_workflow
  org.jdom.Element root = new org.jdom.Element("mcr_workflow");
  root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",
    MCRDefaults.XSI_URL));
  root.setAttribute("type",type);
  root.setAttribute("step",step);
  org.jdom.Document workflow_in = null;
  String dirname = WFM.getDirectoryPath(type);
  // run the loop over all objects in the workflow
  for (int i=0; i<workfiles.size();i++) {
    String fname = dirname+SLASH+(String)workfiles.get(i);
    String wfile = (String)workfiles.get(i);
    org.jdom.Document workflow_out = null;
    org.jdom.Element elm = null;
    try {
      workflow_in = MCRXMLHelper.parseURI(fname,false);
      LOGGER.debug("Workflow file "+wfile+" was readed.");
      }
    catch( Exception ex ) {
      LOGGER.warn( "Can't parse workflow file "+wfile);
      continue; 
      }
    try {
      workflow_out = trans.transform(workflow_in); 
      elm = workflow_out.getRootElement();
      elm.detach();
      }
    catch( Exception ex ) {
      LOGGER.error( "Error while tranforming XML workflow file "+wfile); 
      continue; 
      }
    String ID = elm.getAttributeValue("ID");
    LOGGER.debug("The data ID is "+ID);
    try {
      for (int j=0;j<derifiles.size();j++) {
        String dername = (String)derifiles.get(j);
        LOGGER.debug("Check the derivate file "+dername);
        if (WFM.isDerivateOfObject(type,dername,ID)) {
          org.jdom.Document der_in = null;
          String dname = dirname+SLASH+dername;
          org.jdom.Element der = null;
          String mainfile = "";
          String label = "Derivate of "+ID;
          try {
            der_in = MCRXMLHelper.parseURI(dname,false);
            LOGGER.debug("Derivate file "+dername+" was readed.");
            der = der_in.getRootElement();
            label = der.getAttributeValue("label");
            org.jdom.Element s1 = der.getChild("derivate");
            if (s1 != null) {
              org.jdom.Element s2 = s1.getChild("internals");
              if (s2 != null) {
                org.jdom.Element s3 = s2.getChild("internal");
                if (s3 != null) {
                  mainfile = s3.getAttributeValue("maindoc"); }
                }
              }
            LOGGER.debug("The maindoc name is "+mainfile);
            }
          catch( Exception ex ) {
            LOGGER.warn( "Can't parse workflow file "+dername); }
          String derpath = dername.substring(0,dername.length()-4);
          org.jdom.Element deriv = new org.jdom.Element("derivate");
          deriv.setAttribute("ID",derpath);
          deriv.setAttribute("label",label);
          File dir = new File(dirname,derpath);
          LOGGER.debug("Derivate under "+dir.getName());
          if (dir.isDirectory()) {
            ArrayList dirlist = MCRUtils.getAllFileNames(dir);
            for (int k=0; k< dirlist.size();k++) {
              org.jdom.Element file = new org.jdom.Element("file");
              file.setText(derpath+SLASH+(String)dirlist.get(k));
              File thisfile = new File(dir,(String)dirlist.get(k));
              file.setAttribute("size",String.valueOf(thisfile.length()));
              if (mainfile.equals((String)dirlist.get(k))) {
                file.setAttribute("main","true"); }
              else {
                file.setAttribute("main","false"); }
              deriv.addContent(file);
              }
            derifiles.remove(j); j--;
            }
          elm.addContent(deriv);
          }
        }
      }
    catch( Exception ex ) {
      LOGGER.error( "Error while read derivates for XML workflow file " +(String)workfiles.get(i) ); 
      LOGGER.error( ex.getMessage() ); 
      }
    root.addContent(elm);
    }
  org.jdom.Document workflow_doc = new org.jdom.Document(root);
  
  // return the JDOM
  job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM",workflow_doc); 
  RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
  rd.forward(job.getRequest(), job.getResponse());
  }

}

