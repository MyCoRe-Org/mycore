package org.mycore.common.fo;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.mycore.common.content.MCRContent;

/**
 * This is an interface to use configured XSL-FO formatters for the layout service.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision: 1.8 $ $Date: 2008/05/28 13:43:31 $
 */

public interface MCRFoFormatterInterface {

    void transform(MCRContent input, OutputStream out) throws TransformerException, IOException;
}
