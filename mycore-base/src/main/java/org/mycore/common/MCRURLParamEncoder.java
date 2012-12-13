package org.mycore.common;

/**
 * Utility class to encode URL parameters. Be aware that you shouldn't encode a
 * complete url, only single parameter values.
 * <p>
 * e.g. <b>http://www.mycore.de?param=hello there</b><br/>
 * You should only encode "hello there"
 * </p>
 * @see http://stackoverflow.com/questions/724043/http-url-address-encoding-in-java
 * @author fmucar
 */
public class MCRURLParamEncoder {

    public static String encode(String input) {
        StringBuilder resultStr = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (isUnsafe(ch)) {
                resultStr.append('%');
                resultStr.append(toHex(ch / 16));
                resultStr.append(toHex(ch % 16));
            } else {
                resultStr.append(ch);
            }
        }
        return resultStr.toString();
    }

    private static char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    private static boolean isUnsafe(char ch) {
        if (ch > 128 || ch < 0)
            return true;
        return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
    }

}
