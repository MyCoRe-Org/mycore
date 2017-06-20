package org.mycore.user2.login;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user2.MCRRealmFactory;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttributeMapper;
import org.mycore.user2.MCRUserManager;

/**
 * 
 * @author Ren\u00E9 Adler (eagle)
 */
public class MCRShibbolethLoginServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = LogManager.getLogger(MCRShibbolethLoginServlet.class);

    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String msg = null;

        String uid = (String) req.getAttribute("uid");
        String userId = uid != null ? uid : req.getRemoteUser();

        if (userId != null) {
            final String realmId = userId.contains("@") ? userId.substring(userId.indexOf("@") + 1) : null;
            if (realmId != null && MCRRealmFactory.getRealm(realmId) != null) {
                userId = realmId != null ? userId.replace("@" + realmId, "") : userId;

                final Map<String, Object> attributes = new HashMap<String, Object>();

                final MCRUserAttributeMapper attributeMapper = MCRRealmFactory.getAttributeMapper(realmId);
                for (final String key : attributeMapper.getAttributeNames()) {
                    final Object value = req.getAttribute(key);
                    if (value != null) {
                        LOGGER.info("received " + key + ":" + value);
                        attributes.put(key, value);
                    }
                }

                MCRUserInformation userinfo;

                MCRUser user = MCRUserManager.getUser(userId, realmId);
                if (user != null) {
                    LOGGER.debug("login existing user \"" + user.getUserID() + "\"");

                    attributeMapper.mapAttributes(user, attributes);
                    user.setLastLogin();
                    MCRUserManager.updateUser(user);

                    userinfo = user;
                } else {
                    userinfo = new MCRShibbolethUserInformation(userId, realmId, attributes);
                }

                MCRSessionMgr.getCurrentSession().setUserInformation(userinfo);
                // MCR-1154
                req.changeSessionId();

                res.sendRedirect(res.encodeRedirectURL(req.getParameter("url")));
                return;
            } else {
                msg = "Login from realm \"" + realmId + "\" is not allowed.";
            }
        } else {
            msg = "Principal could not be received from IDP.";
        }

        job.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, msg);
    }
}
