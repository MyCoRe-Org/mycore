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

package mycore.datamodel;

import java.util.*;
import mycore.common.MCRConfiguration;
import mycore.common.MCRException;
import mycore.common.MCRConfigurationException;
import mycore.datamodel.MCRObject;
import mycore.datamodel.MCRQueryInterface;

/**
 * This class is the result list of a XQuery question to the persistence
 * system. With the getElement methode you can get a MCRObjectID from
 * this list. The method use, depending of the persitence type property,
 * a interface to a query transformer form XQuery to the target system
 * query language.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRQueryResult
{
private int vec_max_length;
private Vector mcr_result = null;
private Vector mcr_xml = null;
private MCRQueryInterface mcr_transform = null;
private MCRConfiguration config = null;

/** The start tag for the result collection **/
public static final String TAG_RESULTS_S = "<mcr_results>";
/** The end tag for the result collection **/
public static final String TAG_RESULTS_E = "</mcr_results>";
/** The start tag for one result **/
public static final String TAG_RESULT_S = "<mcr_result>";
/** The end tag for one result **/
public static final String TAG_RESULT_E = "</mcr_result>";
/** The start tag for one MCRObjectId **/
public static final String TAG_MCRID_S = "<mcr_objectid>";
/** The end tag for one MCRObjectId **/
public static final String TAG_MCRID_E = "</mcr_objectid>";
/** The start tag for one XML document **/
public static final String TAG_XML_S = "<mcr_xml>";
/** The end tag for one XML document **/
public static final String TAG_XML_E = "</mcr_xml>";
/* The new line string */
private String NL;

/**
 * This constructor create the MCRQueryResult class with an empty
 * query result vector.
 *
 * @exception MCRConfigurationException     
 *                              an Exception of MyCoRe Configuration
 **/
public MCRQueryResult() throws MCRConfigurationException
  {
  NL = System.getProperty("line.separator");
  config = MCRConfiguration.instance();
  vec_max_length = config.getInt("MCR.query_max_results",10);
  mcr_result = new Vector(vec_max_length);
  mcr_xml = new Vector(vec_max_length);
  }

/**
 * This methode read the properties for the MCRObject type and call
 * the coresponding class for a query to the persistence layer. If
 * it was succesful, the vector of MCRObject's is filled with answers.
 * Then the corresponding XML streams was loaded.
 *
 * @param type 	                the MCRObject type
 * @param query	                the Query string
 * @exception MCRException      general Exception of MyCoRe
 * @exception MCRConfigurationException      
 *                              an Exception of MyCoRe Configuration
 **/
public final void setFromQuery(String type, String query) 
  throws MCRException, MCRConfigurationException
  {
  String persist_type = "";
  String proptype = "MCR.persistence_type_"+type.toLowerCase();
  try {
    persist_type = config.getString(proptype);
    String proppers = "MCR.persistence_"+persist_type.toLowerCase()+
      "_query_name";
    mcr_transform = (MCRQueryInterface)config.getInstanceOf(proppers);
    mcr_result = mcr_transform.getResultList(query,type,vec_max_length);
    }
  catch (Exception e) {
     throw new MCRException(e.getMessage(),e); }
  for (int i=0;i< mcr_result.size();i++) {
    MCRObject obj = new MCRObject();
    mcr_xml.addElement(
      obj.receiveXMLFromDatastore((String)mcr_result.elementAt(i))); 
    }
  }

// Error text
private static String ERRORTEXT =
  "The stream for the MCRQueryResult constructor is false.";

/**
 * This methode fills the internal MCRObjectId- and XML-vector
 * fron a XML input stream with following form:<br>
 * &lt;mcr_results&gt;<br>
 * &lt;mcr_result&gt;<br>
 * &lt;mcr_objectid&gt;MCRObjectId&lt;/mcr_objectid&gt;<br>
 * &lt;mcr_xml&gt;XML&lt;/mcr_xml&gt;<br>
 * &lt;/mcr_result&gt;<br>
 * ...<br>
 * &lt;/mcr_results&gt;<br>
 *
 * @param xmlstream the input straem of the result collection
 * @exception MCRException if the stream has not the correct format.
 * @exception MCRConfigurationException      
 *                              an Exception of MyCoRe Configuration
 **/
