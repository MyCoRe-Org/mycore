package org.mycore.services.acl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRACLEditorServlet extends MCRServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger LOGGER = Logger.getLogger(MCRACLEditorServlet.class);

	public void init() throws MCRConfigurationException, ServletException {
		super.init();
	}

	public void doGetPost(MCRServletJob job) throws Exception {
		MCRSession session = MCRSessionMgr.getCurrentSession();
		HttpServletRequest request = job.getRequest();
		HttpServletResponse response = job.getResponse();
		String mode = request.getParameter("mode");

		MCRACLHIBAccess HIBA = new MCRACLHIBAccess();
		MCRACLXMLProcessing XMLProcessing = new MCRACLXMLProcessing();

		String objidFilter = null;
		String acpoolFilter = null;
		Map filterMap = (Map) session.get("filter");

		if (filterMap != null) {
			objidFilter = (String) filterMap.get("objid");
			acpoolFilter = (String) filterMap.get("acpool");

			if (objidFilter != null && objidFilter.equals("*")) {
				filterMap.remove("objid");
				objidFilter = null;
			}

			if (acpoolFilter != null && acpoolFilter.equals("*")) {
				filterMap.remove("acpool");
				acpoolFilter = null;
			}

			if (acpoolFilter == null && objidFilter == null) {
				session.deleteObject("filter");
			}

		} else
			LOGGER.debug("******** Filter NULL *******");

		LOGGER.info("******* Current Filter: obj: " + objidFilter + "\t ac: " + acpoolFilter);

		System.out.println("#################################################");
		System.out.println("# version 0.041                                 #");
		System.out.println("#################################################");

		boolean doLayout = false;
		Element answer = null;

		String errorMsg = "The request did not contain a valid mode for this servlet!";

		if (mode == null) {
			processSubmission(job, response, objidFilter, acpoolFilter);
		} else if (mode.equals("access")) {
			answer = XMLProcessing.access2XML(HIBA.getAccess(objidFilter, acpoolFilter));
			doLayout = true;
		} else if (mode.equals("getRuleAsItems")) {
			answer = XMLProcessing.ruleSet2Items(HIBA.getAccessRule());
			doLayout = true;
		} else if (mode.equals("getRuleTable")) {
			answer = XMLProcessing.ruleSet2XML(HIBA.getAccessRule());
			doLayout = true;
		} else if (mode.equals("getFilter")) {
			LOGGER.info("******* get Filter!");
			answer = XMLProcessing.accessFilter2XML(objidFilter, acpoolFilter);
			doLayout = true;
		} else if (mode.equals("setFilter")) {
			LOGGER.info("******* set Filter!");
			setFilterFromGet(request, response);
			redirect(response, "servlets/MCRACLEditorServlet?mode=getFilter");
		} else if (mode.equals("removeFilter")) {
			removeFilter(request, response);
			// redirect(response,
			// "servlets/MCRACLEditorServlet?mode=getFilter");
			MCRRequestParameters parms = new MCRRequestParameters(request);
			String sessionID = parms.getParameter("_session");
//			StringBuffer sb = new StringBuffer(getBaseURL());
			StringBuffer sb = new StringBuffer();
            sb.append(parms.getParameter("_webpage"));
            sb.append("?XSL.editor.session.id=");
            sb.append(sessionID);
			redirect(response, sb.toString());
		} else {
			job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, errorMsg);
		}

		if (doLayout)
			doLayout(request, response, answer);

	}

	private void processSubmission(MCRServletJob job, HttpServletResponse response, String objidFilter, String acpoolFilter) throws Exception {
		MCRACLHIBAccess HIBA = new MCRACLHIBAccess();
		MCRACLXMLProcessing XMLProcessing = new MCRACLXMLProcessing();
		MCREditorSubmission sub = (MCREditorSubmission) job.getRequest().getAttribute("MCREditorSubmission");
		Document indoc = sub.getXML();
		
		Element indocRoot = indoc.getRootElement();
		String indocRootName = indocRoot.getName();

		if (indocRootName.equals("mcr_access_filter")) {
			/*
			 * Here we are a little bit inconsistent. Normally MCRACLXMLProcessing should
			 * take care of all the xml stuff, but here we have to move to much data
			 * around for nothing and blow up the code
			 */
			objidFilter = indocRoot.getChildText("objid");
			acpoolFilter = indocRoot.getChildText("acpool");
			setFilter(objidFilter, acpoolFilter);
			
			redirect(response, "modules/module-ACL-editor/web/editor/editor_start_ACL_editor.xml");
		} else if (indocRootName.equals("mcr_access_set")) {
			Document origDoc = new Document().setContent(XMLProcessing.access2XML(HIBA.getAccess(objidFilter, acpoolFilter)));
			Map diffMap = XMLProcessing.findAccessDiff(indoc, origDoc);

			HIBA.saveChanges(diffMap);
			redirect(response, "servlets/MCRACLEditorServlet?mode=access");

		}
	}

	private void setFilterFromGet(HttpServletRequest request, HttpServletResponse response) {
		String newObjIDFilter = request.getParameter("objid");
		String newAcPoolFilter = request.getParameter("acpool");

		setFilter(newObjIDFilter, newAcPoolFilter);
	}

	private void setFilter(String newObjIDFilter, String newAcPoolFilter) {
		MCRSession session = MCRSessionMgr.getCurrentSession();
		Map filterMap = (Map) session.get("filter");
		boolean oldFilterExist = false;

		if (filterMap == null)
			filterMap = new HashMap();
		else
			oldFilterExist = true;

		String oldObjIDFilter = (String) filterMap.get("objid");
		String oldAcPoolFilter = (String) filterMap.get("acpool");

		/*
		 * Here we check what happen to the objid filter depending on its old
		 * and new value.
		 * 
		 * There are two cases where the filter have to be removed: 1)
		 * newObjIDFilter != null && newObjIDFilter.equals("") 2) newObjIDFilter ==
		 * null && oldObjIDFilter != null
		 * 
		 * so the order of the if test is important to keep the code small. When
		 * oldObjIDFilter == null, the rest is skipped regardless of the value
		 * of the or test. The newObjIDFilter.equals("") ist just evualate if
		 * newObjIDFilter != null so no NullPointer exception is expected.
		 * 
		 * When the if test is true so we have to remove the filter value in the
		 * session as well the value in filterMap.
		 * 
		 * When we reach the second if test we have to check the following
		 * cases: 1) newObjIDFilter != null && oldObjIDFilter == null 2)
		 * newObjIDFilter != null && oldObjIDFilter != null
		 * 
		 * In both cases we have to set the new filter regardless of the value
		 * of oldObjIDFilter.
		 * 
		 * When newObjIDFilter == null && oldObjIDFilter == null, nothing to do
		 */
		if (oldObjIDFilter != null && (newObjIDFilter == null || newObjIDFilter.equals(""))) {
			session.deleteObject("objid");
			filterMap.remove("objid");

			LOGGER.info("******* ObjID Filter removed!");
		} else if (newObjIDFilter != null) {
			filterMap.put("objid", newObjIDFilter);
			LOGGER.info("******* new ObjID Filter: " + newObjIDFilter);
		}

		/*
		 * see objid filter
		 */
		if (oldAcPoolFilter != null && (newAcPoolFilter == null || newAcPoolFilter.equals(""))) {
			session.deleteObject("acpool");
			filterMap.remove("acpool");

			LOGGER.info("******* AcPool Filter removed!");
		} else if (newAcPoolFilter != null) {
			filterMap.put("acpool", newAcPoolFilter);
			LOGGER.info("******* new AcPool Filter: " + newAcPoolFilter);
		}

		if (filterMap.size() == 0 && oldFilterExist) {
			session.deleteObject("filter");
			LOGGER.info("******* Filter completely removed!");
		} else {
			session.put("filter", filterMap);
			LOGGER.info("******* Filter updated!");
		}
	}

	private void removeFilter(HttpServletRequest request, HttpServletResponse response) {
		MCRSession session = MCRSessionMgr.getCurrentSession();

		if (session.get("filter") != null)
			session.deleteObject("filter");
	}

	private void redirect(HttpServletResponse response, String url) {
		if (url == null)
			url = "";

		try {
			response.sendRedirect(response.encodeRedirectURL(getBaseURL() + url));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void doLayout(HttpServletRequest request, HttpServletResponse response, Element elem) throws IOException {
		doLayout(request, response, new Document(elem));
	}

	private void doLayout(HttpServletRequest request, HttpServletResponse response, Document doc) throws IOException {
		getLayoutService().doLayout(request, response, doc);
	}

}
