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

import java.text.*;
import java.util.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import mycore.common.MCRException;
import mycore.common.MCRUtils;

/**
 * This class implements all method for handling with the MCRMetaDate part
 * of a metadata object. The MCRMetaDate class present a single item,
 * which has pair of a date and his corresponding optional type.
 * The date is stored as GregorianCalendar.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
final public class MCRMetaDate extends MCRMetaDefault 
  implements MCRMetaInterface 
{
// common data

// MetaDate data
private GregorianCalendar date;

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * All other elements was set to null.
 */
public MCRMetaDate()
  {
  super();
  date = new GregorianCalendar();
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is null, empty or false <b>en</b> was set. The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The type element was set to
 * the value of <em>set_type<em>, if it is null, an empty string was set
 * to the type element. The date element was set to the value of 
 * <em>set_date<em>, if it is null, the date was set to this null.
 *
 * @param set_datapart     the global part of the elements like 'metadata'
 *                         or 'service'
 * @param set_subtag       the name of the subtag
 * @param default_lang     the default language
 * @param set_type         the optional type string
 * @param set_date         the date as GregorianCalendar
 * @exception MCRException if the set_subtag value is null or empty
 */
public MCRMetaDate(String set_datapart, String set_subtag, 
  String default_lang, String set_type, GregorianCalendar set_date) 
  throws MCRException
  {
  super(set_datapart,set_subtag,default_lang,set_type);
  date = null;
  if (set_date != null) { date = set_date; }
  }

/**
 * This method set the languge, type and text. 
 *
 * @param set_lang        the new language string, if this is null or
 *                        empty, nothing is to do
 * @param set_type        the optional type syting
 * @param set_date        the date as GregorianCalendar
 **/
public final void set(String set_lang, String set_type, GregorianCalendar
  set_date)
  {
  setLang(set_lang);
  setType(set_type);
  if (set_date != null) { date = set_date; }
  }

/**
 * This methode set the date to the actual date.
 **/
public final void setDate()
  { date = new GregorianCalendar(); }

/**
 * This methode set the date to the given date.
 *
 * @param set_date        the date as GregorianCalendar
 **/
public final void setDate(GregorianCalendar set_date)
  { 
  date = null;
  if (set_date != null) { date = set_date; }
  }

/**
 * This methode set the date to the given date.
 *
 * @param set_date           a date string
 * @exception MCRException   if the date is bad
 **/
public final void setDate(String set_date) throws MCRException
  {
  date = null;
  if ((set_date == null) || ((set_date = set_date.trim()).length() ==0)) { 
    return; }
  try {
    date = new GregorianCalendar();
    DateFormat df = MCRUtils.getDateFormat(lang);
    date.setTime(df.parse(set_date)); }
  catch (ParseException e) {
    throw new MCRException( "Can't parse date."); }
  }

/**
 * This method get the date element as GregorianCalendar.
 *
 * @return the date
 **/
public final GregorianCalendar getDate()
  { return date; }

/**
 * This methode return the date as string.
 *
 * @return the date
 **/
public final String getDateToString()
  {
  if (date == null) { return ""; }
  DateFormat df = MCRUtils.getDateFormat(lang);
  if (df==null) { return ""; }
  return df.format(date.getTime());
  }

/**
 * This method read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param metadata_langtext_node a relevant DOM element for the metadata
 **/
public final void setFromDOM(Node metadata_date_node)
  {
  super.setFromDOM(metadata_date_node);
  Node date_textelement = metadata_date_node.getFirstChild();
  String temp_date = ((Text)date_textelement).getData().trim();
  if (temp_date==null) { temp_date = ""; }
  setDate(temp_date);
  }

/**
 * This method create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRMetaDate definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML MCRMetaDate part
 **/
public final String createXML() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  StringBuffer sb = new StringBuffer(1024);
  sb.append('<').append(subtag).append(" xml:lang=\"").append(lang)
    .append("\"");
  if ((type != null) && ((type = type.trim()).length() !=0)) {
    sb.append(" type=\"").append(type).append("\""); }
  sb.append('>').append(NL);
  sb.append(getDateToString());
  sb.append("</").append(subtag).append('>').append(NL);
  return sb.toString();
  }

/**
 * This methode create a typed content list for all data in this instance.
 *
 * @param parametric true if the data should parametric searchable
 * @param textsearch true if the data should text searchable
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the MCRObject data
 **/
public final MCRTypedContent createTypedContent(boolean parametric,
  boolean textsearch) throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  tc.addTagElement(tc.TYPE_SUBTAG,subtag);
  tc.addDateElement(date,parametric,textsearch);
  tc.addStringElement(tc.TYPE_ATTRIBUTE,"xml:lang",lang,parametric,textsearch);
  if ((type = type.trim()).length() !=0) {
    tc.addStringElement(tc.TYPE_ATTRIBUTE,"type",type,parametric,textsearch); }
  return tc;
  }

/**
 * This method check the validation of the content of this class.
 * The method returns <em>true</em> if
 * <ul>
 * <li> the subtag is not null or empty
 * <li> the date is not null 
 * </ul>
 * otherwise the method return <em>false</em>
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if (!super.isValid()) { return false; }
  if (date == null) { return false; }
  return true;
  }

/**
 * This method print all data content from the MCRMetaDate class.
 **/
public final void debug()
  {
  System.out.println("MCRMetaDate debug start:");
  super.debug();
  System.out.println("<date>"+getDateToString()+"</date>");
  System.out.println("MCRMetaDate debug end"+NL);
  }

}

