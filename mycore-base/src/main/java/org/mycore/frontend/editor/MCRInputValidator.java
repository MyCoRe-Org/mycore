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
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.frontend.editor.validation.MCRPairValidator;
import org.mycore.frontend.editor.validation.MCRValidatorBuilder;

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

            Element when = xsl.getRootElement().getChild("template", MCRConstants.XSL_NAMESPACE).getChild("choose",
                    MCRConstants.XSL_NAMESPACE).getChild("when", MCRConstants.XSL_NAMESPACE);
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
        if( input == null ) return true;
        Document xml = new Document((Element) input.clone());
        return validateXSLCondition(xml, condition);
    }

    /** Prepares a template stylesheet that is used for checking XSL conditions * */
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

    /** Cache of reusable DateFormat objects * */
    private MCRCache formatCache = new MCRCache(20, "InputValidator DateFormat objects");

    /**
     * Returns a reusable DateFormat object for the given format string. That
     * object may come from a cache.
     */
    private DateFormat getDateTimeFormat(String format) {
        DateFormat df = (DateFormat) formatCache.get(format);

        if (df == null) {
            df = new SimpleDateFormat(format);
            df.setLenient(false);
            formatCache.put(format, df);
        }

        return df;
    }

    /**
     * Compares two input fields using a comparison operator.
     * 
     * @param valueA
     *            the first input string to check
     * @param valueB
     *            the second input string to check
     * @param type
     *            one of "string", "integer", "decimal" or "datetime"
     * @param operator
     *            One of =, <, >, <=, >=, !=
     * @param format
     *            for datetime input, a java.text.SimpleDateFormat pattern; for
     *            decimal input, a ISO-639 language code
     * 
     * @return true if the compare result is true OR one of the input fields is
     *         empty OR one of the input fields is in wrong format.
     */
    public boolean compare(String valueA, String valueB, String operator, String type, String format) {
        try {
            if (valueA == null || valueA.trim().length() == 0) {
                return true;
            }

            if (valueB == null || valueB.trim().length() == 0) {
                return true;
            }

            if (type.equals("string") || type.equals("integer")) {
                MCRPairValidator validator = MCRValidatorBuilder.buildPredefinedCombinedPairValidator();
                validator.setProperty("type", type);
                validator.setProperty("operator", operator);
                return validator.isValidPair(valueA, valueB);
            } else if (type.equals("decimal")) {
                Locale locale = format == null ? Locale.getDefault() : new Locale(format);
                NumberFormat nf = NumberFormat.getNumberInstance(locale);
                double vA = nf.parse(valueA.trim()).doubleValue();
                double vB = nf.parse(valueB.trim()).doubleValue();

                if ("=".equals(operator)) {
                    return vA == vB;
                } else if ("<".equals(operator)) {
                    return vA < vB;
                } else if (">".equals(operator)) {
                    return vA > vB;
                } else if ("<=".equals(operator)) {
                    return vA <= vB;
                } else if (">=".equals(operator)) {
                    return vA >= vB;
                } else if ("!=".equals(operator)) {
                    return !(vA == vB);
                } else {
                    throw new MCRConfigurationException("Unknown compare operator: " + operator);
                }
            } else if (type.equals("datetime")) {
                DateFormat df = getDateTimeFormat(format);
                Date vA = df.parse(valueA.trim());
                Date vB = df.parse(valueB.trim());

                if ("=".equals(operator)) {
                    return vA.equals(vB);
                } else if ("<".equals(operator)) {
                    return vA.before(vB);
                } else if (">".equals(operator)) {
                    return vA.after(vB);
                } else if ("<=".equals(operator)) {
                    return vA.before(vB) || vA.equals(vB);
                } else if (">=".equals(operator)) {
                    return vA.after(vB) || vA.equals(vB);
                } else if ("!=".equals(operator)) {
                    return !vA.equals(vB);
                } else {
                    throw new MCRConfigurationException("Unknown compare operator: " + operator);
                }
            } else {
                throw new MCRConfigurationException("Unknown input data type: " + type);
            }
        } catch (ParseException ex) {
            return true;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    /**
     * Calls a "public static boolean" method in the given class and validates
     * the two values externally using the given method in that class.
     * 
     * @param clazz
     *            the name of the class that contains the validation method
     * @param method
     *            the name of the public static boolean method that should be
     *            called
     * @param value1
     *            the first value to validate
     * @param value2
     *            the second value to validate
     * 
     * @return true, if the two values validate
     */
    public boolean validateExternally(String clazz, String method, String value1, String value2) {
        Class[] argTypes = new Class[2];
        argTypes[0] = String.class;
        argTypes[1] = String.class;
        Object[] args = new Object[2];
        args[0] = value1;
        args[1] = value2;
        Object result = Boolean.FALSE;
        try {
            Method m = Class.forName(clazz).getMethod(method, argTypes);
            result = m.invoke(null, args);
        } catch (Exception ex) {
            String msg = "Exception while validating input using external method";
            throw new MCRException(msg, ex);
        }
        return ((Boolean) result).booleanValue();
    }

    /**
     * Calls a "public static boolean" method in the given class and validates
     * an XML element
     * 
     * @param clazz
     *            the name of the class that contains the validation method
     * @param method
     *            the name of the public static boolean method that should be
     *            called
     * @param elem
     *            the XML element to validate
     * 
     * @return true, if the XML element validates
     */
    public boolean validateExternally(String clazz, String method, Element elem) {
        Class[] argTypes = new Class[1];
        argTypes[0] = Element.class;
        Object[] args = new Object[1];
        args[0] = elem;
        Object result = Boolean.FALSE;
        try {
            Method m = Class.forName(clazz).getMethod(method, argTypes);
            result = m.invoke(null, args);
        } catch (Exception ex) {
            String msg = "Exception while validating input using external method";
            throw new MCRException(msg, ex);
        }
        return ((Boolean) result).booleanValue();
    }
}
