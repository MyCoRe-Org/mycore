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

import java.net.InetAddress;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.frontend.MCRFrontendUtil;

/**
 * This class simply is a container for objects needed during a Servlet session
 * like the HttpServletRequest and HttpServeltResponse. The class provids only
 * get-methods to return the objects set while constructing the job object.
 * 
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRServletJob {
    /** The HttpServletRequest object */
    private HttpServletRequest theRequest = null;

    /** The HttpServletResponse object */
    private HttpServletResponse theResponse = null;

    /**
     * The constructor takes the given objects and stores them in private
     * objects.
     * 
     * @param theRequest
     *            the HttpServletRequest object for this servlet job
     * @param theResponse
     *            the HttpServletResponse object for this servlet job
     */
    public MCRServletJob(HttpServletRequest theRequest, HttpServletResponse theResponse) {
        this.theRequest = theRequest;
        this.theResponse = theResponse;
    }

    /** returns the HttpServletRequest object */
    public HttpServletRequest getRequest() {
        return theRequest;
    }

    /** returns the HttpServletResponse object */
    public HttpServletResponse getResponse() {
        return theResponse;
    }

    /** returns true if the current http request was issued from the local host * */
    public boolean isLocal() {
        try {
            String serverName = theRequest.getServerName();
            String serverIP = InetAddress.getByName(serverName).getHostAddress();
            String remoteIP = MCRFrontendUtil.getRemoteAddr(theRequest);

            return remoteIP.equals(serverIP) || remoteIP.equals("127.0.0.1");
        } catch (Exception ex) {
            String msg = "Exception while testing if http request was from local host";
            throw new MCRConfigurationException(msg, ex);
        }
    }
}
