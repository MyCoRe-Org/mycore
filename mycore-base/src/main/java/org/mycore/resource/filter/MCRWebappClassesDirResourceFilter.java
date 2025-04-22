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

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.hint.MCRResourceHintKeys;

/**
 * {@link MCRWebappClassesDirResourceFilter} is a {@link MCRResourceFilter} that checks if a resource candidate
 * is a resource from the <code>/WEB-INF/classes</code> directory inside the webapp directory.
 * To decide weather such resources are retained or ignored, a {@link MCRResourceFilterMode} value is used.
 * <p>
 * <em>Unless placed there manually, such resources originate from the <code>/WEB-INF/classes</code> directory inside
 * the WAR file. In a usual build, such resources originate from the <code>/src/main/resources</code> directory inside
 * the Maven project that creates the WAR file.</em>
 * <p>
 * It uses the webapp directory hinted at by {@link MCRResourceHintKeys#WEBAPP_DIR}, if present.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRUrlPrefixResourceFilterBase#MODE_KEY} can be used to
 * specify the mode to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.filter.MCRWebappClassesDirResourceFilter
 * [...].Mode=MUST_MATCH
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRWebappClassesDirResourceFilter.Factory.class)
public final class MCRWebappClassesDirResourceFilter extends MCRUrlPrefixResourceFilterBase {

    public MCRWebappClassesDirResourceFilter(MCRResourceFilterMode mode) {
        super(mode);
    }

    @Override
    protected Optional<String> getPrefix(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.WEBAPP_DIR).map(this::getPrefix);
    }

    private String getPrefix(File webappDir) {
        String prefix = webappDir.toURI() + "WEB-INF/classes/";
        logger.debug("Working with webapp resource prefix: {}", prefix);
        return prefix;
    }

    public static class Factory implements Supplier<MCRWebappClassesDirResourceFilter> {

        @MCRProperty(name = MODE_KEY, defaultName = "MCR.Resource.Filter.Default.WebappClassesDir.Mode")
        public String mode;

        @Override
        public MCRWebappClassesDirResourceFilter get() {
            MCRResourceFilterMode mode = MCRResourceFilterMode.valueOf(this.mode);
            return new MCRWebappClassesDirResourceFilter(mode);
        }

    }

}
