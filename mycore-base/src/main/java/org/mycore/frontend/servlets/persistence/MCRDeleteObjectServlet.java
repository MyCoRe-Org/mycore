/**
 *
 */
package org.mycore.frontend.servlets.persistence;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.xml.sax.SAXParseException;

/**
 * Handles DELETE operation on {@link MCRObject}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRDeleteObjectServlet extends MCRPersistenceServlet {

    /**
     *
     */
    private static final long serialVersionUID = 3148314720092349972L;

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response) throws MCRAccessException,
        ServletException, MCRActiveLinkException, SAXParseException, JDOMException, IOException {
        MCRObjectCommands.delete(getProperty(request, "id"));
    }

    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String deletedRedirect = MCRConfiguration.instance().getString("MCR.Persistence.PageDelete").trim();
        response.sendRedirect(response.encodeRedirectURL(MCRFrontendUtil.getBaseURL() + deletedRedirect));
    }

}
