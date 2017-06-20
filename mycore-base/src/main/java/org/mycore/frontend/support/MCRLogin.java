package org.mycore.frontend.support;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;

@XmlRootElement(name = "login")
@XmlType(name = "base-login")
@XmlAccessorType(XmlAccessType.FIELD)
public class MCRLogin {

    @XmlAttribute(required = true)
    protected String user;

    @XmlAttribute(required = true)
    protected boolean guest;

    @XmlElement
    protected Form form;

    public MCRLogin() {
        super();
    }

    public MCRLogin(MCRUserInformation userInformation, String formAction) {
        this();
        this.user = userInformation.getUserID();
        this.guest = userInformation == MCRSystemUserInformation.getGuestInstance();
        this.form = new Form(formAction);
    }

    public static JAXBContext getContext() throws JAXBException {
        return JAXBContext.newInstance(MCRLogin.class);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isGuest() {
        return guest;
    }

    public void setGuest(boolean guest) {
        this.guest = guest;
    }

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    @XmlType
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Form {
        @XmlAttribute
        String action;

        @XmlElement
        List<InputField> input;

        public Form() {
            input = new ArrayList<>();
        }

        public Form(String formAction) {
            this();
            action = formAction;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public List<InputField> getInput() {
            return input;
        }

        public void setInput(List<InputField> input) {
            this.input = input;
        }
    }

    @XmlType
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InputField {
        @XmlAttribute
        String name;

        @XmlAttribute
        String value;

        @XmlAttribute
        String label;

        @XmlAttribute
        String placeholder;

        @XmlAttribute
        boolean isPassword;

        @XmlAttribute
        boolean isHidden;

        public InputField() {
        }

        public InputField(String name, String value, String label, String placeholder, boolean isPassword,
            boolean isHidden) {
            this();
            this.name = name;
            this.value = value;
            this.label = label;
            this.placeholder = placeholder;
            this.isPassword = isPassword;
            this.isHidden = isHidden;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
        }

        public boolean isHidden() {
            return isHidden;
        }

        public void setHidden(boolean isHidden) {
            this.isHidden = isHidden;
        }

        public boolean isPassword() {
            return isPassword;
        }

        public void setPassword(boolean isPassword) {
            this.isPassword = isPassword;
        }
    }

}
