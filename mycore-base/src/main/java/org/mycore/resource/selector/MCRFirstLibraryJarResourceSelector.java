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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.mycore.common.hint.MCRHints;
import org.mycore.resource.hint.MCRResourceHintKeys;

import jakarta.servlet.ServletContext;

/**
 * A {@link MCRFirstLibraryJarResourceSelector} is a {@link MCRResourceSelector} that prioritizes
 * resources by library order.
 * <p>
 * To accomplish this as efficient as possible, it traverses the list of libraries returned by
 * {@link ServletContext#getAttribute(String)} for {@link ServletContext#ORDERED_LIBS} from first to last
 * until it finds a library that contains one of the resource candidates, which is selected. If no
 * library contains a resource candidate, no candidate is selected.
 */
public class MCRFirstLibraryJarResourceSelector extends MCRResourceSelectorBase {

    @Override
    protected List<URL> doSelect(List<URL> resourceUrls, MCRHints hints) {
        for (String library : librariesByServletContextOrder(hints)) {
            logger.debug("Comparing library {} ...", library);
            for (URL resourceUrl : resourceUrls) {
                logger.debug(" ... with resource URL {}", resourceUrl);
                if (resourceUrl.toString().contains(library)) {
                    logger.debug("Found match, using library {}", library);
                    return Collections.singletonList(resourceUrl);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<String> librariesByServletContextOrder(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.SERVLET_CONTEXT)
            .flatMap(this::getOrderedLibs)
            .orElse(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    private Optional<List<String>> getOrderedLibs(ServletContext context) {
        return Optional.ofNullable((List<String>) context.getAttribute(ServletContext.ORDERED_LIBS));
    }

}
