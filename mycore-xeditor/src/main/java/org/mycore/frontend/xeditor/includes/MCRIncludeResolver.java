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

import java.util.Arrays;
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
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCREditorSessionStoreUtils;

/**
 * URI Resolver to handle xed:preload and xed:include.
 * Syntax:
 * 
 * xedInclude:[editorSessionID]:preload:[uri][,uri]...
 * xedInclude:[editorSessionID]:resolveURI:[uri]
 * xedInclude:[editorSessionID]:resolveID:[refID]
 * 
 * @author Frank L\U00FCtzenkirchen
 */
public class MCRIncludeResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger(MCRIncludeResolver.class);

    private MCRURICache cachedURIs = MCRURICache.obtainInstance();

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        LOGGER.debug(() -> "resolving " + href);

        StringTokenizer st = new StringTokenizer(href, ":");
        st.nextToken(); // skip leading "xedInclude:"

        String sessionID = st.nextToken();
        MCRElementCache elementCache = getElementCache(sessionID);

        String action = st.nextToken();
        Element resolved = null;

        if (Objects.equals("resolveID", action)) {
            String id = st.nextToken();
            LOGGER.debug(() -> "including component " + id);
            resolved = elementCache.get(id);
        } else {
            String uri = st.nextToken("").substring(1); // remove leading ":"

            if (Objects.equals("preload", action)) {
                preloadFromURIs(uri, elementCache);
            } else if (Objects.equals("resolveURI", action)) {
                resolved = resolveURI(uri, elementCache);
            }
        }

        return (resolved != null ? new JDOMSource(resolved) : new StreamSource());
    }

    private MCRElementCache getElementCache(String sessionID) {
        MCREditorSession session = MCREditorSessionStoreUtils.getSessionStore().getSession(sessionID);
        return session.getElementCache();
    }

    private Element resolveURI(String uri, MCRElementCache elementCache) {
        // first try the global URI cache
        Element resolved = cachedURIs.get(uri);
        
        // then try the element cache at transformation level
        if (resolved == null) {
            resolved = elementCache.get(uri);
        }
        
        // last, resolve the URI
        if (resolved == null) {
            resolved = MCRURIResolver.instance().resolve(uri);
        }

        if (resolved != null) {
            // offer to cache in global URI cache
            boolean newlyCached = cachedURIs.offer(uri, resolved);
            
            // otherwise, cache at transformation level
            if (!newlyCached) {
                elementCache.put(uri, resolved);
            }
        }

        return resolved;
    }

    private void preloadFromURIs(String uris, MCRElementCache elementCache) {
        MCRPreloadHandler handler = new MCRPreloadHandler(elementCache);

        Arrays.stream(uris.split(","))
            .filter(uri -> !uri.isBlank())
            .forEach(uri -> {
                LOGGER.debug(() -> "preloading " + uri);

                Element resolved = resolveURI(uri, elementCache);
                if (resolved != null) {
                    handler.handlePreloadedElements(resolved);
                }
            });
    }
}
