/*
 *
 * $Revision$ $Date$
 *
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 *
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.resource.provider;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;

import io.github.classgraph.ClassGraph;

/**
 * A {@link MCRResourceProvider} implements a resource lookup strategy.
 */
public interface MCRResourceProvider {

    /**
     * Resolves a {@link MCRResourcePath} using the given hints.
     */
    Optional<URL> provide(MCRResourcePath path, MCRHints hints);

    /**
     * Resolves a {@link MCRResourcePath}, returning all alternatives (i.e. because one module
     * overrides a resource that is also provided by another module). Intended for introspective purposes only.
     */
    List<ProvidedUrl> provideAll(MCRResourcePath path, MCRHints hints);

    Set<PrefixStripper> prefixStrippers(MCRHints hints);

    /**
     * Returns a description of this {@link MCRResourceProvider}.
     */
    MCRTreeMessage compileDescription(Level level);

    final class ProvidedUrl {

        public final URL url;

        public final String origin;

        public ProvidedUrl(URL url, String origin) {
            this.url = url;
            this.origin = origin;
        }

        @Override
        public String toString() {
            return origin + ": " + url;
        }

    }

    interface PrefixStripper {

        Optional<MCRResourcePath> strip(URL url);

    }

    abstract class PrefixStripperBase implements PrefixStripper {

        @Override
        public final Optional<MCRResourcePath> strip(URL url) {
            return Optional.ofNullable(doStrip(url.toString())).flatMap(MCRResourcePath::ofPath);
        }

        protected abstract String doStrip(String value);

    }

    final class BaseDirPrefixStripper extends PrefixStripperBase {

        private final String prefix;

        public BaseDirPrefixStripper(File baseDir) {
            this.prefix = Objects.requireNonNull(baseDir).toURI().toString();
        }

        @Override
        public String doStrip(String value) {
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length());
            }
            return null;
        }

        @Override
        public String toString() {
            return prefix;
        }

        public static Set<PrefixStripper> ofClassLoader(ClassLoader classLoader) {
            Set<PrefixStripper> strippers = new LinkedHashSet<>();
            List<URI> classpath = new ClassGraph().overrideClassLoaders(classLoader).getClasspathURIs();
            classpath.forEach(uri -> {
                if (uri.getScheme().equals("file")) {
                    File file = new File(uri.getPath());
                    if (file.isDirectory()) {
                        strippers.add(new BaseDirPrefixStripper(file));
                    }
                }
            });
            return strippers;
        }

    }

    final class JarUrlPrefixStripper extends PrefixStripperBase {

        public static final PrefixStripper INSTANCE = new JarUrlPrefixStripper();

        public static final Set<PrefixStripper> INSTANCE_SET = Collections.singleton(INSTANCE);

        private JarUrlPrefixStripper() {
        }

        @Override
        public String doStrip(String value) {
            if (value.startsWith("jar:")) {
                int index = value.indexOf('!');
                if (-1 != index) {
                    return value.substring(index + 1);
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "jar:.*!";
        }

    }

}
