package org.mycore.user2.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKeyFactory;

import org.mycore.common.config.MCRConfigurationException;

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
    public static void probeHashAlgorithm(String algorithm) {
        try {
            MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new MCRConfigurationException("Hash algorithm " + algorithm + " unavailable", e);
        }
    }

    public static void probeSecretKeyAlgorithm(String algorithm) {
        try {
            SecretKeyFactory.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new MCRConfigurationException("Secret key algorithm " + algorithm + " unavailable", e);
        }
    }

}
