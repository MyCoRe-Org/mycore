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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRMailer;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.frontend.fileupload.MCRUploadHandlerInterface;
import org.mycore.frontend.fileupload.MCRUploadHandlerManager;
import org.mycore.frontend.workflow.MCRWorkflowManager;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;

/**
 * The servlet start the MyCoRe editor session with some parameters from
 * a HTML form. The parameters are:<br />
 * <li>type - the MCRObjectID type like schrift, text ...</li><br />
 * <li>step - the name of the step like author, editor  ...</li><br />
 * <li>todo - the mode of the editor start like new or edit or change or delete</li><br />
 * <li>tf_mcrid - the MCRObjectID of the data they came from a input field</li><br />
 * <li>se_mcrid - the MCRObjectID of the data they came from a select field</li><br />
 * <li>re_mcrid - the MCRObjectID of the data they is in relation to tf_mcrid/se_mcrid</li><br />
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */

public class MCRStartEditorServlet extends MCRServlet
  {
  // The configuration
  private static Logger LOGGER=Logger.getLogger(MCRStartEditorServlet.class);

  // The workflow manager
  private static MCRWorkflowManager WFM = null;

  // the SQL data table
  private static MCRXMLTableManager MCR_XMLTABLE = null;

  // The file slash
  private static String SLASH = System.getProperty("file.separator");;

  /** Initialisation of the servlet */
  public void init()
    {
    super.init();
    // Workflow Manager
    WFM = MCRWorkflowManager.instance();
    // XML table manager
    MCR_XMLTABLE = MCRXMLTableManager.instance();
    }

  /**
   * This method overrides doGetPost of MCRServlet.<br />
   * The <b>todo</b> value corresponds with <b>tf_mcrid</b> or <b>se_mcrid</b> and with the type of the data model for the privileges that the user need. for some actions you need a third value of re_mcrid for relations (object - derivate). <br />
   *
   * The table shows the possible todo's in the server:<br />
   * <table>
   * <tr><th>TODO</th><th>MCRObjectID from</th><th>used privileg</th><th>description</th></tr>
   * <tr><td>seditobj</td><td>tf_mcrid</td><td>modify-type</td><td>edit an object in the server</td></tr>
   * <tr><td>sdelobj</td><td>tf_mcrid</td><td>delete-type</td><td>delete an object from the server</tr>
   * </table><br />
   *
   * The table shows the possible todo's in the workflow:<br />
   * <table>
   * <tr><th>TODO</th><th>MCRObjectID from</th><th>used privileg</th><th>description</th></tr>
   * <tr><td>wnewobj</td><td></td><td>create-type</td><td>add a new object to the workflow</td></tr>
   * <tr><td>wnewder</td><td>se_mcrid</td><td>create-type</td><td>add a new derivate to the workflow</td></tr>
   * <tr><td>waddfile</td><td>se_mcrid<br />re_mcrid</td><td>create-type</td><td>add a new file to a derivate in the workflow</td></tr>
   * <tr><td>weditobj</td><td>se_mcrid</td><td>modify-type</td><td>edit an object in the workflow</td></tr>
   * <tr><td>weditder</td><td>se_mcrid</td><td>modify-type</td><td>edit an derivate in the workflow</td></tr>
   * <tr><td>wcommit</td><td>se_mcrid</td><td>commit-type</td><td>commit a document to the server</td></tr>
   * <tr><td>wdelobj</td><td>se_mcrid</td><td>delete-type</td><td>delete an object from the workflow</td></tr>
   * <tr><td>wdelder</td><td>se_mcrid<br />re_mcrid</td><td>delete-type</td><td>delete a derivate from the workflow</td></tr>
   * </table><br />
   * <li> If the privileg is not correct it calls <em>editor_error_user.xml</em>.</li><br />
   * <li> If the MCRObjectID is not correct it calls <em>editor_error_mcrid.xml</em>.</li><br />
   * <li> If a store error is occured it calls <em>editor_error_store.xml</em>.</li><br />
   * <li> If <b>CANCEL</b> was pressed it calls <em>editor_cancel.xml</em>.</li><br />
   * <li> If  the privileg is correct it starts the file editor_form_<em>step-type</em>.xml.</li><br />
   */
  public void doGetPost(MCRServletJob job) throws Exception
    {
    // get the MCRSession object for the current thread from the session manager.
    MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
    // get the current user
    String userid = mcrSession.getCurrentUserID();
    //userid = "administrator";
    LOGGER.debug("Curren user for actions = "+userid);
    ArrayList privs = MCRUserMgr.instance().retrieveAllPrivsOfTheUser(userid);
    // get the current language
    String mylang = mcrSession.getCurrentLanguage();
    LOGGER.info("LANG = "+mylang);

    // read the parameter
    // get the step
    String mystep = getProperty(job.getRequest(), "step");
    if (mystep == null) { mystep = ""; }
    if (mystep.length() != 0) { mystep = mystep+"-"; }
    LOGGER.info("STEP = "+mystep);
    // get the type
    String mytype = getProperty(job.getRequest(), "type");
    if (mytype == null) { mytype =
      CONFIG.getString( "MCR.default_project_type", "document" ); }
    if (mytype.length() == 0) { mytype =
      CONFIG.getString( "MCR.default_project_type", "document" ); }
    LOGGER.info("TYPE = "+mytype);
    // get what is to do
    String mytodo = getProperty(job.getRequest(), "todo");
    if ((mytodo==null) || ((mytodo=mytodo.trim()).length()==0)) {
      mytodo = "wnewobj"; }
    if (!mytodo.equals("wnewobj") && !mytodo.equals("wnewder") &&
      !mytodo.equals("waddfile") && !mytodo.equals("weditobj") &&
      !mytodo.equals("weditder") && !mytodo.equals("wcommit") &&
      !mytodo.equals("wdelobj") && !mytodo.equals("wdelder") &&
      !mytodo.equals("seditobj") && !mytodo.equals("sdelobj")) {
      mytodo = "wnewobj"; }
    LOGGER.info("TODO = "+mytodo);
    // get the MCRObjectID from the text filed (TF)
    String mytfmcrid = getProperty(job.getRequest(), "tf_mcrid");
    try {
      MCRObjectID testid = new MCRObjectID(mytfmcrid); }
    catch (Exception e) {
      mytfmcrid = ""; }
    if ((mytfmcrid==null) || ((mytfmcrid=mytfmcrid.trim()).length()==0)) {
      String defaproject = CONFIG.getString( "MCR.default_project_id","MCR" );
      String myproject = CONFIG.getString( "MCR."+mytype+"_project_id","MCR" );
      if (myproject.equals("MCR")) { myproject = defaproject; }
      myproject = myproject+"_"+mytype;
      MCRObjectID mcridnext = new MCRObjectID();
      mcridnext.setNextFreeId( myproject );
      String workdir = CONFIG.getString("MCR.editor_"+mytype+"_directory","/");
      File workf = new File(workdir);
      if (workf.isDirectory()) {
        String [] list = workf.list();
        for (int i=0;i<list.length;i++) {
          if (!list[i].startsWith(myproject)) continue;
          try {
            MCRObjectID mcriddir = new MCRObjectID(list[i].substring(0,list[i].length()-4));
            if (mcridnext.getNumberAsInteger() <= mcriddir.getNumberAsInteger()) {
              mcriddir.setNumber(mcriddir.getNumberAsInteger()+1);
              mcridnext = mcriddir;
              }
            }
          catch (Exception e) { }
          }
        }
      mytfmcrid = mcridnext.getId();
      }
    LOGGER.info("MCRID (TF) = "+mytfmcrid);
    // get the MCRObjectID from the selcet field (SE)
    String mysemcrid = getProperty(job.getRequest(), "se_mcrid");
    if (mysemcrid == null) {
      mysemcrid = ""; }
    else {
      try {
        MCRObjectID testid = new MCRObjectID(mysemcrid); }
      catch (Exception e) {
        mysemcrid = ""; }
      }
    LOGGER.info("MCRID (SE) = "+mysemcrid);
    // get the MCRObjectID from the relation field (RE)
    String myremcrid = getProperty(job.getRequest(), "re_mcrid");
    if (myremcrid == null) {
      myremcrid = ""; }
    else {
      try {
        MCRObjectID testid = new MCRObjectID(myremcrid); }
      catch (Exception e) {
        myremcrid = ""; }
      }
    LOGGER.info("MCRID (RE) = "+myremcrid);

    LOGGER.debug("Base URL : "+getBaseURL());

    // set the pages and language
    String pagedir = CONFIG.getString( "MCR.editor_page_dir",
      "" );
    String myfile = pagedir+"editor_form_"+mystep+mytype+".xml";
    String cancelpage = pagedir+CONFIG.getString( "MCR.editor_page_cancel",
      "editor_cancel.xml" );
    String commitpage = pagedir+CONFIG.getString( "MCR.editor_page_commit",
      "editor_loaded.xml" );
    String deletepage = pagedir+CONFIG.getString( "MCR.editor_page_delete",
      "editor_delete.xml" );
    String usererrorpage = pagedir+CONFIG.getString( "MCR.editor_page_error_user",
      "editor_error_user.xml" );
    String mcriderrorpage = pagedir+CONFIG.getString( "MCR.editor_page_error_mcrid",
      "editor_error_mcrid.xml" );
    String storeerrorpage = pagedir+CONFIG.getString( "MCR.editor_page_error_store",
      "editor_error_store.xml" );
    String deleteerrorpage = pagedir+CONFIG.getString( "MCR.editor_page_error_delete",
      "editor_error_delete.xml" );
    String mymcrid = "";

    // action WNEWOBJ - create a new object
    if (mytodo.equals("wnewobj")) {
      String priv = "create-"+mytype;
      if (!privs.contains(priv)) {
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+usererrorpage));
        return;
        }

      String base = getBaseURL() + myfile;
      Properties params = new Properties();
      params.put( "XSL.editor.source.new", "true" );
      params.put( "XSL.editor.cancel.url", getBaseURL()+cancelpage);
      params.put( "XSL.target.param.0", "mcrid=" + mytfmcrid );
      params.put( "XSL.target.param.1", "type="  + mytype );
      params.put( "XSL.target.param.2", "step="  + mystep );
      job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL( base, params )));
      return;
      }

    // action WNEWDER - create a new derivate
    if (mytodo.equals("wnewder")) {
      String priv = "create-"+mytype;
      if (!privs.contains(priv)) {
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+usererrorpage));
        return;
        }
      myremcrid = mysemcrid;
      mysemcrid =  WFM.createDerivate(myremcrid);
      mytodo = "waddfile";
      }

    // action WADDFILE - create a new file in the derivate
    if (mytodo.equals("waddfile")) {
      if (!checkAccess(myremcrid,userid,privs,"create-"+mytype,false)) {
        job.getResponse().sendRedirect(getBaseURL()+usererrorpage);
        return;
        }
      mymcrid = mysemcrid;
      MCRUploadHandlerManager fum = MCRUploadHandlerManager.instance();
      MCRUploadHandlerInterface fuh = fum.getNewHandle();
      StringBuffer sb = new StringBuffer(pagedir);
      sb.append("editor_").append(mytype).append("_editor.xml");
      fuh.set(myremcrid,mysemcrid,"new",getBaseURL()+sb.toString());
      String fuhid = fum.register(fuh);
      mymcrid = mysemcrid;
      myfile = pagedir+"fileupload.xml";
      String base = getBaseURL() + myfile;
      Properties params = new Properties();
      params.put( "XSL.UploadID", fuhid);
      params.put( "XSL.editor.source.new", "true" );
      params.put( "XSL.editor.cancel.url", getBaseURL()+cancelpage);
      params.put( "XSL.target.param.0", "mcrid=" + mysemcrid );
      params.put( "XSL.target.param.1", "type="  + mytype );
      params.put( "XSL.target.param.2", "step="  + mystep );
      params.put( "XSL.target.param.3", "remcrid="  + myremcrid );
      job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL( base, params )));
      return;
      }

    // action WEDITOBJ - change the object in the workflow
    if (mytodo.equals("weditobj")) {
      if (!checkAccess(mysemcrid,userid,privs,"modify-"+mytype,false)) {
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+usererrorpage));
        return;
        }
      if (mysemcrid.length()==0) {
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+mcriderrorpage));
        return;
        }
      String base = getBaseURL() + myfile;
      Properties params = new Properties();
      params.put( "XSL.editor.source.url", "file://"+CONFIG.getString("MCR.editor_"+mytype+"_directory")+"/"+mysemcrid+".xml" );
      params.put( "XSL.editor.cancel.url", getBaseURL()+cancelpage);
      params.put( "XSL.target.param.0", "mcrid=" + mysemcrid );
      params.put( "XSL.target.param.1", "type="  + mytype );
      params.put( "XSL.target.param.2", "step="  + mystep );
      job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL( base, params )));
      return;
      }

   // action WDELOBJ - delete an object from the workflow
   if (mytodo.equals("wdelobj")) {
     if (!checkAccess(mysemcrid,userid,privs,"delete-"+mytype,false)) {
       job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+usererrorpage));
       return;
       }
     if (mysemcrid.length()==0) {
       job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+mcriderrorpage));
       return;
       }
     StringBuffer sb = new StringBuffer(pagedir);
     sb.append("editor_").append(mytype).append("_editor.xml");
     MCRObjectID mid = new MCRObjectID(mysemcrid);
     WFM.deleteMetadataObject(mytype,mysemcrid);
     List addr = WFM.getMailAddress(mytype,"wdelobj");
     if (addr.size() != 0) {
       String sender = WFM.getMailSender();
       String appl = CONFIG.getString("MCR.editor_mail_application_id","DocPortal");
       String subject = "Automaticaly message from "+appl;
       StringBuffer text = new StringBuffer();
       text.append("Es wurde ein Objekt vom Typ ").append(mytype)
         .append(" mit der ID ").append(mysemcrid)
         .append(" aus dem Workflow gelöscht.");
       LOGGER.info(text.toString());
       try {
         MCRMailer.send(sender,addr,subject,text.toString(),false); }
       catch (Exception ex) {
       LOGGER.error("Can't send a mail to "+addr); }
       }
     job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+sb.toString()));
     return;
     }

   // action WDELDER - delete a derivate from the workflow
   if (mytodo.equals("wdelder")) {
     if (!checkAccess(myremcrid,userid,privs,"delete-"+mytype,false)) {
       job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+usererrorpage));
       return;
       }
     if (mysemcrid.length()==0) {
       job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+mcriderrorpage));
       return;
       }
     StringBuffer sb = new StringBuffer(pagedir);
     sb.append("editor_").append(mytype).append("_editor.xml");
     MCRObjectID mid = new MCRObjectID(mysemcrid);
     WFM.deleteDerivateObject(mytype,mysemcrid);
     List addr = WFM.getMailAddress(mytype,"wdelder");
     if (addr.size() != 0) {
       String sender = WFM.getMailSender();
       String appl = CONFIG.getString("MCR.editor_mail_application_id","DocPortal");
       String subject = "Automaticaly message from "+appl;
       StringBuffer text = new StringBuffer();
       text.append("Es wurde ein Derivate mit der ID ").append(mysemcrid)
         .append(" aus dem Workflow gelöscht.");
       LOGGER.info(text.toString());
       try {
         MCRMailer.send(sender,addr,subject,text.toString(),false); }
       catch (Exception ex) {
       LOGGER.error("Can't send a mail to "+addr); }
       }
     job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+sb.toString()));
     return;
     }

    // action WCOMMIT - commit a object from the workflow to the server
    if (mytodo.equals("wcommit")) {
      if (!checkAccess(mysemcrid,userid,privs,"commit-"+mytype,false)) {
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+usererrorpage));
        return;
        }
      if (mysemcrid.length()==0) {
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+mcriderrorpage));
        return;
        }
      try {
        if (WFM.commitMetadataObject(mytype,mysemcrid)) {
          WFM.deleteMetadataObject(mytype,mysemcrid);
          List addr = WFM.getMailAddress(mytype,"wcommit");
          if (addr.size() != 0) {
            String sender = WFM.getMailSender();
            String appl = CONFIG.getString("MCR.editor_mail_application_id","DocPortal");
            String subject = "Automaticaly message from "+appl;
            StringBuffer text = new StringBuffer();
            text.append("Es wurde ein Objekt vom Typ ").append(mytype)
              .append(" mit der ID ").append(mysemcrid)
              .append(" aus dem Workflow in das System geladen.");
            LOGGER.info(text.toString());
            try {
              MCRMailer.send(sender,addr,subject,text.toString(),false); }
            catch (Exception ex) {
              LOGGER.error("Can't send a mail to "+addr); }
            }
          StringBuffer sb = (new StringBuffer("MCR.type_")).append(mytype).append("_in");
          String searchtype = CONFIG.getString(sb.toString(),mytype);
          sb = new StringBuffer("servlets/MCRQueryServlet?mode=ObjectMetadata&type=");
          sb.append(searchtype).append("&hosts=local&query=%2Fmycoreobject[%40ID%3D\'").append(mysemcrid).append("\']");
          myfile = sb.toString();
          }
        else {
          myfile = storeerrorpage; }
        }
      catch (MCRException e) { myfile = storeerrorpage; }
      job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+myfile));
      return;
      }

    // action SEDITOBJ in the database
    if (mytodo.equals("seditobj")) {
      if (!checkAccess(mytfmcrid,userid,privs,"modify-"+mytype,true)) {
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+usererrorpage));
        return;
        }
      if (mytfmcrid.length()==0) {
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+mcriderrorpage));
        return;
        }
      String url = getBaseURL()+"servlets/MCRQueryServlet?XSL.Style=editor&"+
        "mode=ObjectMetadata&type="+mytype+"&hosts="+
        CONFIG.getString("MCR.editor_baseurl","local")+
        "&query=/mycoreobject%5b@ID=%22"+mytfmcrid+"%22%5d";
      String base = getBaseURL() + myfile;
      Properties params = new Properties();
      params.put( "XSL.editor.source.url", url );
      params.put( "XSL.editor.cancel.url", getBaseURL()+cancelpage);
      params.put( "XSL.target.param.0", "mcrid=" + mytfmcrid );
      params.put( "XSL.target.param.1", "type="  + mytype );
      params.put( "XSL.target.param.2", "step="  + mystep );
      job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL( base, params )));
      return;
      }

   // action SDELOBJ from the database
   if (mytodo.equals("sdelobj")) {
     if (!checkAccess(mytfmcrid,userid,privs,"delete-"+mytype,true)) {
       job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+usererrorpage));
       return;
       }
     if (mytfmcrid.length()==0) {
       job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+mcriderrorpage));
       return;
       }
     MCRObject obj = new MCRObject();
     try {
       obj.deleteFromDatastore(mytfmcrid);
       myfile = deletepage;
       }
     catch (Exception e) { myfile = deleteerrorpage; }
     List addr = WFM.getMailAddress(mytype,"sdelobj");
     if (addr.size() != 0) {
       String sender = WFM.getMailSender();
       String appl = CONFIG.getString("MCR.editor_mail_application_id","DocPortal");
       String subject = "Automaticaly message from "+appl;
       StringBuffer text = new StringBuffer();
       text.append("Es wurde ein Objekt vom Typ ").append(mytype)
         .append(" mit der ID ").append(mytfmcrid)
         .append(" aus dem Server gelöscht.");
       LOGGER.info(text.toString());
       try {
         MCRMailer.send(sender,addr,subject,text.toString(),false); }
       catch (Exception ex) {
       LOGGER.error("Can't send a mail to "+addr); }
       }
     job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL()+myfile));
     return;
     }
   }

  /**
   * The method check the access rights. It returns true if<br />
   * <ul>
   * <li>the user is the owner of the object and his group has the work privileges</li>
   * <li>the group of the user is member in the group of the author and the users grou has the privileg 'editor'</li>
   * </ul>
   *
   * @param mcrid the MCRObjectID
   * @param user the user name
   * @param privs an ArrayList of the privileges of the user
   * @param action the action privileg that is expected
   * @param source true if the data come from the database, false ift it come form the workflow
   * @return true if the access is successful, else return false
   **/
  private final boolean checkAccess(String mcrid, String user, ArrayList privs,
    String action, boolean source) {
    if (user.equals("administrator")) { return true; }
    if (!privs.contains(action)) { return false; }
    // get the user of the object
    org.jdom.Document jdom = null;
    MCRObjectID id = new MCRObjectID(mcrid);
    if (source) {
      byte [] xml = MCR_XMLTABLE.retrieve(id.getTypeId(),id);
      jdom = MCRXMLHelper.parseXML(xml,false);
      }
    else {
      String dirname = WFM.getDirectoryPath(id.getTypeId());
      StringBuffer sb = new StringBuffer(1024);
      sb.append(dirname).append(SLASH).append(mcrid).append(".xml");
      try {
        LOGGER.debug("Read "+sb.toString());
        jdom = MCRXMLHelper.parseURI(sb.toString(),false);
        }
      catch (Exception e) {
        LOGGER.debug("Parse error in file "+sb.toString()); return true; }
      }
    if (jdom == null) { return false; }
    org.jdom.Element root = jdom.getRootElement();
    org.jdom.Element service = root.getChild("service");
    org.jdom.Element servflags = service.getChild("servflags");
    ArrayList ar = new ArrayList();
    List list = servflags.getChildren("servflag");
    if (list != null) {
      String text = null;
      for (int i=0;i<list.size();i++) {
        org.jdom.Element servflag = (org.jdom.Element)list.get(i);
        text = servflag.getText();
        if (text.startsWith("User:")) {
          ar.add(text.substring(5,text.length())); }
        }
      }
    for (int i=0;i<ar.size();i++) {
      LOGGER.debug("User of the object "+((String)ar.get(i))); }
    // check the user is in the servflags
    if (ar.contains(user)) { return true; }
    // check that the user is in the master group of the object user and
    // has the privileg 'editor'
    if (!privs.contains("editor")) { return false; }

    // Determine the list of all groups the current user is a member of, including
    // the implicit ones.

    MCRUser currentUser = MCRUserMgr.instance().retrieveUser(user);
    ArrayList allCurrentUserGroupIDs = currentUser.getAllGroupIDs();

    // For all authors (users in the object servflags) we now check if the current
    // user directly is a member of the primary group of the author or implicitly
    // is a member of a group which itself is a member of the primary group of
    // the author.

    for (int i=0; i<ar.size(); i++) {
      String primaryGroupID = MCRUserMgr.instance().
        getPrimaryGroupIDOfUser((String)ar.get(i));
      MCRGroup primaryGroup = MCRUserMgr.instance().
        retrieveGroup(primaryGroupID);
      if (primaryGroup.hasMember(currentUser)) { return true; }
      ArrayList memberGroupIDsOfPrimaryGroup = primaryGroup.getMemberGroupIDs();
      for (int j = 0; j < allCurrentUserGroupIDs.size(); j++) {
        if (memberGroupIDsOfPrimaryGroup.contains((String)allCurrentUserGroupIDs.get(j))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Builds an url that can be used to redirect the client browser
   * to another page, including http request parameters. The request
   * parameters will be encoded as http get request.
   *
   * @param baseURL the base url of the target webpage
   * @param parameters the http request parameters
   **/
  private String buildRedirectURL( String baseURL, Properties parameters )
  {
    StringBuffer redirectURL = new StringBuffer( baseURL );
    boolean first = true;

    for( Enumeration e = parameters.keys(); e.hasMoreElements(); )
    {
      if( first )
      {
        redirectURL.append( "?" );
        first = false;
      }
      else redirectURL.append( "&" );

      String name  = (String)( e.nextElement() );
      String value = null;
      try {
        value = URLEncoder.encode( parameters.getProperty( name ), "UTF-8" ); }
      catch (UnsupportedEncodingException ex) {
        value = parameters.getProperty( name ); }

      redirectURL.append( name ).append( "=" ).append( value );
    }

    LOGGER.debug( "Sending redirect to " + redirectURL.toString() );
    return redirectURL.toString();
  }
}
