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

package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.mycore.common.MCRException;
import org.mycore.common.MCRConfiguration;

/**
 * This class implements code for the inheritance of metadata of linked
 * objects and the linking of derivates onto an MCRObject. These links
 * are described by the <em>MCRMetaLink</em> class. For links to another
 * object, there are "locators" in use only, and the href variable gives
 * the ID of the linked object, while the label and title attributes can
 * be used freely.
 * Subtag name = "<child>" means a child link from a "parent" object
 * (collected in the "children" and "parents" section of the "structure"
 * part, respectively). The child inherits all heritable metadata of the
 * parent. If the parent itself is a child of another parent, the
 * heritable metadata of this "grand parent" is inherited by the child
 * as well. This mechanism recursively traces the full inheritance
 * hierarchy. So if the grand parent itself has a parent, this grand
 * parent parent's heritable metadata will be inherited and so on.
 * Note, that it is impossible to inherit metadata from multiple parents.
 * In cases of multiple inheritance request, an exception is thrown.
 * A child link cannot occur twice from the same object to the same href
 * (preventing from doubled links).
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
  private MCRMetaLinkID parent = null;
  private ArrayList children = null;
  private ArrayList inherited_metadata = null;
  private ArrayList derivates = null;
  private Logger logger = null;

  /**
   * The constructor initializes NL (non-static, in order to enable
   * different NL's for different objects) and the link vectors
   * the elements of which are MCRMetaLink's.
   */
  public MCRObjectStructure (Logger log)
    {
    NL = System.getProperties().getProperty("line.separator");
    children = new ArrayList ();
    derivates = new ArrayList ();
    logger = log;
    }
  
  /**
   * This method clean the data lists parent, children and derivates of this
   * class.
   **/
  final void clear()
    { parent = null; children.clear(); derivates.clear(); }

  /**
   * The method returns the parent link.
   * 
   * @return MCRMetaLinkID       the corresponding link
   */
  final MCRMetaLinkID getParent ()
    { return parent; }

  /**
   * The method return the parent reference as a MCRObjectID.
   *
   * @return the parent MCRObjectID.
   **/
  final MCRObjectID getParentID()
    { 
    if (parent != null) { return parent.getXLinkHrefID(); }
    else { return null; }
    }

  /**
   * This method set the parent value from a given MCRMetaLinkID.
   *
   * @param in_parent the MCRMetaLinkID to set
   **/
  final void setParent(MCRMetaLinkID in_parent)
    { parent = in_parent; }

  /**
   * The method appends a child ID to the child link list
   * if and only if it is not already contained in the list,
   * preventing from doubly-linked objects.
   * If the link could be added a "true" will be returned,
   * otherwise "false".
   *
   * @param child                the MCRMetaLinkID  of the child
   * @return boolean             true, if successfully done
   */
  final boolean addChild (MCRMetaLinkID child)
    {
    int i, n = children.size();
    for (i = 0; i < n; ++i) {
      if (((MCRMetaLinkID) children.get(i)).getXLinkHref()
        .equals(child.getXLinkHref())) { return false; }
      }
    children.add(child);
    return true;
    }

  /**
   * The method appends a child ID to the child link list
   * if and only if it is not already contained in the list,
   * preventing from doubly-linked objects.
   * If the link could be added a "true" will be returned,
   * otherwise "false".
   *
   * @param href                 the MCRObjectID string of the child
   * @param label                the link's label
   * @param title                the link's title
   * @return boolean             true, if successfully done
   */
  final boolean addChild (MCRObjectID href, String label, String title)
    {
    MCRConfiguration mcr_conf = MCRConfiguration.instance();
    String lang = mcr_conf.getString("MCR.metadata_default_lang");
    MCRMetaLinkID link = new MCRMetaLinkID ("structure","child",lang,false);
    link.setReference(href, label, title);
    int i, n = children.size();
    for (i = 0; i < n; ++i) {
      if (((MCRMetaLinkID) children.get(i)).getXLinkHref().equals(href)) {
        return false; } }
    children.add(link);
    return true;
    }

  /**
   * <em>removeChild</em> removes a child link to another object
   * from the link vector. If the link was found a "true" will be
   * returned, otherwise "false".
   *
   * @param href                 the MCRObjectID of the child
   * @return boolean             true, if successfully completed
   */
  final boolean removeChild (MCRObjectID href)
    {
    logger.debug("Remove child ID "+href.getId());
    int i, n = children.size();
    for (i = 0; i < n; ++i) {
      if (((MCRMetaLinkID) children.get(i)).getXLinkHrefID().equals(href)) {
        children.remove(i); return true; 
        }
      }
    return false;
    }
  
  /**
   * The method returns the number of child links.
   * 
   * @return int               number of children
   */
  public final int getChildSize () 
    { return children.size(); }

  /**
   * The method returns the child link at a given index.
   * 
   * @param index              the index in the link vector
   * @return MCRMetaLink       the corresponding link
   */
  public final MCRMetaLinkID getChild (int index)
    { return (MCRMetaLinkID) children.get(index); }

  /**
   * The method return the child reference as a MCRObjectID.
   *
   * @return the child MCRObjectID.
   **/
  final MCRObjectID getChildID(int index)
    { return ((MCRMetaLinkID)children.get(index)).getXLinkHrefID(); }

  /**
   * <em>addDerivate</em> methode append the given derivate link data
   * to the derivate vector.
   * If the link could be added a "true" will be returned, otherwise
   * "false".
   *
   * @param add_derivate         the link to be added as MCRMetaLinkID
   * @return boolean             true, if successfully completed
   */
  public final void addDerivate(MCRMetaLinkID add_derivate)
    { derivates.add(add_derivate); }

  /**
   * The method return the size of the derivate array.
   *
   * @return the size of the derivate array
   **/
  public final int getDerivateSize()
    { return derivates.size(); }

  /**
   * The method return the derivate form the array with the given index.
   *
   * @param index the index of the list
   * @return the derivate as MCRMetaLinkID or null
   **/
  public final MCRMetaLinkID getDerivate(int index)
    throws IndexOutOfBoundsException
    {
    if ((index<0)||(index>derivates.size())) {
      throw new IndexOutOfBoundsException("Index error in getDerivate()."); }
    return (MCRMetaLinkID) derivates.get(index);
    }

  /**
   * <em>searchForDerivate</em> returns the index of the derivate array
   * if the comparsion of the MCRMetaLinkID input with an item of the
   * derivate array is true.
   *
   * @param in_derivate the MCRMetaLinkID input
   * @return the index of the derivate in the array or -1 if the link
   *   was not found.
   **/
  public final int searchForDerivate(MCRMetaLinkID input)
    {
    int r = -1;
    for (int i=0;i<derivates.size();i++) {
      if (((MCRMetaLinkID)derivates.get(i)).compare(input)) { r = i; break; } }
    return r;
    }

  /**
   * <em>removeDerivate</em> the derivate link from the derivate vector
   * for the given number.
   *
   * @param index                the index of the link to be removed
   * @exception IndexOutOfBoundsException throw this exception, if
   *                             the index is false
   * @return boolean             true, if successfully completed
   */
  public final void removeDerivate (int index)
    throws IndexOutOfBoundsException
    {
    if ((index<0)||(index>derivates.size())) {
      throw new IndexOutOfBoundsException("Index error in removeDerivate()."); }
    derivates.remove(index);
    }

  /**
   * While the preceding methods dealt with the structure's copy in memory only,
   * the following three will affect the operations to or from datastore too.
   * Thereby <em>setFromDOM</em> will read the structure data from an XML
   * input stream (the "structure" entry).
   *
   * @param element the structure node list
   */
  public final void setFromDOM (org.jdom.Element element)
    {
    children.clear();
    org.jdom.Element struct_element = element.getChild("children");
    if (struct_element != null) {
      List struct_links_list = struct_element.getChildren();
      for (int i=0;i<struct_links_list.size();i++) {  
        org.jdom.Element link_element = 
          (org.jdom.Element)struct_links_list.get(i);
        MCRMetaLinkID link = new MCRMetaLinkID();
        link.setDataPart("structure");
        link.setFromDOM(link_element);
        children.add(link);
        }
      }
    // Stricture parent part
    parent = null;
    struct_element = element.getChild("parents");
    if (struct_element != null) {
      List struct_links_list = struct_element.getChildren();
      for (int i=0;i<struct_links_list.size();i++) {  
        org.jdom.Element link_element = 
          (org.jdom.Element)struct_links_list.get(i);
        parent = new MCRMetaLinkID();
        parent.setDataPart("structure");
        parent.setFromDOM(link_element);
        }
      }
    // Structure derivate part
    derivates.clear();
    struct_element = element.getChild("derobjects");
    if (struct_element != null) {
      List struct_links_list = struct_element.getChildren();
      for (int i=0;i<struct_links_list.size();i++) {  
        org.jdom.Element der_element = 
          (org.jdom.Element)struct_links_list.get(i);
        MCRMetaLinkID der = new MCRMetaLinkID();
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
   * @return org.jdom.Element    the structure XML string
   */
  public final org.jdom.Element createXML () throws MCRException
    {
    if (!isValid()) {
      throw new MCRException("The content is not valid."); }
    int i;
    org.jdom.Element elm = new org.jdom.Element("structure");
    if (children.size() > 0) {
      org.jdom.Element elmm = new org.jdom.Element("children");
      for (i = 0; i < children.size(); ++i){
        elmm.addContent(((MCRMetaLink) children.get(i)). createXML()); }
      elm.addContent(elmm); }
    if (parent != null) {
      org.jdom.Element elmm = new org.jdom.Element("parents");
      elmm.addContent(parent.createXML());
      elm.addContent(elmm); }
    if (derivates.size() > 0) {
      org.jdom.Element elmm = new org.jdom.Element("derobjects");
      for (i = 0; i < derivates.size(); ++i) {
        elmm.addContent(((MCRMetaLink) derivates.get(i))
          .createXML()); }
      elm.addContent(elmm);
      }
    return elm;
    }

  /**
   * This methode create a typed content list for all data in this
   * instance.
   *
   * @exception MCRException if the content of this class is not valid
   * @return a MCRTypedContent with the data of the MCRObject data
   **/
  public final MCRTypedContent createTypedContent() throws MCRException
    {
    if (!isValid()) {
      throw new MCRException("The content is not valid."); }
    MCRTypedContent tc = new MCRTypedContent();
    tc.addTagElement(MCRTypedContent.TYPE_MASTERTAG,"structure");
/*
    if (children.size() > 0) {
      tc.addTagElement(tc.TYPE_TAG,"children");
      for (int i=0;i<children.size();i++)
        tc.addMCRTypedContent(((MCRMetaLink) children.get(i))
          .createTypedContent(true));
      }
    if (parent != null) {
      tc.addTagElement(tc.TYPE_TAG,"parents");
      tc.addMCRTypedContent(parent
        .createTypedContent(true));
      if (inherited_metadata == null)
        collectInheritedMetadata();
      tc.addTagElement(tc.TYPE_TAG, "parents_metadata");
      for (int i = 0; i < inherited_metadata.size(); ++i) {
        MCRObjectMetadata meta = (MCRObjectMetadata)
	  inherited_metadata.get(i);
        for (int j = 0; j < meta.size(); ++j)
          tc.addMCRTypedContent(meta.getMetadataElement(meta.tagName(j))
            .createTypedContent());
        }
      }
*/
    // add the derivates for the parametric searchable
    if (derivates.size() > 0) {
      tc.addTagElement(MCRTypedContent.TYPE_TAG,"derobjects");
      for (int i=0;i<derivates.size();i++)
        tc.addMCRTypedContent(((MCRMetaLink) derivates.get(i))
          .createTypedContent(true));
      }
    return tc;
    }

  /**
   * <em>isValid</em> checks whether all of the MCRMetaLink's in the
   * link vectors are valid or not.
   *
   * @return boolean             true, if structure is valid
   */
  public final boolean isValid ()
    {
    for (int i = 0; i < children.size(); ++i) {
      if (! ((MCRMetaLink) children.get(i)).isValid())
        return false;
      }
    if (parent != null) {
      if (! parent.isValid())
        return false;
      }
    for (int i = 0; i < derivates.size(); ++i) {
      if (! ((MCRMetaLinkID) derivates.get(i)).isValid()) {
        return false; }
      if (!((MCRMetaLinkID)derivates.get(i)).getXLinkType().equals("locator")) {
        return false; }
      if (!((MCRMetaLinkID)derivates.get(i)).getXLinkHrefID()
        .getTypeId().toLowerCase().equals("derivate")) {
        return false; }
      }
    return true;
    }

  }

