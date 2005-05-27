/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 *
 * You should have received a copy of the GNU General License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.backend.hibernate;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Date;

import org.mycore.user.MCRUser;

/**
 * Class which extends MCRUser with setters and getters in order to be usable for Hibernate.
 *
 * @see org.mycore.user.MCRUser
 *
 * @author Matthias Kramm
 */
class MCRUserExt extends MCRUser
{
    MCRUserExt() {
    }
    MCRUserExt(MCRUser u) {
	super(u);
    }
    int getNumid() {
        return numID;
    }
    void setNumid(int numid) {
        this.numID = numID;
    }
    String getUid() {
        return ID;
    }
    void setUid(String id) {
        this.ID = ID;
    }
    void setDescription(String description) {
        this.description = description;
    }
    String getPasswd() {
        return passwd;
    }
    void setPasswd(String passwd) {
        this.passwd = passwd;
    }
    String getEnabled() {
        return idEnabled?"true":"false";
    }
    void setEnabled(String enabled) {
        this.idEnabled = "true".equalsIgnoreCase(enabled);
    }
    String getUpd() {
        return updateAllowed?"true":"false";
    }
    void setUpd(String upd) {
        this.updateAllowed = "true".equalsIgnoreCase(upd);
    }
    String getSalutation() {
        return userContact.getSalutation();
    }
    void setSalutation(String salutation) {
        userContact.setSalutation(salutation);
    }
    String getFirstname() {
        return userContact.getFirstName();
    }
    void setFirstname(String firstname) {
        userContact.setFirstName(firstname);
    }
    String getLastname() {
        return userContact.getLastName();
    }
    void setLastname(String lastname) {
        userContact.setLastName(lastname);
    }
    String getStreet() {
        return userContact.getStreet();
    }
    void setStreet(String street) {
        userContact.setStreet(street);
    }
    String getCity() {
        return userContact.getCity();
    }
    void setCity(String city) {
        userContact.setCity(city);
    }
    String getPostalcode() {
        return userContact.getPostalCode();
    }
    void setPostalcode(String postalcode) {
        userContact.setPostalCode(postalcode);
    }
    String getCountry() {
        return userContact.getCountry();
    }
    void setCountry(String country) {
        userContact.setCountry(country);
    }
    String getState() {
        return userContact.getState();
    }
    void setState(String state) {
        userContact.setState(state);
    }
    String getInstitution() {
        return userContact.getInstitution();
    }
    void setInstitution(String institution) {
        userContact.setInstitution(institution);
    }
    String getFaculty() {
        return userContact.getFaculty();
    }
    void setFaculty(String faculty) {
        userContact.setFaculty(faculty);
    }
    String getDepartment() {
        return userContact.getDepartment();
    }
    void setDepartment(String department) {
        userContact.setDepartment(department);
    }
    String getInstitute() {
        return userContact.getInstitute();
    }
    void setInstitute(String institute) {
        userContact.setInstitute(institute);
    }
    String getTelephone() {
        return userContact.getTelephone();
    }
    void setTelephone(String telephone) {
        userContact.setTelephone(telephone);
    }
    String getFax() {
        return userContact.getFax();
    }
    void setFax(String fax) {
        userContact.setFax(fax);
    }
    String getEmail() {
        return userContact.getEmail();
    }
    void setEmail(String email) {
        userContact.setEmail(email);
    }
    String getCellphone() {
        return userContact.getCellphone();
    }
    void setCellphone(String cellphone) {
        userContact.setCellphone(cellphone);
    }
    String getPrimgroup() {
        return primaryGroupID;
    }
    void setPrimgroup(String primgroup) {
        this.primaryGroupID = primgroup;
    }
}
