/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;

import com.google.common.css.SourceCode;
import com.google.common.css.compiler.ast.GssParser;
import com.google.common.css.compiler.ast.GssParserException;
import com.google.common.css.compiler.passes.CompactPrinter;
import com.google.common.css.compiler.passes.NullGssSourceMapGenerator;

import de.larsgrefer.sass.embedded.SassCompilationFailedException;
import de.larsgrefer.sass.embedded.connection.ConnectionFactory;
import de.larsgrefer.sass.embedded.importer.CustomImporter;
import de.larsgrefer.sass.embedded.importer.FileImporter;
import de.larsgrefer.sass.embedded.importer.Importer;

/**
 * Compiles .scss to .css or .min.css using different sources ({@link Importer}s)
 *
 * @author Sebastian Hofmann (mcrshofm)
 * @see MCRServletContextResourceImporter
 */
public class MCRSassCompilerManager {

    private static final String DEVELOPER_MODE_CONFIG_KEY = "MCR.SASS.DeveloperMode";

    private Map<String, String> fileCompiledContentMap = new ConcurrentHashMap<>();

    private Map<String, Date> fileLastCompileDateMap = new ConcurrentHashMap<>();

    private Map<String, String> fileMD5Map = new ConcurrentHashMap<>();

    /**
     * @return the singleton instance of this class
     */
    public static MCRSassCompilerManager getInstance() {
        return MCRSASSCompilerManagerHolder.INSTANCE;
    }

    /**
     * Gets the compiled(&amp;compressed) CSS
     *
     * @param file     the path to a .scss file. File should end with .css or .min.css (The compiler will look for the
     *                 .scss file, then compiles and decides to minify or not).
     * @param importer a additional list of importers
     * @return Optional with the compiled css as string. Empty optional if the fileName is not valid.
     * @throws CompilationException if {@link Compiler#compile(FileContext)} throws
     * @throws IOException if {@link Compiler#compile(FileContext)} throws
     */
    public synchronized Optional<String> getCSSFile(String file, List<Importer> importer)
        throws IOException, SassCompilationFailedException {
        if (!isDeveloperMode() && fileCompiledContentMap.containsKey(file)) {
            return Optional.of(fileCompiledContentMap.get(file));
        } else {
            return Optional.ofNullable(compile(file, importer));
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
     * @throws CompilationException
     */
    private String compile(String name, List<Importer> importer) throws IOException, SassCompilationFailedException {
        String css;
        try (MCRSassCompiler sassCompiler = new MCRSassCompiler(ConnectionFactory.bundled())) {
            importer.forEach(i -> registerImporter(sassCompiler, i));
            String realFileName = getRealFileName(name);
            URL resource = importer.stream()
                .map(i -> toURL(i, realFileName))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> getClass().getResource("/" + name));
            if (resource == null) {
                return null;
            }
            var compileSuccess = sassCompiler.compile(resource);
            css = compileSuccess.getCss();
        }

        boolean compress = name.endsWith(".min.css");
        if (compress) {
            try {
                GssParser parser = new GssParser(new SourceCode(null, css));
                final CompactPrinter cp = new CompactPrinter(parser.parse(), new NullGssSourceMapGenerator());
                cp.setPreserveMarkedComments(true);
                cp.runPass();
                css = cp.getCompactPrintedString();
            } catch (GssParserException e) {
                throw new MCRException("Error while parsing result css with compressor (" + name + ")", e);
            }
        }

        this.fileCompiledContentMap.put(name, css);
        this.fileLastCompileDateMap.put(name, new Date());

        try {
            this.fileMD5Map.put(name, MCRUtils.asMD5String(1, null, css));
        } catch (NoSuchAlgorithmException e) {
            throw new MCRException("Error while generating md5 of result css!", e);
        }

        return css;

    }

    public static String getRealFileName(String name) {
        return name.replace(".min.css", ".scss").replace(".css", ".scss");
    }

    private void registerImporter(MCRSassCompiler sassCompiler, Importer importer) {
        if (importer instanceof FileImporter fi) {
            sassCompiler.registerImporter(fi);
        }
        if (importer instanceof CustomImporter ci) {
            sassCompiler.registerImporter(ci);
        }
    }

    private URL toURL(Importer importer, String name) {
        try {
            if (importer instanceof FileImporter fi) {
                return fi.handleImport(name, false).toURI().toURL();
            }
            if (importer instanceof CustomImporter ci) {
                return new URL(ci.canonicalize(name, false));
            }
        } catch (Exception e) {
            throw new MCRException(e);
        }
        return null;
    }

    /**
     * 
     * @return true if the SASS compiler is run in developer mode
     */
    public boolean isDeveloperMode() {
        return MCRConfiguration2.getBoolean(DEVELOPER_MODE_CONFIG_KEY).orElse(false);
    }

    private static final class MCRSASSCompilerManagerHolder {
        public static final MCRSassCompilerManager INSTANCE = new MCRSassCompilerManager();
    }

}
