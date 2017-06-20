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
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.frontend.cli.MCRDerivateCommands;
import org.xml.sax.SAXParseException;

/**
 * Handles DELTE operation on {@link MCRDerivate}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRDeleteDerivateServlet extends MCRPersistenceServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1581063299429224344L;

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response) throws MCRAccessException,
        ServletException, MCRActiveLinkException, SAXParseException, JDOMException, IOException {
        MCRDerivateCommands.delete(getProperty(request, "id"));
    }

    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect(response.encodeRedirectURL(getReferer(request).toString()));
    }

}
