/*
 * 
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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.axis.providers.java.RPCProvider;
import org.apache.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.MCRSystemUserInformation;

/**
 * Wraps WebService method invocation with hibernate transaction
 * 
 * @author Frank Lützenkirchen
 */
public class MCRRPCProvider extends RPCProvider {

    private static final long serialVersionUID = 1L;

    private final static Logger LOGGER = Logger.getLogger(MCRRPCProvider.class);

    private static AtomicInteger counter = new AtomicInteger();

    /**
     * Wraps WebService method invocation with hibernate transaction
     */
    protected Object invokeMethod(final MessageContext mc, Method method, Object obj, Object[] argValues) throws Exception {
        int count = counter.incrementAndGet();
        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.setLoginTime();

        MCRUserInformation uiGuest = MCRSystemUserInformation.getGuestInstance();
        MCRUserInformation uiContext = new MCRUserInformation() {
            @Override
            public boolean isUserInRole(String role) {
                return Arrays.asList(mc.getRoles()).contains(role);
            }
            
            @Override
            public String getUserID() {
                return mc.getUsername();
            }

            @Override
            public String getUserAttribute(String attribute) {
                return null;
            }
        };
        session.setUserInformation( mc.getUsername() == null ? uiGuest : uiContext );
        
        session.setCurrentIP(mc.getStrProp(Constants.MC_REMOTE_ADDR));
        session.beginTransaction();
        LOGGER.info("WebService call #" + count + " to " + method.getDeclaringClass().getName() + ":" + method.getName());
        long millis = System.currentTimeMillis();
        Object result;

        try {
            result = super.invokeMethod(mc, method, obj, argValues);
            session.commitTransaction();
        } catch (Exception ex) {
            LOGGER.error("Exception while processing WebService " + method.getName(), ex);
            session.rollbackTransaction();
            throw ex;
        } finally {
            MCRSessionMgr.releaseCurrentSession();
            session.close();
        }

        LOGGER.info("WebService call #" + count + " finished in " + (System.currentTimeMillis() - millis) + " ms");
        return result;
    }
}
