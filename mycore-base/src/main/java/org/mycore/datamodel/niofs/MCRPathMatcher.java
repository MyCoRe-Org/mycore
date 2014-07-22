/**
 *
 */
package org.mycore.datamodel.niofs;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

/**
 * @author Thomas Scheffler
 *
 */
public class MCRPathMatcher implements PathMatcher {

    private final Pattern pattern;

    public MCRPathMatcher(final String regex) {
        pattern = getPattern(regex);
    }

    @Override
    public boolean matches(final Path path) {
        return pattern.matcher(path.toString()).matches();
    }

    protected Pattern getPattern(final String pattern) {
        return Pattern.compile(pattern);
    }

}
