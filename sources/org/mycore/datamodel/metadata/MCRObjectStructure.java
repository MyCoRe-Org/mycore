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
import java.util.Vector;
import org.mycore.common.MCRException;

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
  private Vector children = null;
  private MCRMetaLink parent = null;
  private Vector inherited_metadata = null;
  private ArrayList derivates = null;

  /**
   * The constructor initializes NL (non-static, in order to enable
   * different NL's for different objects) and the link vectors
   * the elements of which are MCRMetaLink's.
   */
  public MCRObjectStructure ()
  {
    NL = System.getProperties().getProperty("line.separator");
    children = new Vector ();
    derivates = new ArrayList ();
  }
  
  /**
   * <em>createLink</em> creates an MCRMetaLink with given subtag name,
   * href, label and title.
   * 
   * @param subtag                  subtag name
   * @param href                    ID string of the linked object
   * @param label                   the link's label
   * @param title                   the link's title
   * @return MCRMetaLink            the xlink ("locator" type)
   */
  private static MCRMetaLink createLink (String subtag, String href,
    String label, String title)
    {
    String lang = "en";
    MCRMetaLink link = null;
    try {
      link = new MCRMetaLink ("structure", subtag, lang);
      link.setReference(href, label, title);
      }
     catch (MCRException exc) { ; } // never thrown
     return link;
     }

  /**
   * <em>addChild</em> appends a child link to another object
   * if and only if it is not already contained in the link vector,
   * preventing from doubly-linked objects.
   * If the link could be added a "true" will be returned,
   * otherwise "false".
   *
   * @param href                 the MCRObjectID string of the child
   * @param label                the link's label
   * @param title                the link's title
   * @return boolean             true, if successfully done
   */
  final boolean addChild (String href, String label, String title)
    {
    MCRMetaLink link = createLink("child", href, label, title);
    int i, n = children.size();
    for (i = 0; i < n; ++i) {
      if (((MCRMetaLink) children.elementAt(i)).getXLinkHref().equals(href)) {
        return false; } }
    children.addElement(link);
    return true;
    }

  /**
   * <em>removeChild</em> removes a child link to another object
   * from the link vector. If the link was found a "true" will be
   * returned, otherwise "false".
   *
   * @param href                 the MCRObjectID string of the child
   * @return boolean             true, if successfully completed
   */
  final boolean removeChild (String href)
    {
    int i, n = children.size();
    for (i = 0; i < n; ++i) {
      if (((MCRMetaLink) children.elementAt(i)).getXLinkHref().equals(href)) {
        children.removeElementAt(i);
        return true; 
        }
      }
    return false;
    }

  /**
   * <em>setParent</em> sets the parent link of this object as well
   * as the parent's, grand parent's, a.s.o. heritable metadata
   * MCRObjectMetadata Vector.
   * If the object already has a parent, an exception is thrown
   * (multiple inheritance request).
   *
   * @param href                 the MCRObjectID string of the parent
   * @param label                the link's label
   * @param title                the link's title
   * @param inh_metadata         the parent's heritable metadata
   * @return boolean             true, if successfully completed
   * @exception MCRException     thrown for multiple inheritance
   */
  final boolean setParent (String href, String label, String title,
						   Vector inh_metadata)
    throws MCRException
  {
    if (parent != null)
      throw new MCRException("multiple inheritance request");
    parent = createLink("parent", href, label, title);
    setInheritedMetadata(inh_metadata);
    return true;
  }

  /**
   * <em>removeParent</em> removes the parent link from this object 
   * and the corresponding metadata vector from the inherited_metadata
   * vector.
   * If the link was found a "true" will be returned, otherwise "false".
   *
   * @param href                 the MCRObjectID string of the parent
   * @return boolean             true, if successfully completed
   */
  final boolean removeParent (String href)
    {
    if (parent == null) { return false; }
    if (! parent.getXLinkHref().equals(href)) { return false; }
    parent = null;
    setInheritedMetadata(null);
    return true;
    }
  
  /**
   * <em>setInheritedMetadata</em> is used to set the inherited metadata
   * vector collected from parent, grand parent, and so on.
   * 
   * @param inh_metadata        the inherited metadata vector
   */
  final void setInheritedMetadata (Vector inh_metadata)
  {
    if (inherited_metadata != null)
      inherited_metadata.removeAllElements();
    inherited_metadata = null;
    if (inh_metadata != null)
    {
      inherited_metadata = new Vector();
      for (int i = 0; i < inh_metadata.size(); ++i)
        inherited_metadata.addElement((MCRObjectMetadata) inh_metadata.
										elementAt(i));
    }
  }

  /**
   * <em>getInheritedMetadata</em> returns the inherited metadata vector
   * (null if no parent specified).
   *
   * @return                   Vector of inherited metadata
   */
  final Vector getInheritedMetadata ()
  {
    if (parent == null) return null;
    if (inherited_metadata == null) collectInheritedMetadata();
    Vector meta = new Vector ();
    for (int i = 0; i < inherited_metadata.size(); ++i)
      meta.addElement((MCRObjectMetadata) inherited_metadata.
						elementAt(i));
    return meta;
  }

  /**
   * <em>collectInheritedMetadata</em> collects the metadata inherited
   * from parent, grand parent, a.s.o.
   */
  private final void collectInheritedMetadata ()
  {
    try
    {
      inherited_metadata = new Vector ();
      MCRMetaLink prt = parent;
      MCRObject obj = null;
      MCRObjectMetadata meta = null;
      MCRObjectStructure stru = null;
      while (prt != null)
      {
        obj = new MCRObject ();
        obj.receiveFromDatastore(prt.getXLinkHref());
        meta = obj.getMetadata();
        stru = obj.getStructure();
        inherited_metadata.addElement(meta.getHeritableMetadata());
        prt = stru.getParent();
      }
    }
    catch (Exception exc) { ; }
  }
  
  /**
   * <em>countChildren</em> returns the number of child links.
   * 
   * @return int               number of children
   */
  public final int countChildren () { return children.size(); }

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
   * <em>getParent</em> returns the parent link.
   * 
   * @return MCRMetaLink       the corresponding link
   */
  public final MCRMetaLink getParent ()
    { return parent; }

  /**
   * <em>removeAll</em> removes all links from the link vectors.
   */
  private final void removeAllHeritables ()
  {
    children.removeAllElements();
    parent = null;
    setInheritedMetadata(null);
  }

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
   * <em>removeAllDerivates</em> removes all links from the derivate array.
   */
  private final void removeAllDerivates ()
    { derivates.clear(); }

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
    removeAllHeritables();
    removeAllDerivates();
    org.jdom.Element struct_element = element.getChild("children");
    if (struct_element != null) {
      List struct_links_list = struct_element.getChildren();
      for (int i=0;i<struct_links_list.size();i++) {  
        org.jdom.Element link_element = 
          (org.jdom.Element)struct_links_list.get(i);
        MCRMetaLink link = new MCRMetaLink();
        link.setDataPart("structure");
        link.setFromDOM(link_element);
        children.addElement(link);
        }
      }
    struct_element = element.getChild("parents");
    if (struct_element != null) {
      List struct_links_list = struct_element.getChildren();
      for (int i=0;i<struct_links_list.size();i++) {  
        org.jdom.Element link_element = 
          (org.jdom.Element)struct_links_list.get(i);
        parent = new MCRMetaLink();
        parent.setDataPart("structure");
        parent.setFromDOM(link_element);
        }
      }
    // Structure derivate part
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
      debug(); throw new MCRException("The content is not valid."); }
    int i;
    org.jdom.Element elm = new org.jdom.Element("structure");
    if (children.size() > 0) {
      org.jdom.Element elmm = new org.jdom.Element("children");
      for (i = 0; i < children.size(); ++i){
        elmm.addContent(((MCRMetaLink) children.elementAt(i)). createXML()); }
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
      debug(); throw new MCRException("The content is not valid."); }
    MCRTypedContent tc = new MCRTypedContent();
    tc.addTagElement(tc.TYPE_MASTERTAG,"structure");
