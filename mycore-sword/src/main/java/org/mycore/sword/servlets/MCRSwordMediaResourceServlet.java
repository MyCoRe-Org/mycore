package org.mycore.sword.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.sword.MCRSwordConfigurationDefault;
import org.mycore.sword.manager.MCRSwordMediaManager;
import org.swordapp.server.MediaResourceAPI;

public class MCRSwordMediaResourceServlet extends MCRSwordServlet {

    private MCRSwordConfigurationDefault swordConfiguration;

    private MCRSwordMediaManager mrm;

    private MediaResourceAPI api;

    public void init() throws ServletException {
        swordConfiguration = new MCRSwordConfigurationDefault();
        mrm = new MCRSwordMediaManager();
        api = new MediaResourceAPI(mrm, swordConfiguration);
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
