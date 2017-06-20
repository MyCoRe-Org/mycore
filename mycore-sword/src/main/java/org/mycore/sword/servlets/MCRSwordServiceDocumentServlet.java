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

    private MCRSwordConfigurationDefault swordConfiguration;

    private MCRSwordServiceDocumentManager sdMgr;

    private ServiceDocumentAPI api;

    @Override
    public void init() throws ServletException {
        swordConfiguration = new MCRSwordConfigurationDefault();
        sdMgr = new MCRSwordServiceDocumentManager();
        api = new ServiceDocumentAPI(sdMgr, swordConfiguration);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepareRequest(req, resp);
        api.get(req, resp);
        afterRequest(req, resp);
    }
}
