package org.mycore.datamodel.navigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class I18nItem implements NavigationItem {

    @XmlAttribute(name = "i18nKey")
    private String i18nKey;

    private HashMap<String, String> labelMap;

    public I18nItem() {
        this.labelMap = new HashMap<String, String>();
    }

    public String getI18n() {
        return i18nKey;
    }
    public void setI18n(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    public void addLabel(String language, String text) {
        this.labelMap.put(language, text);
    }
    public void removeLabel(String language) {
        this.labelMap.remove(language);
    }
    public boolean containsLabel(String language) {
        return this.labelMap.containsKey(language);
    }
    public String getLabel(String language) {
        return this.labelMap.get(language);
    }

    @XmlElement(name = "label")
    public Label[] getLabelArray() {
        List<Label> list = new ArrayList<Label>();
        for (Entry<String, String> entry : this.labelMap.entrySet()) {
            list.add(new Label(entry.getKey(), entry.getValue()));
        }
        return list.toArray(new Label[list.size()]);
    }
    public void setLabelArray(Label[] labelArray) {
        for(Label label : labelArray) {
            this.labelMap.put(label.getLanguage(), label.getText());
        }
    }

    public static class Label {
        @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
        private String language;
        @XmlValue
        private String text;
        public Label() {
            this(null, null);
        }
        public Label(String language, String text) {
            this.language = language;
            this.text = text;
        }
        public String getLanguage() {
            return language;
        }
        public String getText() {
            return text;
        }
    }
}
