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

abstract public class MCRCheckEditorServlet extends MCRServlet
{
// The configuration
protected static MCRConfiguration CONFIG;
protected static Logger logger=Logger.getLogger(MCRCheckEditorServlet.class);

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
abstract public boolean hasPrivileg(ArrayList privs, String type);

/**
 * The method is a dummy and return an URL with the next working step.
 *
 * @param ID the MCRObjectID of the MCRObject
 * @return the next URL as String
 **/
abstract public String loadMetadata(MCRObjectID ID) throws Exception;

/**
 * The method send a message to the mail address for the MCRObjectType.
 *
 * @param ID the MCRObjectID of the MCRObject
 **/
abstract public void sendMail(MCRObjectID ID);

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
  String oldmcrid = parms.getParameter( "mcrid" );
  String oldtype = parms.getParameter( "type" );
  String oldstep = parms.getParameter( "step" );
  logger.debug("XSL.target.param.0 = "+oldmcrid);
  logger.debug("XSL.target.param.1 = "+oldtype);
  logger.debug("XSL.target.param.2 = "+oldstep);

  // get the MCRSession object for the current thread from the session manager.
  MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
  String userid = mcrSession.getCurrentUserID();
  //userid = "administrator";
  logger.debug("Curren user for edit check = "+userid);
  ArrayList privs = MCRUserMgr.instance().retrieveAllPrivsOfTheUser(userid);
  if (!hasPrivileg(privs,oldtype)) {
    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+"editor_error_user.xml"));
    return;
    }
  String lang = mcrSession.getCurrentLanguage();
  logger.info("LANG = "+lang);

  // prepare the MCRObjectID's for the Metadata
  String mmcrid = "";
  boolean hasid = false;
  try {
    mmcrid = indoc.getRootElement().getAttributeValue("ID"); 
    if (mmcrid==null) {
      mmcrid = oldmcrid; }
    else {
      hasid = true; }
    }
  catch (Exception e) {
    mmcrid = oldmcrid; }
  MCRObjectID ID = new MCRObjectID(mmcrid);
  if (!ID.getTypeId().equals(oldtype)) {
    ID = new MCRObjectID(oldmcrid); hasid = false; }
  if (!hasid) {
    indoc.getRootElement().setAttribute("ID",ID.getId()); }

  // Save the incoming to a file
  byte [] outxml = MCRUtils.getByteArray(indoc);
  String savedir = CONFIG.getString("MCR.editor_"+ID.getTypeId()+"_directory");
  String NL = System.getProperty("file.separator");
  String fullname = savedir+NL+ID.getId()+".xml";
  storeMetadata(outxml,job,ID,fullname,lang);

  // create a metadata object and prepare it
  org.jdom.Document outdoc = prepareMetadata((org.jdom.Document)indoc.clone(),
    ID,job,oldstep,lang);
  outxml = MCRUtils.getByteArray(outdoc);

  // Save the prepared metadata object 
  storeMetadata(outxml,job,ID,fullname,lang);

  // call the loadMetadata and sendMail methods
  String url = loadMetadata(ID);
  sendMail(ID);
  job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+url));
  }

/**
 * The method stores the data in a working directory dependenced of the type.
 *
 * @param outdoc the prepared JDOM object
 * @param job the MCRServletJob
 * @param the MCRObjectID of the MCRObject/MCRDerivate
 * @param fullname the file name where the JDOM was stored.
 * @param lang the current langauge
 **/
public final void storeMetadata(byte [] outxml, MCRServletJob job,
  MCRObjectID ID, String fullname, String lang ) throws Exception
  {
  if (outxml == null) return;
  // Save the prepared MCRObject/MCRDerivate to a file
  try {
    FileOutputStream out = new FileOutputStream(fullname);
    out.write(outxml);
    out.flush();
    }
  catch (IOException ex) {
    logger.error( ex.getMessage() );
    logger.error( "Exception while store to file " + fullname );
    errorHandlerIO(job,lang);
    return;
    }
  logger.info( "Object "+ID.getId()+" stored under "+fullname+"." );
  }

/**
 * The method read the incoming JDOM tree in a MCRObject and prepare this
 * by the following rules. After them it return a JDOM as result of 
 * MCRObject.createXML().<br/>
 * <li>remove all target of MCRMetaClassification they have not a categid attribute.</li><br/>
 * <li>remove all target of MCRMetaLangText they have an empty text</li><br/>
 *
 * @param jdom_in the JDOM tree from the editor
 * @param ID      the MCRObjectID of the MCRObject
 * @param job     the MCRServletJob data 
 * @param step    the current workflow step
 * @param lang    the current language
 **/
