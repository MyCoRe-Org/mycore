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
import java.net.URI;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
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
    private final HttpServletRequest theRequest;

    /** The HttpServletResponse object */
    private final HttpServletResponse theResponse;

    private String sessionID;

    private static final ThreadLocal<MCRServletJob> CURRENT = new ThreadLocal<>();

    static {
        addSessionListener();
    }

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

    /**
     * Saves this instance as the 'current' servlet job.
     *
     * Can be retrieved afterwards by {@link #getCurrent()}.
     * @throws IllegalStateException if {@link MCRSessionMgr#hasCurrentSession()} returns false
     */
    public void setAsCurrent() throws IllegalStateException {
        if (!MCRSessionMgr.hasCurrentSession()) {
            throw new IllegalStateException("No current MCRSession available");
        }
        final MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setFirstURI(() -> URI.create(getRequest().getRequestURI()));
        sessionID = mcrSession.getID();
        CURRENT.set(this);
    }

    /**
     * Returns the instance saved by the current thread via {@link #setAsCurrent()}.
     * @return {@link Optional#empty()} if no servlet job is available for the current {@link MCRSession}
     */
    public static Optional<MCRServletJob> getCurrent() {
        final MCRServletJob servletJob = CURRENT.get();
        final Optional<MCRServletJob> rv = Optional.ofNullable(servletJob)
            .filter(job -> MCRSessionMgr.hasCurrentSession())
            .filter(job -> MCRSessionMgr.getCurrentSession().getID().equals(job.sessionID));
        if (rv.isEmpty()) {
            CURRENT.remove();
        }
        return rv;
    }

    private static void addSessionListener() {
        MCRSessionMgr.addSessionListener(event -> {
            switch (event.getType()) {
                case passivated:
                case destroyed:
                    CURRENT.remove();
                    break;
                default:
                    break;
            }
        });
    }

}
