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

package org.mycore.backend.remote;

import java.net.*;
import java.io.*;

/**
 * This class handles a Socket-Connection to a Client in a thread.
 *
 * @author Mathias Zarick
 * @version $Revision$ $Date$
 */
public class MCRRemoteQueryServerThread extends Thread {
    private Socket socket = null;
    private String connector;

// constructor
    public MCRRemoteQueryServerThread(Socket socket) {
	super("RemoteQueryServerThread");
	this.socket = socket;
    }

// lets the thread run
// input is read from the client
// the output is generated considering the MCRRemoteQueryProtocol
    public void run() {
        try {
	    connector = socket.getInetAddress().getHostName();
            MCRSocketCommunicator sc = new MCRSocketCommunicator(socket);

	    String input, output;
	    MCRRemoteQueryProtocol rqp = new MCRRemoteQueryProtocol();
            try {
              output = rqp.processInput(null);
	      System.out.println("output:" + output );  //
              sc.write(output);
	      while (! output.equals("bye")) {
                input = sc.read();
                System.out.println("input:" + input); //
                try { output = rqp.processInput(input); }
                catch (ProtocolException pe) {
                   // pe.printStackTrace(System.err);
                   System.err.println(pe.getMessage());
                   output = "bye";
                }
		System.out.println("output:" + output);  //
                sc.write(output);
	       }
            } catch (SocketException se) {
                  System.err.println("Connection to " + connector + " abnormally aborted");
            }

            sc.close();

	} catch (IOException ioe) {
	    ioe.printStackTrace(System.err);
	}
    }
}
