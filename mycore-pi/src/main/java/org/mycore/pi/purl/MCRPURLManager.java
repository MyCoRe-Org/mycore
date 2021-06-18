/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.pi.purl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * PURL Manager to register Persistent URLs on a PURL server
 * <p>
 * for further documentation see PURLZ Wiki:
 * https://code.google.com/archive/p/persistenturls/wikis
 * <p>
 * Hint:
 * -----
 * Please check in your code that you do not register / override regular PURLs in test / development
 * by checking:
 * if (resolvingURL.contains("localhost")) {
 * purl = "/test" + purl;
 * }
 *
 * @author Robert Stephan
 */
public class MCRPURLManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ADMIN_PATH = "/admin";

    private static final String PURL_PATH = ADMIN_PATH + "/purl";

    private static final String COOKIE_HEADER_PARAM = "Cookie";

    private String purlServerBaseURL;

    private String cookie = null;

    /**
     * sets the session cookie, if the login was successful
     *
     * @param purlServerURL - the base URL of the PURL server
     * @param user          - the PURL server user
     * @param password      - the user's password
     */
    public void login(String purlServerURL, String user, String password) {
        HttpURLConnection conn = null;
        try {
            purlServerBaseURL = purlServerURL;
            // Get Cookie
            URL url = new URL(purlServerBaseURL + ADMIN_PATH + "/login/login.bsh?referrer=/docs/index.html");
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            conn.getHeaderFields()
                .getOrDefault("Set-Cookie", List.of())
                .forEach(cookie -> {
                    this.cookie = cookie;
                    LOGGER.debug("Cookie: " + cookie);
                });
            conn.disconnect();

            // Login
            String data = "id=" + URLEncoder.encode(user, StandardCharsets.UTF_8);
            data += "&passwd=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

            url = new URL(purlServerBaseURL + ADMIN_PATH + "/login/login-submit.bsh");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);

            conn.setDoOutput(true);
            try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                wr.write(data);
                wr.flush();
                if (conn.getResponseCode() == 200) {
                    LOGGER.info(conn.getRequestMethod() + conn.getURL() + " -> " + conn.getResponseCode());
                } else {
                    LOGGER.error(conn.getRequestMethod() + conn.getURL() + " -> " + conn.getResponseCode());
                }

                // Get the response
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                    StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = rd.readLine()) != null) {
                        if ("PURL User Login Failure".equals(line.trim())) {
                            cookie = null;
                            break;
                        }

                    }
                }
            }
            conn.disconnect();

        } catch (IOException e) {
            if (!e.getMessage().contains(
                "Server returned HTTP response code: 403 for URL: ")) {
                LOGGER.error(e);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * logout from PURL server
     */
    public void logout() {
        HttpURLConnection conn = null;
        int responseCode = -1;
        try {
            URL url = new URL(purlServerBaseURL + ADMIN_PATH + "/logout?referrer=/docs/index.html");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);

            conn.setDoOutput(true);
            try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                wr.flush();
            }
            responseCode = conn.getResponseCode();
            LOGGER.debug(conn.getRequestMethod() + conn.getURL() + " -> " + responseCode);
        } catch (IOException e) {
            if (!e.getMessage().contains(
                "Server returned HTTP response code: 403 for URL: ")) {
                LOGGER.error(conn.getRequestMethod() + conn.getURL() + " -> " + responseCode);
                LOGGER.error(e);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // returns the response code

    /**
     * register a new PURL
     *
     * @param purl        - the PURL
     * @param target      the target URL
     * @param type        - the PURL type
     * @param maintainers - the maintainers
     * @return the HTTP Status Code of the request
     */
    public int registerNewPURL(String purl, String target, String type, String maintainers) {
        int response = 0;
        HttpURLConnection conn = null;
        try {
            // opener.open("http://localhost:8080/admin/purl/net/test2",
            // urllib.urlencode(dict(type="410", maintainers="admin"))).read().close() #
            // Create a 410 purl

            URL url = new URL(purlServerBaseURL + PURL_PATH + purl);
            LOGGER.debug(url.toString());

            String data = "target=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
            data += "&maintainers=" + maintainers;
            data += "&type=" + type;

            LOGGER.debug(data);

            // Send data

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);
            conn.setRequestMethod("POST");

            conn.setDoOutput(true);

            try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                wr.write(data);
                wr.flush();
            }
            response = conn.getResponseCode();

            if (response != 200 && conn.getErrorStream() != null && LOGGER.isErrorEnabled()) {
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(),
                    StandardCharsets.UTF_8))) {
                    String line;
                    LOGGER.error(conn.getRequestMethod() + conn.getURL() + " -> " + conn.getResponseCode());
                    while ((line = rd.readLine()) != null) {
                        LOGGER.error(line);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return response;
    }

    /**
     * updates an existing PURL
     *
     * @param purl        - the PURL (relative URL)
     * @param target      - the target URL
     * @param type        - the PURL type
     * @param maintainers list of maintainers (PURL server users or groups)
     * @return the HTTP Status Code of the request
     */
    public int updateExistingPURL(String purl, String target, String type, String maintainers) {
        int response = 0;
        HttpURLConnection conn = null;
        try {
            // opener.open("http://localhost:8080/admin/purl/net/test2",
            // urllib.urlencode(dict(type="410", maintainers="admin"))).read().close() #
            // Create a 410 purl

            String strURL = purlServerBaseURL + PURL_PATH + purl;
            strURL += "?target=" + URLEncoder.encode(target, StandardCharsets.UTF_8) + "&maintainers=" + maintainers
                + "&type=" + type;

            URL url = new URL(strURL);
            LOGGER.debug(url.toString());

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);
            conn.setRequestMethod("PUT");
            response = conn.getResponseCode();

            if (response != 200 && conn.getErrorStream() != null && LOGGER.isErrorEnabled()) {
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(),
                    StandardCharsets.UTF_8))) {
                    String line = null;
                    LOGGER.error(conn.getRequestMethod() + conn.getURL() + " -> " + conn.getResponseCode());
                    while ((line = rd.readLine()) != null) {
                        LOGGER.error(line);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return response;
    }

    /**
     * deletes an existing PURL
     *
     * @param purl
     * @return the HTTP Status Code of the request
     */
    public int deletePURL(String purl) {
        int response = 0;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(purlServerBaseURL + PURL_PATH + purl);
            LOGGER.debug(url.toString());

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);
            conn.setRequestMethod("DELETE");
            response = conn.getResponseCode();

            if (response != 200 || conn.getErrorStream() != null && LOGGER.isErrorEnabled()) {
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(),
                    StandardCharsets.UTF_8))) {
                    String line = null;
                    LOGGER.error(conn.getRequestMethod() + conn.getURL() + " -> " + conn.getResponseCode());
                    while ((line = rd.readLine()) != null) {
                        LOGGER.error(line);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return response;
    }

    /**
     * check if a purl has the given target url
     *
     * @param purl      - the purl
     * @param targetURL - the target URL
     * @return true, if the target URL is registered at the given PURL
     */
    public boolean isPURLTargetURLUnchanged(String purl, String targetURL) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(purlServerBaseURL + PURL_PATH + purl);
            conn = (HttpURLConnection) url.openConnection();
            int response = conn.getResponseCode();

            if (response == 200) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(conn.getInputStream());
                /*
                 * <purl status="1"> <id>/test/rosdok/ppn750527188</id> <type>302</type>
                 * <maintainers><uid>rosdok</uid><uid>test</uid></maintainers>
                 * <target><url>http://localhost:8080/rosdok/resolve/id/
                 * rosdok_document_0000000259</url></target> </purl>
                 */
                Element eTarget = (Element) doc.getDocumentElement().getElementsByTagName("target").item(0);
                Element eTargetUrl = (Element) eTarget.getElementsByTagName("url").item(0);
                return targetURL.equals(eTargetUrl.getTextContent().trim());
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return false;
    }
}