public final void setFromXMLResultStream(String xmlstream) 
  {
  NL = System.getProperty("line.separator");
  config = MCRConfiguration.instance();
  vec_max_length = config.getInt("MCR.query_max_results",10);
  mcr_result = new Vector(vec_max_length);
  mcr_xml = new Vector(vec_max_length);
  int itagresultss = xmlstream.indexOf(TAG_RESULTS_S);
  if (itagresultss == -1) { throw new MCRException(ERRORTEXT); }
  itagresultss += TAG_RESULTS_S.length();
  int itagresultse = xmlstream.indexOf(TAG_RESULTS_E,itagresultss);
  if (itagresultse == -1) { throw new MCRException(ERRORTEXT); }
  int itagresults = xmlstream.indexOf(TAG_RESULT_S,itagresultss);
  if (itagresults == -1) { return; }
  int ltagresulte = TAG_RESULT_E.length();
  int itagresulte = 0;
  int itagmcrids = 0;
  int ltagmcrids = TAG_MCRID_S.length();
  int itagmcride = 0;
  int itagxmls = 0;
  int ltagxmls = TAG_XML_S.length();
  int itagxmle = 0;
  while (itagresults != -1) {
    itagmcrids = xmlstream.indexOf(TAG_MCRID_S,itagresults);
    if (itagmcrids == -1) { throw new MCRException(ERRORTEXT); }
    itagmcride = xmlstream.indexOf(TAG_MCRID_E,itagmcrids);
    if (itagmcride == -1) { throw new MCRException(ERRORTEXT); }
    itagxmls = xmlstream.indexOf(TAG_XML_S,itagresults);
    if (itagxmls == -1) { throw new MCRException(ERRORTEXT); }
    itagxmle = xmlstream.indexOf(TAG_XML_E,itagxmls);
    if (itagmcride == -1) { throw new MCRException(ERRORTEXT); }
    mcr_result.addElement(xmlstream.substring(itagmcrids+ltagmcrids,itagmcride)
      .trim());
    mcr_xml.addElement(xmlstream.substring(itagxmls+ltagxmls,itagxmle)
      .trim());
    itagresulte = xmlstream.indexOf(TAG_RESULT_E,itagresults);
    if (itagresulte == -1) { throw new MCRException(ERRORTEXT); }
    itagresults = xmlstream.indexOf(TAG_RESULT_S,itagresulte+ltagresulte);
    }
  }

/**
 * This methode return a XML stream of the result collection in form of<br>
 * &lt;mcr_results&gt;<br>
 * &lt;mcr_result&gt;<br>
 * &lt;mcr_objectid&gt;MCRObjectId&lt;/mcr_objectid&gt;<br>
 * &lt;mcr_xml&gt;XML&lt;/mcr_xml&gt;<br>
 * &lt;/mcr_result&gt;<br>
 * ...<br>
 * &lt;/mcr_results&gt;<br>
 *
 * @exception MCRException if the result list is empty.
 * @return the result collection as an XML stream.
 **/
public final String getXMLResultStream()
  {
  if (mcr_result.size()==0) { 
    throw new MCRException("The MCRQueryResult vector is empty."); }
  StringBuffer sb = new StringBuffer(204800);
  sb.append(TAG_RESULTS_S).append(NL);
  for (int i=0;i<mcr_result.size();i++) {
    sb.append(TAG_RESULT_S).append(NL);
    sb.append(TAG_MCRID_S).append(mcr_result.elementAt(i)).append(TAG_MCRID_E)
      .append(NL);
    sb.append(TAG_XML_S).append(mcr_xml.elementAt(i)).append(TAG_XML_E)
      .append(NL);
    sb.append(TAG_RESULT_E).append(NL);
    }
  sb.append(TAG_RESULTS_E).append(NL);
  return sb.toString();
  }

/**
 * This methode return the size of the result vector.
 *
 * @return the size of the result vector
 **/
public final int getSize()
  { return mcr_result.size(); }

/**
 * This methode get one MCRObjectId as string from the result list
 * for a given element number.
 *
 * @param index              the index of the Element
 * return a MCRObjectId of the index or null if no object for this index
 * was found.
 **/
public final String getMCRObjectIdOfElement(int index)
  {
  if ((index < 0) || (index > mcr_result.size())) { return null; }
  return (String) mcr_result.elementAt(index);
  }

/**
 * This methode get one XML stream as string from the result list
 * for a given element number.
 *
 * @param index              the index of the Element
 * return a XML stream of the index or null if no object for this index
 * was found.
 **/
public final String getXMLOfElement(int index)
  {
  if ((index < 0) || (index > mcr_xml.size())) { return null; }
  return (String) mcr_xml.elementAt(index);
  }

/**
 * This methode print the MCRObjectId as string from the result list.
 **/
public final void debug()
  {
  System.out.println("Result list:");
  for (int i=0;i< mcr_result.size();i++) {
    System.out.println("  "+(String)mcr_result.elementAt(i)+"\n"); 
    System.out.println("  "+(String)mcr_xml.elementAt(i)+"\n"); }
  System.out.println();
  }
}

