/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.servlets;

import java.net.InetAddress;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Transaction;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfigurationException;

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

    private Transaction transaction = null;

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

    /**
     * starts a new database transaction.
     */
    public void beginTransaction() {
        transaction = MCRHIBConnection.instance().getSession().beginTransaction();
    }

    /**
     * commits the database transaction.
     * Commit is only done if {@link #isTransactionActive()} returns true.
     */
    public void commitTransaction() {
        if (isTransactionActive())
            transaction.commit();
    }

    /**
     * forces the database transaction to roll back.
     * Roll back is only performed if {@link #isTransactionActive()} returns true.
     */
    public void rollbackTransaction() {
        if (isTransactionActive())
            transaction.rollback();
    }

    /**
     * Is the transaction still alive?
     * @return true if the transaction is still alive
     */
    public boolean isTransactionActive() {
        return transaction != null && transaction.isActive();
    }

    /** returns true if the current http request was issued from the local host * */
    public boolean isLocal() {
        try {
            String serverName = theRequest.getServerName();
            String serverIP = InetAddress.getByName(serverName).getHostAddress();
            String remoteIP = MCRServlet.getRemoteAddr(theRequest);

            return (remoteIP.equals(serverIP) || remoteIP.equals("127.0.0.1"));
        } catch (Exception ex) {
            String msg = "Exception while testing if http request was from local host";
            throw new MCRConfigurationException(msg, ex);
        }
    }
}
