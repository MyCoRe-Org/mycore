package org.mycore.user2.login;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRShibbolethLoginServlet extends MCRServlet {

	private static final long serialVersionUID = 1L;

	private static Logger LOGGER = Logger
			.getLogger(MCRShibbolethLoginServlet.class);

	public void doGetPost(MCRServletJob job) throws Exception {
		HttpServletRequest req = job.getRequest();
		HttpServletResponse res = job.getResponse();
		String uid = (String) req.getAttribute("uid");
		String userID = uid != null ? uid : req.getRemoteUser();

		if (userID != null) {
			LOGGER.info("Received principal:" + userID);
			LOGGER.info("E-mail:" + req.getAttribute("mail"));
			LOGGER.info("Display-Name:" + req.getAttribute("displayName"));
			String eduPersonAffiliationValue = (String) req
					.getAttribute("eduPersonAffiliation");

			Set<String> roles = Collections.emptySet();
			if (eduPersonAffiliationValue != null) {
				LOGGER.info("eduPersonAffiliation:" + eduPersonAffiliationValue);
				String[] eduPersonAffiliations = eduPersonAffiliationValue
						.split(";");
				MCRLDAPGroupMapper groupMapper = MCRConfiguration.instance()
						.getSingleInstanceOf("",
								MCRLDAPGroupPropertyMapper.class.getName());
				roles = groupMapper.getRoles(eduPersonAffiliations);
			} else {
				LOGGER.warn("eduPersonAffiliation is empty - use default role");
			}
			Map<String, String> attributes = new HashMap<>();
			Enumeration<String> attributeNames = req.getAttributeNames();
			while (attributeNames.hasMoreElements()) {
				String key = attributeNames.nextElement();
				LOGGER.info("also received " + key + ":" + req.getAttribute(key).toString());
				attributes.put(key, req.getAttribute(key).toString());
			}
			MCRUserInformation userinfo = new MCRShibbolethUserInformation(
					userID, roles, attributes);
			MCRSessionMgr.getCurrentSession().setUserInformation(userinfo);
			res.sendRedirect(res.encodeRedirectURL(req.getParameter("url")));
		} else {
			job.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED,
					"Principal could not be received from IDP.");
		}

	}
}
