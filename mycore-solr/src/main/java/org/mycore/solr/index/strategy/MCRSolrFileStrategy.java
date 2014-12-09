package org.mycore.solr.index.strategy;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Base interface for <code>MCRFile</code> specific strategies.
 * 
 * @author Matthias Eichner
 *
 */
public interface MCRSolrFileStrategy {

    boolean check(Path file, BasicFileAttributes attrs);
}
