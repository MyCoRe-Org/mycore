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
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationItem;

/**
 * This class implements all method for handling with the MCRMetaClassification
 *  part of a metadata object. The MCRMetaClassification class present a 
 * link to a category of a classification.
 * <p>
 * &lt;tag class="MCRMetaClassification" heritable="..."&gt;<br>
 * &lt;subtag classid="..." categid="..." /&gt;<br>
 * &lt;/tag&gt;<br>
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRMetaClassification extends MCRMetaDefault 
  implements MCRMetaInterface 
{

/** The length of the classification ID **/
public static final int MAX_CLASSID_LENGTH = MCRObjectID.MAX_LENGTH;
public static final int MAX_CATEGID_LENGTH = 128;

// MCRMetaClassification data
protected String classid;
protected String categid;

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * The classid and categid value was set to an empty string.
 */
public MCRMetaClassification()
  {
  super();
  classid = "";
  categid = "";
  }

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * The subtag element was set to the value of <em>set_subtag<em>. If the 
 * value of <em>set_subtag</em> is null or empty an exception was throwed. 
 * The type element was set to an empty string.
 * the <em>set_classid</em> and the <em>categid</em> must be not null
 * or empty!
 *
 * @param set_datapart     the global part of the elements like 'metadata'
 *                         or 'service'
 * @param set_subtag       the name of the subtag
 * @param set_inherted     a value >= 0
 * @param set_classid      the classification ID
 * @param set_categid      the category ID
 * @exception MCRException if the set_subtag value, the set_classid value or
 * the set_categid are null, empty, too long or not a MCRObjectID
 */
public MCRMetaClassification(String set_datapart, String set_subtag, 
  int set_inherted, String set_classid, String set_categid) 
  throws MCRException
  {
  super(set_datapart,set_subtag,"en","",set_inherted);
  setValue(set_classid,set_categid);
  }

/**
 * The method return the classification ID.
 *
 * @return the classId
 **/
public final String getClassId()
  { return classid; }
 
/**
 * The method return the category ID.
 *
 * @return the categId
 **/
public final String getCategId()
  { return categid; }
 
/**
 * This method set values of classid and categid.
 *
 * @param set_classid      the classification ID
 * @param set_categid      the category ID
 * @exception MCRException if the set_classid value or
 * the set_categid are null, empty, too long or not a MCRObjectID
 **/
public final void setValue(String set_classid, String set_categid)
  throws MCRException
  {
  if ((set_classid==null) || ((set_classid=set_classid.trim()).length()==0)) {
    throw new MCRException("The classid is empty."); }
  if ((set_categid==null) || ((set_categid=set_categid.trim()).length()==0)) {
    throw new MCRException("The categid is empty."); }
  if (set_classid.length() > MAX_CLASSID_LENGTH) {
    throw new MCRException("The classid is too long."); }
  try {
    MCRObjectID mid = new MCRObjectID(set_classid); 
    classid = mid.getId(); }
  catch (Exception e) {
    throw new MCRException("The classid is not MCRObjectID."); }
  if (set_categid.length() > MAX_CATEGID_LENGTH) {
    throw new MCRException("The categid is too long."); }
  categid = set_categid;
  }

/**
 * This method read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param element a relevant JDOM element for the metadata
 * @exception MCRException if the set_classid value or
 * the set_categid are null, empty, too long or not a MCRObjectID
 **/
public void setFromDOM(org.jdom.Element element)
  throws MCRException
  {
  super.setFromDOM(element);
  String set_classid = element.getAttributeValue("classid");
  String set_categid = element.getAttributeValue("categid");
  setValue(set_classid,set_categid);
  }

/**
 * This method create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRMetaClassification definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a JDOM Element with the XML MCRClassification part
 **/
public org.jdom.Element createXML() throws MCRException
  {
  if (!isValid()) {
    throw new MCRException("The content of MCRMetaClassification is not valid."); }
  org.jdom.Element elm = new org.jdom.Element(subtag);
  elm.setAttribute("inherited",(new Integer(inherited)).toString()); 
  elm.setAttribute("classid",classid); 
  elm.setAttribute("categid",categid); 
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
    throw new MCRException("The content of MCRMetaClassification is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  if(!parasearch) { return tc; } 
  tc.addTagElement(MCRTypedContent.TYPE_SUBTAG,subtag);
  tc.addClassElement(classid);
  tc.addCategElement(categid);
  return tc;
  }

/**
 * This methode create a String for all text searchable data in this instance.
 *
 * @param textsearch true if the data should text searchable
 * @exception MCRException if the content of this class is not valid
 * @return an empty String, because the content is not text searchable.
 **/
public String createTextSearch(boolean textsearch)
  throws MCRException
  {
  return "";
  }

/**
 * This method check the validation of the content of this class.
 * The method returns <em>true</em> if
 * <ul>
 * <li> the subtag is not null or empty
 * </ul>
 * otherwise the method return <em>false</em>
 *
 * @return a boolean value
 **/
public boolean isValid()
  {
  if (!super.isValid()) { return false; }
  try {
    MCRClassificationItem cl = 
      MCRClassificationItem.getClassificationItem(classid);
    if (cl==null) { return false; }
    MCRCategoryItem ci = cl.getCategoryItem(categid);
    if (ci==null) { return false; }
    }
  catch (Exception e) { return false; }
  return true;
  }

/**
 * This method make a clone of this class.
 **/
public Object clone()
  {
  MCRMetaClassification out = new MCRMetaClassification(datapart,subtag,
    inherited,classid,categid);
    return (Object)out;
  }

}

