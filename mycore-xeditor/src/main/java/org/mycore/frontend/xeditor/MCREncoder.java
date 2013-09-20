package org.mycore.frontend.xeditor;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class MCREncoder {

    public static String encode(String text) {
        try {
            return Hex.encodeHexString(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String decode(String text) {
        try {
            return new String(Hex.decodeHex(text.toCharArray()));
        } catch (DecoderException ex) {
            throw new RuntimeException(ex);
        }
    }

}
