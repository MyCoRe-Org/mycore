package org.mycore.common.content.streams;

import org.mycore.common.MCRUtils;
import org.mycore.common.digest.MCRDigest;

import java.io.InputStream;
import java.security.DigestInputStream;

/**
 * Builds a checksum String while content goes through this input stream.
 */
public class MCRDigestInputStream extends DigestInputStream {

    public MCRDigestInputStream(InputStream stream, MCRDigest.Algorithm digestAlgorithm) {
        super(stream, MCRUtils.buildMessageDigest(digestAlgorithm));
    }

    /**
     * Returns the message digest that has been built during reading of the underlying input stream.
     *
     * @return the message digest checksum of all bytes that have been read
     */
    public byte[] getDigest() {
        return getMessageDigest().digest();
    }

    /**
     * Returns the digest checksum as a hex String
     *
     * @return the digest checksum as a String of hex digits
     */
    public String getDigestAsHexString() {
        return MCRUtils.toHexString(getDigest());
    }

}
