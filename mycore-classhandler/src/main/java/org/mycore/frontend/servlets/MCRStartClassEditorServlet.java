/**
 * $RCSfile: MCRStartClassEditorServlet.java,v $
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

import static org.jdom.Namespace.XML_NAMESPACE;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.classifications.MCRClassificationEditor;
import org.mycore.datamodel.classifications.MCRClassificationEditorFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;

/**
 * The servlet start the MyCoRe class editor session with some parameters from a
 * HTML form. The parameters are:<br />
 * <li> name="todo" values like 'create-classification, modify-classification,
 * delete-classification, up and down' </li>
 * <li> name="path" uri to page after editactions </li>
 * <li> name="clid" classification id </li>
 * <li> name="categid" category id </li>
 * 
 * @author Anja Schaar
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */

public class MCRStartClassEditorServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRStartClassEditorServlet.class);

    private static enum ReturnStatus {
        success, fail
    }

    /**
     * Replace the doGetPost method of MCRServlet. This method will be called
     * two times when using the classification editor. Firtst time it prepare
     * date for the editor and second time it execute the operation.
     */
    public void think(MCRServletJob job) throws Exception {
        String todo = "";
        String todo2 = "";
        String clid = "";
        String categid = "";
        String path = "";

        // read the XML data if given from Editorsession
        MCREditorSubmission sub = (MCREditorSubmission) (job.getRequest().getAttribute("MCREditorSubmission"));

        // read the parameter
        MCRRequestParameters parms;
        if (sub == null)
            parms = new MCRRequestParameters(job.getRequest());
        else {
            parms = sub.getParameters();
        }

        // read the parameter
        todo = parms.getParameter("todo");
        todo2 = parms.getParameter("todo2");
        path = parms.getParameter("path");

        // get the Classification
        clid = parms.getParameter("clid");

        categid = parms.getParameter("categid");

        if (todo == null)
            todo = "";
        if (todo2 == null)
            todo2 = "";

        LOGGER.debug("MCRStartClassEditorServlet TODO: " + todo);
        LOGGER.debug("MCRStartClassEditorServlet TODO2: " + todo2);
        LOGGER.debug("MCRStartClassEditorServlet CLID: " + clid);
        LOGGER.debug("MCRStartClassEditorServlet CATEGID: " + categid);

        String pagedir = MCRConfiguration.instance().getString("MCR.classeditor_page_dir", "");
        String myfile = "editor_form_" + todo + ".xml";

        String usererrorpage = pagedir + MCRConfiguration.instance().getString("MCR.classeditor_page_error_user", "editor_error_user.xml");
        String cancelpage = pagedir + MCRConfiguration.instance().getString("MCR.classeditor_page_cancel", "classeditor_cancel.xml");
        String icerrorpage = pagedir + MCRConfiguration.instance().getString("MCR.classeditor_page_error_id", "classeditor_error_clid.xml");
        String iderrorpage = pagedir + MCRConfiguration.instance().getString("MCR.classeditor_page_error_delete", "editor_error_delete.xml");
        String imerrorpage = pagedir + MCRConfiguration.instance().getString("MCR.classeditor_page_error_move", "classeditor_error_move.xml");
        String imperrorpage = pagedir + MCRConfiguration.instance().getString("MCR.classeditor_page_error_import", "classeditor_error_import.xml");
        String isaveerrorpage = pagedir + MCRConfiguration.instance().getString("MCR.classeditor_page_error_save", "classeditor_error_save.xml");
        String ipurgeerrorpage = pagedir + MCRConfiguration.instance().getString("MCR.classeditor_page_error_purge", "classeditor_error_purge.xml");

        String referrer = job.getRequest().getHeader("Referer");
        if (referrer == null || referrer.equals("")) {
            referrer = getBaseURL() + cancelpage;
        }

        if (needsCreatePrivilege(todo, todo2)) {
            if (!(MCRAccessManager.checkPermission("create-classification"))) {
                setResponsePage(job, ReturnStatus.fail, getBaseURL() + usererrorpage);
                return;
            }
        } else if (needsDeleteRight(todo, todo2)) {
            if (!(MCRAccessManager.checkPermission(clid, "deletedb"))) {
                setResponsePage(job, ReturnStatus.fail, getBaseURL() + usererrorpage);
                return;
            }
        } else {
            if (!(MCRAccessManager.checkPermission(clid, "writedb"))) {
                setResponsePage(job, ReturnStatus.fail, getBaseURL() + usererrorpage);
                return;
            }
        }

        // nach Editoraufruf von new/modify auf commit
        if ("commit-classification".equals(todo)) {
            org.jdom.Document indoc = sub.getXML();
            boolean bret = false;

            // for debug
            if (LOGGER.isDebugEnabled()) {
                XMLOutputter outputter = new XMLOutputter();
                LOGGER.debug(outputter.outputString(indoc));
            }

            if ("create-category".equals(todo2) || "modify-category".equals(todo2)) {
                if ("create-category".equals(todo2)) {
                    // create
                    if (!getClassificationEditor().isLocked(clid)) {
                        MCRCategoryID id = new MCRCategoryID(clid, categid);
                        bret = getClassificationEditor().createCategoryInClassification(indoc, id);
                    }
                } else {
                    // modify
                    if (!getClassificationEditor().isLocked(clid)) {
                        MCRCategoryID id = new MCRCategoryID(clid, categid);
                        bret = getClassificationEditor().modifyCategoryInClassification(indoc, id);
                    }
                }
                if (bret)
                    setResponsePage(job, ReturnStatus.success, path + "&categid=" + categid + "&clid=" + clid);
            } else {
                if (path.indexOf("&clid") > 0) {
                    // Classification abschneiden um wieder auf der
                    // Classifikationsstartseite zu landen
                    path = path.substring(0, path.indexOf("&clid"));
                }
                if ("create-classification".equals(todo2)) {
                    bret = getClassificationEditor().createNewClassification(indoc);
                } else if ("modify-classification".equals(todo2)) {
                    if (!getClassificationEditor().isLocked(clid)) {
                        bret = getClassificationEditor().modifyClassificationDescription(indoc, clid);
                    }
                } else if ("import-classification".equals(todo2)) {
                    String fname = parms.getParameter("/mycoreclass/pathes/path").trim();
                    fname = getClassificationEditor().setTempFile(fname, (FileItem) sub.getFiles().get(0));
                    String sUpdate = parms.getParameter("/mycoreclass/update");
                    boolean update = sUpdate == null || "true".equals(sUpdate);
                    bret = getClassificationEditor().importClassification(update, fname);
                    getClassificationEditor().deleteTempFile();
                    if (!bret) {
                        setResponsePage(job, ReturnStatus.fail, getBaseURL() + imperrorpage);
                        return;
                    }
                }
                if (bret)
                    setResponsePage(job, ReturnStatus.success, path);
            }
            setResponsePage(job, ReturnStatus.fail, getBaseURL() + icerrorpage);
            return;
        }

        if ("up-category".equals(todo) || "down-category".equals(todo) || "left-category".equals(todo) || "right-category".equals(todo)) {
            boolean bret = false;
            if (!getClassificationEditor().isLocked(clid)) {
                bret = getClassificationEditor().moveCategoryInClassification(categid, clid, todo.substring(0, todo.indexOf("-")));
            }
            if (bret) {
                setResponsePage(job, ReturnStatus.success, path + "&categid=" + categid + "&clid=" + clid);
            }
            setResponsePage(job, ReturnStatus.fail, getBaseURL() + imerrorpage);
            return;
        }

        // first call, direct without editor
        else if ("delete-category".equals(todo)) {
            // l?schen
            if (!getClassificationEditor().isLocked(clid)) {
                int cnt = getClassificationEditor().deleteCategoryInClassification(clid, categid);

                if (cnt == 0) { // deleted, no more references
                    setResponsePage(job, ReturnStatus.success, path + "&clid=" + clid);
                } else {
                    // not delete cause references exist
                    setResponsePage(job, ReturnStatus.fail, getBaseURL() + iderrorpage);
                }
            }
            return;
        }

        // first call, direct without editor
        else if ("delete-classification".equals(todo)) {
            if (!getClassificationEditor().isLocked(clid)) {
                boolean cnt = getClassificationEditor().deleteClassification(clid);
                if (cnt) { // deleted, no more references
                    path = getBaseURL() + "browse?mode=edit";
                    setResponsePage(job, ReturnStatus.success, path);
                }
                // not delete cause references exist
                setResponsePage(job, ReturnStatus.fail, getBaseURL() + iderrorpage);
            }
            return;
        }

        // first call of editor, build the import dialogue
        else if ("import-classification".equals(todo)) {
            String base = getBaseURL() + myfile;
            Properties params = new Properties();
            params.put("cancelUrl", referrer);
            params.put("clid", clid);
            params.put("path", path);
            params.put("todo2", todo);
            params.put("todo", "commit-classification");
            setResponsePage(job, ReturnStatus.success, buildRedirectURL(base, params));
            return;
        }

        else if ("save-all".equals(todo)) {
            if (getClassificationEditor().saveAll()) {
                setResponsePage(job, ReturnStatus.success, path + "&clid=" + clid);
            }
            setResponsePage(job, ReturnStatus.fail, getBaseURL() + isaveerrorpage);
            return;
        } else if ("purge-all".equals(todo)) {
            if (getClassificationEditor().purgeAll()) {
                setResponsePage(job, ReturnStatus.success, path + "&clid=" + clid);
            }
            setResponsePage(job, ReturnStatus.fail, getBaseURL() + ipurgeerrorpage);
            return;
        }
        // first call of editor, build the editor dialogue
        if ("create-category".equals(todo) || "modify-category".equals(todo) || "create-classification".equals(todo) || "modify-classification".equals(todo)) {

            String base = getBaseURL() + myfile;
            final String sessionObjectID = "classificationEditor";
            Properties params = new Properties();
            StringBuffer sb = new StringBuffer();
            boolean isEdited = clid.length() > 0 && getClassificationEditor().isEdited(MCRCategoryID.rootID(clid));
            MCRCategory classif = null;
            if (isEdited) {
                classif = getClassificationEditor().getClassification(MCRCategoryID.rootID(clid), false);
                LOGGER.info("CLASSIF: " + classif.getId());
            }

            if ("modify-classification".equals(todo)) {
                if (isEdited) {
                    sb.append("session:").append(sessionObjectID);
                    MCRSessionMgr.getCurrentSession().put(sessionObjectID, MCRCategoryTransformer.getMetaDataDocument(classif, true).getRootElement());
                } else {
                    sb.append("classification:metadata:0:children:").append(clid);
                }
                params.put("sourceUri", sb.toString());

            }
            if ("create-classification".equals(todo)) {
                String idBase = MCRConfiguration.instance().getString("MCR.SWF.Project.ID", "DocPortal") + "_class";
                MCRObjectID cli = MCRObjectID.getNextFreeId(idBase);

                Element classRoot = new Element("mycoreclass").setAttribute("ID", cli.toString());
                params.put("sourceUri", "session:" + sessionObjectID);
                MCRSessionMgr.getCurrentSession().put(sessionObjectID, classRoot);
            }
            if ("modify-category".equals(todo)) {
                if (isEdited) {
                    sb.append("session:").append(sessionObjectID);
                    Element classRoot = new Element("mycoreclass").setAttribute("ID", classif.getId().getRootID());
                    MCRLabel label = classif.getCurrentLabel();
                    Element le = new Element("label");
                    le.setAttribute("lang", label.getLang(), XML_NAMESPACE);
                    if (label.getText() != null) {
                        le.setAttribute("text", label.getText());
                    }
                    if (label.getDescription() != null) {
                        le.setAttribute("description", label.getDescription());
                    }
                    classRoot.addContent(le);

                    Element categs = new Element("categories");
                    MCRCategoryID id = new MCRCategoryID(classif.getId().getRootID(), categid);
                    MCRCategory cat = getClassificationEditor().findCategory(classif, id);
                    categs.addContent(MCRCategoryTransformer.getMetaDataElement(cat, true));
                    classRoot.addContent(categs);
                    MCRSessionMgr.getCurrentSession().put(sessionObjectID, classRoot);
                } else {
                    sb.append("classification:metadata:0:children:").append(clid).append(':').append(categid);
                }
                params.put("sourceUri", sb.toString());
                params.put("categid", categid);
            }
            if ("create-category".equals(todo)) {
                if (isEdited) {
                    sb.append("session:").append(sessionObjectID);
                    MCRSessionMgr.getCurrentSession().put(sessionObjectID, MCRCategoryTransformer.getMetaDataDocument(classif, true).getRootElement());
                } else {
                    sb.append("classification:metadata:0:children:").append(clid);
                }
                params.put("sourceUri", sb.toString());
                params.put("categid", categid);
            }
            params.put("cancelUrl", referrer);
            params.put("clid", clid);
            params.put("path", path);
            params.put("todo2", todo);
            params.put("todo", "commit-classification");
            setResponsePage(job, ReturnStatus.success, buildRedirectURL(base, params));
            return;
        }
        /* Wrong input data, write warning log */
        LOGGER.warn("MCRStartClassEditorServlet default Case - Nothing to do ? " + todo);
        setResponsePage(job, ReturnStatus.success, path);
    }

    public void setResponsePage(MCRServletJob job, ReturnStatus status, String url) {
        String key = MCRStartClassEditorServlet.class.toString();
        String value = job.getResponse().encodeRedirectURL(url);
        key += "." + status;
        job.getRequest().setAttribute(key, value);
    }

    public String getResponsePage(MCRServletJob job, ReturnStatus status) {
        String key = MCRStartClassEditorServlet.class.toString() + "." + status;
        return (String) job.getRequest().getAttribute(key);
    }

    public void render(MCRServletJob job, Exception thinkException) throws IOException {
        String successURL = getResponsePage(job, ReturnStatus.success);
        String failURL = getResponsePage(job, ReturnStatus.fail);

        if (thinkException == null && successURL != null) {
            job.getResponse().sendRedirect(successURL);
        } else {
            if (failURL != null)
                job.getResponse().sendRedirect(failURL);
            else
                generateErrorPage(job.getRequest(), job.getResponse(), 500, thinkException.getMessage(), thinkException, false);
        }
    }

    private boolean needsCreatePrivilege(String todo, String todo2) {
        return (todo.equals("commit-classification") && (todo2.equals("create-classification") || todo2.equals("import-classification")));
    }

    private boolean needsDeleteRight(String todo, String todo2) {
        return todo.equals("delete-classification");
    }

    public MCRClassificationEditor getClassificationEditor() {
        return MCRClassificationEditorFactory.getInstance();
    }

}
