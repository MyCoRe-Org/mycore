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

package org.mycore.services.webservices;

import java.lang.reflect.Method;

import org.apache.axis.MessageContext;
import org.apache.axis.providers.java.RPCProvider;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;

/**
 * Wraps WebService method invocation with hibernate transaction
 * 
 * @author Frank Lützenkirchen
 */
public class MCRRPCProvider extends RPCProvider {

    private final static Logger LOGGER = Logger.getLogger(MCRRPCProvider.class);
    
    private static long counter = 0;
    
    /**
     * Wraps WebService method invocation with hibernate transaction
     */
    protected Object invokeMethod(MessageContext mc, Method method, Object obj, Object[] argValues) throws Exception {
        counter++;
        LOGGER.info("WebService call #" + counter + " to " + method.getDeclaringClass().getName() + ":" + method.getName());
        Transaction tx = MCRHIBConnection.instance().getSession().beginTransaction();
        long millis = System.currentTimeMillis();
        Object result;

        try {
            result = super.invokeMethod(mc, method, obj, argValues);
            try {
                if (tx != null && tx.isActive())
                    tx.commit();
            } catch (RuntimeException ex) {
                MCRHIBConnection.instance().getSession().close();
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while processing WebService " + method.getName());
            LOGGER.error(ex.getClass().getName() + ": " + ex.getLocalizedMessage());
            LOGGER.error(ex);
            if (tx != null)
                tx.rollback();
            throw ex;
        }

        LOGGER.info("WebService call #" + counter + " finished in " + (System.currentTimeMillis() - millis) + " ms");
        return result;
    }
}
