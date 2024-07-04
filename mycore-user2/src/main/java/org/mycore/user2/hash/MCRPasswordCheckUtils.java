package org.mycore.user2.hash;

public abstract class MCRPasswordCheckUtils {

    public static boolean fixedEffortEquals(String a, String b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return fixedEffortEquals(a.toCharArray(), b.toCharArray());
    }

    public static boolean fixedEffortEquals(char[] a, char[] b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        int result = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    public static boolean fixedEffortEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        int result = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

}
