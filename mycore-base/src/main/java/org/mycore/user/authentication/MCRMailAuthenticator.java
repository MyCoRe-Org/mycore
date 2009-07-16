/**
 * 
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

import java.util.Properties;
import javax.mail.*;
import org.mycore.common.*;

/**
 * Authenticates a user by checking the password against a
 * POP3 or IMAP mail server. Configuration properties are:
 * 
 * MCR.UserAuthenticator.[ID].Host
 *   the host name of the mail server
 * MCR.UserAuthenticator.[ID].Protocol
 *   the mail protocol, "imap" or "pop3", default is "imap"
 */
public class MCRMailAuthenticator implements MCRAuthenticator {

    /** The host name of the mail server */
    private String host;

    /** The protocol to use, "imap" or "pop3" */
    private String protocol;

    private Properties prop;

    public void init(String ID) {
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.UserAuthenticator." + ID;
        host = config.getString(prefix + ".Host");
        protocol = config.getString(prefix + ".Protocol", "imap");
        if (!(protocol.equals("imap") || protocol.equals("pop3"))) {
            String msg = "Unsupported protocol for " + prefix + ": " + protocol;
            throw new MCRConfigurationException(msg);
        }

        prop = new Properties();
        prop.put("mail." + protocol + ".host", host);
    }

    public boolean authenticate(String username, String password) {
        try {
            Session session = Session.getInstance(prop);
            Store store = session.getStore(protocol);
            store.connect(host, username, password);
            store.close();
        } catch (AuthenticationFailedException e) {
            return false;
        } catch (Exception ex) {
            String msg = "Exception while communicating with mail server";
            throw new MCRConfigurationException(msg, ex);
        }
        return true;
    }

    /** A small test application, modify source code to test class */
    public static void main(String[] args) {
        MCRMailAuthenticator ma = new MCRMailAuthenticator();
        ma.host = "mailbox.uni-duisburg-essen.de";
        ma.protocol = "imap";
        ma.prop = new Properties();
        ma.prop.put("mail." + ma.protocol + ".host", ma.host);
        System.out.println("Authenticated: " + ma.authenticate("hrz120", "Wrong"));
    }
}
