/**
 *
 */
package org.mycore.saxon;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mycore.common.MCRSuppressWarning;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public final class MCRTestExtensions {

    private MCRTestExtensions() {
    }

    @MCRSuppressWarning("saxon")
    public static boolean isDate(Date d) {
        return true;
    }

    public static boolean isNone() {
        return true;
    }

    public static boolean isBoolean(Boolean b) {
        return true;
    }

    public static boolean isBool(boolean b) {
        return true;
    }

    public static boolean isInteger(Integer i) {
        return true;
    }

    public static boolean isInt(int i) {
        return true;
    }

    public static boolean isLong(Long l) {
        return true;
    }

    public static boolean islong(long l) {
        return true;
    }

    public static boolean isString(String s) {
        return true;
    }

    public static boolean isBigInteger(BigInteger bi) {
        return true;
    }

    public static boolean isBigDecimal(BigDecimal bd) {
        return true;
    }

    public static boolean isNode(Node n) {
        return true;
    }

    public static boolean isNodeList(NodeList n) {
        return true;
    }

    public static Boolean getBoolean() {
        return true;
    }

    public static boolean getBool() {
        return true;
    }

    public static Integer getInteger() {
        return 1;
    }

    public static int getInt() {
        return 1;
    }

    public static Long getLong() {
        return 1l;
    }

    public static long getlong() {
        return 1l;
    }

    public static String getString() {
        return "test";
    }

    public static BigInteger getBigInteger() {
        return BigInteger.ONE;
    }

    public static BigDecimal getBigDecimal() {
        return BigDecimal.ONE;
    }

    public static Node getNode() throws ParserConfigurationException, SAXException, IOException {
        Document xslDoc = getXSLDocument();
        return xslDoc.getDocumentElement();
    }

    public static NodeList getNodeList() throws ParserConfigurationException, SAXException, IOException {
        Document xslDoc = getXSLDocument();
        return xslDoc.getDocumentElement().getChildNodes();
    }

    private static Document getXSLDocument() throws ParserConfigurationException, SAXException, IOException {
        URL testXSL = MCRTestExtensions.class.getClassLoader().getResource("extensionTests.xsl");
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document xslDoc = documentBuilder.parse(testXSL.toString());
        return xslDoc;
    }

    public static String concat(String s1) {
        return s1;
    }

    public static String concat(String s1, String s2) {
        return s1 + s2;
    }

    public static String concat(String s1, String s2, String s3) {
        return s1 + s2 + s3;
    }
}
