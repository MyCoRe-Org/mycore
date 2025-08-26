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

package org.mycore.resource.filter;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.common.MCRResourceTracer;
import org.mycore.resource.common.MCRResourceUtils;
import org.mycore.resource.hint.MCRResourceHintKeys;

/**
 * A {@link MCRConfigDirLibraryResourceFilter} is a {@link MCRResourceFilter} that checks if a resource candidate
 * is a resource from a JAR file placed in the <code>/lib</code> directory in the config directory.
 * To decide weather such resources are retained or ignored, a {@link MCRResourceFilterMode} value is used.
 * <p>
 * It uses the config directory hinted at by {@link MCRResourceHintKeys#CONFIG_DIR}, if present.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRUrlPrefixResourceFilterBase#MODE_KEY} can be used to
 * specify the mode to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.filter.MCRConfigDirLibraryResourceFilter
 * [...].Mode=MUST_MATCH
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRConfigDirLibraryResourceFilter.Factory.class)
public final class MCRConfigDirLibraryResourceFilter extends MCRUrlPrefixResourceFilterBase {

    public MCRConfigDirLibraryResourceFilter(MCRResourceFilterMode mode) {
        super(mode);
    }

    @Override
    protected Optional<String> getPrefix(MCRHints hints, MCRResourceTracer tracer) {
        return hints.get(MCRResourceHintKeys.CONFIG_DIR).map(configDir -> getPrefix(configDir, tracer));
    }

    private String getPrefix(Path configDir, MCRResourceTracer tracer) {
        String prefix = "jar:" + MCRResourceUtils.toFileUrl(configDir) + "lib/";
        tracer.trace(() -> "Looking for config dir library prefix: " + prefix);
        return prefix;
    }

    public static class Factory implements Supplier<MCRConfigDirLibraryResourceFilter> {

        @MCRProperty(name = MODE_KEY, defaultName = "MCR.Resource.Filter.Default.ConfigDirLibrary.Mode")
        public String mode;

        @Override
        public MCRConfigDirLibraryResourceFilter get() {
            MCRResourceFilterMode mode = MCRResourceFilterMode.valueOf(this.mode);
            return new MCRConfigDirLibraryResourceFilter(mode);
        }

    }

}