/*
    if (children.size() > 0) {
      tc.addTagElement(tc.TYPE_TAG,"children");
      for (int i=0;i<children.size();i++)
        tc.addMCRTypedContent(((MCRMetaLink) children.elementAt(i))
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
	  inherited_metadata.elementAt(i);
        for (int j = 0; j < meta.size(); ++j)
          tc.addMCRTypedContent(meta.getMetadataElement(meta.tagName(j))
            .createTypedContent());
        }
      }
*/
    // add the derivates for the parametric searchable
    if (derivates.size() > 0) {
      tc.addTagElement(tc.TYPE_TAG,"derobjects");
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
      if (! ((MCRMetaLink) children.elementAt(i)).isValid())
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

  /**
   * <em>debug</em> prints all information about the structure of the
   * document (contained in the link vectors).
   */
  public final void debug ()
    {
    System.out.println("MCRObjectStructure debug start");
    int i, n;
    MCRMetaLink link = null;
    n = children.size();
    System.out.println("The structure contains "+n+" children :");
    for (i = 0; i < n; ++i) {
      link = (MCRMetaLink) children.elementAt(i);
      link.debug(); }
    n = 0;
    if (parent != null) n = 1;
    System.out.println("The structure contains "+n+" parents :");
    if (parent != null) {
      parent.debug();
      if (inherited_metadata == null)
        collectInheritedMetadata();
      n = inherited_metadata.size();
      System.out.println("The object inherits metadata from " + n +
						 " forefather(s) :");
      for (i = 0; i < n; ++i) {
        System.out.println("-->" + i + "<--");
        MCRObjectMetadata meta = (MCRObjectMetadata)
	  inherited_metadata.elementAt(i);
        int j, m = meta.size();
        System.out.println("From this forefather the object inherits " +
          m + " metadata :");
        for (j = 0; j < m; ++j) {
          System.out.println("->" + j + "<-");
          System.out.println(meta.tagName(j) + " :");
          meta.getMetadataElement(meta.tagName(j)).debug();
        }
      }
    }
    n = derivates.size();
    System.out.println("The structure contains "+n+" derobjects :");
    for (i = 0; i < n; ++i) {
      link = (MCRMetaLink) derivates.get(i);
      link.debug(); }
    System.out.println("MCRObjectStructure debug end"+NL);
    }
  }

