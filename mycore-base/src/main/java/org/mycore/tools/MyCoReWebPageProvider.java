/**
 * 
 */
package org.mycore.tools;

import org.apache.log4j.Logger;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

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
    /** German language key */
    public static final String EN = "en";

    /** English language key */
    public static final String DE = "de";

    private static Logger LOGGER = Logger.getLogger(MyCoReWebPageProvider.class);

    private Document xml;

    public MyCoReWebPageProvider() {
        xml = new Document();
        xml.setDocType(new DocType("MyCoReWebPage"));
        xml.setRootElement(new Element("MyCoReWebPage"));
    }

    /**
     * Adds a section to the MyCoRe webpage.
     * @param title the title of the section
     * @param text the message contained within the section
     * @param lang the language of the section specified by a language key.
     */
    public void addSection(String title, String text, String lang) {
        if (text == null || lang == null || lang.length() != 2) {
            LOGGER.warn("Section could not be added as either \"text\" or \"lang\" is null");
            return;
        }
        Element section = new Element("section");
        section.setAttribute("lang", lang, Namespace.XML_NAMESPACE);

        if (title == null || title.length() == 0) {
            section.setAttribute("title", title);
        }

        Element paragraph = new Element("p");
        section.addContent(paragraph);
        paragraph.setText(text);
        xml.getRootElement().addContent(section);
    }

    /**
     * @return an xml document with root element is <code> &lt;MyCoReWebPage/&gt;</code> 
     */
    public Document getXML() {
        return this.xml;
    }
}