protected org.jdom.Document prepareMetadata(org.jdom.Document jdom_in,
  MCRObjectID ID, MCRServletJob job, String step, String lang) throws Exception
  {
  ArrayList errorlog = new ArrayList();
  // add the namespaces (this is a workaround)
  org.jdom.Element root = jdom_in.getRootElement();
  root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
  root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",
    MCRDefaults.XSI_URL));
  // check the label
  String label = root.getAttributeValue("label");
  if ((label == null) || ((label = label.trim()).length() ==0)) { 
    root.setAttribute("label",ID.getId()); }
  // remove the path elements from the incoming
  org.jdom.Element pathes = root.getChild("pathes");
  if (pathes != null) { root.removeChildren("pathes"); }
  // structure
  boolean hasparent = false;
  org.jdom.Element structure = root.getChild("structure");
  if (structure == null) {
    root.addContent(new Element("structure")); }
  else {
    List structurelist = structure.getChildren();
    if (structurelist != null) { 
      int structurelistlen = structurelist.size();
      for (int j=0;j<structurelistlen;j++) {
        org.jdom.Element datatag = (org.jdom.Element)structurelist.get(j);
        String mcrclass = datatag.getAttributeValue("class");
        List datataglist = datatag.getChildren();
        int datataglistlen = datataglist.size();
        for (int k=0;k<datataglistlen;k++) {
          org.jdom.Element datasubtag = (org.jdom.Element)datataglist.get(k);
          // MCRMetaLinkID
          if (mcrclass.equals("MCRMetaLinkID")) {
            String href = datasubtag.getAttributeValue("href");
            if (href == null) { 
              datatag.removeContent(datasubtag); k--; datataglistlen--; continue; }
            if (datasubtag.getAttribute("type") != null) {
              datasubtag.getAttribute("type").setNamespace(
                org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
            if (datasubtag.getAttribute("href") != null) {
              datasubtag.getAttribute("href").setNamespace(
                org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
            if (datasubtag.getAttribute("title") != null) {
              datasubtag.getAttribute("title").setNamespace(
                org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
            if (datasubtag.getAttribute("label") != null) {
              datasubtag.getAttribute("label").setNamespace(
                org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
            try {
              MCRMetaLinkID test = new MCRMetaLinkID();
              test.setFromDOM(datasubtag);
              if (!test.isValid()) throw new MCRException("");
              }
            catch (Exception e) {
              errorlog.add("Element "+datasubtag.getName()+" is not valid.");
              datatag.removeContent(datasubtag); k--; datataglistlen--;
              continue;
              }
            continue;
            }
          }
        datataglist = datatag.getChildren();
        if (datataglist.size() == 0) {
          structure.removeContent(datatag); j--; structurelistlen--; }
        else {
          if (datatag.getName().equals("parents")) { hasparent = true; } }
        }
      }
    }
  // set the schema
  String mcr_schema = "";
  if (hasparent) {
     logger.debug("A parrent was found."); }
  else {
     logger.debug("No parrent was found."); }
  mcr_schema = "datamodel-"+ID.getTypeId()+".xsd";
  root.setAttribute("noNamespaceSchemaLocation",mcr_schema,
    org.jdom.Namespace.getNamespace("xsi",MCRDefaults.XSI_URL));
  // metadata
  org.jdom.Element metadata = root.getChild("metadata");
  metadata.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);
  List metadatalist = metadata.getChildren();
  int metadatalistlen = metadatalist.size();
  for (int j=0;j<metadatalistlen;j++) {
    org.jdom.Element datatag = (org.jdom.Element)metadatalist.get(j);
    String mcrclass = datatag.getAttributeValue("class");
    List datataglist = datatag.getChildren();
    int datataglistlen = datataglist.size();
    for (int k=0;k<datataglistlen;k++) {
      org.jdom.Element datasubtag = (org.jdom.Element)datataglist.get(k);
      // MCRMetaLangText
      if (mcrclass.equals("MCRMetaLangText")) {
        String text = datasubtag.getTextNormalize();
        if ((text == null) || ((text = text.trim()).length() ==0)) { 
          datatag.removeContent(datasubtag); k--; datataglistlen--; continue; }
        if (datasubtag.getAttribute("lang") != null) {
          datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE); }
        try {
          MCRMetaLangText test = new MCRMetaLangText();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      // MCRMetaClassification
      if (mcrclass.equals("MCRMetaClassification")) {
        String categid = datasubtag.getAttributeValue("categid");
        if (categid == null) { 
          datatag.removeContent(datasubtag); k--; datataglistlen--; continue; }
        try {
          MCRMetaClassification test = new MCRMetaClassification();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      // MCRMetaLink
      if (mcrclass.equals("MCRMetaLink")) {
        String href = datasubtag.getAttributeValue("href");
        if (href == null) { 
          datatag.removeContent(datasubtag); k--; datataglistlen--; continue; }
        if (datasubtag.getAttribute("type") != null) {
          datasubtag.getAttribute("type").setNamespace(
            org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
        if (datasubtag.getAttribute("href") != null) {
          datasubtag.getAttribute("href").setNamespace(
            org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
        if (datasubtag.getAttribute("title") != null) {
          datasubtag.getAttribute("title").setNamespace(
            org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
        if (datasubtag.getAttribute("label") != null) {
          datasubtag.getAttribute("label").setNamespace(
            org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
        try {
          MCRMetaLink test = new MCRMetaLink();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      // MCRMetaLinkID
      if (mcrclass.equals("MCRMetaLinkID")) {
        String href = datasubtag.getAttributeValue("href");
        if (href == null) { 
          datatag.removeContent(datasubtag); k--; datataglistlen--; continue; }
        if (datasubtag.getAttribute("type") != null) {
          datasubtag.getAttribute("type").setNamespace(
            org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
        if (datasubtag.getAttribute("href") != null) {
          datasubtag.getAttribute("href").setNamespace(
            org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
        if (datasubtag.getAttribute("title") != null) {
          datasubtag.getAttribute("title").setNamespace(
            org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
        if (datasubtag.getAttribute("label") != null) {
          datasubtag.getAttribute("label").setNamespace(
            org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL)); }
        try {
          MCRMetaLinkID test = new MCRMetaLinkID();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      // MCRMetaDate
      if (mcrclass.equals("MCRMetaDate")) {
        String text = datasubtag.getTextNormalize();
        if ((text == null) || ((text = text.trim()).length() ==0)) { 
          datatag.removeContent(datasubtag); k--; datataglistlen--; continue; }
        if (datasubtag.getAttribute("lang") != null) {
          datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE); }
        try {
          MCRMetaDate test = new MCRMetaDate();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      // MCRMetaNumber
      if (mcrclass.equals("MCRMetaNumber")) {
        String text = datasubtag.getTextNormalize();
        if ((text == null) || ((text = text.trim()).length() ==0)) { 
          datatag.removeContent(datasubtag); k--; datataglistlen--; continue; }
        if (datasubtag.getAttribute("lang") != null) {
          datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE); }
        try {
          MCRMetaNumber test = new MCRMetaNumber();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      if (mcrclass.equals("MCRMetaAddress")) {
        if (datasubtag.getAttribute("lang") != null) {
          datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE); }
        try {
          MCRMetaAddress test = new MCRMetaAddress();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      if (mcrclass.equals("MCRMetaInstitutionName")) {
        if (datasubtag.getAttribute("lang") != null) {
          datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE); }
        try {
          MCRMetaInstitutionName test = new MCRMetaInstitutionName();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      if (mcrclass.equals("MCRMetaPersonName")) {
        if (datasubtag.getAttribute("lang") != null) {
          datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE); }
        try {
          MCRMetaPersonName test = new MCRMetaPersonName();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      if (mcrclass.equals("MCRMetaBoolean")) {
        try {
          MCRMetaBoolean test = new MCRMetaBoolean();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      logger.error("To do for type "+mcrclass+" not found.");
      }
    datataglist = datatag.getChildren();
    if (datataglist.size() == 0) {
          metadata.removeContent(datatag); j--; metadatalistlen--; }
    }
  // service
  org.jdom.Element service = root.getChild("service");
  List servicelist = service.getChildren();
  int servicelistlen = servicelist.size();
  for (int j=0;j<servicelistlen;j++) {
    org.jdom.Element datatag = (org.jdom.Element)servicelist.get(j);
    if (datatag.getName().equals("servflags")) {
      MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
      String userid = mcrSession.getCurrentUserID();
      MCRMetaLangText line = new MCRMetaLangText("servflags","servflag",
        "de","",0,"plain","User:"+userid);
      datatag.addContent(line.createXML());
      }
    String mcrclass = datatag.getAttributeValue("class");
    List datataglist = datatag.getChildren();
    int datataglistlen = datataglist.size();
    for (int k=0;k<datataglistlen;k++) {
      org.jdom.Element datasubtag = (org.jdom.Element)datataglist.get(k);
      // MCRMetaLangText
      if (mcrclass.equals("MCRMetaLangText")) {
        String text = datasubtag.getTextNormalize();
        if ((text == null) || ((text = text.trim()).length() ==0)) { 
          datatag.removeContent(datasubtag); k--; datataglistlen--; continue; }
        if (datasubtag.getAttribute("lang") != null) {
          datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE); }
        try {
          MCRMetaLangText test = new MCRMetaLangText();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      // MCRMetaDate
      if (mcrclass.equals("MCRMetaDate")) {
        String text = datasubtag.getTextNormalize();
        if ((text == null) || ((text = text.trim()).length() ==0)) { 
          datatag.removeContent(datasubtag); k--; datataglistlen--; continue; }
        if (datasubtag.getAttribute("lang") != null) {
          datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE); }
        try {
          MCRMetaDate test = new MCRMetaDate();
          test.setFromDOM(datasubtag);
          if (!test.isValid()) throw new MCRException("");
          }
        catch (Exception e) {
          errorlog.add("Element "+datasubtag.getName()+" is not valid.");
          datatag.removeContent(datasubtag); k--; datataglistlen--;
          continue;
          }
        continue;
        }
      }
    datataglist = datatag.getChildren();
    if (datataglist.size() == 0) {
          service.removeContent(datatag); j--; servicelistlen--; }
    }
  // load the incoming
  MCRObject obj = new MCRObject();
  org.jdom.Document jdom_out = jdom_in;
  try {
    // load the JODM object
    byte [] xml = MCRUtils.getByteArray(jdom_in);
    obj.setFromXML(xml,true);
    // return the XML tree
    jdom_out = obj.createXML();
    }
  catch (MCRException e) {
    errorlog.add(e.getMessage()); 
    Exception ex = e.getException();
    if (ex != null) {
      errorlog.add(ex.getMessage()); 
      }
    }
  errorHandlerValid(job,errorlog,ID,step,lang);
  return jdom_out;
  }
  
/**
 * A method to handle valid errors.
 **/
private final void errorHandlerValid(MCRServletJob job, ArrayList logtext,
  MCRObjectID ID, String step, String lang) throws Exception
  {
  if (logtext.size()==0) return;
  // write to the log file
  for (int i=0;i<logtext.size();i++) {
    logger.error((String)logtext.get(i)); }
  // prepare editor with error messages
  String pagedir = CONFIG.getString( "MCR.editor_page_dir","" );
  String myfile = pagedir+"editor_error_formular.xml";
  org.jdom.Document jdom = null;
  try {
    InputStream in = (new URL(getBaseURL()+myfile+"?XSL.Style=xml"))
      .openStream();
    if( in == null ) throw new MCRConfigurationException(
      "Can't read editor file " + myfile );
    jdom = new org.jdom.input.SAXBuilder().build( in );
    org.jdom.Element root = jdom.getRootElement();
    List sectionlist = root.getChildren("section");
    for (int i=0;i<sectionlist.size();i++) {
      org.jdom.Element section = (org.jdom.Element)sectionlist.get(i);
      if (!section.getAttributeValue("lang",org.jdom.Namespace.XML_NAMESPACE)
        .equals(lang.toLowerCase())) { continue; }
      org.jdom.Element p = new org.jdom.Element("p");
      section.addContent(0,p);
      org.jdom.Element center = new org.jdom.Element("center");
      // the error message
      org.jdom.Element table = new org.jdom.Element("table");
      table.setAttribute("width","80%");
      for (int j=0;j<logtext.size();j++) {
        org.jdom.Element tr = new org.jdom.Element("tr");
        org.jdom.Element td = new org.jdom.Element("td");
        org.jdom.Element el = new org.jdom.Element("font");
        el.setAttribute("color","red");
        el.addContent((String)logtext.get(j));
        td.addContent(el);
        tr.addContent(td);
        table.addContent(tr);
        }
      center.addContent(table);
      section.addContent(1,center);
      p = new org.jdom.Element("p");
      section.addContent(2,p);
      // the edit button
      org.jdom.Element form = section.getChild("form");
      form.setAttribute("action",(new URL(getBaseURL()))+"servlets/MCRStartEditorServlet");
      org.jdom.Element input1 = new org.jdom.Element("input");
      input1.setAttribute("name","lang");
      input1.setAttribute("type","hidden");
      input1.setAttribute("value",lang);
      form.addContent(input1);
      org.jdom.Element input2 = new org.jdom.Element("input");
      input2.setAttribute("name","se_mcrid");
      input2.setAttribute("type","hidden");
      input2.setAttribute("value",ID.getId());
      form.addContent(input2);
      org.jdom.Element input3 = new org.jdom.Element("input");
      input3.setAttribute("name","type");
      input3.setAttribute("type","hidden");
      input3.setAttribute("value",ID.getTypeId());
      form.addContent(input3);
      }
    }
  catch (org.jdom.JDOMException e) {
    throw new MCRException("Can't read editor file "+myfile+" or it has a parse error.",e);
    }
System.out.println(jdom);
  // restart editor
  job.getRequest().setAttribute( "MCRLayoutServlet.Input.JDOM",  jdom  );
  job.getRequest().setAttribute( "XSL.Style", lang );
  RequestDispatcher rd = getServletContext().getNamedDispatcher( "MCRLayoutServlet" );
  rd.forward(job.getRequest(), job.getResponse());
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
  String pagedir = CONFIG.getString( "MCR.editor_page_dir","" );
  job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+pagedir+"editor_error_store.xml")); 
  }

}
