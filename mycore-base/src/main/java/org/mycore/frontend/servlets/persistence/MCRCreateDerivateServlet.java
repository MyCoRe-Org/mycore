/**
 * 
 */
package org.mycore.frontend.servlets.persistence;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRDerivate;

/**
 * Handles CREATE operation on {@link MCRDerivate}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCreateDerivateServlet extends MCRPersistenceServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 4735574336094275787L;

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response)
        throws MCRAccessException {
        String objectID = getObjectId(request);
        if (!MCRAccessManager.checkPermission(objectID, PERMISSION_WRITE)) {
            throw MCRAccessException.missingPermission("Add derivate.", objectID, PERMISSION_WRITE);
        }
    }

    /**
     * redirects to new derivate upload form.
     *
     * At least "id" HTTP parameter is required to succeed.
     * <dl>
     *   <dt>id</dt>
     *   <dd>object ID of the parent mycore object (required)</dd>
     * </dl>
     * @param job
     * @throws IOException
     * @throws ServletException 
     * @throws MCRAccessException 
     */
    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        redirectToUploadForm(getServletContext(), request, response, getObjectId(request), null);
    }

    private String getObjectId(HttpServletRequest request) {
        return getProperty(request, "id");
    }

}
