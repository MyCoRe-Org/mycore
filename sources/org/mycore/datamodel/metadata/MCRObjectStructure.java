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

import java.util.Vector;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import mycore.common.MCRException;

/**
 * This class treats the outer structure of documents, that is the
 * linking from one object to another one. A link is described by the
 * <em>MCRMetaLink</em> class. The href variable of MCRMetaLink gives
 * the ID of the linked document, while the label variable is used for
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
  private Vector the_derivates = null;

  /**
   * The constructor initializes NL (non-static, in order to enable
   * different NL's for different objects) and the_links (the elements
   * of this "link vector" are MCRMetaLink's).
   */
  public MCRObjectStructure ()
  {
    NL = System.getProperties().getProperty("line.separator");
    the_links = new Vector();
    the_derivates = new Vector();
  }

  /**
   * <em>addLink</em> methode append the given link data to the link vector.
   * If the link could be added a "true" will be returned, otherwise "false".
   *
   * @param add_link             the link to be added
   * @return boolean             true, if operation successfully completed
   */
  public final boolean addLink (MCRMetaLink add_link)
  {
    the_links.addElement(add_link);
    return true;
  }

  /**
   * <em>addDerivate</em> methode append the given derivate link data to the
   * derivate vector.
   * If the link could be added a "true" will be returned, otherwise "false".
   *
   * @param add_derivate         the link to be added
   * @return boolean             true, if operation successfully completed
   */
  public final boolean addDerivate (MCRMetaLink add_derivate)
  {
    the_derivates.addElement(add_derivate);
    return true;
  }

  /**
   * <em>removeLink</em> the link from the link vector for the given number.
   *
   * @param rem_link             the link to be removed
   * @return boolean             true, if operation successfully completed
   */
  public final boolean removeLink (int number)
  {
    the_links.removeElementAt(number);
    return true;
  }

  /**
   * <em>removeDerivate</em> the derivate link from the derivate vector for 
   * the given number.
   *
   * @param rem_derivate         the link to be removed
   * @return boolean             true, if operation successfully completed
   */
  public final boolean removeDerivate (int number)
  {
    the_derivates.removeElementAt(number);
    return true;
  }

  /**
   * <em>removeAllLinks</em> removes all links from the link vector.
   */
  public final void removeAll ()
  {
    the_links.removeAllElements();
    the_derivates.removeAllElements();
  }

  /**
   * While the preceding methods dealt with the structure's copy in memory only,
   * the following three will affect the operations to or from datastore too.
   * Thereby <em>setFromDOM</em> will read the structure data from an XML
   * input stream (the "structure" entry).
   *
   * @param dom_element_list        the structure node list
   */
  public final void setFromDOM (NodeList dom_element_list)
  {
    removeAll();
    Node link_element, der_element;
    NodeList link_list, der_list;
    Element struct_element = (Element)dom_element_list.item(0);
    // Structure link part
    NodeList struct_links_list =
      struct_element.getElementsByTagName("links");
    if (struct_links_list.getLength()>0) {
      Node struct_links_element = struct_links_list.item(0);
      NodeList struct_link_list = struct_links_element.getChildNodes();
      int link_len = struct_link_list.getLength();
      for (int i=0;i<link_len;i++) {  
        link_element = struct_link_list.item(i);
        if (link_element.getNodeType() != Node.ELEMENT_NODE) { continue; }
        MCRMetaLink link = new MCRMetaLink();
        link.setDataPart("structure");
        link.setFromDOM(link_element);
        addLink(link);
        }
      }
    // Structure derivate part
    NodeList struct_ders_list =
      struct_element.getElementsByTagName("derivates");
    if (struct_ders_list.getLength()>0) {
      Node struct_ders_element = struct_ders_list.item(0);
      NodeList struct_der_list = struct_ders_element.getChildNodes();
      int der_len = struct_der_list.getLength();
      for (int i=0;i<der_len;i++) {  
        der_element = struct_der_list.item(i);
        if (der_element.getNodeType() != Node.ELEMENT_NODE) { continue; }
        MCRMetaLink der = new MCRMetaLink();
        der.setDataPart("structure");
        der.setFromDOM(der_element);
        addDerivate(der);
        }
      }
  }

  /**
   * <em>createXML</em> is the inverse of setFromDOM and converts the
   * structure's memory copy into an XML string.
   *
   * @exception MCRException if the content of this class is not valid
   * @return String              the structure XML string
   */
  public final String createXML () throws MCRException
  {
    if (!isValid()) {
      debug();
      throw new MCRException("The content is not valid."); }
    int i, n = the_links.size(), m = the_derivates.size();
    if ((n==0)&&(m==0)) { return "<structure/>" + NL; }
    StringBuffer sb = new StringBuffer(2048);
    sb.append("<structure>").append(NL);
    sb.append("<links>").append(NL);
    for (i = 0; i < n; ++i) {
      sb.append(((MCRMetaLink) the_links.elementAt(i)).createXML()); }
    sb.append("</links>").append(NL);
    sb.append("<derivates>").append(NL);
    for (i = 0; i < m; ++i) {
      sb.append(((MCRMetaLink) the_derivates.elementAt(i)).createXML()); }
    sb.append("</derivates>").append(NL);
    sb.append("</structure>").append(NL);
    return sb.toString();
  }

  /**
   * <em>createTS</em> creates a text search string from structure data.
   * In cases of parent links, it looks for the parent's inheritable
   * metadata and appends these to the text search string. The output
   * string depends on the persistency database implementation.
   *
   * @param mcr_query            implementor of MCRQueryInterface
   * @exception MCRException if the content of this class is not valid
   * @return String              text search output string
   */
  public final String createTS (Object mcr_query) throws MCRException
  {
    if (!isValid()) {
      debug();
      throw new MCRException("The content is not valid."); }
    int i, n = the_links.size(), m = the_derivates.size();
    if ((n==0)&&(m==0)) { return ""; }
    StringBuffer sb = new StringBuffer(2048);
    if (n!=0) {
    for (i=0;i<n;i++) {
      sb.append(((MCRMetaLink) the_links.elementAt(i))
        .createTS(mcr_query,"links")); }
      }
    else { sb.append(""); }
    if (m!=0) {
      for (i=0;i<m;i++) {
        sb.append(((MCRMetaLink) the_derivates.elementAt(i))
          .createTS(mcr_query,"derivates")); }
      }
    else { sb.append(""); }
    return sb.toString();
  }

  /**
   * <em>isValid</em> checks whether all of the MCRMetaLink's in the
   * link vector are valid or not.
   *
   * @return boolean             true, if structure is valid
   */
  public final boolean isValid ()
  {
    for (int i = 0; i < the_links.size(); ++i) {
      if (! ((MCRMetaLink) the_links.elementAt(i)).isValid())
        return false;
      }
    for (int i = 0; i < the_derivates.size(); ++i) {
      if (! ((MCRMetaLink) the_derivates.elementAt(i)).isValid())
        return false;
      }
    return true;
  }

  /**
   * <em>debug</em> prints all information about the structure of the
   * document (contained in the link vector).
   */
  public final void debug ()
  {
    System.out.println("The document's structure data :");
    int i, n = the_links.size(), m = the_derivates.size();
    MCRMetaLink link = null;
    System.out.println("The outer structure consists of " + n + " links :");
    for (i = 0; i < n; ++i)
    {
      link = (MCRMetaLink) the_links.elementAt(i);
      System.out.println("-->"+i+"<--");
      link.debug();
    }
    System.out.println("The outer structure consists of " + n + " derivates :");
    for (i = 0; i < m; ++i)
    {
      link = (MCRMetaLink) the_derivates.elementAt(i);
      System.out.println("-->"+i+"<--");
      link.debug();
    }
    System.out.println("The end of the document's structure data is reached.");
  }
}

