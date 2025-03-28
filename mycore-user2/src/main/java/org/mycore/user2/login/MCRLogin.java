/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.user2.login;

import org.mycore.common.MCRUserInformation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@XmlRootElement(name = "login")
@XmlType(name = "user2-login")
@XmlAccessorType(XmlAccessType.FIELD)
public class MCRLogin extends org.mycore.frontend.support.MCRLogin {

    @XmlAttribute(required = true)
    String realm;

    @XmlAttribute
    String realmParameter;

    @XmlAttribute
    boolean loginFailed;

    @XmlElement
    String returnURL;

    @XmlElement
    String errorMessage;

    public MCRLogin() {
        super();
    }

    public MCRLogin(MCRUserInformation userInformation, String returnURL, String formAction) {
        super(userInformation, formAction);
        this.returnURL = returnURL;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRealmParameter() {
        return realmParameter;
    }

    public void setRealmParameter(String realmParameter) {
        this.realmParameter = realmParameter;
    }

    public boolean isLoginFailed() {
        return loginFailed;
    }

    public void setLoginFailed(boolean loginFailed) {
        this.loginFailed = loginFailed;
    }

    public String getReturnURL() {
        return returnURL;
    }

    public void setReturnURL(String returnURL) {
        this.returnURL = returnURL;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
