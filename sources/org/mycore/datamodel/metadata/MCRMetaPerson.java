/**
 * $RCSfile$
 * $Revision$ $Date$
 * RCSfile: MCRMetaPerson.java,v $
 * Revision: 1.5 $ $Date$
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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import mycore.common.MCRException;

/**
 * This class implements all methods for handling with the MCRMetaPerson part
 * of a metadata object. The MCRMetaPerson class represents a natural person
 * specified by a list of names. 
 *
 * @author J. Vogler
 * @version $Revision$ $Date$
 **/
final public class MCRMetaPerson extends MCRMetaDefault 
  implements MCRMetaInterface 
{

// MetaPerson data
private String firstname;
private String callname;
private String surename;
private String academic;
private String peerage;

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * All other elemnts are set to an empty string.
 */
public MCRMetaPerson()
  {
  super();
  firstname = "";
  callname  = "";
  surename  = "";
  academic  = "";
  peerage   = "";
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is null, empty or false <b>en</b> was set. The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The type element was set to
 * the value of <em>set_type<em>, if it is null, an empty string was set
 * to the type element. The firstname, callname, surename, academic and
 * peerage element was set to the value of <em>set_...<em>, if they are null,
 * an empty string was set to this element.
 *
 * @param set_subtag      the name of the subtag
 * @param default_lang    the default language
 * @param set_type        the optional type string
 * @param set_firstname   the first name
 * @param set_callname    the call name
 * @param set_surename    the sure name
 * @param set_academic    the academic title
 * @param set_peerage     the peerage title
 * @exception MCRException if the parameter values are invalid
 **/
public MCRMetaPerson(String set_subtag, String default_lang, String set_type,
  String set_firstname, String set_callname, String set_surename, String
  set_academic, String set_peerage) throws MCRException
  {
  super(set_subtag,default_lang,set_type);
  firstname = "";
  callname  = "";
  surename  = "";
  academic  = "";
  peerage   = "";
  set(set_firstname,set_callname,set_surename,set_academic,set_peerage);
  }

/**
 * This methode set all name componets. 
 *
 * @param set_firstname   the first name
 * @param set_callname    the call name
 * @param set_surename    the sure name
 * @param set_academic    the academic title
 * @param set_peerage     the peerage title
 **/
public final void set(String set_firstname, String set_callname, String 
  set_surename, String set_academic, String set_peerage)
  {
  if ((set_firstname == null) || (set_callname == null) ||
      (set_surename  == null) || (set_academic == null) || 
      (set_peerage   == null)) {
    throw new MCRException("One parameter is null."); }
  firstname = set_firstname.trim();
  callname  = set_callname.trim();
  surename  = set_surename.trim();
  academic  = set_academic.trim();
  peerage   = set_peerage.trim();
  }

/**
 * This method get the firstname text element.
 *
 * @return the firstname
 **/
public final String getFirstname()
  { return firstname; }

/**
 * This method get the callname text element.
 *
 * @return the callname
 **/
public final String getCallname()
  { return callname; }

/**
 * This method get the surename text element.
 *
 * @return the surename
 **/
public final String getSurename()
  { return surename; }

/**
 * This method get the academic text element.
 *
 * @return the academic
 **/
public final String getAcademic()
  { return academic; }

/**
 * This method get the peerage text element.
 *
 * @return the peerage
 **/
public final String getPeerage()
  { return peerage; }

/**
 * This method reads the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param metadata_person_node       a relevant DOM element for the metadata
 **/
public final void setFromDOM(Node metadata_person_node)
  {
  super.setFromDOM(metadata_person_node);
  NodeList childs_name_nodelist = metadata_person_node.getChildNodes();
  firstname = seekElementText("firstname", childs_name_nodelist);
  callname  = seekElementText("callname" , childs_name_nodelist);
  surename  = seekElementText("surename" , childs_name_nodelist);
  academic  = seekElementText("academic" , childs_name_nodelist);
  peerage   = seekElementText("peerage"  , childs_name_nodelist);
  }

private final String seekElementText(String seek_element, 
  NodeList childs_nodelist)
  {
  String temp_child_node_name;
  String seek_element_text = "";
  int length_childs_nodelist = childs_nodelist.getLength();
  for (int i = 0; i < length_childs_nodelist; i++) {
    temp_child_node_name = childs_nodelist.item(i).getNodeName();
    if ( temp_child_node_name.equals(seek_element) ) {
      Node seek_node  = childs_nodelist.item(i);
      NodeList seek_childs_nodelist = seek_node.getChildNodes();
      Node text_node  = seek_childs_nodelist.item(0);
      int text_node_type   = text_node.getNodeType();
      if ( text_node_type  == Node.TEXT_NODE ) {
        seek_element_text = text_node.getNodeValue().trim();
        return seek_element_text.trim(); }
      }
    }
  return seek_element_text;
  } 

/**
 * This method creates a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRMetaPerson definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML MCRMetaPerson part
 **/
public final String createXML() throws MCRException
  {
  if (!isValid()) {
    throw new MCRException("The content is not valid."); }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("<").append(subtag).append(" type=\"").append(type)
    .append("\" xml:lang=\"").append(lang).append("\" >").append(NL);
  if ((firstname = firstname.trim()).length() !=0) {
    sb.append("<firstname>").append(firstname).append("</firstname>").append(NL); }
  if ((callname  = callname .trim()).length() !=0) {
    sb.append("<callname>") .append(callname) .append("</callname>") .append(NL); }
  if ((surename  = surename .trim()).length() !=0) {
    sb.append("<surename>") .append(surename) .append("</surename>") .append(NL); }
  if ((academic  = academic .trim()).length() !=0) {
    sb.append("<academic>") .append(academic) .append("</academic>") .append(NL); }
  if ((peerage   = peerage  .trim()).length() !=0) {
    sb.append("<peerage>")  .append(peerage)  .append("</peerage>")  .append(NL); }
  sb.append("</").append(subtag).append(">").append(NL); 
  return sb.toString();
  }

/**
 * This method creates a Text Search stream for all data in this class, defined
 * by the MyCoRe TS MCRMetaPerson definition for the given tag and subtag.
 * The content of this stream is depending on the implementation for
 * the persistence database chosen by the <em>MCR.persistence_type</em> 
 * configuration.
 *
 * @param mcr_query   a class implementing the <b>MCRQueryInterface</b>
 * @exception MCRException if the content of this class is not valid
 * @return a TS string with the TS MCRMetaPerson part
 **/
public final String createTS(Object mcr_query) throws MCRException
  {
  if (!isValid()) {
    throw new MCRException("The content is not valid."); }
  String [] sattrib = null;
  String [] svalue = null;
  StringBuffer sb = new StringBuffer(1024);
  if (type.trim().length()!=0) {
    sattrib = new String[1]; sattrib[0] = "type";
    svalue = new String[1]; svalue[0] = type;
    }
  if (firstname.trim().length()!=0) {
    sb.append(((MCRQueryInterface)mcr_query).createSearchStringText(subtag,
      sattrib,svalue,"firstname",null,null,firstname)); }
  if (surename.trim().length() !=0) {
    sb.append(((MCRQueryInterface)mcr_query).createSearchStringText(subtag,
      sattrib,svalue,"surename",null,null,surename)); }
  if (callname.trim().length() !=0) {
    sb.append(((MCRQueryInterface)mcr_query).createSearchStringText(subtag,
      sattrib,svalue,"callname",null,null,callname)); }
  if (academic.trim().length() !=0) {
    sb.append(((MCRQueryInterface)mcr_query).createSearchStringText(subtag,
      sattrib,svalue,"academic",null,null,academic)); }
  if (peerage.trim().length()  !=0) {
    sb.append(((MCRQueryInterface)mcr_query).createSearchStringText(subtag,
      sattrib,svalue,"peerage",null,null,peerage)); }
  return sb.toString();
  }

/**
 * This method checks the validation of the content of this class.
 * The method returns <em>false</em> if
 * <ul>
 * <li> the firstname is empty and
 * <li> the callname  is empty and
 * <li> the surename  is empty and
 * <li> the academic  is empty and
 * <li> the peerage   is empty
 * </ul>
 * otherwise the method returns <em>true</em>.
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if (((firstname=firstname.trim()).length() ==0) && 
      ((callname =callname.trim()).length()  ==0) &&
      ((surename =surename.trim()).length()  ==0) &&
      ((academic =academic.trim()).length()  ==0) &&
      ((peerage  =peerage.trim()).length()   ==0)) {
    return false; }
  return true;
  }

/**
 * This metod prints all data content from the MCRMetaPerson class.
 **/
public final void debug()
  {
  super.debug();
  if (firstname.length()!=0) {
    System.out.println("firstname : "+firstname); }
  if (callname.length()!=0) {
    System.out.println("callname  : "+callname); }
  if (surename.length()!=0) {
    System.out.println("surename  : "+surename); }
  if (academic.length()!=0) {
    System.out.println("academic  : "+academic); }
  if (peerage.length()!=0) {
    System.out.println("peerage   : "+peerage); }
  System.out.println("--- ---");
  }

}

