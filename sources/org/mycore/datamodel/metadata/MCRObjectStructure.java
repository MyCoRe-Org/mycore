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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class treats the outer structure of documents, that is the
 * linking from one object to another one. A link is described by the
 * <em>MCRMetaLink</em> class. The href variable of MCRMetaLink gives
 * the ID of the linked document, while the role variable is used for
 * further description of the link. Two special cases of roles are
 * supported: "role=child" declares the linked object to be a child of
 * the linking object and possibly inherit metadata from it as given
 * by the "heritable" attribute of the parent's metadata block, while
 * "role=parent" means the inverse direction of a child link.
 * Not supported by this class are links from or to a defined place
 * of a document (inner structure and combination of inner and outer
 * structures of the objects). This will possibly be done in a later
 * extension of <em>MCRMetaLink</em> and <em>MCRObjectStructure</em>.
 *
 * @author Mathias Hegner 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRObjectStructure
{
  private String NL = null;
  private Vector the_links = null;

  /**
   * The constructor initializes NL (non-static, in order to enable
   * different NL's for different objects) and the_links (the elements
   * of this "link vector" are MCRMetaLink's).
   */
  public MCRObjectStructure ()
  {
    NL = System.getProperties().getProperty("line.separator");
    the_links = new Vector();
  }

  /**
   * <em>addLink</em> checks whether a (new) link is already contained
   * in the link vector (identical role and href) or not. If not, the
   * link will be added to the vector, otherwise nothing will be done,
   * preventing double-linked objects.
   * If the link could be added a "true" will be returned, otherwise "false".
   *
   * @param add_link             the link to be added
   * @return boolean             true, if operation successfully completed
   */
  public final boolean addLink (MCRMetaLink add_link)
  {
    String role = add_link.getRole();
    String role_trim = null;
    if (role == null) role = "";
    role = role.trim();
    String href = add_link.getHrefToString();
    MCRMetaLink link = null;
    for (int i = 0; i < the_links.size(); ++i)
    {
      link = (MCRMetaLink) the_links.elementAt(i);
      role_link = link.getRole();
      if (role_link == null) role_link = "";
      role_link = role_link.trim();
      if (href.equals(link.getHrefToString()) && role.equals(role_link))
        return false;
    }
    the_links.addElement(add_link);
    return true;
  }

  /**
   * <em>removeLink</em> checks whether a link is contained in the link
   * vector (identical role and href) or not. If so, the link will be
   * removed from the vector, returning true. Otherwise nothing is done,
   * returning false.
   *
   * @param rem_link             the link to be removed
   * @return boolean             true, if operation successfully completed
   */
  public final boolean removeLink (MCRMetaLink rem_link)
  {
    String role = rem_link.getRole();
    String role_link = null;
    if (role == null) role = "";
    role = role.trim();
    String href = rem_link.getHrefToString();
    MCRMetaLink link = null;
    for (int i = 0; i < the_links.size(); ++i)
    {
      link = (MCRMetaLink) the_links.elementAt(i);
      role_link = link.getRole();
      if (role_link == null) role_link = "";
      role_link = role_link.trim();
      if (href.equals(link.getHrefToString()) && role.equals(role_link))
      {
        the_links.removeElementAt(i);
        return true;
      }
    }
    return false;
  }

  /**
   * <em>removeAllLinks</em> removes all links from the link vector.
   */
  public final void removeAllLinks ()
  {
    the_links.removeAllElements();
  }

  /**
   * While the preceding methods dealt with the structure's copy in memory only,
   * the following three will affect the operations to or from datastore too.
   * Thereby <em>setFromDOM</em> will read the structure data from an XML
   * input stream (the "structure" entry).
   *
   * @param dom_elem_list        the structure node list
   */
  public final void setFromDOM (NodeList dom_elem_list)
  {
    removeAllLinks();
    Node struct_node = dom_elem_list.item(0);
    NodeList link_list = struct_node.getChildNodes();
    int i, n = link_list.getLength();
    MCRMetaLink link = null;
    Node link_node = null;
    for (i = 0; i < n; ++i)
    {
      link = new MCRMetaLink();
      link_node = link_list.item(i);
      link.setFromDOM(link_node);
      // When read from an XML input stream, the check for double-linkings
      // is omitted. XML files are considered to be clean, this will increase
      // the load performance !
      the_links.addElement(link);
    }
  }

  /**
   * <em>createXML</em> is the inverse of setFromDOM and converts the
   * structure's memory copy into an XML string.
   *
   * @return String              the structure XML string
   */
  public final String createXML ()
  {
    String xml_str = "<structure>" + NL;
    int i, n = the_links.size();
    MCRMetaLink link = null;
    for (i = 0; i < n; ++i)
    {
      link = (MCRMetaLink) the_links.elementAt(i);
      xml_str += link.createXML();
    }
    xml_str += "</structure>" + NL;
    return xml_str;
  }

  /**
   * <em>createTS</em> creates a text search string from structure data.
   * In cases of parent links, it looks for the parent's inheritable
   * metadata and appends these to the text search string. The output
   * string depends on the persistency database implementation.
   *
   * @param mcr_query            implementor of MCRQueryInterface
   * @return String              text search output string
   */
  public final String createTS (Object mcr_query)
  {
    String ts_str = "" + NL;
    int i, n = the_links.size();
    MCRMetaLink link = null;
    String role = null;
    for (i = 0; i < n; ++i)
    {
      link = (MCRMetaLink) the_links.elementAt(i);
      ts_str += link.createTS(mcr_query);
      role = link.getRole();
      if (role == null) continue;
      if (! role.trim().equals("parent")) continue;
      // treat the parent case here:
/*
##### metadata inheritance not yet supported in this version,
##### to be done next time !!
*/
    }
    return ts_str;
  }

  /**
   * <em>isValid</em> checks whether all of the MCRMetaLink's in the
   * link vector are valid or not.
   *
   * @return boolean             true, if structure is valid
   */
  public final boolean isValid ()
  {
    for (int i = 0; i < the_links.size(); ++i)
      if (! ((MCRMetaLink) the_links.elementAt(i)).isValid())
        return false;
    return true;
  }

  /**
   * <em>debug</em> prints all information about the structure of the
   * document (contained in the link vector).
   */
  public final void debug ()
  {
    System.out.println("The document's structure data :");
    int i, n = the_links.size();
    MCRMetaLink link = null;
    System.out.println("The outer structure consists of " + n + " links :");
    for (i = 0; i < n; ++i)
    {
      link = (MCRMetaLink) the_links.elementAt(i);
      System.out.println("" + i + " : " + link.getRole() + " : " +
        link.getHrefToString());
    }
    System.out.println("The end of the document's structure data is reached.");
  }
}

