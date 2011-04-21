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
 * @author Matthias Eichner
 */
public class MyCoReWebPageProvider {

    /** German language key */
    public static final String EN = "en";

    /** English language key */
    public static final String DE = "de";

    public static final String XML_MYCORE_WEBPAGE = "MyCoReWebPage";
    public static final String XML_SECTION = "section";
    public static final String XML_LANG = "lang";
    public static final String XML_TITLE = "title";
    public static final String XML_META = "meta";
    public static final String XML_LOG = "log";
    public static final String XML_LASTEDITOR = "lastEditor";
    public static final String XML_LABELPATH = "labelPath";
    public static final String XML_DATE = "date";
    public static final String XML_TIME = "time";

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm";

    private Document xml;

    public MyCoReWebPageProvider() {
        this.xml = new Document();
        this.xml.setDocType(new DocType(XML_MYCORE_WEBPAGE));
        this.xml.setRootElement(new Element(XML_MYCORE_WEBPAGE));
    }

    /**
     * Adds a section to the MyCoRe webpage.
     * @param title the title of the section
     * @param xmlAsString xml string which is added to the section
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
        Element section = new Element(XML_SECTION);
        if(lang != null) {
            section.setAttribute(XML_LANG, lang, Namespace.XML_NAMESPACE);
        }
        if (title != null && !title.equals("")) {
            section.setAttribute(XML_TITLE, title);
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
        Element meta = this.xml.getRootElement().getChild(XML_META);
        if(meta == null) {
            meta = new Element(XML_META);
            this.xml.getRootElement().addContent(meta);
        }
        Element log = meta.getChild(XML_LOG);
        if(log == null) {
            log = new Element(XML_LOG);
            meta.addContent(log);
        }
        // update attributes
        if(editor != null) {
            log.setAttribute(XML_LASTEDITOR, editor);
        }
        if(labelPath != null) {
            log.setAttribute(XML_LABELPATH, labelPath);
        }
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        log.setAttribute(XML_DATE, dateFormat.format(date));
        SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
        log.setAttribute(XML_TIME, timeFormat.format(date));
    }

    /**
     * @return an xml document with root element is <code> &lt;MyCoReWebPage/&gt;</code> 
     */
    public Document getXML() {
        return this.xml;
    }
}
