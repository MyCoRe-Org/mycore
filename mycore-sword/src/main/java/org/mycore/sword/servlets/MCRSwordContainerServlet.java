package org.mycore.sword.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.sword.MCRSwordConfigurationDefault;
import org.mycore.sword.manager.MCRSwordContainerManager;
import org.mycore.sword.manager.MCRSwordStatementManager;
import org.swordapp.server.ContainerAPI;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordContainerServlet extends MCRSwordServlet {

    private MCRSwordConfigurationDefault swordConfiguration;

    private MCRSwordContainerManager containerManager;

    private MCRSwordStatementManager statementManager;

    private ContainerAPI api;

    public void init() {
        swordConfiguration = new MCRSwordConfigurationDefault();
        containerManager = new MCRSwordContainerManager();
        statementManager = new MCRSwordStatementManager();
        api = new ContainerAPI(containerManager, statementManager, swordConfiguration);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.get(req, resp);
        afterRequest(req, resp);
    }

    public void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.head(req, resp);
        afterRequest(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.post(req, resp);
        afterRequest(req, resp);
    }

    public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.put(req, resp);
        afterRequest(req, resp);
    }

    public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.delete(req, resp);
        afterRequest(req, resp);
    }
}
