/**
 * 
 */
package org.mycore.user2.login;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.mycore.common.MCRUserInformation;

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
