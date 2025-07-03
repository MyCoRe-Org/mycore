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

package org.mycore.resource.selector;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.mycore.common.hint.MCRHints;
import org.mycore.resource.common.MCRResourceTracer;
import org.mycore.resource.hint.MCRResourceHintKeys;

import jakarta.servlet.ServletContext;

/**
 * A {@link MCRFirstServletLibraryResourceSelector} is a {@link MCRResourceSelector} that prioritizes
 * resources by library order.
 * <p>
 * To accomplish this as efficient as possible, it traverses the list of libraries returned by
 * {@link ServletContext#getAttribute(String)} for {@link ServletContext#ORDERED_LIBS} from first to last
 * until it finds a library that contains one of the resource candidates, which is selected. If no
 * library contains a resource candidate, no candidate is selected.
 * <p>
 * It uses the {@link ServletContext} hinted at by {@link MCRResourceHintKeys#SERVLET_CONTEXT}, if present.
 * <p>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.selector.MCRFirstServletLibraryJarResourceSelector
 * </code></pre>
 */
public final class MCRFirstServletLibraryResourceSelector extends MCRResourceSelectorBase {

    @Override
    protected List<URL> doSelect(List<URL> resourceUrls, MCRHints hints, MCRResourceTracer tracer) {
        for (String libraryJarName : librariesJarNames(hints)) {
            tracer.trace(() -> "Looking for library JAR infix /WEB-INF/lib/" + libraryJarName + "! ...");
            for (URL resourceUrl : resourceUrls) {
                tracer.trace(() -> "... in resource URL " + resourceUrl);
                if (matches(resourceUrl.toString(), libraryJarName)) {
                    tracer.trace(() -> "Found match, using library JAR " + libraryJarName);
                    return List.of(resourceUrl);
                }
            }
        }
        return List.of();
    }

    /**
     * Checks if a given resource URL
     * (for example: <code>jar:file:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib/library.jar!/foo/bar</code>)
     * matches a given library Jar name (for example: <code>library.jar</code>), i.e. if the given resource url
     * is a Jar URL and if the given resource URL contains the given library Jar name directly before the <code>!</code>
     * delimiter and if that library Jar name is directly proceeded by <code>/WEB-INF/lib/</code>.
     */
    private static boolean matches(String resourceUrl, String libraryJarName) {
        if (!resourceUrl.startsWith("jar:file:")) {
            return false;
        }
        int delimiterIndex = resourceUrl.indexOf('!');
        if (delimiterIndex == -1) {
            return false;
        }
        if (!containsBefore(resourceUrl, delimiterIndex, libraryJarName)) {
            return false;
        }
        if (!containsBefore(resourceUrl, delimiterIndex - libraryJarName.length(), "/WEB-INF/lib/")) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("PMD.ForLoopVariableCount")
    private static boolean containsBefore(String string, int index, String substring) {
        if (substring.length() > index) {
            return false;
        }
        for (int i = substring.length() - 1, j = index - 1; i > -1; i--, j--) {
            if (string.charAt(j) != substring.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private List<String> librariesJarNames(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.SERVLET_CONTEXT)
            .flatMap(this::getOrderedLibs)
            .orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    private Optional<List<String>> getOrderedLibs(ServletContext context) {
        return Optional.ofNullable((List<String>) context.getAttribute(ServletContext.ORDERED_LIBS));
    }

}
