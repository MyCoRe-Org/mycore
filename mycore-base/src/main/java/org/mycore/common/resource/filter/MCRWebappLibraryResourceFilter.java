package org.mycore.common.resource.filter;

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.resource.hint.MCRResourceHintKeys;

/**
 * A {@link MCRConfigDirLibraryResourceFilter} is a {@link MCRResourceFilter} that checks if a resource
 * candidate is a resource from a JAR file placed in the WAR file.
 * <p>
 * It uses the webapp directory hinted at by {@link MCRResourceHintKeys#WEBAPP_DIR}, if present.
 */
@MCRConfigurationProxy(proxyClass = MCRWebappLibraryResourceFilter.Factory.class)
public class MCRWebappLibraryResourceFilter extends MCRUrlPrefixResourceFilterBase {

    public MCRWebappLibraryResourceFilter(MCRResourceFilterMode mode) {
        super(mode);
    }

    @Override
    protected Optional<String> getPrefix(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.WEBAPP_DIR).map(this::getPrefix);
    }

    private String getPrefix(File webappDir) {
        String prefix = "jar:" + webappDir.toURI() + "WEB-INF/lib/";
        getLogger().debug("Working with webapp library prefix: {}", prefix);
        return prefix;
    }

    public static class Factory implements Supplier<MCRWebappLibraryResourceFilter> {

        @MCRProperty(name = "Mode", defaultName = "MCR.Resource.Filter.Default.WebappLibrary.Mode")
        public String mode;

        @Override
        public MCRWebappLibraryResourceFilter get() {
            MCRResourceFilterMode mode = MCRResourceFilterMode.valueOf(this.mode);
            return new MCRWebappLibraryResourceFilter(mode);
        }

    }

}
