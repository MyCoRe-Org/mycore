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

import java.io.*;
import java.net.*;


/**
 * This class provides methods for communication via a socket connection.
 *
 * @author Mathias Zarick
 * @version $Revision$ $Date$
 */
public class MCRSocketCommunicator {

  private Socket socket = null;
  private BufferedOutputStream out = null;
  private BufferedInputStream in = null;

// constructor get the socket and build a new communicator
  public MCRSocketCommunicator(Socket socket) throws IOException {
     this.socket = socket;
     out = new BufferedOutputStream(this.socket.getOutputStream());
     in = new BufferedInputStream(this.socket.getInputStream());
  }

// read from the socket
  public String read() throws IOException {
     String result="";
     int resultPart;
     do { resultPart = in.read();
          result = result + (char) resultPart;
        }
     while (in.available() > 0 );
     return result;
  }

// read number bytes from the socket
  public String read(int number) throws IOException {
     String result="";
     int resultPart;
     for (int i=0;i<number;i++) {
       resultPart = in.read();
       result = result + (char) resultPart;
     }
     return result;
  }

// write to a socket
  public void write(String toWrite) throws IOException {
     for (int i = 0; i < toWrite.length(); i++)
        out.write( (int) toWrite.charAt(i));
     out.flush();
  }

// close the communicator (that also closes the socket)
  public void close() throws IOException {
     out.close();
     in.close();
     socket.close();
  }
}
