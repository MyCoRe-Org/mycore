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

import org.mycore.common.MCRException;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRUtils;

/**
 * This class implements all method for generic handling with the MCRMetaLink part
 * of a metadata object. The MCRMetaLink class present two types. At once
 * a reference to an URL. At second a bidirectional link between two URL's. 
 * Optional you can append the reference
 * with the label attribute. See to W3C XLink Standard for more informations.
 * <p>
 * &lt;tag class="MCRMetaLink" heritable="..." parasearch="..."&gt;<br>
 * &lt;subtag xlink:type="locator" xlink:href="<em>URL</em>" xlink:label="..." xlink:title="..."/&gt;<br>
 * &lt;subtag xlink:type="arc" xlink:from="<em>URL</em>" xlink:to="URL"/&gt;<br>
 * &lt;/tag&gt;<br>
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRMetaLink extends MCRMetaDefault 
  implements MCRMetaInterface 
{

/** The length of XLink:type **/
public static final int MAX_XLINK_TYPE_LENGTH = 8;
/** The length of XLink:href **/
public static final int MAX_XLINK_HREF_LENGTH = 128;
/** The length of XLink:label **/
public static final int MAX_XLINK_LABEL_LENGTH = 128;
/** The length of XLink:title **/
public static final int MAX_XLINK_TITLE_LENGTH = 128;
/** The length of XLink:from **/
public static final int MAX_XLINK_FROM_LENGTH = MCRObjectID.MAX_LENGTH;
/** The length of XLink:to **/
public static final int MAX_XLINK_TO_LENGTH = MCRObjectID.MAX_LENGTH;

// MetaLink data
protected String href;
protected String label;
protected String title;
protected String linktype;
protected String from, to;

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * All other elemnts was set to an empty value.
 */
