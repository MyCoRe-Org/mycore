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
import mycore.common.MCRException;
import mycore.datamodel.MCRObject;

/**
 * This class is the cache of one result list included all XML files
 * as result of one query. They holds informations about host, rank and
 * the XML files. You can get the complete result or elements of them
 * as XML file for transforming with XSLT.
 *
 * @author Jens Kupferschmidt
 * @author Mathias Zarick
 * @version $Revision$ $Date$
 **/
public class MCRQueryResultArray
{

// data
private ArrayList host;
private ArrayList mcr_id;
private ArrayList rank;
private ArrayList xml;
private static String NL;

/** The tag for the result collection **/
public static final String TAG_RESULTS = "mcr_results";
/** The tag for one result **/
public static final String TAG_RESULT = "mcr_result";
/** The attribute of the host name **/
public static final String ATTR_HOST = "host";
/** The attribute of the MCRObjectId **/
public static final String ATTR_ID = "id";
/** The attribute of the rank **/
public static final String ATTR_RANK = "rank";

/**
 * This constructor create the MCRQueryResultArray class with an empty
 * query result list.
 **/
public MCRQueryResultArray()
  {
  NL = System.getProperty("line.separator");
  host = new ArrayList();
  mcr_id = new ArrayList();
  rank = new ArrayList();
  xml = new ArrayList();
  }

/**
 * This constructor create the MCRQueryResultArray class with a given
 * query result list.
 *
 * @param in a MCRQueryResultArray as input
 **/
public MCRQueryResultArray(MCRQueryResultArray in)
  {
  NL = System.getProperty("line.separator");
  host = new ArrayList();
  mcr_id = new ArrayList();
  rank = new ArrayList();
  xml = new ArrayList();
  for (int i=0;i<in.size();i++) {
    host.add(in.getHost(i));
    mcr_id.add(in.getId(i));
    rank.add(new Integer(in.getRank(i)));
    xml.add(in.getXML(i));
    }
  }

/**
 * This methode return the size of the result list.
 *
 * @return the size of the result list
 **/
public final int size()
  { return host.size(); }

/**
 * This methode return the host of an element index.
 *
 * @param index   the index in the list
 * @return an empty string if the index is outside the border, else return
 * the host name
 **/
public final String getHost(int index)
  {
  if ((index<0)||(index>host.size())) { return ""; }
  return (String) host.get(index);
  }

/**
 * This method sets an host element at a specified position.
 *
 * @param index   the index in the list
 * @param newhost the new value in the list
 * @return an empty string if the index is outside the border, else return
 * the host name
 **/
public final void setHost(int index,String newhost)
  {
  host.set(index,newhost);
  }

/**
 * This methode return the MCRObjectId of an element index as string.
 *
 * @param index   the index in the list
 * @return an empty string if the index is outside the border, else return
 * the MCRObjectId as string.
 **/
public final String getId(int index)
  {
  if ((index<0)||(index>mcr_id.size())) { return ""; }
  return (String) mcr_id.get(index);
  }

/**
 * This methode return the rank of an element index.
 *
 * @param index   the index in the list
 * @return -1 if the index is outside the border, else return the rank
 **/
public final int getRank(int index)
  {
  if ((index<0)||(index>rank.size())) { return -1; }
  return ((Integer)rank.get(index)).intValue();
  }

/**
 * This methode return the XML stream without header of an element index.
 *
 * @param index   the index in the list
 * @return an empty string if the index is outside the border, else return
 * the NON well formed XML without header
 **/
public final String getXMLBody(int index)
  {
  if ((index<0)||(index>host.size())) { return ""; }
  return (String) xml.get(index);
  }

/**
 * This methode return the well formed XML stream of an element index.
 *
 * @param index   the index in the list
 * @return an empty string if the index is outside the border, else return
 * the well formed XML without header
 **/
public final String getXML(int index)
  {
  if ((index<0)||(index>host.size())) { return ""; }
  StringBuffer sb = new StringBuffer(2048);
  sb.append(MCRObject.XML_HEADER).append(NL);
  sb.append((String)xml.get(index));
  return sb.toString();
  }

/**
 * This methode add one element to the result list.
 *
 * @param in_host    the host input as a string
 * @param in_id      the MCRObjectId input as a string
 * @param in_rank    the rank input as an integer
 * @param in_xml     the well formed XML stream as a string
 **/
public final void add(String in_host, String in_id, int in_rank, String in_xml)
  {
  host.add(in_host);
  mcr_id.add(in_id);
  rank.add(new Integer(in_rank));
  int i = in_xml.indexOf(NL);
  xml.add(in_xml.substring(i+NL.length(),in_xml.length()));
  }

/**
 * This methode return a well formed XML stream of the result collection
 * in form of<br>
 * &lt;?xml version="1.0" encoding="iso-8859-1"?&gt;<br>
 * &lt;mcr_results&gt;<br>
 * &lt;mcr_result host="<em>host</em> id="<em>MCRObjectId</em>"
 *  rank="<em>rank</em>" &gt;<br>
 * &lt;mcrobject&gt;<br>
 * ...<br>
 * &lt;/mcrobject&gt;<br>
 * &lt;/mcr_result&gt;<br>
 * ...<br>
 * &lt;/mcr_results&gt;<br>
 *
 * @return the result collection as an XML stream.
 **/
public final String exportAll()
  {
  StringBuffer sb = new StringBuffer(204800);
  sb.append(MCRObject.XML_HEADER).append(NL);
  sb.append('<').append(TAG_RESULTS).append('>').append(NL);
  for (int i=0;i<rank.size();i++) {
    sb.append('<').append(TAG_RESULT).append(' ').append(ATTR_HOST)
      .append("=\"").append(host.get(i)).append("\" ").append(ATTR_ID)
      .append("=\"").append(mcr_id.get(i)).append("\" ").append(ATTR_RANK)
      .append("=\"").append(rank.get(i)).append("\" >").append(NL);
    sb.append(xml.get(i)).append(NL);
    sb.append("</").append(TAG_RESULT).append('>').append(NL);
    }
  sb.append("</").append(TAG_RESULTS).append('>').append(NL);
  return sb.toString();
  }

/**
 * This methode return a well formed XML stream of one result
 * in form of<br>
 * &lt;?xml version="1.0" encoding="iso-8859-1"?&gt;<br>
 * &lt;mcr_results&gt;<br>
 * &lt;mcr_result host="<em>host</em> id="<em>MCRObjectId</em>"
 *  rank="<em>rank</em>" &gt;<br>
 * &lt;mcrobject&gt;<br>
 * ...<br>
 * &lt;/mcrobject&gt;<br>
 * &lt;/mcr_result&gt;<br>
 * &lt;/mcr_results&gt;<br>
 *
 * @return one result as an XML stream. If index is out of border an
 * empty XML file was returned.
 **/
public final String exportElement(int index)
  {
  StringBuffer sb = new StringBuffer(204800);
  sb.append(MCRObject.XML_HEADER).append(NL);
  sb.append('<').append(TAG_RESULTS).append('>').append(NL);
  if ((index>=0)&&(index<=host.size())) {
    sb.append('<').append(TAG_RESULT).append(' ').append(ATTR_HOST)
      .append("=\"").append(host.get(index)).append("\" ").append(ATTR_ID)
      .append("=\"").append(mcr_id.get(index)).append("\" ").append(ATTR_RANK)
      .append("=\"").append(rank.get(index)).append("\" >").append(NL);
    sb.append(xml.get(index)).append(NL);
    sb.append("</").append(TAG_RESULT).append('>').append(NL);
    }
  sb.append("</").append(TAG_RESULTS).append('>').append(NL);
  return sb.toString();
  }

private static String ERRORTEXT =
  "The stream for the MCRQueryResultArray import is false.";

/**
 * This methode import a well formed XML stream of results and add it
 * to a NEW list.
 * in form of<br>
 * &lt;?xml version="1.0" encoding="iso-8859-1"?&gt;<br>
 * &lt;mcr_results&gt;<br>
 * &lt;mcr_result host="<em>host</em> id="<em>MCRObjectId</em>"
 *  rank="<em>rank</em>" &gt;<br>
 * &lt;mcrobject&gt;<br>
 * ...<br>
 * &lt;/mcrobject&gt;<br>
 * &lt;/mcr_result&gt;<br>
 * &lt;/mcr_results&gt;<br>
 *
 * @param the XML input stream
 **/
public final void importAll(String in)
  {
  host = new ArrayList();
  mcr_id = new ArrayList();
  rank = new ArrayList();
  xml = new ArrayList();
  importElements(in);
  }

/**
 * This methode import a well formed XML stream of results and add it to an
 * EXIST list.
 * in form of<br>
 * &lt;?xml version="1.0" encoding="iso-8859-1"?&gt;<br>
 * &lt;mcr_results&gt;<br>
 * &lt;mcr_result host="<em>host</em> id="<em>MCRObjectId</em>"
 *  rank="<em>rank</em>" &gt;<br>
 * &lt;mcrobject&gt;<br>
 * ...<br>
 * &lt;/mcrobject&gt;<br>
 * &lt;/mcr_result&gt;<br>
 * &lt;/mcr_results&gt;<br>
 *
 * @param the XML input stream
 **/
public final void importElements(String in)
  {
  int itagresultss = in.indexOf("<"+TAG_RESULTS+">");
  if (itagresultss == -1) { throw new MCRException(ERRORTEXT); }
  itagresultss = itagresultss+TAG_RESULTS.length()+2;
  int itagresultse = in.indexOf("</"+TAG_RESULTS+">",itagresultss);
  if (itagresultse == -1) { throw new MCRException(ERRORTEXT); }
  int itagresults = in.indexOf("<"+TAG_RESULT,itagresultss);
  if (itagresults == -1) { return; }
  int itagresulte = 0;
  int iattrstart = 0;
  int iattrend = 0;
  while (itagresults != -1) {
    itagresulte = in.indexOf(">",itagresults);
    if (itagresulte == -1) { throw new MCRException(ERRORTEXT); }
    iattrstart = in.indexOf(ATTR_HOST+"=\"",itagresults);
    if ((iattrstart == -1) || (iattrstart>itagresulte)) {
      throw new MCRException(ERRORTEXT); }
    iattrstart += ATTR_HOST.length()+2;
    iattrend = in.indexOf("\"",iattrstart);
    if ((iattrend == -1) || (iattrend>itagresulte)) {
      throw new MCRException(ERRORTEXT); }
    String inhost = in.substring(iattrstart,iattrend);
    iattrstart = in.indexOf(ATTR_ID+"=\"",itagresults);
    if ((iattrstart == -1) || (iattrstart>itagresulte)) {
      throw new MCRException(ERRORTEXT); }
    iattrstart += ATTR_ID.length()+2;
    iattrend = in.indexOf("\"",iattrstart);
    if ((iattrend == -1) || (iattrend>itagresulte)) {
      throw new MCRException(ERRORTEXT); }
    String inid = in.substring(iattrstart,iattrend);
    iattrstart = in.indexOf(ATTR_RANK+"=\"",itagresults);
    if ((iattrstart == -1) || (iattrstart>itagresulte)) {
      throw new MCRException(ERRORTEXT); }
    iattrstart += ATTR_RANK.length()+2;
    iattrend = in.indexOf("\"",iattrstart+1);
    if ((iattrend == -1) || (iattrend>itagresulte)) {
      throw new MCRException(ERRORTEXT); }
    Integer inrank = null;
    try {
      inrank = new Integer(in.substring(iattrstart+1,iattrend)); }
    catch (NumberFormatException e) {
      throw new MCRException(ERRORTEXT); }
    itagresults = itagresulte+1;
    itagresulte = in.indexOf("</"+TAG_RESULT+">",itagresulte);
    String inxml = in.substring(itagresults,itagresulte).trim();
    host.add(inhost);
    mcr_id.add(inid);
    rank.add(inrank);
    xml.add(inxml);
    itagresults = in.indexOf("<"+TAG_RESULT,itagresulte);
    }

  }

}

