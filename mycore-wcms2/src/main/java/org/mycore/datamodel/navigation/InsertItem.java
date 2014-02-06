package org.mycore.datamodel.navigation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "insert")
@XmlAccessorType(XmlAccessType.NONE)
public class InsertItem implements NavigationItem {

    @XmlAttribute
    private String uri;

    public String getURI() {
        return uri;
    }

    public void setURI(String uri) {
        this.uri = uri;
    }

}
