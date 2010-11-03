/**
 * 
 */
package org.mycore.frontend.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.i18n.MCRTranslation;
import org.mycore.tools.MyCoReWebPageProvider;

/**
 * @author basti
 * 
 */
public class MCRDerivateServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    static Logger LOGGER = Logger.getLogger(MCRDerivateServlet.class);

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {

        HttpServletRequest request = job.getRequest();
        if (request == null) {
            LOGGER.info(" # Request == null");
            return;
        }

        // Check the Parameter
        String myCoreDerivateId = null;
        String myCoreObjectId = null;
        String file = null;
        String equals;

        for (Object parms : request.getParameterMap().keySet()) {

            equals = "derivateid";
            if (parms.toString().equals(equals)) {
                myCoreDerivateId = request.getParameter(equals);
            }

            equals = "objectid";
            if (parms.toString().equals(equals)) {
                myCoreObjectId = request.getParameter(equals);
            }

            equals = "file";
            if (parms.toString().equals(equals)) {
                file = request.getParameter(equals);
            }
        }

        if (myCoreDerivateId == null) {
            String msg = MCRTranslation.translate("MCRDerivateServlet.error.noDerivateId");
            sendError("error", msg, MyCoReWebPageProvider.DE, job);
            return;
        }

        if (myCoreObjectId == null) {
            String msg = MCRTranslation.translate("MCRDerivateServlet.error.noObjectId");
            sendError("error", msg, MyCoReWebPageProvider.DE, job);
            return;
        }

        if (file == null) {
            String msg = MCRTranslation.translate("MCRDerivateServlet.error.noFile");
            sendError("error", msg, MyCoReWebPageProvider.DE, job);
            return;
        }

        if (!MCRAccessManager.checkPermission(myCoreDerivateId, "deletedb")) {//
            return;
        }

        // check what to do
        String val = request.getParameter("todo");

        if (val.equals("ssetfile") && ssetfile(myCoreDerivateId, myCoreObjectId, file)) {
            StringBuffer sb = new StringBuffer();
            sb.append(getBaseURL()).append("servlets/MCRFileNodeServlet/").append(myCoreDerivateId).append("/?hosts=local");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));

        } else if (val.equals("sdelfile") && sdelfile(myCoreDerivateId, myCoreObjectId, file)) {
            StringBuffer sb = new StringBuffer();
            sb.append(getBaseURL()).append("servlets/MCRFileNodeServlet/").append(myCoreDerivateId).append("/?hosts=local");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));
        } else {
            String test = MCRTranslation.translate("MCRDerivate.error.Unkown");
            sendError("error", test, MyCoReWebPageProvider.DE, job);
        }
    }

    /**
     * The method set the main file of a derivate object that is stored in the
     * server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'writedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    private boolean ssetfile(String derivateId, String ObjectId, String file) throws IOException {
        try {
            MCRObjectID mcrid = MCRObjectID.getInstance(derivateId);
            MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(mcrid);
            der.getDerivate().getInternals().setMainDoc(file);
            MCRMetadataManager.updateMCRDerivateXML(der);
            return true;
        } catch (MCRException ex) {
            LOGGER.error("Exception while store to derivate " + derivateId);

            return false;
        }
    }

    /**
     * The method delete a file from a derivate object that is stored in the
     * server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'deletedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    private boolean sdelfile(String derivateId, String ObjectId, String file) throws IOException {
        MCRDirectory rootdir = MCRDirectory.getRootDirectory(derivateId);
        try {
            rootdir.getChildByPath(file).delete();
            return true;
        } catch (Exception ex) {
            LOGGER.warn("Can't remove file " + file, ex);
            return false;
        }
    }

    private static void sendError(String title, String message, String lang, MCRServletJob job) throws IOException {
        MyCoReWebPageProvider t = new MyCoReWebPageProvider();
        t.addSection(title, message, lang);
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), t.getXML().getDocument());
    }
}
