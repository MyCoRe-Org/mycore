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

package mycore.user;

import java.io.*;
import java.util.Vector;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import mycore.xml.MCRXMLHelper;

/**
 * Instances of this class store address information of MyCoRe users. MCRUserAddress
 * is part of the MyCoRe user component and must not be used directly by other components.
 * MCRUserAddress is aggregated by MCRUser and all user objects are managed by the
 * user manager (the instance of the singleton MCRUserMgr), which is the only class of
 * the MyCoRe user component that other components should use.
 *
 * @see mycore.user.MCRUserMgr
 * @see mycore.user.MCRUser
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserAddress
{
  private String salutation = "";
  private String firstname = "";
  private String lastname = "";
  private String street = "";
  private String city = "";
  private String postalcode = "";
  private String country = "";
  private String institution = "";
  private String faculty = "";
  private String department = "";
  private String institute = "";
  private String telephone = "";
  private String fax = "";
  private String email = "";
  private String cellphone = "";

  /**
   * Constructor. All address attributes of a user are taken from a DOM NodeList
   * which must be passed as a parameter.
   *
   * @param addressElements a DOM NodeList containing the address attributes of a user
   */
  public MCRUserAddress(NodeList addressElements)
  {
    salutation  = trim(MCRXMLHelper.getElementText("salutation", addressElements));
    firstname   = trim(MCRXMLHelper.getElementText("firstname", addressElements));
    lastname    = trim(MCRXMLHelper.getElementText("lastname", addressElements));
    street      = trim(MCRXMLHelper.getElementText("street", addressElements));
    city        = trim(MCRXMLHelper.getElementText("city", addressElements));
    postalcode  = trim(MCRXMLHelper.getElementText("postalcode", addressElements));
    country     = trim(MCRXMLHelper.getElementText("country", addressElements));
    institution = trim(MCRXMLHelper.getElementText("institution", addressElements));
    faculty     = trim(MCRXMLHelper.getElementText("faculty", addressElements));
    department  = trim(MCRXMLHelper.getElementText("department", addressElements));
    institute   = trim(MCRXMLHelper.getElementText("institute", addressElements));
    telephone   = trim(MCRXMLHelper.getElementText("telephone", addressElements));
    fax         = trim(MCRXMLHelper.getElementText("fax", addressElements));
    email       = trim(MCRXMLHelper.getElementText("email", addressElements));
    cellphone   = trim(MCRXMLHelper.getElementText("cellphone", addressElements));
  }

  /**
   * Construtor. All address attributes are passed as Strings
   */
  public MCRUserAddress(String salutation, String firstname, String lastname,
                        String street, String city, String postalcode,
                        String country, String institution, String faculty,
                        String department, String institute, String telephone,
                        String fax, String email, String cellphone)
  {
    this.salutation  = trim(salutation);
    this.firstname   = trim(firstname);
    this.lastname    = trim(lastname);
    this.street      = trim(street);
    this.city        = trim(city);
    this.postalcode  = trim(postalcode);
    this.country     = trim(country);
    this.institution = trim(institution);
    this.faculty     = trim(faculty);
    this.department  = trim(department);
    this.institute   = trim(institute);
    this.telephone   = trim(telephone);
    this.fax         = trim(fax);
    this.email       = trim(email);
    this.cellphone   = trim(cellphone);
  }

  /**
   * The following methods simply return the attributes...
   */
  public String getSalutation()
  { return salutation; }

  public String getFirstName()
  { return firstname; }

  public String getLastName()
  { return lastname; }

  public String getStreet()
  { return street; }

  public String getCity()
  { return city; }

  public String getPostalCode()
  { return postalcode; }

  public String getCountry()
  { return country; }

  public String getInstitution()
  { return institution; }

  public String getFaculty()
  { return faculty; }

  public String getDepartment()
  { return department; }

  public String getInstitute()
  { return institute; }

  public String getTelephone()
  { return telephone; }

  public String getFax()
  { return fax; }

  public String getEmail()
  { return email; }

  public String getCellphone()
  { return cellphone; }

  /**
   * This method returns the user address information as an XML-Stream. This output
   * is used by the corresponding user object to create a full XML representation
   * of the user.
   *
   * @param NL   newline character, should be "\n" if you want newlines behind
   *             every tag or "" if you want the XML-stream as one line
   */
  public String getAddressAsXmlElement(String NL) throws Exception
  {
    Vector requiredUserAttributes = MCRUserPolicy.instance().getRequiredUserAttributes();
    StringBuffer sb = new StringBuffer();
    sb.append("<address>").append(NL);

    if (requiredUserAttributes.contains("salutation"))
      sb.append("<salutation required=\"true\">");
    else
      sb.append("<salutation>");
    sb.append(salutation).append("</salutation>").append(NL);

    if (requiredUserAttributes.contains("firstname"))
      sb.append("<firstname required=\"true\">");
    else
      sb.append("<firstname>");
    sb.append(firstname).append("</firstname>").append(NL);

    if (requiredUserAttributes.contains("lastname"))
      sb.append("<lastname required=\"true\">");
    else
      sb.append("<lastname>");
    sb.append(lastname).append("</lastname>").append(NL);

    if (requiredUserAttributes.contains("street"))
      sb.append("<street required=\"true\">");
    else
      sb.append("<street>");
    sb.append(street).append("</street>").append(NL);

    if (requiredUserAttributes.contains("city"))
      sb.append("<city required=\"true\">");
    else
       sb.append("<city>");
    sb.append(city).append("</city>").append(NL);

    if (requiredUserAttributes.contains("postalcode"))
      sb.append("<postalcode required=\"true\">");
    else
      sb.append("<postalcode>");
    sb.append(postalcode).append("</postalcode>").append(NL);

    if (requiredUserAttributes.contains("country"))
      sb.append("<country required=\"true\">");
    else
      sb.append("<country>");
    sb.append(country).append("</country>").append(NL);

    if (requiredUserAttributes.contains("institution"))
      sb.append("<institution required=\"true\">");
    else
      sb.append("<institution>");
    sb.append(institution).append("</institution>").append(NL);

    if (requiredUserAttributes.contains("faculty"))
      sb.append("<faculty required=\"true\">");
    else
      sb.append("<faculty>");
    sb.append(faculty).append("</faculty>").append(NL);

    if (requiredUserAttributes.contains("department"))
      sb.append("<department required=\"true\">");
    else
      sb.append("<department>");
    sb.append(department).append("</department>").append(NL);

    if (requiredUserAttributes.contains("institute"))
      sb.append("<institute required=\"true\">");
    else
      sb.append("<institute>");
    sb.append(institute).append("</institute>").append(NL);

    if (requiredUserAttributes.contains("telephone"))
      sb.append("<telephone required=\"true\">");
    else
      sb.append("<telephone>");
    sb.append(telephone).append("</telephone>").append(NL);

    if (requiredUserAttributes.contains("fax"))
      sb.append("<fax required=\"true\">");
    else
      sb.append("<fax>");
    sb.append(fax).append("</fax>").append(NL);

    if (requiredUserAttributes.contains("email"))
      sb.append("<email required=\"true\">");
    else
      sb.append("<email>");
    sb.append(email).append("</email>").append(NL);

    if (requiredUserAttributes.contains("cellphone"))
      sb.append("<cellphone required=\"true\">");
    else
      sb.append("<cellphone>");
    sb.append(cellphone).append("</cellphone>").append(NL);

    sb.append("</address>");
    return sb.toString();
  }

  /**
   * This method overrides the <code>toString()</code> method inherited from
   * <code>java.lang.Object</code>. All address attributes of a user are returned
   * as a string (in table formatted output for "nice" listings).
   *
   * @return returns the address information of the user in a table formatted
   *         output, all in one string
   */
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("salutation     : ").append(salutation ).append("\n");
    sb.append("firstname      : ").append(firstname  ).append("\n");
    sb.append("lastname       : ").append(lastname   ).append("\n");
    sb.append("street         : ").append(street     ).append("\n");
    sb.append("city           : ").append(city       ).append("\n");
    sb.append("postalcode     : ").append(postalcode ).append("\n");
    sb.append("country        : ").append(country    ).append("\n");
    sb.append("institution    : ").append(institution).append("\n");
    sb.append("faculty        : ").append(faculty    ).append("\n");
    sb.append("department     : ").append(department ).append("\n");
    sb.append("institute      : ").append(institute  ).append("\n");
    sb.append("telephone      : ").append(telephone  ).append("\n");
    sb.append("fax            : ").append(fax        ).append("\n");
    sb.append("email          : ").append(email      ).append("\n");
    sb.append("cellphone      : ").append(cellphone  );
    return sb.toString();
  }

  /**
   * This <code>toString()</code> method returns all address information in
   * one string. The various attributes are separated by a separator string, which
   * must be provided as a parameter.
   *
   * @param separator String sequence for separating the various address attributes
   */
  public String toString(String separator)
  {
    StringBuffer sb = new StringBuffer();
    sb.append(salutation ).append(separator)
      .append(firstname  ).append(separator)
      .append(lastname   ).append(separator)
      .append(street     ).append(separator)
      .append(city       ).append(separator)
      .append(postalcode ).append(separator)
      .append(country    ).append(separator)
      .append(institution).append(separator)
      .append(faculty    ).append(separator)
      .append(department ).append(separator)
      .append(institute  ).append(separator)
      .append(telephone  ).append(separator)
      .append(fax        ).append(separator)
      .append(email      ).append(separator)
      .append(cellphone  ).append(separator);
    return sb.toString();
  }

  /**
   * This helper method replaces null with an empty string and trims whitespace from
   * non-null strings.
   */
  private static String trim(String s)
  { return (s != null) ? s.trim() : ""; }
}
