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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import mycore.common.MCRException;
import mycore.datamodel.MCRObjectID;

/**
 * This class implements all method for handling with the MCRMetaLink part
 * of a metadata object. The MCRMetaLink class present a reference of
 * a other MCRObject with a role of them.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
final public class MCRMetaLink extends MCRMetaDefault 
  implements MCRMetaInterface 
{

// MetaLink data
private String role;
private MCRObjectID href;

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * All other elemnts was set to an empty string.
 */
public MCRMetaLink()
  {
  super();
  role = "";
  href = null;
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is null, empty or false <b>en</b> was set. The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The type element was set to
 * the value of <em>set_type<em>, if it is null, an empty string was set
 * to the type element. The role element was set to the value of 
 * <em>set_role<em>, if it is null, an empty string was set
 * to the role element. The href element is a MCRObjectID string. It was
 * set to <em>set_href</em>, if it is null or empty, null was set.
 *
 * @param set_datapart     the global part of the elements like 'metadata'
 *                         or 'service'
 * @param set_subtag       the name of the subtag
 * @param default_lang     the default language
 * @param set_type         the optional type string
 * @param set_href         the reference ID
 * @param set_role         the role string
 * @exception MCRException if the set_subtag value is null or empty
 */
public MCRMetaLink(String set_datapart, String set_subtag, 
  String default_lang, String set_type, String set_href, String set_role)
   throws MCRException
  {
  super(set_datapart,set_subtag,default_lang,set_type);
  href = null;
  href = new MCRObjectID(set_href);
  role = "";
  if (set_role != null) { role = set_role.trim(); }
  }

/**
 * This method set the href and role. 
 *
 * @param set_href        the reference ID
 * @param set_role        the new role string
 **/
public final void set(String set_href, String set_role)
  {
  if (set_href != null) { href = new MCRObjectID(set_href); }
  if (set_role != null) { role = set_role.trim(); }
  }

/**
 * This method set the role. 
 *
 * @param set_role        the new role string
 **/
public final void setRole(String set_role)
  { if (set_role != null) { role = set_role.trim(); } }

/**
 * This method set the reference.
 *
 * @param set_href        the new reference string
 **/
public final void setHref(String set_href)
  { if (set_href != null) { href = new MCRObjectID(set_href); } }

/**
 * This method get the role element.
 *
 * @return the role
 **/
public final String getRole()
  { return role; }

/**
 * This method get the href element.
 *
 * @return the href as string
 **/
public final String getHrefToString()
  { return href.getId(); }

/**
 * This method get the href element.
 *
 * @return the href as MCRObjectId
 **/
public final MCRObjectID getHerf()
  { return href; }

/**
 * This method read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param dom_metadata_node a relevant DOM element for the metadata
 **/
public final void setFromDOM(Node dom_metadata_node)
  {
  super.setFromDOM(dom_metadata_node);
  String temp = ((Element)dom_metadata_node).getAttribute("xlink:role");
  if ((temp!=null) && ((temp = temp.trim()).length() !=0)) {
    role = temp; }
  temp = ((Element)dom_metadata_node).getAttribute("xlink:href");
  if ((temp!=null) && ((temp = temp.trim()).length() !=0)) {
    href = new MCRObjectID(temp); }
  }

/**
 * This method create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRMetaLink definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML MCRMetaLink part
 **/
public final String createXML() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  StringBuffer sb = new StringBuffer(1024);
  sb.append('<').append(subtag).append(" xlink:href=\"").append(href.getId());
  if ((role != null) && ((role = role.trim()).length() !=0)) {
    sb.append("\" xlink:role=\"").append(role).append("\""); }
  sb.append("/>").append(NL);
  return sb.toString();
  }

/**
 * This method create a Text Search stream for all data in this class, defined
 * by the MyCoRe TS MCRMetaLink definition for the given tag and subtag.
 * The content of this stream is depended by the implementation for
 * the persistence database. It was choose over the
 * <em>MCR.persistence_type</em> configuration.
 *
 * @param mcr_query   a class they implement the <b>MCRQueryInterface</b>
 * @param tag                the tagname of an element list
 * @exception MCRException if the content of this class is not valid
 * @return a TS string with the TS MCRMetaLink part
 **/
public final String createTS(Object mcr_query, String tag) throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  String [] sattrib = null;
  String [] svalue = null;
  if ((role != null) && ((role = role.trim()).length() !=0)) {
    sattrib = new String[1]; sattrib[0] = "role";
    svalue = new String[1]; svalue[0] = role; 
    }
  StringBuffer sb = new StringBuffer(1024);
  sb.append(((MCRQueryInterface)mcr_query).createSearchStringHref(datapart,
    tag,subtag,sattrib,svalue,href));
  return sb.toString();
  }

/**
 * This method check the validation of the content of this class.
 * The method returns <em>true</em> if
 * <ul>
 * <li> the subtag is not null or empty
 * <li> the href is a valid MCRObjectId
 * </ul>
 * otherwise the method return <em>false</em>
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if (!super.isValid()) { return false; }
  if (!href.isValid()) { return false; }
  return true;
  }

/**
 * This method print all data content from the MCRMetaLink class.
 **/
public final void debug()
  {
  System.out.println("MCRMetaLink debug start:");
  super.debug();
  System.out.println("<href>"+href.getId()+"</href>");
  System.out.println("<role>"+role+"</role>");
  System.out.println("MCRMetaLink debug end"+NL);
  }

}

