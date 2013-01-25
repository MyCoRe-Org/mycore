/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.migration21_22.user;

import org.apache.log4j.Logger;

/**
 * Instances of this class store contact information of MyCoRe users.
 * MCRUserContact is part of the MyCoRe user component and must not be used
 * directly by other components. MCRUserContact is aggregated by MCRUser and all
 * user objects are managed by the user manager (the instance of the singleton
 * MCRUserMgr).
 * 
 * @see org.mycore.migration21_22.user.MCRUser
 * 
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRUserContact {
    private static Logger logger = Logger.getLogger(MCRUserContact.class.getName());

    /** The maximum length of the salutation string */
    public final static int salutation_len = 24;

    /** The maximum length of the firstname string */
    public final static int firstname_len = 64;

    /** The maximum length of the lastname string */
    public final static int lastname_len = 32;

    /** The maximum length of the street string */
    public final static int street_len = 64;

    /** The maximum length of the city string */
    public final static int city_len = 32;

    /** The maximum length of the postalcode string */
    public final static int postalcode_len = 32;

    /** The maximum length of the country string */
    public final static int country_len = 32;

    /** The maximum length of the state string */
    public final static int state_len = 32;

    /** The maximum length of the institution string */
    public final static int institution_len = 64;

    /** The maximum length of the faculty string */
    public final static int faculty_len = 64;

    /** The maximum length of the department string */
    public final static int department_len = 64;

    /** The maximum length of the institute string */
    public final static int institute_len = 64;

    /** The maximum length of the telephone string */
    public final static int telephone_len = 32;

    /** The maximum length of the fax string */
    public final static int fax_len = 32;

    /** The maximum length of the email string */
    public final static int email_len = 64;

    /** The maximum length of the cellphone string */
    public final static int cellphone_len = 32;

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
     */
    public MCRUserContact() {
        init();
    }

    /**
     * Constructor. All address and contact attributes are passed as Strings
     */
    public MCRUserContact(String salutation, String firstname, String lastname, String street, String city, String postalcode,
            String country, String state, String institution, String faculty, String department, String institute, String telephone,
            String fax, String email, String cellphone) {
        init();
        this.salutation = MCRUserObject.trim(salutation, salutation_len);
        this.firstname = MCRUserObject.trim(firstname, firstname_len);
        this.lastname = MCRUserObject.trim(lastname, lastname_len);
        this.street = MCRUserObject.trim(street, street_len);
        this.city = MCRUserObject.trim(city, city_len);
        this.postalcode = MCRUserObject.trim(postalcode, postalcode_len);
        this.country = MCRUserObject.trim(country, country_len);
        this.state = MCRUserObject.trim(state, state_len);
        this.institution = MCRUserObject.trim(institution, institution_len);
        this.faculty = MCRUserObject.trim(faculty, faculty_len);
        this.department = MCRUserObject.trim(department, department_len);
        this.institute = MCRUserObject.trim(institute, institute_len);
        this.telephone = MCRUserObject.trim(telephone, telephone_len);
        this.fax = MCRUserObject.trim(fax, fax_len);
        this.email = MCRUserObject.trim(email, email_len);
        this.cellphone = MCRUserObject.trim(cellphone, cellphone_len);
    }

    @Override
    public Object clone() {
        return new MCRUserContact(salutation, firstname, lastname, street, city, postalcode, country, state, institution, faculty,
                department, institute, telephone, fax, email, cellphone);
    }

    /**
     * Constructor. All address and contact attributes are provided as a JDOM
     * Element.
     * 
     * @param elm
     *            the JDOM Element
     */
    public MCRUserContact(org.jdom2.Element elm) {
        init();
        setFromJDOMElement(elm);
    }

    /**
     * All address and contact attributes are passed as a JDOM Element.
     * 
     * @param elm
     *            the JDOM Element
     */
    public final void setFromJDOMElement(org.jdom2.Element elm) {
        if (elm == null) {
            return;
        }

        salutation = MCRUserObject.trim(elm.getChildTextTrim("contact.salutation"), salutation_len);
        firstname = MCRUserObject.trim(elm.getChildTextTrim("contact.firstname"), firstname_len);
        lastname = MCRUserObject.trim(elm.getChildTextTrim("contact.lastname"), lastname_len);
        street = MCRUserObject.trim(elm.getChildTextTrim("contact.street"), street_len);
        city = MCRUserObject.trim(elm.getChildTextTrim("contact.city"), city_len);
        postalcode = MCRUserObject.trim(elm.getChildTextTrim("contact.postalcode"), postalcode_len);
        country = MCRUserObject.trim(elm.getChildTextTrim("contact.country"), country_len);
        state = MCRUserObject.trim(elm.getChildTextTrim("contact.state"), state_len);
        institution = MCRUserObject.trim(elm.getChildTextTrim("contact.institution"), institution_len);
        faculty = MCRUserObject.trim(elm.getChildTextTrim("contact.faculty"), faculty_len);
        department = MCRUserObject.trim(elm.getChildTextTrim("contact.department"), department_len);
        institute = MCRUserObject.trim(elm.getChildTextTrim("contact.institute"), institute_len);
        telephone = MCRUserObject.trim(elm.getChildTextTrim("contact.telephone"), telephone_len);
        fax = MCRUserObject.trim(elm.getChildTextTrim("contact.fax"), fax_len);
        email = MCRUserObject.trim(elm.getChildTextTrim("contact.email"), email_len);
        cellphone = MCRUserObject.trim(elm.getChildTextTrim("contact.cellphone"), cellphone_len);
    }

    /**
     * This method initializes this object with empty attributes.
     */
    private void init() {
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
    public final String getSalutation() {
        return salutation;
    }

    public final String getFirstName() {
        return firstname;
    }

    public final String getLastName() {
        return lastname;
    }

    public final String getStreet() {
        return street;
    }

    public final String getCity() {
        return city;
    }

    public final String getPostalCode() {
        return postalcode;
    }

    public final String getCountry() {
        return country;
    }

    public final String getState() {
        return state;
    }

    public final String getInstitution() {
        return institution;
    }

    public final String getFaculty() {
        return faculty;
    }

    public final String getDepartment() {
        return department;
    }

    public final String getInstitute() {
        return institute;
    }

    public final String getTelephone() {
        return telephone;
    }

    public final String getFax() {
        return fax;
    }

    public final String getEmail() {
        return email;
    }

    public final String getCellphone() {
        return cellphone;
    }

    public final void setSalutation(String v) {
        salutation = v;
    }

    public final void setFirstName(String v) {
        firstname = v;
    }

    public final void setLastName(String v) {
        lastname = v;
    }

    public final void setStreet(String v) {
        street = v;
    }

    public final void setCity(String v) {
        city = v;
    }

    public final void setPostalCode(String v) {
        postalcode = v;
    }

    public final void setCountry(String v) {
        country = v;
    }

    public final void setState(String v) {
        state = v;
    }

    public final void setInstitution(String v) {
        institution = v;
    }

    public final void setFaculty(String v) {
        faculty = v;
    }

    public final void setDepartment(String v) {
        department = v;
    }

    public final void setInstitute(String v) {
        institute = v;
    }

    public final void setTelephone(String v) {
        telephone = v;
    }

    public final void setFax(String v) {
        fax = v;
    }

    public final void setEmail(String v) {
        email = v;
    }

    public final void setCellphone(String v) {
        cellphone = v;
    }

    /**
     * This method returns the user contact information as a JDOM Element. This
     * output is used by the corresponding user object to create a full XML
     * representation of the user.
     * 
     * @return JDOM Element including data fields of this class
     */
    public final org.jdom2.Element toJDOMElement() {
        org.jdom2.Element address = new org.jdom2.Element("user.contact");
        org.jdom2.Element Salutation = new org.jdom2.Element("contact.salutation").setText(salutation);
        org.jdom2.Element Firstname = new org.jdom2.Element("contact.firstname").setText(firstname);
        org.jdom2.Element Lastname = new org.jdom2.Element("contact.lastname").setText(lastname);
        org.jdom2.Element Street = new org.jdom2.Element("contact.street").setText(street);
        org.jdom2.Element City = new org.jdom2.Element("contact.city").setText(city);
        org.jdom2.Element Postalcode = new org.jdom2.Element("contact.postalcode").setText(postalcode);
        org.jdom2.Element Country = new org.jdom2.Element("contact.country").setText(country);
        org.jdom2.Element State = new org.jdom2.Element("contact.state").setText(state);
        org.jdom2.Element Institution = new org.jdom2.Element("contact.institution").setText(institution);
        org.jdom2.Element Faculty = new org.jdom2.Element("contact.faculty").setText(faculty);
        org.jdom2.Element Department = new org.jdom2.Element("contact.department").setText(department);
        org.jdom2.Element Institute = new org.jdom2.Element("contact.institute").setText(institute);
        org.jdom2.Element Telephone = new org.jdom2.Element("contact.telephone").setText(telephone);
        org.jdom2.Element Fax = new org.jdom2.Element("contact.fax").setText(fax);
        org.jdom2.Element Email = new org.jdom2.Element("contact.email").setText(email);
        org.jdom2.Element Cellphone = new org.jdom2.Element("contact.cellphone").setText(cellphone);

        // Aggregate address element
        address.addContent(Salutation).addContent(Firstname).addContent(Lastname).addContent(Street).addContent(City)
                .addContent(Postalcode).addContent(Country).addContent(State).addContent(Institution).addContent(Faculty).addContent(
                        Department).addContent(Institute).addContent(Telephone).addContent(Fax).addContent(Email).addContent(Cellphone);

        return address;
    }

    /**
     * This method prints the attributes of this object when in debug mode.
     */
    public final void debug() {
        logger.debug("Salutation      : " + salutation);
        logger.debug("Firstname       : " + firstname);
        logger.debug("Lastname        : " + lastname);
        logger.debug("Street          : " + street);
        logger.debug("City            : " + city);
        logger.debug("Postalcode      : " + postalcode);
        logger.debug("Country         : " + country);
        logger.debug("State           : " + state);
        logger.debug("Institution     : " + institution);
        logger.debug("Faculty         : " + faculty);
        logger.debug("Department      : " + department);
        logger.debug("Institute       : " + institute);
        logger.debug("Telephone       : " + telephone);
        logger.debug("Fax             : " + fax);
        logger.debug("Email           : " + email);
        logger.debug("Cellphone       : " + cellphone);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MCRUserContact)) {
            return false;
        }
        MCRUserContact uc = (MCRUserContact) obj;
        if (this == uc) {
            return true;
        }
        if (hashCode() != hashCode()) {
            //acording to the hashCode() contract
            return false;
        }
        return fastEquals(uc);
    }

    private boolean fastEquals(MCRUserContact uc) {
        return cellphone == uc.cellphone && (city == uc.city || city.equals(uc.city))
                && (country == uc.country || country.equals(uc.country))
                && (department == uc.department || department.equals(uc.department)) && (email == uc.email || email.equals(uc.email))
                && (faculty == uc.faculty || faculty.equals(uc.faculty)) && (fax == uc.fax || fax.equals(uc.fax))
                && (firstname == uc.firstname || firstname.equals(uc.firstname))
                && (institute == uc.institute || institute.equals(uc.institute))
                && (institution == uc.institution || institution.equals(uc.institution))
                && (lastname == uc.lastname || lastname.equals(uc.lastname))
                && (postalcode == uc.postalcode || postalcode.equals(uc.postalcode))
                && (salutation == uc.salutation || salutation.equals(uc.salutation)) && (state == uc.state || state.equals(uc.state))
                && (street == uc.street || street.equals(uc.street)) && (telephone == uc.telephone || telephone.equals(uc.telephone));
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + cellphone.hashCode();
        result = 37 * result + city.hashCode();
        result = 37 * result + department.hashCode();
        result = 37 * result + email.hashCode();
        result = 37 * result + faculty.hashCode();
        result = 37 * result + fax.hashCode();
        result = 37 * result + firstname.hashCode();
        result = 37 * result + institute.hashCode();
        result = 37 * result + institution.hashCode();
        result = 37 * result + lastname.hashCode();
        result = 37 * result + postalcode.hashCode();
        result = 37 * result + salutation.hashCode();
        result = 37 * result + state.hashCode();
        result = 37 * result + street.hashCode();
        result = 37 * result + telephone.hashCode();
        return result;
    }
}
