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

package org.mycore.common.content.transformer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.sax.TransformerHandler;

import org.jdom2.Content;
import org.jdom2.DefaultJDOMFactory;
import org.jdom2.Document;
import org.jdom2.JDOMFactory;
import org.jdom2.Text;
import org.jdom2.transform.JDOMResult;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Transforms XML content using a static XSL stylesheet.
 * The stylesheet is configured via
 * 
 * MCR.ContentTransformer.{ID}.Stylesheet
 * 
 * Resulting MCRContent holds XML.
 * Use {@link MCRXSLTransformer} if you want to produce non XML output.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRXSL2XMLTransformer extends MCRXSLTransformer {

    private static MCRCache<String, MCRXSL2XMLTransformer> INSTANCE_CACHE = new MCRCache<>(100,
        "MCRXSLTransformer instance cache");

    public MCRXSL2XMLTransformer() {
        super();
    }

    public MCRXSL2XMLTransformer(String... stylesheets) {
        super(stylesheets);
    }

    public static MCRXSL2XMLTransformer getInstance(String... stylesheets) {
        String key = stylesheets.length == 1 ? stylesheets[0] : Arrays.toString(stylesheets);
        MCRXSL2XMLTransformer instance = INSTANCE_CACHE.get(key);
        if (instance == null) {
            instance = new MCRXSL2XMLTransformer(stylesheets);
            INSTANCE_CACHE.put(key, instance);
        }
        return instance;
    }

    @Override
    protected MCRContent getTransformedContent(MCRContent source, XMLReader reader,
        TransformerHandler transformerHandler) throws IOException, SAXException {
        JDOMResult result = new JDOMResult();
        transformerHandler.setResult(result);
        // Parse the source XML, and send the parse events to the
        // TransformerHandler.
        reader.parse(source.getInputSource());
        Document resultDoc = getDocument(result);
        if (resultDoc == null) {
            throw new MCRConfigurationException("Stylesheets " + Arrays.asList(templateSources)
                + " does not return any content for " + source.getSystemId());
        }
        return new MCRJDOMContent(resultDoc);
    }

    private Document getDocument(JDOMResult result) {
        Document resultDoc = result.getDocument();
        if (resultDoc == null) {
            //Sometimes a transformation produces whitespace strings
            //JDOM would produce a empty document if it detects those
            //So we remove them, if they exists.
            List<Content> transformResult = result.getResult();
            int origSize = transformResult.size();
            Iterator<Content> iterator = transformResult.iterator();
            while (iterator.hasNext()) {
                Content content = iterator.next();
                if (content instanceof Text) {
                    String trimmedText = ((Text) content).getTextTrim();
                    if (trimmedText.length() == 0) {
                        iterator.remove();
                    }
                }
            }
            if (transformResult.size() < origSize) {
                JDOMFactory f = result.getFactory();
                if (f == null) {
                    f = new DefaultJDOMFactory();
                }
                resultDoc = f.document(null);
                resultDoc.setContent(transformResult);
            }
        }
        return resultDoc;
    }

    @Override
    protected String getDefaultExtension() {
        return "xml";
    }

    @Override
    public String getEncoding() {
        return MCRConstants.DEFAULT_ENCODING;
    }

}
