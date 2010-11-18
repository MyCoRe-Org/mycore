/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConstants;

/**
 * This class provides input validation methods for editor data.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRInputValidator {
    /** Template stylesheet for checking XSL conditions * */
    private Document stylesheet = null;

    /** XSL transformer factory * */
    private TransformerFactory factory = null;

    /** Creates a new, reusable input validator * */
    private MCRInputValidator() {
        stylesheet = prepareStylesheet();
        factory = TransformerFactory.newInstance();
    }

    private static MCRInputValidator singleton;

    public static synchronized MCRInputValidator instance() {
        if (singleton == null) {
            singleton = new MCRInputValidator();
        }

        return singleton;
    }

    /** Cache of reusable stylesheets for checking XSL conditions * */
    private MCRCache xslcondCache = new MCRCache(20, "InputValidator XSL conditions");

    private boolean validateXSLCondition(Document xml, String condition) {
        Source xmlsrc = new JDOMSource(xml);

        Document xsl = (Document) xslcondCache.get(condition);

        if (xsl == null) {
            xsl = (Document) stylesheet.clone();

            Element when = xsl.getRootElement().getChild("template", MCRConstants.XSL_NAMESPACE).getChild("choose", MCRConstants.XSL_NAMESPACE).getChild("when", MCRConstants.XSL_NAMESPACE);
            when.setAttribute("test", condition);
            xslcondCache.put(condition, xsl);
        }

        try {
            Transformer transformer = factory.newTransformer(new JDOMSource(xsl));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(xmlsrc, new StreamResult(out));
            out.close();

            return "t".equals(out.toString("UTF-8"));
        } catch (TransformerConfigurationException e) {
            String msg = "Could not build XSL transformer";
            throw new org.mycore.common.MCRConfigurationException(msg, e);
        } catch (UnsupportedEncodingException e) {
            String msg = "UTF-8 encoding seems not to be supported?";
            throw new org.mycore.common.MCRConfigurationException(msg, e);
        } catch (TransformerException e) {
            String msg = "Probably syntax error in this XSL condition: " + condition;
            throw new org.mycore.common.MCRConfigurationException(msg, e);
        } catch (IOException e) {
            String msg = "IOException in memory, this should never happen";
            throw new org.mycore.common.MCRConfigurationException(msg, e);
        }
    }

    public boolean validateXSLCondition(Element input, String condition) {
        if (input == null)
            return true;
        Document xml = new Document((Element) input.clone());
        return validateXSLCondition(xml, condition);
    }

    /**
     * Prepares a template stylesheet that is used for checking XSL conditions *
     */
    private synchronized Document prepareStylesheet() {
        Element stylesheet = new Element("stylesheet").setAttribute("version", "1.0");
        stylesheet.setNamespace(MCRConstants.XSL_NAMESPACE);

        for (Namespace ns : MCRConstants.getStandardNamespaces()) {
            if (!ns.equals(MCRConstants.XSL_NAMESPACE)) {
                stylesheet.addNamespaceDeclaration(ns);
            }
        }

        Element output = new Element("output", MCRConstants.XSL_NAMESPACE);
        output.setAttribute("method", "text");
        stylesheet.addContent(output);

        Element template = new Element("template", MCRConstants.XSL_NAMESPACE).setAttribute("match", "/*");
        stylesheet.addContent(template);

        Element choose = new Element("choose", MCRConstants.XSL_NAMESPACE);
        template.addContent(choose);

        Element when = new Element("when", MCRConstants.XSL_NAMESPACE);
        when.addContent("t");

        Element otherwise = new Element("otherwise", MCRConstants.XSL_NAMESPACE);
        otherwise.addContent("f");
        choose.addContent(when).addContent(otherwise);

        return new Document(stylesheet);
    }
}
