/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.io.Serial;

import org.mycore.sword.MCRSwordConfigurationDefault;
import org.mycore.sword.manager.MCRSwordMediaManager;
import org.swordapp.server.MediaResourceAPI;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MCRSwordMediaResourceServlet extends MCRSwordServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private transient MediaResourceAPI api;

    @Override
    public void init() {
        MCRSwordConfigurationDefault swordConfiguration = new MCRSwordConfigurationDefault();
        MCRSwordMediaManager mrm = new MCRSwordMediaManager();
        api = new MediaResourceAPI(mrm, swordConfiguration);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.get(req, resp);
        afterRequest(req, resp);
    }

    @Override
    public void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.head(req, resp);
        afterRequest(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.post(req, resp);
        afterRequest(req, resp);
    }

    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.put(req, resp);
        afterRequest(req, resp);
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.delete(req, resp);
        afterRequest(req, resp);
    }
}
