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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.MCRConfiguration;
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
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRUserContact
{
  private static Logger logger =
    Logger.getLogger(MCRUserContact.class.getName());
  private static MCRConfiguration config = null;

  private String salutation = "";
  private String firstname = "";
  private String lastname = "";
  private String street = "";
  private String city = "";
  private String postalcode = "";
  private String country = "";
  private String state = "";
  private String institution = "";
  private String faculty = "";
  private String department = "";
  private String institute = "";
  private String telephone = "";
  private String fax = "";
  private String email = "";
  private String cellphone = "";

  /**
   * Constructor for an empty object.
   **/
  public MCRUserContact()
    { init(); }

  /**
   * Constructor. All address and contact attributes are passed as Strings
   */
  public MCRUserContact(String salutation, String firstname, String lastname,
                        String street, String city, String postalcode,
                        String country, String state, String institution, 
                        String faculty, String department, String institute, 
                        String telephone, String fax, String email, 
                        String cellphone)
  {
    this.salutation  = trim(salutation);
    this.firstname   = trim(firstname);
    this.lastname    = trim(lastname);
    this.street      = trim(street);
    this.city        = trim(city);
    this.postalcode  = trim(postalcode);
    this.country     = trim(country);
    this.state       = trim(state);
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
   * Constructor. All address and contact attributes are passed in a
   * JDOM Element.
   * @param elm the JDOM Element
   */
  public MCRUserContact(org.jdom.Element elm)
    { 
    init(); 
    setFromJDOMElement(elm);
    }
 
  /**
   * All address and contact attributes are passed in a JDOM Element.
   * @param elm the JDOM Element
   */
  public void setFromJDOMElement(org.jdom.Element elm)
    {
    if (elm == null) { return; }
    this.salutation  = trim(elm.getChildTextTrim("contact.salutation"));
    this.firstname   = trim(elm.getChildTextTrim("contact.firstname"));
    this.lastname    = trim(elm.getChildTextTrim("contact.lastname"));
    this.street      = trim(elm.getChildTextTrim("contact.street"));
    this.city        = trim(elm.getChildTextTrim("contact.city"));
    this.postalcode  = trim(elm.getChildTextTrim("contact.postalcode"));
    this.country     = trim(elm.getChildTextTrim("contact.country"));
    this.state       = trim(elm.getChildTextTrim("contact.state"));
    this.institution = trim(elm.getChildTextTrim("contact.institution"));
    this.faculty     = trim(elm.getChildTextTrim("contact.faculty"));
    this.department  = trim(elm.getChildTextTrim("contact.faculty"));
    this.institute   = trim(elm.getChildTextTrim("contact.department"));
    this.telephone   = trim(elm.getChildTextTrim("contact.institute"));
    this.fax         = trim(elm.getChildTextTrim("contact.fax"));
    this.email       = trim(elm.getChildTextTrim("contact.email"));
    this.cellphone   = trim(elm.getChildTextTrim("contact.cellphone"));
    }

  /**
   * The method initialized this object with empty data.
   **/
  private final void init()
    {
    salutation = "";
    firstname = "";
    lastname = "";
    street = "";
    city = "";
    postalcode = "";
    country = "";
    state = "";
    institution = "";
    faculty = "";
    department = "";
    institute = "";
    telephone = "";
    fax = "";
    email = "";
    cellphone = "";
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

  public String getState()
  { return state; }

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
 **/
  public org.jdom.Element toJDOMElement()
  {
  org.jdom.Element address     = new org.jdom.Element("user.contact");
  org.jdom.Element Salutation  = 
    new org.jdom.Element("contact.salutation").setText(salutation);
  org.jdom.Element Firstname   = 
    new org.jdom.Element("contact.firstname").setText(firstname);
  org.jdom.Element Lastname    = 
    new org.jdom.Element("contact.lastname").setText(lastname);
  org.jdom.Element Street      = 
    new org.jdom.Element("contact.street").setText(street);
  org.jdom.Element City        = 
    new org.jdom.Element("contact.city").setText(city);
  org.jdom.Element Postalcode  = 
    new org.jdom.Element("contact.postalcode").setText(postalcode);
  org.jdom.Element Country     = 
    new org.jdom.Element("contact.country").setText(country);
  org.jdom.Element State       = 
    new org.jdom.Element("contact.state").setText(state);
  org.jdom.Element Institution = 
    new org.jdom.Element("contact.institution").setText(institution);
  org.jdom.Element Faculty     = 
    new org.jdom.Element("contact.faculty").setText(faculty);
  org.jdom.Element Department  = 
   new org.jdom.Element("contact.department").setText(department);
  org.jdom.Element Institute   = 
    new org.jdom.Element("contact.institute").setText(institute);
  org.jdom.Element Telephone   = 
    new org.jdom.Element("contact.telephone").setText(telephone);
  org.jdom.Element Fax         = 
    new org.jdom.Element("contact.fax").setText(fax);
  org.jdom.Element Email       = 
    new org.jdom.Element("contact.email").setText(email);
  org.jdom.Element Cellphone   = 
    new org.jdom.Element("contact.cellphone").setText(cellphone);
  // Aggregate address element
  address.addContent(Salutation)
         .addContent(Firstname)
         .addContent(Lastname)
         .addContent(Street)
         .addContent(City)
         .addContent(Postalcode)
         .addContent(Country)
         .addContent(State)
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
 * The method print a debug over thi data.
 **/
public void debug()
  {
  config = MCRConfiguration.instance();
  PropertyConfigurator.configure(config.getLoggingProperties());
  logger.debug("Salutation      : "+salutation);
  logger.debug("Firstname       : "+firstname);
  logger.debug("Lastname        : "+lastname);
  logger.debug("Street          : "+street);
  logger.debug("City            : "+city);
  logger.debug("Postalcode      : "+postalcode);
  logger.debug("Country         : "+country);
  logger.debug("State           : "+state);
  logger.debug("Institution     : "+institution);
  logger.debug("Faculty         : "+faculty);
  logger.debug("Department      : "+department);
  logger.debug("Institute       : "+institute);
  logger.debug("Telephone       : "+telephone);
  logger.debug("Fax             : "+fax);
  logger.debug("Email           : "+email);
  logger.debug("Cellphone       : "+cellphone);
  }

/**
 * This helper method replaces null with an empty string and trims whitespace 
 * from non-null strings.
 **/
private static String trim(String s)
  { return (s != null) ? s.trim() : ""; }
}
