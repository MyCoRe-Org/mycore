/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jdom2.Content;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.mycore.frontend.servlets.MCRServlet;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * This class provides a simple way to dynamically create MyCoRe webpages. These pages might be rendered 
 * through the layout service.
 * 
 * <br>Example:
 *  <pre>
 *   MyCoReWebPageProvider wp = new MyCoReWebPageProvider();
 *   wp.addSection("Section Title", "Section Text", MyCoReWebPageProvider.DE);
 *   Document xml = wp.getXML();
 *   
 *   //call the layout service of an {@link MCRServlet}
 *   getLayoutService().doLayout(job.getRequest(), job.getResponse(), xml);
 *  </pre>
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
    public Element addSection(String title, String xmlAsString, String lang) throws IOException, SAXParseException,
        JDOMException {
        String sb = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<!DOCTYPE MyCoReWebPage PUBLIC \"-//MYCORE//DTD MYCOREWEBPAGE 1.0//DE\" "
            + "\"http://www.mycore.org/mycorewebpage.dtd\">" + "<MyCoReWebPage>" + xmlAsString + "</MyCoReWebPage>";
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setEntityResolver((publicId, systemId) -> {
            String resource = systemId.substring(systemId.lastIndexOf("/"));
            InputStream is = getClass().getResourceAsStream(resource);
            if (is == null) {
                throw new IOException(new FileNotFoundException("Unable to locate resource " + resource));
            }
            return new InputSource(is);
        });
        StringReader reader = new StringReader(sb);
        Document doc = saxBuilder.build(reader);
        return this.addSection(title, doc.getRootElement().cloneContent(), lang);
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
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ROOT);
        log.setAttribute(XML_DATE, dateFormat.format(date));
        SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT, Locale.ROOT);
        log.setAttribute(XML_TIME, timeFormat.format(date));
    }

    /**
     * @return an xml document with root element is <code> &lt;MyCoReWebPage/&gt;</code> 
     */
    public Document getXML() {
        return this.xml;
    }

}
