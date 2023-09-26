package org.mycore.resource.filter;

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.hint.MCRResourceHintKeys;

/**
 * A {@link MCRConfigDirLibraryResourceFilter} is a {@link MCRResourceFilter} that checks if a resource
 * candidate is a resource from a JAR file placed in the <code>/lib</code> directory in the config directory.
 * <p>
 * It uses the config directory hinted at by {@link MCRResourceHintKeys#CONFIG_DIR}, if present.
 */
@MCRConfigurationProxy(proxyClass = MCRConfigDirLibraryResourceFilter.Factory.class)
public class MCRConfigDirLibraryResourceFilter extends MCRUrlPrefixResourceFilterBase {

    public MCRConfigDirLibraryResourceFilter(MCRResourceFilterMode mode) {
        super(mode);
    }

    @Override
    protected Optional<String> getPrefix(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.CONFIG_DIR).map(this::getPrefix);
    }

    private String getPrefix(File configDir) {
        String prefix = "jar:" + configDir.toURI() + "lib/";
        getLogger().debug("Working with config dir library prefix: {}", prefix);
        return prefix;
    }

    public static class Factory implements Supplier<MCRConfigDirLibraryResourceFilter> {

        @MCRProperty(name = "Mode", defaultName = "MCR.Resource.Filter.Default.ConfigDirLibrary.Mode")
        public String mode;

        @Override
        public MCRConfigDirLibraryResourceFilter get() {
            MCRResourceFilterMode mode = MCRResourceFilterMode.valueOf(this.mode);
            return new MCRConfigDirLibraryResourceFilter(mode);
        }

    }

}
