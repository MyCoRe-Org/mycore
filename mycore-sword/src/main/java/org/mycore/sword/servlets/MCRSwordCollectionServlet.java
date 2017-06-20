package org.mycore.sword.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.sword.MCRSwordConfigurationDefault;
import org.mycore.sword.manager.MCRSwordCollectionManager;
import org.swordapp.server.CollectionAPI;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordCollectionServlet extends MCRSwordServlet {

    private MCRSwordConfigurationDefault swordConfiguration;

    private MCRSwordCollectionManager colMgr;

    private CollectionAPI api;

    public void init() {
        swordConfiguration = new MCRSwordConfigurationDefault();
        colMgr = new MCRSwordCollectionManager();
        api = new CollectionAPI(colMgr, colMgr, swordConfiguration);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.get(req, resp);
        afterRequest(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.post(req, resp);
        afterRequest(req, resp);
    }
}
