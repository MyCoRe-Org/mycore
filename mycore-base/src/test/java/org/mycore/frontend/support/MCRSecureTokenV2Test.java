/**
 * 
 */
package org.mycore.frontend.support;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;

import org.junit.Test;

/**
 * @author Thomas Scheffler(yagee)
 */
public class MCRSecureTokenV2Test {

    /**
     * Test method for {@link org.mycore.frontend.support.MCRSecureTokenV2#getHash()}.
     */
    @Test
    public final void testGetHash() {
        MCRSecureTokenV2 token = getWowzaSample();
        assertEquals("TgJft5hsjKyC5Rem_EoUNP7xZvxbqVPhhd0GxIcA2oo=", token.getHash());
        token = getWowzaSample2();
        assertEquals("5_A2m7LV6pTuLN3lUPvAUN2xI8x_BDrgfXfVi_gT-GA=", token.getHash());
    }

    /**
     * Test method for {@link org.mycore.frontend.support.MCRSecureTokenV2#toURI(java.lang.String, java.lang.String)}.
     */
    @Test
    public final void testToURL() throws URISyntaxException {
        MCRSecureTokenV2 token = getWowzaSample();
        String baseURL = "http://192.168.1.1:1935/";
        String suffix = "/playlist.m3u8";
        String hashParameterName = "myTokenPrefixhash";
        String expectedURL = baseURL + "vod/sample.mp4" + suffix
            + "?myTokenPrefixstarttime=1395230400&myTokenPrefixendtime=1500000000&myTokenPrefixCustomParameter=abcdef&"
            + hashParameterName + "=TgJft5hsjKyC5Rem_EoUNP7xZvxbqVPhhd0GxIcA2oo=";
        assertEquals(expectedURL, token.toURI(baseURL, suffix, hashParameterName).toString());
    }

    private static MCRSecureTokenV2 getWowzaSample() {
        String contentPath = "vod/sample.mp4";
        String sharedSecret = "mySharedSecret";
        String ipAddress = "192.168.1.2";
        String[] parameters = new String[] { "myTokenPrefixstarttime=1395230400", "myTokenPrefixendtime=1500000000",
            "myTokenPrefixCustomParameter=abcdef" };
        MCRSecureTokenV2 token = new MCRSecureTokenV2(contentPath, ipAddress, sharedSecret, parameters);
        return token;
    }

    private static MCRSecureTokenV2 getWowzaSample2() {
        String contentPath = "vod/_definst_/mp4:Überraschung.mp4";
        String sharedSecret = "JUnitSecret";
        String ipAddress = "192.168.1.2";
        String[] parameters = { };
        return new MCRSecureTokenV2(contentPath, ipAddress, sharedSecret, parameters);
    }

}
