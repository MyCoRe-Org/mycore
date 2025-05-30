/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.net.URI;
import java.net.URISyntaxException;
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
 * For further documentation see <a href="https://code.google.com/archive/p/persistenturls/wikis">PURLZ Wiki</a>
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

    private String cookie;

    /**
     * sets the session cookie, if the login was successful
     *
     * @param purlServerURL - the base URL of the PURL server
     * @param user          - the PURL server user
     * @param password      - the user's password
     */
    public void login(String purlServerURL, String user, String password) {
        purlServerBaseURL = purlServerURL;
        // Get Cookie
        try {
            URL url = new URI(purlServerBaseURL + ADMIN_PATH + "/login/login.bsh?referrer=/docs/index.html").toURL();
            HttpURLConnection cookieCon = (HttpURLConnection) url.openConnection();
            try {
                cookieCon.connect();

                cookieCon.getHeaderFields()
                    .getOrDefault("Set-Cookie", List.of())
                    .forEach(cookie -> {
                        this.cookie = cookie;
                        LOGGER.debug(() -> "Cookie: " + cookie);
                    });
            } finally {
                cookieCon.disconnect();
            }
            // Login
            String data = "id=" + URLEncoder.encode(user, StandardCharsets.UTF_8);
            data += "&passwd=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

            url = new URI(purlServerBaseURL + ADMIN_PATH + "/login/login-submit.bsh").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                conn.setRequestMethod("POST");
                conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);

                conn.setDoOutput(true);
                try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                    wr.write(data);
                    wr.flush();
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        LOGGER.info(() -> buildLogMessage(conn, responseCode));
                    } else {
                        LOGGER.error(() -> buildLogMessage(conn, responseCode));
                    }

                    // Get the response
                    try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                        StandardCharsets.UTF_8))) {
                        String line = rd.readLine();
                        while (line != null) {
                            if ("PURL User Login Failure".equals(line.trim())) {
                                cookie = null;
                                break;
                            }
                            line = rd.readLine();
                        }
                    }
                }
            } finally {
                conn.disconnect();
            }
        } catch (IOException | URISyntaxException e) {
            //TODO: ugly, ugly, ugly: check responseCode instead of message
            if (!e.getMessage().contains(
                "Server returned HTTP response code: 403 for URL: ")) {
                LOGGER.error(e::getMessage, e);
            }
        }
    }

    /**
     * logout from PURL server
     */
    public void logout() {
        String requestMethod = "POST";
        URI uri = URI.create(purlServerBaseURL + ADMIN_PATH + "/logout?referrer=/docs/index.html");
        int responseCode = -1;
        try {
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                conn.setRequestMethod(requestMethod);
                conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);

                conn.setDoOutput(true);
                try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                    wr.flush();
                }
                responseCode = conn.getResponseCode();
                int finalResponseCode = responseCode;
                LOGGER.debug(() -> buildLogMessage(conn, finalResponseCode));
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            if (!e.getMessage().contains(
                "Server returned HTTP response code: 403 for URL: ")) {
                int finalResponseCode = responseCode;
                LOGGER.error(() -> requestMethod + " " + uri + " -> " + finalResponseCode, e);
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

            URL url = new URI(purlServerBaseURL + PURL_PATH + purl).toURL();
            LOGGER.debug(url);

            StringBuilder data = new StringBuilder();
            data.append("target=").append(URLEncoder.encode(target, StandardCharsets.UTF_8));
            data.append("&maintainers=").append(maintainers);
            data.append("&type=").append(type);

            LOGGER.debug(data);

            // Send data

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);
            conn.setRequestMethod("POST");

            conn.setDoOutput(true);

            try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                wr.write(data.toString());
                wr.flush();
            }
            response = conn.getResponseCode();

            if (response != 200 && conn.getErrorStream() != null && LOGGER.isErrorEnabled()) {
                logError(conn);
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

            URL url = new URI(strURL).toURL();
            LOGGER.debug(url);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);
            conn.setRequestMethod("PUT");
            response = conn.getResponseCode();

            if (response != 200 && conn.getErrorStream() != null && LOGGER.isErrorEnabled()) {
                logError(conn);
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
     * @return the HTTP Status Code of the request
     */
    public int deletePURL(String purl) {
        int response = 0;
        HttpURLConnection conn = null;
        try {
            URL url = new URI(purlServerBaseURL + PURL_PATH + purl).toURL();
            LOGGER.debug(url);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);
            conn.setRequestMethod("DELETE");
            response = conn.getResponseCode();

            if (response != 200 || conn.getErrorStream() != null && LOGGER.isErrorEnabled()) {
                logError(conn);
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
            URL url = new URI(purlServerBaseURL + PURL_PATH + purl).toURL();
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

    /**
     * return the PURL metadata
     *
     * @param purl      - the purl
     * @return an XML document containing the metadata of the PURL
     *        or null if the PURL does not exist
     */
    public Document retrievePURLMetadata(String purl, String targetURL) {
        HttpURLConnection conn = null;
        try {
            URL url = new URI(purlServerBaseURL + PURL_PATH + purl).toURL();
            conn = (HttpURLConnection) url.openConnection();
            int response = conn.getResponseCode();

            if (response == 200) {

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();

                return db.parse(conn.getInputStream());
                /* <purl status="1">
                 *   <id>/test/rosdok/ppn750527188</id>
                 *   <type>302</type>
                 *   <maintainers>
                 *     <uid>rosdok</uid>
                 *     <uid>test</uid>
                 *   </maintainers>
                 *   <target>
                 *     <url>http://localhost:8080/rosdok/resolve/id/rosdok_document_0000000259</url>
                 *   </target>
                 * </purl>
                 */
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    /**
     * check if a PURL exists
     *
     * @param purl      - the purl
     * @return true, if the given PURL is known
     */
    public boolean existsPURL(String purl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URI(purlServerBaseURL + PURL_PATH + purl).toURL();
            conn = (HttpURLConnection) url.openConnection();
            int response = conn.getResponseCode();
            return response == 200;
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return false;
    }

    private static String buildLogMessage(HttpURLConnection conn, int finalResponseCode) {
        return conn.getRequestMethod() + " " + conn.getURL() + " -> " + finalResponseCode;
    }

    private void logError(HttpURLConnection connection) throws IOException {
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getErrorStream(),
            StandardCharsets.UTF_8))) {
            String method = connection.getRequestMethod();
            URL url = connection.getURL();
            int code = connection.getResponseCode();
            LOGGER.error(() -> method + " " + url + " -> " + code);
            String line = rd.readLine();
            while (line != null) {
                LOGGER.error(line);
                line = rd.readLine();
            }
        }
    }

}
