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
 * the ID of the linked document, while the label and title variables
 * can be used for further description of the link. Four types of links
 * are supported. Subtag name = "<linkTo...>" (corresponding links are
 * collected in the "<linksTo>" section of the "<structure...>" part)
 * means a simple link to another object, while subtag name = "linkFrom"
 * (in the "linksFrom" section) means the inverse of such a link.
 * Subtag name = "child" means a child link to a "parent" object
 * (collected in the "children" and "parents" section, respectively).
 * In this case, the child inherits all heritable metadata of the parent(s).
 * Note, that it is impossible to a priori prohibit multiple inheritance,
 * the MCRObjectStructure object can not decide what to do in cases
 * of such a "multiple parents" request. The user of this class still
 * should prevent or support multiple inheritance.
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
  private Vector linksTo = null;
  private Vector linksFrom = null;
  private Vector children = null;
  private Vector parents = null;
  private Vector inherited_metadata = null;
  // deprecated:
  private Vector the_links = null;
  private Vector the_derivates = null;

  /**
   * The constructor initializes NL (non-static, in order to enable
   * different NL's for different objects) and the link vectors
   * the elements of which are MCRMetaLink's.
   */
  public MCRObjectStructure ()
  {
    NL = System.getProperties().getProperty("line.separator");
    linksTo = new Vector ();
	linksFrom = new Vector ();
    children = new Vector ();
	parents = new Vector ();
	inherited_metadata = new Vector ();
	// deprecated:
	the_links = new Vector();
    the_derivates = new Vector ();
  }
  
  /**
   * <em>areIdentLocators</em> checks whether two given MCRMetaLink's have
   * identical href, label and title.
   * If so, a "true" will be returned, otherwise "false".
   * 
   * @param link1        1st link in comparison
   * @param link2        2nd link in comparison
   * @return boolean     result of the comparison
   */
  private static boolean areIdentLocators (MCRMetaLink link1, MCRMetaLink link2)
  {
	  String href1 = link1.getXLinkHrefToString();
	  String href2 = link2.getXLinkHrefToString();
	  if (! href1.equals(href2)) return false;
	  String label1 = link1.getXLinkLabel();
	  if (label1 == null) label1 = "";
	  String label2 = link2.getXLinkLabel();
	  if (label2 == null) label2 = "";
	  if (! label1.equals(label2)) return false;
	  String title1 = link1.getXLinkTitle();
	  if (title1 == null) title1 = "";
	  String title2 = link2.getXLinkTitle();
	  if (title2 == null) title2 = "";
	  if (! title1.equals(title2)) return false;
	  return true;
  }
  
  /**
   * <em>createLink</em> creates an MCRMetaLink from given subtag name, href,
   * label and title.
   * 
   * @param subtag                  subtag name
   * @param href                    MCRObjectID string of the linked object
   * @param label                   the link's label
   * @param title                   the link's title
   * @return MCRMetaLink            the xlink ("locator" type)
   */
  private static MCRMetaLink createLink (String subtag, String href,
										 String label, String title)
  {
	  String lang = "en";
	  MCRMetaLink link = null;
	  try
	  {
		  link = new MCRMetaLink ("structure", subtag, lang, "locator",
								  href, label, title);
	  }
	  catch (MCRException exc) { ; } // never thrown
	  return link;
  }

  /**
   * <em>addLinkTo</em> appends a link to another object if and only if
   * not already contained in the link vector, preventing from 
   * doubly-linked objects.
   * If the link could be added a "true" will be returned, otherwise "false".
   *
   * @param href                 the MCRObjectID string of the linked object
   * @param label                the link's label
   * @param title                the link's title
   * @return boolean             true, if operation successfully completed
   */
  final boolean addLinkTo (String href, String label, String title)
  {
	  MCRMetaLink link = createLink("linkTo", href, label, title);
	  int i, n = linksTo.size();
	  for (i = 0; i < n; ++i)
		  if (areIdentLocators((MCRMetaLink) linksTo.elementAt(i), link))
			  return false;
	  linksTo.addElement(link);
	  return true;
  }

  /**
   * <em>removeLinkTo</em> removes a link to another object from the link vector.
   * If the link was found a "true" will be returned, otherwise "false".
   *
   * @param href                 the MCRObjectID string of the linked object
   * @param label                the link's label
   * @param title                the link's title
   * @return boolean             true, if operation successfully completed
   */
  final boolean removeLinkTo (String href, String label, String title)
  {
	  MCRMetaLink link = createLink("linkTo", href, label, title);
	  int i, n = linksTo.size();
	  for (i = 0; i < n; ++i)
		  if (areIdentLocators((MCRMetaLink) linksTo.elementAt(i), link))
	  {
		  linksTo.removeElementAt(i);
		  return true;
	  }
	  return false;
  }

  /**
   * <em>addLinkFrom</em> appends a link from another object if and only if
   * not already contained in the link vector, preventing from 
   * doubly-linked objects.
   * If the link could be added a "true" will be returned, otherwise "false".
   *
   * @param href                 the MCRObjectID string of the linked object
   * @param label                the link's label
   * @param title                the link's title
   * @return boolean             true, if operation successfully completed
   */
  final boolean addLinkFrom (String href, String label, String title)
  {
	  MCRMetaLink link = createLink("linkFrom", href, label, title);
	  int i, n = linksFrom.size();
	  for (i = 0; i < n; ++i)
		  if (areIdentLocators((MCRMetaLink) linksFrom.elementAt(i), link))
			  return false;
	  linksFrom.addElement(link);
	  return true;
  }

  /**
   * <em>removeLinkFrom</em> removes a link from another object from the link vector.
   * If the link was found a "true" will be returned, otherwise "false".
   *
   * @param href                 the MCRObjectID string of the linked object
   * @param label                the link's label
   * @param title                the link's title
   * @return boolean             true, if operation successfully completed
   */
  final boolean removeLinkFrom (String href, String label, String title)
  {
	  MCRMetaLink link = createLink("linkFrom", href, label, title);
	  int i, n = linksFrom.size();
	  for (i = 0; i < n; ++i)
		  if (areIdentLocators((MCRMetaLink) linksFrom.elementAt(i), link))
	  {
		  linksFrom.removeElementAt(i);
		  return true;
	  }
	  return false;
  }

  /**
   * <em>addChild</em> appends a child link to another object if and only if
   * not already contained in the link vector, preventing from 
   * doubly-linked objects.
   * If the link could be added a "true" will be returned, otherwise "false".
   *
   * @param href                 the MCRObjectID string of the linked object
   * @param label                the link's label
   * @param title                the link's title
   * @return boolean             true, if operation successfully completed
   */
  final boolean addChild (String href, String label, String title)
  {
	  MCRMetaLink link = createLink("child", href, label, title);
	  int i, n = children.size();
	  for (i = 0; i < n; ++i)
		  if (areIdentLocators((MCRMetaLink) children.elementAt(i), link))
			  return false;
	  children.addElement(link);
	  return true;
  }

  /**
   * <em>removeChild</em> removes a child link to another object from the link vector.
   * If the link was found a "true" will be returned, otherwise "false".
   *
   * @param href                 the MCRObjectID string of the linked object
   * @param label                the link's label
   * @param title                the link's title
   * @return boolean             true, if operation successfully completed
   */
  final boolean removeChild (String href, String label, String title)
  {
	  MCRMetaLink link = createLink("child", href, label, title);
	  int i, n = children.size();
	  for (i = 0; i < n; ++i)
		  if (areIdentLocators((MCRMetaLink) children.elementAt(i), link))
	  {
		  children.removeElementAt(i);
		  return true;
	  }
	  return false;
  }

  /**
   * <em>addParent</em> appends a parent link to another object if and only if
   * not already contained in the link vector, preventing from 
   * doubly-linked objects.The corresponding metadata vector is appended to the
   * inherited_metadata vector.
   * If the link could be added a "true" will be returned, otherwise "false".
   *
   * @param href                 the MCRObjectID string of the linked object
   * @param label                the link's label
   * @param title                the link's title
   * @param inh_metadata         the Vector of the parent's heritable metadata
   * @return boolean             true, if operation successfully completed
   */
  final boolean addParent (String href, String label, String title,
						   Vector inh_metadata)
  {
	  MCRMetaLink link = createLink("parent", href, label, title);
	  int i, n = parents.size();
	  for (i = 0; i < n; ++i)
		  if (areIdentLocators((MCRMetaLink) parents.elementAt(i), link))
			  return false;
	  parents.addElement(link);
	  inherited_metadata.addElement(inh_metadata);
	  return true;
  }

  /**
   * <em>removeParent</em> removes a parent link to another object from the link
   * vector and the corresponding metadata vector from the inherited_metadata vector.
   * If the link was found a "true" will be returned, otherwise "false".
   *
   * @param href                 the MCRObjectID string of the linked object
   * @param label                the link's label
   * @param title                the link's title
   * @return boolean             true, if operation successfully completed
   */
  final boolean removeParent (String href, String label, String title)
  {
	  MCRMetaLink link = createLink("parent", href, label, title);
	  int i, n = parents.size();
	  for (i = 0; i < n; ++i)
		  if (areIdentLocators((MCRMetaLink) parents.elementAt(i), link))
	  {
		  parents.removeElementAt(i);
		  inherited_metadata.removeElementAt(i);
		  return true;
	  }
	  return false;
  }
  
  /**
   * <em>setInhMetaFromMCRObj</em> is used by the corresponding MCRObject
   * to set the inherited metadata vector collected from all parents.
   * 
   * @param inh_metadata        the inherited metadata vector
   */
  final void setInhMetaFromMCRObj (Vector inh_metadata)
  {
	  inherited_metadata = inh_metadata;
  }
  
  /**
   * <em>countLinksTo</em> returns the number of links to another object.
   * 
   * @return int               number of links to another object
   */
  public final int countLinksTo () { return linksTo.size(); }

  /**
   * <em>countLinksFrom</em> returns the number of links from another object.
   * 
   * @return int               number of links from another object
   */
  public final int countLinksFrom () { return linksFrom.size(); }

  /**
   * <em>countChildren</em> returns the number of child links.
   * 
   * @return int               number of children
   */
  public final int countChildren () { return children.size(); }

  /**
   * <em>countParents</em> returns the number of parent links.
   * 
   * @return int               number of parents
   */
  public final int countParents () { return parents.size(); }
  
  /**
   * <em>countInheritedMetadata</em> returns the number of items in the
   * inherited metadata vector, which are Vectors of MCRObjectMetadata's.
   * Note that this is not necessarily the number of parents (though
   * one MCRObjectMetadata Vector is inherited by each parent). In cases
   * of calling this routine after setFromDOM but prior to setInhMetaFromMCRObj
   * it will return zero, since the inherited metadata are NOT (doubly)
   * saved in the children's XML part !!!
   * 
   * @return int               number of metadata vectors
   */
  public final int countInheritedMetadata ()
  {
	  return inherited_metadata.size();
  }

  /**
   * <em>getLinkTo</em> returns the link to another object at a given index.
   * 
   * @param index              the index in the link vector
   * @return MCRMetaLink       the corresponding link
   */
  public final MCRMetaLink getLinkTo (int index)
  {
	  return (MCRMetaLink) linksTo.elementAt(index);
  }

  /**
   * <em>getLinkFrom</em> returns the link from another object at a given index.
   * 
   * @param index              the index in the link vector
   * @return MCRMetaLink       the corresponding link
   */
  public final MCRMetaLink getLinkFrom (int index)
  {
	  return (MCRMetaLink) linksFrom.elementAt(index);
  }

  /**
   * <em>getChild</em> returns the child link at a given index.
   * 
   * @param index              the index in the link vector
   * @return MCRMetaLink       the corresponding link
   */
  public final MCRMetaLink getChild (int index)
  {
	  return (MCRMetaLink) children.elementAt(index);
  }

  /**
   * <em>getParent</em> returns the parent link at a given index.
   * 
   * @param index              the index in the link vector
   * @return MCRMetaLink       the corresponding link
   */
  public final MCRMetaLink getParent (int index)
  {
	  return (MCRMetaLink) parents.elementAt(index);
  }

  /**
   * <em>getInheritedMetadata</em> returns the MCRObjectMetadata Vector
   * at a given index.
   * 
   * @param index              the index in the inherited metadata vector
   * @return Vector            the corresponding MCRObjectMetadata Vector
   */
  public final Vector getInheritedMetadata (int index)
  {
	  return (Vector) inherited_metadata.elementAt(index);
  }

  /**
   * <em>addLink</em> methode append the given link data to the
   * link vector.
   * If the link could be added a "true" will be returned, otherwise "false".
   *
   * @deprecated
   * @param add_link             the link to be added
   * @return boolean             true, if operation successfully completed
   */
  public final boolean addLink (MCRMetaLink add_link)
  {
    the_links.addElement(add_link);
    return true;
  }

  /**
   * <em>removeLink</em> the link from the link vector for 
   * the given number.
   *
   * @deprecated
   * @param rem_link             the link to be removed
   * @return boolean             true, if operation successfully completed
   */
  public final boolean removeLink (int number)
  {
    the_links.removeElementAt(number);
    return true;
  }

  /**
   * <em>addDerivate</em> methode append the given derivate link data to the
   * derivate vector.
   * If the link could be added a "true" will be returned, otherwise "false".
   *
   * @deprecated
   * @param add_derivate         the link to be added
   * @return boolean             true, if operation successfully completed
   */
  public final boolean addDerivate (MCRMetaLink add_derivate)
  {
    the_derivates.addElement(add_derivate);
    return true;
  }

  /**
   * <em>removeDerivate</em> the derivate link from the derivate vector for 
   * the given number.
   *
   * @deprecated
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
  private final void removeAll ()
  {
    linksTo.removeAllElements();
    linksFrom.removeAllElements();
    children.removeAllElements();
    parents.removeAllElements();
    inherited_metadata.removeAllElements();
	// deprecated:
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
    NodeList struct_links_list = null;
    struct_links_list = struct_element.getElementsByTagName("linksTo");
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
        linksTo.addElement(link);
        }
      }
    struct_links_list = struct_element.getElementsByTagName("linksFrom");
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
        linksFrom.addElement(link);
        }
      }
    struct_links_list = struct_element.getElementsByTagName("children");
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
        children.addElement(link);
        }
      }
    struct_links_list = struct_element.getElementsByTagName("parents");
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
        parents.addElement(link);
        }
      }
	// deprecated:
    struct_links_list = struct_element.getElementsByTagName("links");
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
    int i, n, m = the_derivates.size();
    StringBuffer sb = new StringBuffer(2048);
    sb.append("<structure>").append(NL);
	n = linksTo.size();
    sb.append("<linksTo>").append(NL);
    for (i = 0; i < n; ++i) {
      sb.append(((MCRMetaLink) linksTo.elementAt(i)).createXML()); }
    sb.append("</linksTo>").append(NL);
	n = linksFrom.size();
    sb.append("<linksFrom>").append(NL);
    for (i = 0; i < n; ++i) {
      sb.append(((MCRMetaLink) linksFrom.elementAt(i)).createXML()); }
    sb.append("</linksFrom>").append(NL);
	n = children.size();
    sb.append("<children>").append(NL);
    for (i = 0; i < n; ++i) {
      sb.append(((MCRMetaLink) children.elementAt(i)).createXML()); }
    sb.append("</children>").append(NL);
	n = parents.size();
    sb.append("<parents>").append(NL);
    for (i = 0; i < n; ++i) {
      sb.append(((MCRMetaLink) parents.elementAt(i)).createXML()); }
    sb.append("</parents>").append(NL);
	// deprecated:
	n = the_links.size();
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
 * This methode create a typed content list for all data in this instance.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the MCRObject data
 **/
