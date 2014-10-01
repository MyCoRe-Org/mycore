/**
 * 
 */
package org.mycore.tools;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Content;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.frontend.servlets.MCRServlet;
import org.xml.sax.SAXParseException;

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
     * 
     * @param title the title of the section
     * @param xmlAsString xml string which is added to the section
     * @param lang the language of the section specified by a language key.
     * @return added section
     */
    public Element addSection(String title, String xmlAsString, String lang) throws IOException, SAXParseException {
        StringBuilder sb = new StringBuilder("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" ");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        sb.append("<html><head><meta content=\"text/html; charset=UTF-8\"/><title>temp</title></head><body>");
        sb.append(xmlAsString);
        sb.append("</body></html>");
        Document doc = MCRXMLParserFactory.getParser().parseXML(new MCRStringContent(sb.toString()));
        Element tmpRoot = doc.getRootElement();
        Element body = tmpRoot.getChild("body", Namespace.getNamespace("http://www.w3.org/1999/xhtml"));
        List<Content> bodyContent = new ArrayList<>();
        for (int i = 0; i < body.getContentSize(); i++) {
            Content content = body.getContent(i).detach();
            getRidOfXHTMLNamespace(content);
            bodyContent.add(content);
        }
        return this.addSection(title, bodyContent, lang);
    }

    /**
     * Adds a section to the MyCoRe webpage.
     * @param title the title of the section
     * @param content jdom element which is added to the section
     * @param lang the language of the section specified by a language key.
     * @return added section
     */
    public Element addSection(String title, Content content, String lang) {
        List<Content> contentList = new ArrayList<>(1);
        contentList.add(content);
        return addSection(title, contentList, lang);
    }

    /**
     * Adds a section to the MyCoRe webpage
     * @param title the title of the section
     * @param content list of content added to the section
     * @param lang the language of the section specified by a language key.
     * @return added section
     */
    public Element addSection(String title, List<Content> content, String lang) {
        Element section = new Element(XML_SECTION);
        if (lang != null) {
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
        if (meta == null) {
            meta = new Element(XML_META);
            this.xml.getRootElement().addContent(meta);
        }
        Element log = meta.getChild(XML_LOG);
        if (log == null) {
            log = new Element(XML_LOG);
            meta.addContent(log);
        }
        // update attributes
        if (editor != null) {
            log.setAttribute(XML_LASTEDITOR, editor);
        }
        if (labelPath != null) {
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

    /**
     * Remove xhtml namespace of all children.
     * 
     * @param e
     */
    private void getRidOfXHTMLNamespace(Content c) {
        if (!(c instanceof Element)) {
            return;
        }
        Element e = (Element) c;
        String xhtmlURI = "http://www.w3.org/1999/xhtml";
        if (e.getNamespace().getURI().equals(xhtmlURI)) {
            e.setNamespace(Namespace.NO_NAMESPACE);
        }
        Iterator<Element> iterator = e.getDescendants(new ElementFilter(Namespace.getNamespace(xhtmlURI))).iterator();
        while (iterator.hasNext()) {
            iterator.next().setNamespace(Namespace.NO_NAMESPACE);
        }
    }
}
