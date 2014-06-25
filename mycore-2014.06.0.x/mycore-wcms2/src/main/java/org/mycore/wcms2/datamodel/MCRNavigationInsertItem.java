package org.mycore.wcms2.datamodel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "insert")
@XmlAccessorType(XmlAccessType.NONE)
public class MCRNavigationInsertItem implements MCRNavigationBaseItem {

    @XmlAttribute
    private String uri;

    public String getURI() {
        return uri;
    }

    public void setURI(String uri) {
        this.uri = uri;
    }

}
