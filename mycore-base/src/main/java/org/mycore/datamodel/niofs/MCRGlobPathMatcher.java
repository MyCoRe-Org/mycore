/**
 *
 */
package org.mycore.datamodel.niofs;

import java.nio.file.FileSystem;
import java.nio.file.PathMatcher;
import java.util.Optional;
import java.util.regex.Pattern;

import org.mycore.common.function.MCRFunctions;

/**
 * @author Thomas Scheffler
 *
 */
public class MCRGlobPathMatcher extends MCRPathMatcher {
    /**
     * A {@link PathMatcher} that accepts 'glob' syntax
     * @param globPattern pattern in {@link FileSystem#getPathMatcher(String) 'glob' syntax}
     */
    public MCRGlobPathMatcher(final String globPattern) {
        super(globPattern);
    }

    @Override
    protected Pattern getPattern(final String globPattern) {
        return Optional.of(globPattern)
            .map(MCRFunctions::convertGlobToRegex)
            .map(Pattern::compile)
            .get();
    }

}
