package org.mycore.solr.index.cs.strategy;

import java.util.regex.Pattern;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.ifs.MCRFile;

/**
 * Strategy that depends on a files mime type. By default images are
 * ignored. You can use the MCR.Module-solr.MimeTypePattern property to
 * set an application specific pattern. Be aware that this is the ignore
 * pattern, the {@link #check(MCRFile)} method will return false if it
 * matches.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrMimeTypeStrategy implements MCRSolrFileStrategy {

    private final static Pattern IGNORE_PATTERN;

    static {
        String acceptPattern = MCRConfiguration.instance().getString("MCR.Module-solr.MimeTypePattern", "image/.*");
        IGNORE_PATTERN = Pattern.compile(acceptPattern);
    }

    @Override
    public boolean check(MCRFile file) {
        String mimeType = MCRXMLFunctions.getMimeType(file.getAbsolutePath());
        return !IGNORE_PATTERN.matcher(mimeType).matches();
    }

}
