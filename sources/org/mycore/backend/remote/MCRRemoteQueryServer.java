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

package mycore.communication;

import java.net.*;
import java.io.*;
import mycore.common.*;

/**
 * This class provides a remote Query Server for distributed
 * queries in MyCoRe,
 * also methods start and stop a Server
 *
 * @author Mathias Zarick
 * @version $Revision$ $Date$
*/
public class MCRRemoteQueryServer {
  protected static int queryPort;
  protected static String semaphoreLocation;
// end of input-parameters
// To know whether the Server runs or not, this semphore file is created
  protected static File runSemaphore;

  protected static void init() {
    MCRConfiguration config = MCRConfiguration.instance();
    queryPort = config.getInt("MCR.communication_rqs_port");
    semaphoreLocation = config.getString("MCR.communication_rqs_workdir") + "/runSemaphore";
    runSemaphore = new File(semaphoreLocation);
  }

  public static void main(String[] args) throws IOException {
    init();
    String helpScreen = "usage: java mycore.communication.MCRRemoteQueryServer (start|stop|restart|status|help)\n\n"
      + "start   - start RQS\n"
      + "stop    - stop RQS\n"
      + "restart - restart RQS or start if not running\n"
      + "status  - tests whether RQS is running\n"
      + "help    - this screen\n";

    if (args.length == 0)
       System.out.print(helpScreen);
    else if (args[0].equals("start"))
       run();
    else if (args[0].equals("stop")) {
       System.out.println(getStatus());
       shutdown();
    }
    else if (args[0].equals("restart")) {
       System.out.println(getStatus());
       shutdown();
       run();
    }
    else if (args[0].equals("status"))
       System.out.println(getStatus());
    else System.out.print(helpScreen);
  }

// run the Server
  protected static void run() throws IOException {
     ServerSocket queryServerSocket = null;
     try {
        runSemaphore.createNewFile();
        PrintWriter tempPrintWriter = new PrintWriter(new FileOutputStream(runSemaphore));
        tempPrintWriter.print(queryPort);
        tempPrintWriter.close();
     } catch (IOException e) {
        System.out.println("Could not write to " + semaphoreLocation + ".");
        System.exit(-1);
     }

     try {
        queryServerSocket = new ServerSocket(queryPort);
     } catch (IOException e) {
        System.err.println("Could not listen on port: " + queryPort + ".");
        System.exit(-1);
     }

     System.out.println("Server is running on port " + queryPort + ".");

//  The Server shall make new Threads for each connection as long as the
//  semaphore-file exists
//  each Client-connection gets its own thread
     while (runSemaphore.exists()) {
	new MCRRemoteQueryServerThread(queryServerSocket.accept()).start();
     }

     queryServerSocket.close();
     System.out.println("Server stopped.");
  }

// shutdown the Server
  protected static void shutdown() {
     MCRSocketCommunicator sc = null;
     int port = getPort();

     if (port != 0) {
        runSemaphore.delete();
        // a last connect to the Server to shut it down, because the Server
        // blocks till a connection is made
        // the Server closes not until all client-connections have ended
        try {
           sc = new MCRSocketCommunicator(new Socket("127.0.0.1", port));
           String scread = sc.read();
           sc.write("shutdown");
           sc.close();
        }
        catch (IOException ioe) {
        }
        System.out.println("Server stopped.");
     }
  }

// get the Port where the Server runs, it only looks in the semaphore-file
// retuns 0, if no Server is running
  protected static int getPort() {
     BufferedReader br = null;
     int port = 0;
     try {
        br = new BufferedReader(new FileReader(runSemaphore));
        port = Integer.parseInt(br.readLine());
        br.close();
     } catch (IOException ioe) {
     }
     return port;
  }

// gets the Status, if running or not and on which port to a String
  protected static String getStatus() {
     int port = getPort();
     String status;
     if (port != 0)
       status = "Server is runnnig on port " + port + ".";
     else status = "Server is not running.";
     return status;
  }

}
