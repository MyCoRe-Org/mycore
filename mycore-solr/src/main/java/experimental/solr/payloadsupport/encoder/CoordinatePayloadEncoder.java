/**
 * 
 */
package experimental.solr.payloadsupport.encoder;

import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.index.Payload;

/**
 * @author shermann
 *
 */
public class CoordinatePayloadEncoder implements PayloadEncoder {

    @Override
    public Payload encode(char[] buffer) {
        Payload toReturn = new Payload();

        byte[] asBytes = null;
        try {
            asBytes = (new String(buffer)).getBytes("UTF-16");
            toReturn.setData(asBytes);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return toReturn;
    }

    @Override
    public Payload encode(char[] buffer, int offset, int length) {
        char[] part = new char[length];
        System.arraycopy(buffer, offset, part, 0, length);
        return this.encode(part);
    }
}
