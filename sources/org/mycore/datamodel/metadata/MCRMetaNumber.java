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
import org.w3c.dom.Text;
import mycore.common.MCRException;

/**
 * This class implements all method for handling with the MCRMetaNumber part
 * of a metadata object. The MCRMetaNumber class present a number value
 * in decimal format and optional a type and a measurement. The number
 * can has the format like <em>xxxx.xxx</em> or <em>xxxx,xxx</em>. There
 * was stored three numbers after the dot and nine befor them. Also you can
 * store an integer.
 * <p>
 * &lt;tag class="MCRMetaNumber" heritable="..."&gt;<br>
 * &lt;subtag type="..." xml:lang="..." measurement="..."&gt;<br>
 * xxxx.xxx or xxx<br>
 * &lt;/subtag&gt;<br>
 * &lt;/tag&gt;<br>
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
final public class MCRMetaNumber extends MCRMetaDefault 
  implements MCRMetaInterface 
{

// MCRMetaNumber data
private double number;
private String dimension;
private String measurement;

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * The number was set to zero, the measurement and the dimension was set to 
 * an empty string.
 */
public MCRMetaNumber()
  {
  super();
  number = 0.;
  dimension = "";
  measurement = "";
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is null, empty or false <b>en</b> was set. The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The dimension element was set to
 * the value of <em>set_dimension<em>, if it is null, an empty string was set
 * to the type element. The measurement element was set to the value of 
 * <em>set_measurement<em>, if it is null, an empty string was set
 * to the measurement element.  The number string <em>set_number</em>
 * was set to the number element, if it is null or not a number, a 
 * MCRException was thowed.
 *
 * @param set_datapart     the global part of the elements like 'metadata'
 *                         or 'service'
 * @param set_subtag       the name of the subtag
 * @param default_lang     the default language
 * @param set_dimension    the optional dimension string
 * @param set_measurement  the optional measurement string
 * @param set_number       the number string
 * @exception MCRException if the set_subtag value is null or empty or if
 *   the number string is not in a number format
 */
public MCRMetaNumber(String set_datapart, String set_subtag, 
  String default_lang, String set_dimension, String set_measurement, 
  String set_number) throws MCRException
  {
  super(set_datapart,set_subtag,default_lang,"");
  set_number = set_number.trim();
  number = 0.;
  try {
    if (set_number == null) {
      throw new MCRException( "The format of a number is false."); }
    set_number.replace(',','.');
    number = (new Double(set_number)).doubleValue(); }
  catch (NumberFormatException e) {
    throw new MCRException( "The format of a number is false."); }
  dimension = "";
  if (set_dimension != null) { dimension = set_dimension; }
  measurement = "";
  if (set_measurement != null) { measurement = set_measurement; }
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is null, empty or false <b>en</b> was set. The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The dimension element was set to
 * the value of <em>set_dimension<em>, if it is null, an empty string was set
 * to the type element. The measurement element was set to the value of
 * <em>set_measurement<em>, if it is null, an empty string was set
 * to the measurement element.  The number <em>set_number</em>
 * was set to the number element.
 *
 * @param set_datapart     the global part of the elements like 'metadata'
 *                         or 'service'
 * @param set_subtag       the name of the subtag
 * @param default_lang     the default language
 * @param set_dimension    the optional dimension string
 * @param set_number       the number value
 * @exception MCRException if the set_subtag value is null or empty
 */
public MCRMetaNumber(String set_datapart, String set_subtag, 
  String default_lang, String set_dimension,  String set_measurement,
  double set_number) throws MCRException
  {
  super(set_datapart,set_subtag,default_lang,"");
  number = set_number;
  dimension = "";
  if (set_dimension != null) { dimension = set_dimension; }
  measurement = "";
  if (set_measurement != null) { measurement = set_measurement; }
  }

/**
 * This method set the dimension, if it is null, an empty string was set
 * to the dimension element.
 *
 * @param set_dimension the dimension string
 **/
public final void setDimension(String set_dimension)
  {
  dimension = "";
  if (set_dimension != null) { dimension = set_dimension; }
  }

/**
 * This method set the measurement, if it is null, an empty string was set
 * to the measurement element.
 *
 * @param set_measurement the measurement string
 **/
public final void setMeasurement(String set_measurement)
  {
  measurement = "";
  if (set_measurement != null) { measurement = set_measurement; }
  }

/**
 * This method set the number, if it is null or not a number, a
 * MCRException was thowed.
 *
 * @param set_number       the number string
 * @exception MCRException if the number string is not in a number format
 **/
public final void setNumber(String set_number)
  {
  String sset_number = set_number.replace(',','.');
  sset_number = sset_number.trim();
  number = 0.;
  try {
    if (sset_number == null) {
      throw new MCRException( "The format of a number is false."); }
    number = (new Double(sset_number)).doubleValue(); }
  catch (NumberFormatException e) {
    throw new MCRException( "The format of a number is false."); }
  }

/**
 * This method set the number.
 *
 * @param set_number       the number value
 **/
public final void setNumber(double set_number)
  { number = set_number; }

/**
 * This method get the dimension element.
 *
 * @return the dimension String
 **/
public final String getDimension()
  { return dimension; }

/**
 * This method get the measurement element.
 *
 * @return the measurement String
 **/
public final String getMeasurement()
  { return measurement; }

/**
 * This method get the number element.
 *
 * @return the number
 **/
public final double getNumber()
  { return number; }

/**
 * This method get the number element as String.
 *
 * @return the number String
 **/
public final String getNumberToString()
  { return (new Double(number)).toString(); }

/**
 * This method read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param metadata_number_node a relevant DOM element for the metadata
 **/
public final void setFromDOM(Node metadata_number_node)
  {
  super.setFromDOM(metadata_number_node);
  String temp_meas = ((Element)metadata_number_node)
    .getAttribute("measurement");
  if ((temp_meas!=null) && ((temp_meas = temp_meas.trim()).length() !=0)) {
    measurement = temp_meas; }
  String temp_dim = ((Element)metadata_number_node)
    .getAttribute("dimension");
  if ((temp_dim!=null) && ((temp_dim = temp_dim.trim()).length() !=0)) {
    dimension = temp_dim; }
  Node number_textelement = metadata_number_node.getFirstChild();
  String temp_value = ((Text)number_textelement).getData().trim();
  if (temp_value==null) { number = 0.; return; }
  setNumber(temp_value);
  }

/**
 * This method create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRNumber definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a JDOM Element with the XML MCRNumber part
 **/
public final org.jdom.Element createXML() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  org.jdom.Element elm = new org.jdom.Element(subtag);
  elm.setAttribute("xml:lang",lang);
  if ((dimension != null) && ((dimension = dimension.trim()).length() !=0)) {
    elm.setAttribute("dimension",dimension); }
  if ((measurement != null) && 
      ((measurement = measurement.trim()).length() !=0)) {
    elm.setAttribute("measurement",measurement); }
  elm.addContent(getNumberToString());
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
public final MCRTypedContent createTypedContent(boolean parametric,
  boolean textsearch) throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  tc.addTagElement(tc.TYPE_SUBTAG,subtag.toUpperCase());
  tc.addDoubleElement(number,parametric,textsearch);
  tc.addStringElement(tc.TYPE_ATTRIBUTE,"xml:lang",lang,parametric,textsearch);
  if ((type = type.trim()).length() !=0) {
    tc.addStringElement(tc.TYPE_ATTRIBUTE,"type",type,parametric,textsearch); }
  if ((dimension = dimension.trim()).length() !=0) {
    tc.addStringElement(tc.TYPE_ATTRIBUTE,"dimension",dimension,parametric,
      textsearch); }
  if ((measurement = measurement.trim()).length() !=0) {
    tc.addStringElement(tc.TYPE_ATTRIBUTE,"measurement",measurement,
      parametric,textsearch); }
  return tc;
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
public final boolean isValid()
  {
  if (!super.isValid()) { return false; }
  return true;
  }

/**
 * This method print all data content from the MCRMetaNumber class.
 **/
public final void debug()
  {
  System.out.println("MCRMetaNumber debug start:");
  super.debug();
  System.out.println("<dimension>"+dimension+"</dimension>");
  System.out.println("<measurement>"+measurement+"</measurement>");
  System.out.println(number);
  System.out.println("MCRMetaNumber debug end"+NL);
  }

}

