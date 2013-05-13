package org.mycore.services.acl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Properties;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.transform.JDOMSource;

public class MCRACLResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String target = href.substring(href.indexOf(":") + 1);
        String[] part = target.split(":");
        String method = part[0].toLowerCase();

        Properties p = new Properties();

        try {
            if (part.length >= 2) {
                parseProperties(p, part[1]);
            }
            if ("getruleeditor".equals(method)) {
                return new JDOMSource(MCRAclEditorStdImpl.getRuleEditor(p));
            } else if ("getpermeditor".equals(method)) {
                return new JDOMSource(MCRAclEditorStdImpl.getPermEditor(p));
            } else if ("getruleasitems".equals(method)) {
                return new JDOMSource(MCRAclEditorStdImpl.getRuleAsItems(p));
            }
        } catch (Exception exc) {
            throw new TransformerException(exc);
        }
        throw new TransformerException(new IllegalArgumentException("Unknown method " + method + " in uri " + href));
    }

    private void parseProperties(Properties p, String toParse) throws UnsupportedEncodingException {
        toParse = toParse.replaceAll("\\?", "");
        String[] propertyArray = toParse.split("&");
        for (String propertyString : propertyArray) {
            String[] property = propertyString.split("=");
            if (property.length == 2 && property[0].length() > 0 && property[1].length() > 0) {
                p.put(property[0], URLDecoder.decode(property[1], "UTF-8"));
            }
        }
    }

}
