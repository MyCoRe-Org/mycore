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
import mycore.common.MCRUtils;

/**
 * This class is designed to to have a basic class for all metadata.
 * The class has inside a ArrayList that holds all metaddata elements
 * for one XML tag.
 * Furthermore, this class supports the linking of a document owing this
 * metadata element to another document, the id of which is given in the
 * xlink:href attribute of the MCRMetaLink representing the link. The
 * class name of such a metadata element must be MCRMetaLink, and the
 * metadata element is considered to be a folder of links.
 *
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @version $Revision$ $Date$
 **/
public class MCRMetaElement
{

// common data
private static String NL =
  new String((System.getProperties()).getProperty("line.separator"));
private static String DEFAULT_LANGUAGE = "de";
private String META_PACKAGE_NAME = "mycore.datamodel.";
private static String LINK_CLASS_NAME = "MCRMetaLink";

// MetaElement data
private String lang = null;
private String classname = null;
private String tag = null;
private boolean heritable;
private ArrayList list = null;

/**
 * This is the constructor of the MCRMetaElement class. 
 * The default language for the element was set to <b>en</b>.
 **/
public MCRMetaElement()
  {
  lang = DEFAULT_LANGUAGE; 
  classname = "";
  tag = "";
  heritable = false;
  list = new ArrayList();
  }

/**
 * This is the constructor of the MCRMetaElement class. 
 * The default language for the element was set. If the default languge
 * is empty or false <b>de</b> was set.
 *
 * @param default_lang     the default language
 **/
public MCRMetaElement(String default_lang)
  {
  if ((default_lang == null) || 
    ((default_lang = default_lang.trim()).length() ==0)) {
    lang = DEFAULT_LANGUAGE; }
  else {
    lang = default_lang; }
  classname = "";
  tag = "";
  heritable = false;
  list = new ArrayList();
  }

/**
 * This methode return the name of this metadata class as string.
 *
 * @return the name of this metadata class as string
 **/
public final String getClassName()
  { return classname; }

/**
 * This methode return the instance of an element from the list with index i.
 *
 * @return the instance of an element, if index is out of range return null
 **/
public final MCRMetaInterface getElement(int index)
  { 
  if ((index<0) || (index>list.size())) { return null; }
  return (MCRMetaInterface)list.get(index); 
  }

/**
 * This methode return the heritable of this metadata as boolean value.
 *
 * @return the heritable of this metadata class
 **/
public final boolean getHeritable()
  { return heritable; }

/**
 * This methode return the default language of this metadata class as string.
 *
 * @return the default language of this metadata class as string
 **/
public final String getLang()
  { return lang; }

/**
 * This methode return the tag of this metadata class as string.
 *
 * @return the tag of this metadata class as string
 **/
public final String getTag()
  { return tag; }

/**
 * This methode set the heritable for the metadata class.
 *
 * @param heritable            the heritable as boolean value
 */
public void setHeritable(boolean heritable)
  {
  this.heritable = false;
  if (heritable) { this.heritable = heritable; return; }
  }

/**
 * This methode set the heritable for the metadata class.
 *
 * @param heritable            the heritable as string
 */
public void setHeritable(String heritable)
  {
  this.heritable = false;
  if ((heritable == null) || ((heritable = heritable.trim()).length() ==0))
    { return; }
  if (heritable.equals("true")) { this.heritable = true; }
  }

/**
 * This methode set the tag for the metadata class.
 *
 * @param tag                   the tag for the metadata class
 */
public void setTag(String tag)
  {
  if ((tag == null) || ((tag = tag.trim()).length() ==0)) { return; }
  this.tag = tag;
  }

/**
 * This methode set the element class name for the metadata elements.
 *
 * @param classname             the class name for the metadata elements
 */
public void setClassName(String classname)
  {
  if ((classname == null) || ((classname = classname.trim()).length() ==0)) {
    return; }
  this.classname = classname;
  }

/**
 * <em>size</em> returns the number of elements in this instance.
 * 
 * @return int                  the size of "list"
 */
public int size ()
{
	return list.size();
}

/**
 * <em>isLinkFolder</em> checks whether the class name is MCRMetaLink or not.
 * 
 * @return boolean              true, if the elements of this instance are links
 */
public final boolean isLinkFolder ()
{
	if (classname == null) return false;
	return classname.trim().equals(LINK_CLASS_NAME);
}

/**
 * <em>addLink</em> adds an MCRMetaLink to the list if and only if the
 * classname of this MCRMetaElement is MCRMetaLink and the link to be added
 * is of type "locator".
 * 
 * @param link                  an MCRMetaLink to be added
 * @return boolean              true, if successfully completed
 */
public final boolean addLink (MCRMetaLink link)
{
	if (size() == 0) setClassName(LINK_CLASS_NAME);
	if (! isLinkFolder()) return false;
	if (! link.getXLinkType().equals("locator")) return false;
	return list.add(link);
}

/**
 * <em>indexOfLink</em> searches for a link with given href (and optionally
 * with given subtag and/or title) in the list starting from arbitrary position
 * and returns the index of its first occurrence, or -1 if not found.
 * 
 * @param str_href              the link's destination id
 * @param str_subtag            the subtag name to search for (or null)
 * @param str_title             the title to search for (or null)
 * @param start                 the start position
 * @return int                  the position of the link's first occurrence
 */
public final int indexOfLink (String str_href, String str_subtag, String str_title,
								int start)
{
	int i, k, n;
	MCRMetaLink link = null;
	for (i = start, k = -1, n = size(); i < n; ++i)
	{
		link = (MCRMetaLink) list.get(i);
		if (! link.getXLinkHrefToString().equals(str_href)) continue;
		if (str_subtag != null)
			if (! link.getSubTag().equals(str_subtag)) continue;
		if (str_title != null)
			if (! link.getXLinkTitle().equals(str_title)) continue;
		k = i;
		break;
	}
	return k;
}

/**
 * <em>indexOfLink</em> searches for a link with given href (and optionally
 * with given subtag and/or title) in the list and returns the index of its
 * first occurrence, or -1 if not found.
 * 
 * @param str_href              the link's destination id
 * @param str_subtag            the subtag name to search for (or null)
 * @param str_title             the title to search for (or null)
 * @return int                  the position of the link's first occurrence
 */
public final int indexOfLink (String str_href, String str_subtag, String str_title)
{
	return indexOfLink(str_href, str_subtag, str_title, 0);
}

/**
 * <em>removeLink</em> removes an MCRMetaLink from the list if and only if the
 * classname of this MCRMetaElement is MCRMetaLink.
 * 
 * @param index                 the index of the link to be removed
 * @return boolean              true, if successfully completed
 */
public final boolean removeLink (int index)
{
	if (! isLinkFolder()) return false;
	if (index < 0 || index >= list.size()) return false;
	list.remove(index);
	return true;
}

/**
 * <em>removeLink</em> searches for a link with given href (and optionally
 * with given subtag and/or title) in the list and removes it if found
 * returning true. If not found it returns false.
 * 
 * @param str_href              the link's destination id
 * @param str_subtag            the subtag name to search for (or null)
 * @param str_title             the title to search for (or null)
 * @return boolean              true, if successfull removed
 */
public final boolean removeLink (String str_href, String str_subtag, String str_title)
{
	return removeLink(indexOfLink(str_href, str_subtag, str_title));
}

/**
 * <em>removeAllLinks</em> removes all links from the list if and only if the
 * classname of this MCRMetaElement is MCRMetaLink.
 * 
 * @return boolean              true, if successfully completed
 */
public final boolean removeAllLinks ()
{
	if (size() == 0) return true;
	if (! isLinkFolder()) return false;
	list.clear();
	return true;
}

/**
 * This methode read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param element a relevant JDOM element for the metadata
 * @exception MCRException if the class can't loaded
 **/
public final void setFromDOM(org.jdom.Element element) throws MCRException
  {
  tag = element.getName();
  classname = element.getAttributeValue("class");
  String fullname = META_PACKAGE_NAME+classname;
  setHeritable(element.getAttributeValue("heritable"));
  List element_list = element.getChildren();
  int len = element_list.size();
  for (int i=0;i<len;i++) {
    org.jdom.Element subtag = (org.jdom.Element)element_list.get(i);
    Object obj = new Object();
    try {
      obj = Class.forName(fullname).newInstance();
      ((MCRMetaInterface)obj).setLang(lang);
      ((MCRMetaInterface)obj).setFromDOM(subtag);
      }
    catch (ClassNotFoundException e) {
      throw new MCRException(classname+" ClassNotFoundException"); }
    catch (IllegalAccessException e) {
      throw new MCRException(classname+" IllegalAccessException"); }
    catch (InstantiationException e) {
      throw new MCRException(classname+" InstantiationException"); }
    list.add(obj);
    }
  }

/**
 * This methode create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRLangText definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a JDOM Element with the XML Element part
 **/
public final org.jdom.Element createXML() throws MCRException
  {
  if (!isValid()) {
    throw new MCRException("MCRMetaElement : The content is not valid."); }
  org.jdom.Element elm = new org.jdom.Element(tag);
  elm.setAttribute("class",classname);
  elm.setAttribute("heritable",new Boolean(heritable).toString());
  for (int i=0;i<list.size();i++) {
    elm.addContent(((MCRMetaInterface)list.get(i)).createXML()); }
  return elm;
  }

/**
 * This methode create a typed content list for all data in this instance.
 *
 * @param parametric true if the data should parametric searchable
 * @param textsearch true if the data should text searchable
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the MCRObject data
 **/
public final MCRTypedContent createTypedContent() throws MCRException
  {
  if (!isValid()) {
    throw new MCRException("MCRMetaElement : The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  tc.addTagElement(tc.TYPE_TAG,tag);
  tc.addStringElement(tc.TYPE_ATTRIBUTE,"class",classname,true,false);
  for (int i=0;i<list.size();i++) {
    tc.addMCRTypedContent(((MCRMetaInterface)list.get(i))
      .createTypedContent(true,true)); }
  return tc;
  }

/**
 * This methode check the validation of the content of this class.
 * The methode returns <em>true</em> if
 * <ul>
 * <li> the classname is not null or empty
 * <li> the tag is not null or empty
 * <li> if the list is empty
 * <li> the lang value was supported
 * </ul>
 * otherwise the methode return <em>false</em>
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if ((classname == null) || ((classname = classname.trim()).length() ==0)) {
    return false; }
  if (list.size() == 0) { return false; }
  if ((tag == null) || ((tag = tag.trim()).length() ==0)) {
    return false; }
  if (!MCRUtils.isSupportedLang(lang)) { return false; }
  return true;
  }

/**
 * This methode print all elements of the metadata class.
 **/
public final void debug()
  {
  System.out.println("MCRMetaElement debug start");
  System.out.println("<classname>"+classname+"</classname>");
  System.out.println("<lang>"+lang+"</lang>");
  System.out.println("<tag>"+tag+"</tag>");
  System.out.println("<heritable>"+heritable+"</heritable>");
  for (int i=0;i<list.size();i++) {
    ((MCRMetaInterface)list.get(i)).debug(); }
  System.out.println("MCRMetaElement debug end"+NL);
  }

}

