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
            getLogger().debug("Comparing library {} ...", library);
            for (URL resourceUrl : resourceUrls) {
                getLogger().debug(" ... with resource URL {}", resourceUrl);
                if (resourceUrl.toString().contains(library)) {
                    getLogger().debug("Found match, using library {}", library);
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
