/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.frontend.xeditor;

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRWrappedContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.xsl.MCRParameterCollector;

/**
 * @author Frank Lützenkirchen
 */
public class MCRXEditorTransformer {

    private final MCREditorSession editorSession;

    private final MCRParameterCollector transformationParameters;

    public MCRXEditorTransformer(MCREditorSession editorSession, MCRParameterCollector transformationParameters) {
        this.editorSession = editorSession;
        this.transformationParameters = transformationParameters;
    }

    public MCRContent transform(MCRContent editorSource) throws IOException {
        editorSession.getSubmission().clear();

        MCRTransformerHelper helper = new MCRTransformerHelper(editorSession);
        editorSession.setTransformerHelper(helper);

        MCRContentTransformer transformer = MCRContentTransformerFactory.getTransformer("xeditor");
        if (!(transformer instanceof MCRParameterizedTransformer)) {
            throw new MCRException("Xeditor needs parameterized MCRContentTransformer: " + transformer);
        }

        transformationParameters.setParameter("SessionID", editorSession.getID());

        MCRParameterizedTransformer parameterizedTransformer = (MCRParameterizedTransformer) transformer;
        MCRContent result = parameterizedTransformer.transform(editorSource, transformationParameters);
        if (result instanceof MCRWrappedContent wrappedContent
            && result.getClass().getName().contains(MCRXSLTransformer.class.getName())) {
            //lazy transformation make JUnit tests fail
            result = wrappedContent.getBaseContent();
        }

        editorSession.setTransformerHelper(null);

        Document xed = content2xml(result);

        editorSession.getValidator().setNewValidationRules(xed);

        return new MCRJDOMContent(xed);
    }

    private Document content2xml(MCRContent result) throws IOException {
        try {
            return result.asXML();
        } catch (JDOMException neverExpected) {
            throw new IOException(neverExpected);
        }
    }
}
