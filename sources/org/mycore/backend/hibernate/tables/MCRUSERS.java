/*
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
 */

package org.mycore.backend.hibernate.tables;

import java.sql.Timestamp;

public class MCRUSERS{
    
    private MCRUSERSPK key;
    private String creator;
    private Timestamp creationdate;
    private Timestamp modifieddate;
    private String description;
    private String passwd;
    private String enabled;
    private String upd;
    private String salutation;
    private String firstname;
    private String lastname;
    private String street;
    private String city;
    private String postalcode;
    private String country;
    private String state;
    private String institution;
    private String faculty;
    private String department;
    private String institute;
    private String telephone;
    private String fax;
    private String email;
    private String cellphone;
    private String primgroup;
    
    public MCRUSERS(){
        this.key = new MCRUSERSPK();
    }
    
    public MCRUSERS(int numid, String uid){
        this.key = new MCRUSERSPK();
        key.setNumid(numid);
        key.setUid(uid);
    }

    /**
    * @hibernate.property
    * column="Primary Key"
    * not-null="true"
    * update="true"
    */
    public MCRUSERSPK getKey() {
        return key;
    }
    public void setKey(MCRUSERSPK key) {
        this.key = key;
    }
    
    /**
     * @hibernate.property
     * column="NUMID"
     * not-null="true"
     * update="true"
     */
    public int getNumid() {
        return key.getNumid();
    }
    public void setNumid(int numid) {
        key.setNumid(numid);
    }
    
    /**
     * @hibernate.property
     * column="UID"
     * not-null="true"
     * update="true"
     */
    public String getUid() {
        return key.getUid();
    }
    public void setUid(String uid) {
        key.setUid(uid);
    }
    
    /**
    * @hibernate.property
    * column="CREATOR"
    * not-null="true"
    * update="true"
    */
    public String getCreator() {
        return creator;
    }
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
    * @hibernate.property
    * column="CREATIONDATE"
    * not-null="true"
    * update="true"
    */
    public Timestamp getCreationdate() {
        return creationdate;
    }
    public void setCreationdate(Timestamp creationdate) {
        this.creationdate = creationdate;
    }

    /**
    * @hibernate.property
    * column="MODIFIEDDATE"
    * not-null="true"
    * update="true"
    */
    public Timestamp getModifieddate() {
        return modifieddate;
    }
    public void setModifieddate(Timestamp modifieddate) {
        this.modifieddate = modifieddate;
    }

    /**
    * @hibernate.property
    * column="DESCRIPTION"
    * not-null="true"
    * update="true"
    */
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    /**
    * @hibernate.property
    * column="PASSWD"
    * not-null="true"
    * update="true"
    */
    public String getPasswd() {
        return passwd;
    }
    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    /**
    * @hibernate.property
    * column="ENABLED"
    * not-null="true"
    * update="true"
    */
    public String getEnabled() {
        return enabled;
    }
    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    /**
    * @hibernate.property
    * column="UPD"
    * not-null="true"
    * update="true"
    */
    public String getUpd() {
        return upd;
    }
    public void setUpd(String upd) {
        this.upd = upd;
    }

    /**
    * @hibernate.property
    * column="SALUTATION"
    * not-null="true"
    * update="true"
    */
    public String getSalutation() {
        return salutation;
    }
    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    /**
    * @hibernate.property
    * column="FIRSTNAME"
    * not-null="true"
    * update="true"
    */
    public String getFirstname() {
        return firstname;
    }
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    /**
    * @hibernate.property
    * column="LASTNAME"
    * not-null="true"
    * update="true"
    */
    public String getLastname() {
        return lastname;
    }
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    /**
    * @hibernate.property
    * column="STREET"
    * not-null="true"
    * update="true"
    */
    public String getStreet() {
        return street;
    }
    public void setStreet(String street) {
        this.street = street;
    }

    /**
    * @hibernate.property
    * column="CITY"
    * not-null="true"
    * update="true"
    */
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }

    /**
    * @hibernate.property
    * column="POSTALCODE"
    * not-null="true"
    * update="true"
    */
    public String getPostalcode() {
        return postalcode;
    }
    public void setPostalcode(String postalcode) {
        this.postalcode = postalcode;
    }

    /**
    * @hibernate.property
    * column="COUNTRY"
    * not-null="true"
    * update="true"
    */
    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }

    /**
    * @hibernate.property
    * column="STATE"
    * not-null="true"
    * update="true"
    */
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }

    /**
    * @hibernate.property
    * column="INSTITUTION"
    * not-null="true"
    * update="true"
    */
    public String getInstitution() {
        return institution;
    }
    public void setInstitution(String institution) {
        this.institution = institution;
    }

    /**
    * @hibernate.property
    * column="FACULTY"
    * not-null="true"
    * update="true"
    */
    public String getFaculty() {
        return faculty;
    }
    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    /**
    * @hibernate.property
    * column="DEPARTMENT"
    * not-null="true"
    * update="true"
    */
    public String getDepartment() {
        return department;
    }
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
    * @hibernate.property
    * column="INSTITUTE"
    * not-null="true"
    * update="true"
    */
    public String getInstitute() {
        return institute;
    }
    public void setInstitute(String institute) {
        this.institute = institute;
    }

    /**
    * @hibernate.property
    * column="TELEPHONE"
    * not-null="true"
    * update="true"
    */
    public String getTelephone() {
        return telephone;
    }
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    /**
    * @hibernate.property
    * column="FAX"
    * not-null="true"
    * update="true"
    */
    public String getFax() {
        return fax;
    }
    public void setFax(String fax) {
        this.fax = fax;
    }

    /**
    * @hibernate.property
    * column="EMAIL"
    * not-null="true"
    * update="true"
    */
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    /**
    * @hibernate.property
    * column="CELLPHONE"
    * not-null="true"
    * update="true"
    */
    public String getCellphone() {
        return cellphone;
    }
    public void setCellphone(String cellphone) {
        this.cellphone = cellphone;
    }

    /**
    * @hibernate.property
    * column="PRIMGROUP"
    * not-null="true"
    * update="true"
    */
    public String getPrimgroup() {
        return primgroup;
    }
    public void setPrimgroup(String primgroup) {
        this.primgroup = primgroup;
    }
}
