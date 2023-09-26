package org.mycore.resource.filter;

import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;

/**
 * A {@link MCRLibraryResourceFilter} is a {@link MCRResourceFilter} that checks if a resource
 * candidate is a resource from a JAR file.
 */
@MCRConfigurationProxy(proxyClass = MCRLibraryResourceFilter.Factory.class)
public class MCRLibraryResourceFilter extends MCRUrlPrefixResourceFilterBase {

    public MCRLibraryResourceFilter(MCRResourceFilterMode mode) {
        super(mode);
    }

    @Override
    protected Optional<String> getPrefix(MCRHints hints) {
        return Optional.of("jar:");
    }

    public static class Factory implements Supplier<MCRLibraryResourceFilter> {

        @MCRProperty(name = "Mode", defaultName = "MCR.Resource.Filter.Default.Library.Mode")
        public String mode;

        @Override
        public MCRLibraryResourceFilter get() {
            MCRResourceFilterMode mode = MCRResourceFilterMode.valueOf(this.mode);
            return new MCRLibraryResourceFilter(mode);
        }

    }

}
