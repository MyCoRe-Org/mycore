/**
 * 
 */
package org.mycore.solr.experimental.payload.analyzers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * @author shermann
 *
 */
public class MCRXML2StringWithPayloadProvider {

    private static final XPathFactory XPATH_FACTORY = XPathFactory.instance();
    private List<Element> tokenList;

    /**
     * @param source
     * @throws FileNotFoundException
     */
    public MCRXML2StringWithPayloadProvider(File source) throws FileNotFoundException {
        this(new FileInputStream(source));
    }

    /**
     * @param source
     */
    public MCRXML2StringWithPayloadProvider(InputStream source) {
        try {
            createDocument(source);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param inputStream the InputStream to build the document from, stream is closed after method call 
     * @throws IOException
     */
    private void createDocument(InputStream inputStream) throws IOException {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document xmlSource = saxBuilder.build(inputStream);
            XPathExpression<Element> xp = XPATH_FACTORY.compile("//Word", Filters.element());
            tokenList = xp.evaluate(xmlSource);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            inputStream.close();
        }
    }

    /**
     * Method returns a {@link Document} that contains all the words with their payloads.
     * 
     * @return a {@link Document}
     */
    public Document getWordPayloadDocument() {
        Element payloads = new Element("payloads");
        Document payloadDocument = new Document(payloads);

        for (Element w : tokenList) {
            String tokenWithPayload = this.getTokenWithPayload(w);
            payloads.addContent(new Element("payload").setText(tokenWithPayload));
        }

        return payloadDocument;
    }

    /**
     * Method returns a String where each word is followed a payload information (separated by a |)
     * 
     * @return {@link String}
     * @throws IOException
     */
    public String getFlatDocument() throws IOException {
        StringBuilder b = new StringBuilder();

        for (Element w : tokenList) {
            String tokenWithPayload = this.getTokenWithPayload(w);
            b.append(tokenWithPayload);
        }

        return b.toString().trim();
    }

    private String getTokenWithPayload(Element w) {
        String text = w
            .getText()
            .replaceAll("\"", "")
            .replaceAll("'", "")
            .replaceAll("\\(", "")
            .replaceAll("\\)", "")
            .replaceAll(",", "")
            .replaceAll("[0-9]*", "")
            .replaceAll("\\.", "");

        if (text.trim().length() == 0) {
            return "";
        }

        String x = w.getAttributeValue("x");
        String y = w.getAttributeValue("y");
        String data = "x" + x + "y" + y;
        return (text + "|" + data + "\n");
    }
}
