package org.mycore.solr.index.strategy;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRXMLFunctions;

/**
 * Strategy that depends on a files mime type. By default images are
 * ignored. You can use the MCR.Module-solr.MimeTypeStrategy.Pattern property to
 * set an application specific pattern. Be aware that this is the ignore
 * pattern, the {@link #check(Path, BasicFileAttributes)} method will return false if it
 * matches.
 * 
 * @author Matthias Eichner
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrMimeTypeStrategy implements MCRSolrFileStrategy {

    private final static Pattern IGNORE_PATTERN;

    static {
        String acceptPattern = MCRConfiguration.instance().getString(CONFIG_PREFIX + "MimeTypeStrategy.Pattern");
        IGNORE_PATTERN = Pattern.compile(acceptPattern);
    }

    @Override
    public boolean check(Path file, BasicFileAttributes attrs) {
        String mimeType = MCRXMLFunctions.getMimeType(file.getFileName().toString());
        return !IGNORE_PATTERN.matcher(mimeType).matches();
    }

}
