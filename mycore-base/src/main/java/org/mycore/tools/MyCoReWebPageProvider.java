/**
 * 
 */
package org.mycore.tools;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdom.Content;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * This class provides a simple way to dynamically create MyCoRe webpages. These pages might be rendered 
 * through the layout service.
 * 
 * <br/>Example:
 * <code>
 *  <pre>
 *   MyCoReWebPageProvider wp = new MyCoReWebPageProvider();
 *   wp.addSection("Section Title", "Section Text", MyCoReWebPageProvider.DE);
 *   Document xml = wp.getXML();
 *   
 *   //call the layout service of an {@link MCRServlet}
 *   getLayoutService().doLayout(job.getRequest(), job.getResponse(), xml);
 *  </pre>
 *
 *   
 * </code>
 * 
 * @author shermann
 */
public class MyCoReWebPageProvider {
    public static final String MYCORE_WEBPAGE = "MyCoReWebPage";

    /** German language key */
    public static final String EN = "en";

    /** English language key */
    public static final String DE = "de";

    private Document xml;

    public MyCoReWebPageProvider() {
        this.xml = new Document();
        this.xml.setDocType(new DocType(MYCORE_WEBPAGE));
        this.xml.setRootElement(new Element(MYCORE_WEBPAGE));
    }

    /**
     * Adds a section to the MyCoRe webpage.
     * @param title the title of the section
     * @param xmlAsText xml string which is added to the section
     * @param lang the language of the section specified by a language key.
     * @return added section
     */
    public Element addSection(String title, String xmlAsString, String lang) throws IOException, JDOMException {
        Content content = null;
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            StringReader reader = new StringReader(xmlAsString);
            Document doc = saxBuilder.build(reader);
            content = doc.getRootElement();
            content.detach();
        } catch(JDOMParseException jdomParseExc) {
            content = new Text(xmlAsString);
        }
        return this.addSection(title, content, lang);
    }

    /**
     * Adds a section to the MyCoRe webpage.
     * @param title the title of the section
     * @param content jdom element which is added to the section
     * @param lang the language of the section specified by a language key.
     * @return added section
     */
    public Element addSection(String title, Content content, String lang) {
        Element section = new Element("section");
        if(lang != null) {
            section.setAttribute("lang", lang, Namespace.XML_NAMESPACE);
        }
        if (title != null && !title.equals("")) {
            section.setAttribute("title", title);
        }
        section.addContent(content);
        this.xml.getRootElement().addContent(section);
        return section;
    }

    /**
     * Updates the meta element of the webpage.
     * 
     * @param editor last editor of webpage
     * @param labelPath path info
     */
    public void updateMeta(String editor, String labelPath) {
        // get meta & log element
        Element meta = this.xml.getRootElement().getChild("meta");
        if(meta == null) {
            meta = new Element("meta");
            this.xml.getRootElement().addContent(meta);
        }
        Element log = meta.getChild("log");
        if(log == null) {
            log = new Element("log");
            meta.addContent(log);
        }
        // update attributes
        if(editor != null) {
            log.setAttribute("lastEditor", editor);
        }
        if(labelPath != null) {
            log.setAttribute("labelPath", labelPath);
        }
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        log.setAttribute("date", dateFormat.format(date));
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        log.setAttribute("time", timeFormat.format(date));
    }

    /**
     * @return an xml document with root element is <code> &lt;MyCoReWebPage/&gt;</code> 
     */
    public Document getXML() {
        return this.xml;
    }
}
