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
import mycore.classifications.MCRClassification;
import mycore.xml.MCRQueryResultArray;
import mycore.xml.MCRQueryInterface;

/**
 * This class realizes the RemoteQueryProtocol in the view
 * of the Server.
 * the Server can use the processInput method to generate the
 * correct output from a given output
 *
 * @author Mathias Zarick
 * @version $Revision$ $Date$
 */
public class MCRRemoteQueryProtocol {

// the states of the server
    private static final int WAITING = 0;
    private static final int SENT_CONNECTED = 1;
    private static final int SENT_EXPECTING_HOST = 2;
    private static final int SENT_EXPECTING_QUERY = 3;
    private static final int SENT_RECEIVED_QUERY = 4;
    private static final int SENT_RESULT_SIZE = 5;
    private static final int SENT_RESULT = 6;

// first it is waiting
    private int state = WAITING;
    private int bytes = 0;
    private byte [] queryResult;
    private String host;

    private boolean queryInputSyntaxIsCorrect(String input) {
    return input.indexOf("***") > 0;
    }

    private String extractType(String input) {
    return input.substring(0,input.indexOf('*'));
    }

    private String extractQuery(String input) {
    return input.substring(input.indexOf('*')+3);
    }

// generate the output
// the output depends on the state of the server and on the given input
// the input comes from the client
    public String processInput(String theInput) throws ProtocolException
    {
        String theOutput = null;

        if (state == WAITING) {
            theOutput = "connected";
            state = SENT_CONNECTED;
        } else if (state == SENT_CONNECTED) {
            try {
              if (theInput.equals("shutdown")) {
                theOutput = "bye";
                state = WAITING;
              }
              else if (theInput.substring(0,15).equals("query in bytes:")) {
                bytes = Integer.parseInt(theInput.substring(15));

                theOutput = "ok expecting host";
                state = SENT_EXPECTING_HOST;
              } else throw new ProtocolException("Communication Error! Error Code 1");
            }
            catch (NumberFormatException nfe) {
              throw new ProtocolException("Communication Error! Error Code 2");
            }
            catch (IndexOutOfBoundsException ioobe) {
              throw new ProtocolException("Communication Error! Error Code 3");
            }
        } else if (state == SENT_EXPECTING_HOST) {
            host = theInput;
            theOutput = "ok expecting query";
            state = SENT_EXPECTING_QUERY;
        } else if (state == SENT_EXPECTING_QUERY) {
            if (theInput.length() == bytes) {
              try {
                if (queryInputSyntaxIsCorrect(theInput)) {
                  String type = extractType(theInput);
                  String query = extractQuery(theInput);
                  // start the local query
                  MCRQueryResultArray result = new MCRQueryResultArray();
                  if (type.equalsIgnoreCase("class")) {
                    MCRClassification cl = new MCRClassification();
                    org.jdom.Document jdom = cl.search(query);
                    if (jdom != null) {
                      org.jdom.Element el = jdom.getRootElement();
                      String id = el.getAttributeValue("ID");
                      MCRQueryResultArray res = new MCRQueryResultArray();
                      res.add("local",id,1,el);
                      result.importElements(res);
                      }
                    }
                  else {
                    MCRConfiguration config = MCRConfiguration.instance();
                    int vec_length = config.getInt("MCR.query_max_results",10);
                    String persist_type = config
                      .getString("MCR.persistence_type","cm7");
                    String proppers = "MCR.persistence_"+persist_type
                      .toLowerCase()+"_query_name";
                    MCRQueryInterface mcr_query = (MCRQueryInterface)config
                      .getInstanceOf(proppers);
                    result = mcr_query.getResultList(query,type,vec_length);
                    }
                  for (int i=0; i<result.size();i++)
                    result.setHost(i,host);
                  try {
                    queryResult = result.exportAllToByteArray(); }
                  catch (IOException e) {}
                  }
                else queryResult=(new String("")).getBytes();
              }
              catch (MCRException mcre) {
                     mcre.printStackTrace(System.err);
                     queryResult=(new String("<ERROR/>")).getBytes();
              }
              theOutput = "received bytes:" + bytes;
              state = SENT_RECEIVED_QUERY;
            }
            else throw new ProtocolException("Communication Error! Error Code 4");
        } else if (state == SENT_RECEIVED_QUERY) {
            if (theInput.equals("ok expecting result size")) {
              theOutput = "result in bytes:" + queryResult.length;
              state = SENT_RESULT_SIZE;
            }
            else throw new ProtocolException("Communication Error! Error Code 5");
        } else if (state == SENT_RESULT_SIZE) {
            if (theInput.equals("ok expecting result")) {
              theOutput = new String(queryResult);
              state = SENT_RESULT;
            }
            else throw new ProtocolException("Communication Error! Error Code 6");
        } else if (state == SENT_RESULT) {
            try {
              if ( (theInput.substring(0,15).equals("received bytes:")) &&
                   (queryResult.length == Integer.parseInt(theInput.substring(15))) ) {
                theOutput = "bye";
                state = WAITING;
              }
              else throw new ProtocolException("Communication Error! Error Code 7");
            }
            catch (NumberFormatException nfe) {
              throw new ProtocolException("Communication Error! Error Code 8");
            }
            catch (IndexOutOfBoundsException ioobe) {
              throw new ProtocolException("Communication Error! Error Code 9");
            }
        }
        return theOutput;
    }
}

