/**
 * 
 */
package org.mycore.tools;

import java.io.ByteArrayOutputStream;

import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author shermann
 *
 */
public class DocumentTool {

    /** Transforms the given Document into a String 
     * 
     * @return the xml document as {@link String} or null if an {@link Exception} occurs   
     * */
    public static String documentAsString(Document doc) {
        String value = null;
        try {
            XMLOutputter op = new XMLOutputter(Format.getPrettyFormat());
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            op.output(doc, os);
            os.flush();
            value = new String(os.toByteArray());
            os.close();
        } catch (Exception e) {
            return null;
        }
        return value;
    }
}