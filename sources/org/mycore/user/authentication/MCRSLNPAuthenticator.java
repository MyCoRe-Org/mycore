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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;

/**
 * Authenticates a user by checking the password against a
 * university library server supporting the Simple Library Network
 * Protocol (SLNP), for example ALEPH and SISIS systems.
 * Configuration properties are:
 * 
 * MCR.UserAuthenticator.[ID].Host
 *   the host name of the SLNP server
 * MCR.UserAuthenticator.[ID].Port
 *   the SLNP port
 */
public class MCRSLNPAuthenticator {
    /** The address of the SLNP server */
    private InetAddress addr;

    /** The port number of the SLNP port on that server */
    private int port;

    /** This pattern identifies the password in the SLNP response */
    private static String pinPattern = "601 OpacPin:";

    public void init(String ID) {
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.UserAuthenticator." + ID;

        port = config.getInt(prefix + ".Port");
        String host = config.getString(prefix + ".Host");
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
            String msg = "SLNP host unknown for " + prefix;
            throw new MCRConfigurationException(msg, ex);
        }
    }

    public boolean authenticate(String username, String password) {
        Socket socket = null;
        boolean authenticated = false;

        try {
            socket = new Socket(addr, port);

            PrintStream ps = new PrintStream(socket.getOutputStream());
            ps.println("SLNPAlleBenutzerDaten");
            ps.println("BenutzerNummer:" + username);
            ps.println("SLNPEndCommand");
            ps.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            for (int i = 0; i < 20; i++) {
                String line = br.readLine();
                if ((line == null) || line.startsWith("510") || line.startsWith("250")) {
                    break;
                }
                if (line.startsWith(pinPattern)) {
                    authenticated = line.equals(pinPattern + password);
                    break;
                }
            }
        } catch (Exception ex) {
            String msg = "Exception while communicating with SLNP Server";
            throw new MCRException(msg, ex);
        } finally {
            if (socket != null)
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
        }
        return authenticated;
    }

    /** A small test application, modify source code to test class */
    public static void main(String[] args) throws Exception {
        MCRSLNPAuthenticator auth = new MCRSLNPAuthenticator();
        auth.addr = InetAddress.getByName("aleph420.bibl.uni-essen.de");
        auth.port = 5441;
        System.out.println("Authenticated: " + auth.authenticate("UEGSTest", "Wrong"));
    }
}
