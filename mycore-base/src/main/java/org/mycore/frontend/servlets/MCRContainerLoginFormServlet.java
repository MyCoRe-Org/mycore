/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.servlets;

import java.util.Arrays;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.frontend.support.MCRLogin;
import org.mycore.services.i18n.MCRTranslation;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRContainerLoginFormServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final String LOGIN_ATTR = MCRContainerLoginFormServlet.class.getCanonicalName();

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#think(org.mycore.frontend.servlets.MCRServletJob)
     */
    @Override
    protected void think(MCRServletJob job) throws Exception {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        MCRUserInformation userInformation = mcrSession.getUserInformation();
        MCRLogin login = new MCRLogin(userInformation, getFormAction());
        login.getForm().getInput().addAll(Arrays.asList(getUserNameField(), getPasswordField()));
        job.getRequest().setAttribute(LOGIN_ATTR, new MCRJAXBContent<>(MCRLogin.getContext(), login));
    }

    private String getFormAction() {
        return "j_security_check";
    }

    private MCRLogin.InputField getUserNameField() {
        String userNameText = MCRTranslation.translate("component.user2.login.form.userName");
        return new MCRLogin.InputField("j_username", null, userNameText, userNameText, false, false);
    }

    private MCRLogin.InputField getPasswordField() {
        String passwordText = MCRTranslation.translate("component.user2.login.form.password");
        return new MCRLogin.InputField("j_password", null, passwordText, passwordText, true, false);
    }

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#render(org.mycore.frontend.servlets.MCRServletJob, java.lang.Exception)
     */
    @Override
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        if (ex != null) {
            throw ex;
        }
        MCRContent source = (MCRContent) job.getRequest().getAttribute(LOGIN_ATTR);
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), source);
    }

}
