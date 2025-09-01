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

package org.mycore.sass;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;

import de.larsgrefer.sass.embedded.SassCompilationFailedException;
import de.larsgrefer.sass.embedded.connection.ConnectionFactory;
import de.larsgrefer.sass.embedded.importer.Importer;
import jakarta.servlet.ServletContext;

/**
 * Compiles .scss to .css or .min.css using different sources ({@link Importer}s)
 *
 * @author Sebastian Hofmann (mcrshofm)
 */
public final class MCRSassCompilerManager {

    private static final String DEVELOPER_MODE_CONFIG_KEY = "MCR.SASS.DeveloperMode";

    private final Map<String, String> fileCompiledContentMap = new ConcurrentHashMap<>();

    private final Map<String, Date> fileLastCompileDateMap = new ConcurrentHashMap<>();

    private final Map<String, String> fileMD5Map = new ConcurrentHashMap<>();

    private MCRSassCompilerManager(){
    }

    /**
     * @return the singleton instance of this class
     */
    public static MCRSassCompilerManager getInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

    /**
     * Gets the compiled(&amp;compressed) CSS
     *
     * @param file the path to a .scss file. File should end with .css or .min.css (The compiler will look for the
     *             .scss file, then compiles and decides to minify or not).
     * @param servletContext the servlet context to locate web resources
     * @return Optional with the compiled css as string. Empty optional if the fileName is not valid.
     * @throws SassCompilationFailedException if compiling sass input fails
     * @throws IOException                    if communication with dart-sass or reading input fails
     */
    public synchronized Optional<String> getCSSFile(String file, ServletContext servletContext)
        throws IOException, SassCompilationFailedException {
        if (!isDeveloperMode() && fileCompiledContentMap.containsKey(file)) {
            return Optional.of(fileCompiledContentMap.get(file));
        } else {
            return Optional.ofNullable(compile(file));
        }
    }

    /**
     * @param name the name of file
     * @return the time when the specific file was compiled last time.
     */
    public Optional<Date> getLastCompiled(String name) {
        return Optional.ofNullable(fileLastCompileDateMap.get(name));
    }

    /**
     * @param name the name of file
     * @return the md5 hash of the last compiled specific file.
     */
    public Optional<String> getLastMD5(String name) {
        return Optional.ofNullable(fileMD5Map.get(name));
    }

    /**
     * Just compiles a file and fills all maps.
     *
     * @param name the name of the file (with .min.css or .css ending)
     * @return the compiled css
     * @throws SassCompilationFailedException if compiling sass input fails
     * @throws IOException                    if communication with dart-sass or reading input fails
     */
    private String compile(String name)
        throws IOException, SassCompilationFailedException {
        String css;
        try (MCRSassCompiler sassCompiler = new MCRSassCompiler(ConnectionFactory.bundled())) {
            String realFileName = getRealFileName(name);
            var compileSuccess = sassCompiler.compile(realFileName);
            css = compileSuccess.getCss();
        }

        //boolean compress = name.endsWith(".min.css");
        //if (compress) {
        // […] // css = MCRSassCompressor.compress(css);
        //}

        this.fileCompiledContentMap.put(name, css);
        this.fileLastCompileDateMap.put(name, new Date());

        try {
            this.fileMD5Map.put(name, MCRUtils.asMD5String(1, null, css));
        } catch (NoSuchAlgorithmException e) {
            throw new MCRException("Error while generating md5 of result css!", e);
        }

        return css;

    }

    /**
     * Replaces {@code (.min).css} with {@code .scss}
     * @param name filename a browser expects
     * @return the filename available as a resource
     */
    public static String getRealFileName(String name) {
        return name.replace(".min.css", ".scss").replace(".css", ".scss");
    }

    /**
     * 
     * @return true if the SASS compiler is run in developer mode
     */
    public boolean isDeveloperMode() {
        return MCRConfiguration2.getBoolean(DEVELOPER_MODE_CONFIG_KEY).orElse(false);
    }

    private static final class LazyInstanceHolder {
        public static final MCRSassCompilerManager SINGLETON_INSTANCE = new MCRSassCompilerManager();
    }

}
