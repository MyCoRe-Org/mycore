package org.mycore.frontend.editor.validation;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRUtils;

import com.google.common.base.Optional;

public class MCRXSLConditionTester {

    private static Logger LOGGER = Logger.getLogger(MCRXSLConditionTester.class);

    private String condition;

    public MCRXSLConditionTester(String condition) {
        this.condition = condition;
    }

    public boolean testCondition(Document xml) throws Exception {
        Document xsl = buildXSLStylesheet(condition);
        String output = transform(xml, xsl);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format("Condition: {0}Stylesheet:\n{1}\nInput:\n{2}\nResult: {3}", condition,
                MCRUtils.documentAsString(xsl), MCRUtils.documentAsString(xml), output));
        }
        return "true".equals(output);
    }

    private Document buildXSLStylesheet(String condition) {
        Element stylesheet = new Element("stylesheet").setAttribute("version", "1.0");
        stylesheet.setNamespace(MCRConstants.XSL_NAMESPACE);

        for (Namespace ns : MCRConstants.getStandardNamespaces()) {
            if (!ns.equals(MCRConstants.XSL_NAMESPACE)) {
                stylesheet.addNamespaceDeclaration(ns);
            }
        }

        Element output = new Element("output", MCRConstants.XSL_NAMESPACE);
        output.setAttribute("method", "text");
        output.setAttribute("encoding", "UTF-8");
        stylesheet.addContent(output);

        Element template = new Element("template", MCRConstants.XSL_NAMESPACE);
        template.setAttribute("match", "/*");
        stylesheet.addContent(template);

        Element choose = new Element("choose", MCRConstants.XSL_NAMESPACE);
        template.addContent(choose);

        Element when = new Element("when", MCRConstants.XSL_NAMESPACE);
        when.setAttribute("test", condition);
        when.addContent("true");
        choose.addContent(when);

        Element otherwise = new Element("otherwise", MCRConstants.XSL_NAMESPACE);
        otherwise.addContent("false");
        choose.addContent(otherwise);

        Document document = new Document(stylesheet);
        if (LOGGER.isDebugEnabled()) {
        }
        return document;
    }

    private String transform(Document input, Document xsl) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new JDOMSource(xsl));
        String encoding = Optional.fromNullable(transformer.getOutputProperty(OutputKeys.ENCODING)).or(
            StandardCharsets.UTF_8.name());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new JDOMSource(input), new StreamResult(out));
        out.close();
        return out.toString(encoding);
    }
}