public final MCRTypedContent createTypedContent() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  tc.addTagElement(tc.TYPE_MASTERTAG,"structure");
  tc.addTagElement(tc.TYPE_TAG,"linksTo");
  for (int i=0;i<linksTo.size();i++) {
    tc.addMCRTypedContent(((MCRMetaLink) linksTo.elementAt(i))
      .createTypedContent(true,true));
    }
  tc.addTagElement(tc.TYPE_TAG,"linksFrom");
  for (int i=0;i<linksFrom.size();i++) {
    tc.addMCRTypedContent(((MCRMetaLink) linksFrom.elementAt(i))
      .createTypedContent(true,true));
    }
  tc.addTagElement(tc.TYPE_TAG,"children");
  for (int i=0;i<children.size();i++) {
    tc.addMCRTypedContent(((MCRMetaLink) children.elementAt(i))
      .createTypedContent(true,true));
    }
  tc.addTagElement(tc.TYPE_TAG,"parents");
  for (int i=0;i<parents.size();i++) {
    tc.addMCRTypedContent(((MCRMetaLink) parents.elementAt(i))
      .createTypedContent(true,true));
    }
  //append the inherited metadata here, to be done next time:
  //#####
  // deprecated:
  tc.addTagElement(tc.TYPE_TAG,"links");
  for (int i=0;i<the_links.size();i++) {
    tc.addMCRTypedContent(((MCRMetaLink) the_links.elementAt(i))
      .createTypedContent(true,true));
    }
  tc.addTagElement(tc.TYPE_TAG,"derivates");
  for (int i=0;i<the_derivates.size();i++) {
    tc.addMCRTypedContent(((MCRMetaLink) the_derivates.elementAt(i))
      .createTypedContent(true,true));
    }
  return tc;
  }

  /**
   * <em>isValid</em> checks whether all of the MCRMetaLink's in the
   * link vector are valid or not.
   *
   * @return boolean             true, if structure is valid
   */
  public final boolean isValid ()
  {
    for (int i = 0; i < linksTo.size(); ++i) {
      if (! ((MCRMetaLink) linksTo.elementAt(i)).isValid())
        return false;
      }
    for (int i = 0; i < linksFrom.size(); ++i) {
      if (! ((MCRMetaLink) linksFrom.elementAt(i)).isValid())
        return false;
      }
    for (int i = 0; i < children.size(); ++i) {
      if (! ((MCRMetaLink) children.elementAt(i)).isValid())
        return false;
      }
    for (int i = 0; i < parents.size(); ++i) {
      if (! ((MCRMetaLink) parents.elementAt(i)).isValid())
        return false;
      }
	// deprecated:
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
    int i, n, m = the_derivates.size();
    MCRMetaLink link = null;
	n = linksTo.size();
    System.out.println("The outer structure contains " + n + " linksTo :");
    for (i = 0; i < n; ++i)
    {
      link = (MCRMetaLink) linksTo.elementAt(i);
      System.out.println("-->"+i+"<--");
      link.debug();
    }
	n = linksFrom.size();
    System.out.println("The outer structure contains " + n + " linksFrom :");
    for (i = 0; i < n; ++i)
    {
      link = (MCRMetaLink) linksFrom.elementAt(i);
      System.out.println("-->"+i+"<--");
      link.debug();
    }
	n = children.size();
    System.out.println("The outer structure contains " + n + " children :");
    for (i = 0; i < n; ++i)
    {
      link = (MCRMetaLink) children.elementAt(i);
      System.out.println("-->"+i+"<--");
      link.debug();
    }
	n = parents.size();
    System.out.println("The outer structure contains " + n + " parents :");
    for (i = 0; i < n; ++i)
    {
      link = (MCRMetaLink) parents.elementAt(i);
      System.out.println("-->"+i+"<--");
      link.debug();
	  //append the inherited metadata here, to be done next time:
	  //#####
    }
	// deprecated:
	n = the_links.size();
    System.out.println("The outer structure contains " + n + " links :");
    for (i = 0; i < n; ++i)
    {
      link = (MCRMetaLink) the_links.elementAt(i);
      System.out.println("-->"+i+"<--");
      link.debug();
    }
    System.out.println("The outer structure contains " + m + " derivates :");
    for (i = 0; i < m; ++i)
    {
      link = (MCRMetaLink) the_derivates.elementAt(i);
      System.out.println("-->"+i+"<--");
      link.debug();
    }
    System.out.println("The end of the document's structure data is reached.");
  }
}

