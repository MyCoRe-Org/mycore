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

package org.mycore.frontend.xeditor.includes;

import java.util.Objects;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCREditorSessionStoreUtils;

/**
 * URI Resolver to handle xed:preload and xed:include.
 * Syntax:
 * 
 * xedInclude:editorSessionID:preload:uri(s)
 * xedInclude:editorSessionID:resolveURI:uri
 * xedInclude:editorSessionID:resolveID:refID
 * 
 * @author Frank L\U00FCtzenkirchen
 */
public class MCRIncludeResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger(MCRIncludeResolver.class);

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        LOGGER.debug(() -> "resolving " + href);

        StringTokenizer st = new StringTokenizer(href, ":");
        st.nextToken(); // skip leading "xedInclude:"

        String sessionID = st.nextToken();
        MCRIncludeHandler handler = getIncludeHandler(sessionID);

        String action = st.nextToken();

        if (Objects.equals("resolveID", action)) {
            String id = st.nextToken();
            Element resolved = handler.resolveID(id);
            if (resolved != null) {
                return new JDOMSource(resolved);
            }
        } else {
            String uri = st.nextToken("").substring(1); // remove leading ":"

            if (Objects.equals("preload", action)) {
                handler.preloadFromURIs(uri);
            } else if (Objects.equals("resolveURI", action)) {
                Element resolved = handler.resolveURI(uri);
                return new JDOMSource(resolved);
            }
        }
        return new StreamSource(); // fallback
    }

    private MCRIncludeHandler getIncludeHandler(String sessionID) {
        MCREditorSession session = MCREditorSessionStoreUtils.getSessionStore().getSession(sessionID);
        return session.getIncludeHandler();
    }
}
