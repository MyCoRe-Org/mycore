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

package org.mycore.user;

import java.io.*;
import java.util.Vector;
import org.jdom.Element;

/**
 * Instances of this class store contact information of MyCoRe users. MCRUserContact
 * is part of the MyCoRe user component and must not be used directly by other components.
 * MCRUserContact is aggregated by MCRUser and all user objects are managed by the
 * user manager (the instance of the singleton MCRUserMgr).
 *
 * @see org.mycore.user.MCRUserMgr
 * @see org.mycore.user.MCRUser
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserContact
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
   * Construtor. All address and contact attributes are passed as Strings
   */
  public MCRUserContact(String salutation, String firstname, String lastname,
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
   * This method returns the user contact information as a JDOM Element. This output
   * is used by the corresponding user object to create a full XML representation
   * of the user.
   *
   * @returns
   *   JDOM Element including data fields of this class
   */
  public Element toJDOMElement() throws Exception
  {
    Element address     = new Element("user.contact");
    Element Salutation  = new Element("contact.salutation").setText(salutation);
    Element Firstname   = new Element("contact.firstname").setText(firstname);
    Element Lastname    = new Element("contact.lastname").setText(lastname);
    Element Street      = new Element("contact.street").setText(street);
    Element City        = new Element("contact.city").setText(city);
    Element Postalcode  = new Element("contact.postalcode").setText(postalcode);
    Element Country     = new Element("contact.country").setText(country);
    Element Institution = new Element("contact.institution").setText(institution);
    Element Faculty     = new Element("contact.faculty").setText(faculty);
    Element Department  = new Element("contact.department").setText(department);
    Element Institute   = new Element("contact.institute").setText(institute);
    Element Telephone   = new Element("contact.telephone").setText(telephone);
    Element Fax         = new Element("contact.fax").setText(fax);
    Element Email       = new Element("contact.email").setText(email);
    Element Cellphone   = new Element("contact.cellphone").setText(cellphone);

    // Aggregate address element
    address.addContent(Salutation)
           .addContent(Firstname)
           .addContent(Lastname)
           .addContent(Street)
           .addContent(City)
           .addContent(Postalcode)
           .addContent(Country)
           .addContent(Institution)
           .addContent(Faculty)
           .addContent(Department)
           .addContent(Institute)
           .addContent(Telephone)
           .addContent(Fax)
           .addContent(Email)
           .addContent(Cellphone);

    return address;
  }

  /**
   * This helper method replaces null with an empty string and trims whitespace from
   * non-null strings.
   */
  private static String trim(String s)
  { return (s != null) ? s.trim() : ""; }
}