public MCRMetaLink()
  {
  super();
  href = "";
  label = "";
  title = "";
  linktype = null;
  from = "";
  to = "";
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is null, empty or false <b>en</b> was set. The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. 
 *
 * @param set_datapart     the global part of the elements like 'metadata'
 *                         or 'service' or so
 * @param set_subtag       the name of the subtag
 * @param default_lang     the default language
 * @param set_inherted     a value >= 0
 * @exception MCRException if the set_datapart or set_subtag value is null or empty
 */
public MCRMetaLink(String set_datapart, String set_subtag, String default_lang,
  int set_inherted)
  throws MCRException
  {
  super(set_datapart,set_subtag,default_lang,"",set_inherted);
  href = "";
  label = "";
  title = "";
  linktype = null;
  from = "";
  to = "";
  }

/**
 * This method set a reference with xlink:href, xlink:label and xlink:title. 
 *
 * @param set_href        the reference 
 * @param set_label       the new label string
 * @param set_title       the new title string
 * @exception MCRException if the set_href value is null or empty
 **/
public void setReference(String set_href, String set_label, 
  String set_title) throws MCRException
  {
  linktype = "locator";
  if ((set_href==null) || ((set_href = set_href.trim()).length() ==0)) {
    throw new MCRException("The href value is null or empty."); }
  href = set_href.trim(); 
  if (set_label==null) { label = ""; } else { label = set_label.trim(); }
  if (set_title==null) { title = ""; } else { title = set_title.trim(); }
  }

/**
 * This method set a bidirectional link with xlink:from, xlink:to and 
 * xlink:title. 
 *
 * @param set_from        the source 
 * @param set_to          the target 
 * @param set_title       the new title string
 * @exception MCRException if the from or to element is  null or empty
 **/
public void setBiLink(String set_from, String set_to, String set_title)
  throws MCRException
  {
  linktype = "arc";
  if ((set_from==null) || ((set_from = set_from.trim()).length() ==0)) {
    throw new MCRException("The from value is null or empty."); }
  else { 
    from = set_from.trim(); }
  if ((set_to==null) || ((set_to = set_to.trim()).length() ==0)) {
    throw new MCRException("The to value is null or empty."); }
  else { 
    to = set_to.trim(); }
  if (set_title==null) { title = ""; } else { title = set_title.trim(); }
  }

/**
 * This method get the xlink:type element.
 *
 * @return the xlink:type
 **/
public final String getXLinkType()
  { return linktype; }

/**
 * This method get the xlink:href element as string.
 *
 * @return the xlink:href element as string
 **/
public final String getXLinkHref()
  { return href; }

/**
 * This method get the xlink:label element.
 *
 * @return the xlink:label
 **/
public final String getXLinkLabel()
  { return label; }

/**
 * This method get the xlink:title element.
 *
 * @return the xlink:title
 **/
public final String getXLinkTitle()
  { return title; }

/**
 * This method get the xlink:from element as string.
 *
 * @return the xlink:from element as string
 **/
public final String getXLinkFrom()
  { return from; }

/**
 * This method get the xlink:to element as string.
 *
 * @return the xlink:to element as string
 **/
public final String getXLinkTo()
  { return to; }

/**
 * The methode compare this instance of MCRMetaLink with a input object of the
 * class type MCRMetaLink. The both instances are equal, if:<br>
 * <ul><br>
 * <li>for the type 'arc' the 'from' and 'to' element is equal</li><br>
 * <li>for the type 'locator' the 'href' element is equal</li><br>
 * </ul><br>
 *
 * @param in_derivate the MCRMetaLink input
 * @return true if it is compare, else return false
 **/
public final boolean compare(MCRMetaLink input)
  {
  if (linktype.equals("locator")) {
    if ((linktype.equals(input.getXLinkType())) &&
        (href.equals(input.getXLinkHref()))) { return true; }
    }
  if (linktype.equals("arc")) {
    if ((linktype.equals(input.getXLinkType())) &&
        (from.equals(input.getXLinkFrom())) &&
        (to.equals(input.getXLinkTo()))) { return true; }
    }
  return false;
  }

/**
 * This method read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param element a relevant DOM element for the metadata
 * @exception MCRException if the xlink:type is not locator or arc or if 
 * href or from and to are null or empty
 **/
public void setFromDOM(org.jdom.Element element) throws MCRException
  {
  super.setFromDOM(element);
  super.type = "";
  String temp = element.getAttributeValue("type",org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
  if ((temp!=null) && ((temp = temp.trim()).length() !=0)) {
    if ((temp.equals("locator"))||(temp.equals("arc"))) {
      linktype = temp; }
    else {
      linktype = null;
      throw new MCRException("The xlink:type is not locator or arc."); }
    }
  else {
    linktype = null;
    throw new MCRException("The xlink:type is not locator or arc."); }
  if (linktype.equals("locator")) {
    String temp1 = (String)element.getAttributeValue("href",org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
    String temp2 = (String)element.getAttributeValue("label",org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
    String temp3 = (String)element.getAttributeValue("title",org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
    setReference(temp1,temp2,temp3);
    }
  else {
    String temp1 = (String)element.getAttributeValue("from",org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
    String temp2 = (String)element.getAttributeValue("to",org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
    String temp3 = (String)element.getAttributeValue("title",org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
    setBiLink(temp1,temp2,temp3);
    }
  }

/**
 * This method create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRMetaLink definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a JDOM Element with the XML MCRMetaLink part
 **/
public org.jdom.Element createXML() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content of MCRMetaLink is not valid."); }
  org.jdom.Element elm = new org.jdom.Element(subtag);
  elm.setAttribute("inherited",(new Integer(inherited)).toString()); 
  elm.setAttribute("type",linktype,org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
  if (linktype.equals("locator")) {
    elm.setAttribute("href",href,org.jdom.Namespace.getNamespace("xlink",
      MCRDefaults.XLINK_URL));
    if ((label != null) && ((label = label.trim()).length() !=0)) {
      elm.setAttribute("label",label,org.jdom.Namespace.getNamespace("xlink",
        MCRDefaults.XLINK_URL)); }
    if ((title != null) && ((title = title.trim()).length() !=0)) {
      elm.setAttribute("title",title,org.jdom.Namespace.getNamespace("xlink",
        MCRDefaults.XLINK_URL)); }
    }
  else {
    elm.setAttribute("from",from,org.jdom.Namespace.getNamespace("xlink",
      MCRDefaults.XLINK_URL));
    elm.setAttribute("to",to,org.jdom.Namespace.getNamespace("xlink",
      MCRDefaults.XLINK_URL));
    if ((title != null) && ((title = title.trim()).length() !=0)) {
      elm.setAttribute("title",title,org.jdom.Namespace.getNamespace("xlink",
        MCRDefaults.XLINK_URL)); }
    }
  return elm;
  }

/**
 * This methode create a typed content list for all data in this instance.
 *
 * @param parasearch true if the data should parametric searchable
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the MCRObject data
 **/
public MCRTypedContent createTypedContent(boolean parasearch)
  throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content of MCRMetaLink is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  if(!parasearch) { return tc; }
  tc.addTagElement(MCRTypedContent.TYPE_SUBTAG,subtag);
  tc.addLinkElement("type",linktype);
  if (linktype.equals("locator")) {
    tc.addLinkElement("href",href);
    tc.addLinkElement("label",label);
    tc.addLinkElement("title",title);
    }
  else {
    tc.addLinkElement("from",from);
    tc.addLinkElement("to",to);
    tc.addLinkElement("title",title);
    }
  return tc;
  }

/**
 * This methode create a String for all text searchable data in this instance.
 * Only the title string was returned.
 *
 * @param textsearch true if the data should text searchable
 * @exception MCRException if the content of this class is not valid
 * @return an empty String, because the content is not text searchable.
 **/
public final String createTextSearch(boolean textsearch)
  throws MCRException
  {
  return title;
  }

/**
 * This method check the validation of the content of this class.
 * The method returns <em>true</em> if
 * <ul>
 * <li> the subtag is not null or empty
 * <li> the xlink:type not "locator" or "arc"
 * <li> the from or to are not valid
 * </ul>
 * otherwise the method return <em>false</em>
 *
 * @return a boolean value
 **/
public boolean isValid()
  {
  if (!super.isValid()) { return false; }
  if (linktype == null) { return false; }
  if ((!linktype.equals("locator"))&&(!linktype.equals("arc"))) { 
    return false; }
  if (linktype.equals("arc")) {
    if (from.equals("")) { return false; }
    if (to.equals("")) { return false; }
    }
  if (linktype.equals("locator")) {
    if (href.equals("")) { return false; }
    }
  return true;
  }

/**
 * This method make a clone of this class.
 **/
public final Object clone()
  {
  MCRMetaLink out = new MCRMetaLink(datapart,subtag,lang,inherited);
  out.linktype = linktype;
  out.title = title;
  out.type = type;
  out.href = href;
  out.to = to;
  out.from = from;
  return (Object)out;
  }

/**
 * This method put debug data to the logger (for the debug mode).
 **/
public final void debug()
  {
  logger.debug("Start Class : MCRMetaLink");
  super.debugDefault();
  logger.debug("Link Type          = "+linktype);
  logger.debug("Label              = "+label);
  logger.debug("Title              = "+title);
  logger.debug("HREF               = "+href);
  logger.debug("FROM               = "+from);
  logger.debug("TO                 = "+to);
  logger.debug("Stop");
  logger.debug("");
  }

}

