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

package org.mycore.sword.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.sword.MCRSwordConfigurationDefault;
import org.mycore.sword.manager.MCRSwordServiceDocumentManager;
import org.swordapp.server.ServiceDocumentAPI;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordServiceDocumentServlet extends MCRSwordServlet {

    private ServiceDocumentAPI api;

    @Override
    public void init() throws ServletException {
        MCRSwordConfigurationDefault swordConfiguration = new MCRSwordConfigurationDefault();
        MCRSwordServiceDocumentManager sdMgr = new MCRSwordServiceDocumentManager();
        api = new ServiceDocumentAPI(sdMgr, swordConfiguration);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.get(req, resp);
        afterRequest(req, resp);
    }
}
