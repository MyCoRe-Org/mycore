/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.user.authentication;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.callback.*;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;

/**
 * Authenticates a user by checking the password against a
 * JAAS LoginModule. You need a jaas.conf file, for example
 * the following configuration to authenticate using Kerberos service:
 * 
 * MCRJAASAuthenticator { 
 *  com.sun.security.auth.module.Krb5LoginModule required 
 *    principal="myhost@Example.CORP" useTicketCache=false; 
 * };
 * 
 * The following configuration property must be set:
 * 
 * MCR.UserAuthenticator.[ID].LoginContextID
 *   the ID of the JAAS login context, as declared in jaas.conf.
 *   Default is "MCRJAASAuthenticator" as in the sample jaas.conf above.
 */
public class MCRJAASAuthenticator {

    private String contextID;
    
    public void init(String ID) {
      MCRConfiguration config = MCRConfiguration.instance();
      String prefix = "MCR.UserAuthenticator." + ID + ".";
      contextID = config.getString(prefix + "LoginContextID", "MCRJAASAuthenticator");
    }

    public boolean authenticate(final String username, final String password) {
        LoginContext ctx = null;
        try {
            ctx = new LoginContext(contextID, new CallbackHandler() {
                public void handle(Callback callbacks[]) throws UnsupportedCallbackException {
                    for (int i = 0; i < callbacks.length; i++) {
                        if (callbacks[i] instanceof NameCallback) {
                            NameCallback nc = (NameCallback) callbacks[0];
                            nc.setName(username);
                        } else if (callbacks[i] instanceof PasswordCallback) {
                            PasswordCallback pc = (PasswordCallback) callbacks[i];
                            pc.setPassword(password.toCharArray());
                        } else {
                            throw new UnsupportedCallbackException(callbacks[i]);
                        }
                    }
                }
            });
        } catch (Exception ex) {
            String msg = "Exception while creating JAAS login context";
            throw new MCRConfigurationException(msg, ex);
        }
        try {
            ctx.login();
            return true;
        } catch (LoginException le) {
            return false;
        }
    }

    /** 
     * A small test application, modify source code to test class.
     * You need a jaas.conf file. Start main method with
     * java -Djava.security.auth.login.config=jaas.conf org.mycore.user.authentication.MCRJAASAuthenticator 
     */
    public static void main(String[] args) {
        MCRJAASAuthenticator ma = new MCRJAASAuthenticator();
        ma.contextID = "MCRJAASAuthenticator";
        System.out.println("Authenticated: " + ma.authenticate("hrz120", "Wrong"));
    }
}
