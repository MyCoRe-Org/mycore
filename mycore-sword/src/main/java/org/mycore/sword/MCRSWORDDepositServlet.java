/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.sword;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.purl.sword.atom.Generator;
import org.purl.sword.atom.Summary;
import org.purl.sword.atom.Title;
import org.purl.sword.base.ChecksumUtils;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.ErrorCodes;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDErrorDocument;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.server.SWORDServer;

/**
 * After retrival of service document a remote user is able to deposit content
 * to a collection chosen from the service document. An instance of this class
 * reads all information which can be retrieved from the
 * {@link HttpServletRequest} and fills a new {@link Deposit} object with the
 * data. The data itself contains the following information:
 * <ul>
 * <li>username and password</li>
 * <li>checked md5</li>
 * <li>deposited file</li>
 * <li>behalf of user</li>
 * <li>X-Packaging</li>
 * <li>X-No-Op</li>
 * <li>X-Verbose</li>
 * <li>Slug</li>
 * <li>Content Disposition</li>
 * <li>Remote IP-Adress</li>
 * <li>Content-Type</li>
 * <li>Content-Length</li>
 * </ul>
 * Retrieved information is validated within the process. The implementation of
 * {@link SWORDServer} is called with the {@link MCRServletJob} and the
 * {@link Deposit} as parameters.
 * @see <a href="http://www.swordapp.org/docs/sword-profile-1.3.html">SWORD APP Profile</a>
 * @author Nils Verheyen
 * 
 */
public class MCRSWORDDepositServlet extends MCRServlet {

    private static final long serialVersionUID = -1527183655067834313L;

    /** Sword repository */
    protected SWORDServer myRepository;
    
    /** contains the authenticator used for requests. */
    protected MCRSWORDAuthenticator authenticator;

    /** Counter */
    private static AtomicInteger counter = new AtomicInteger(0);

    /** Logger */
    private static Logger LOG = Logger.getLogger(MCRSWORDDepositServlet.class);

    /**
     * Initialise the servlet
     * 
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();

        myRepository = MCRSWORDUtils.createServer();
        authenticator = MCRSWORDUtils.createAuthenticator();
    }

    /**
     * Process the Get request. This will return an unimplemented response.
     */
    protected void doGet(MCRServletJob job) throws ServletException, IOException {
        // Send a '501 Not Implemented'
        job.getResponse().sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    @Override
    protected void doPost(MCRServletJob job) throws Exception {

        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        // Create the Deposit request
        Deposit d = new Deposit();
        Date date = new Date();
        LOG.info("Starting deposit processing at " + date.toString() + " by " + request.getRemoteAddr());

        // Are there any authentication details?
        Pair<String, String> requestAuthData = MCRSWORDUtils.readBasicAuthData(request);
        if (requestAuthData != null && authenticator.authenticate(requestAuthData.first, requestAuthData.second)) {

            LOG.info("requesting user: " + requestAuthData.first);
            
            d.setUsername(requestAuthData.first);
            d.setPassword(requestAuthData.second);
        } else {
            
            LOG.info("unauthorized request for service document send -> host: " + request.getRemoteHost());
            
            String s = "BASIC realm=\"" + MCRSWORDUtils.getAuthRealm() + "\"";
            response.setHeader("WWW-Authenticate", s);
            response.setStatus(401);
            return;
        }

        // Set up some variables
        File file = null;
        FileInputStream fis = null;

        // Do the processing
        try {
            long maxUploadSize = MCRSWORDUtils.getMaxUploadSize();
            
            // Check the size is OK
            long fLength = request.getContentLength();
            if ((maxUploadSize != -1) && (fLength > maxUploadSize)) {
                MCRSWORDUtils.makeErrorDocument(ErrorCodes.MAX_UPLOAD_SIZE_EXCEEDED, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                        "The uploaded file exceeded the maximum file size this server will accept (the file is " + fLength
                                + " kB but the server will only accept files as large as " + maxUploadSize + " kB)", request, response);
                return;
            }

            // Write the file to the temp directory
            file = new File(MCRSWORDUtils.getTempUploadDir(), "SWORD-" + request.getRemoteAddr() + "-" + counter.addAndGet(1));
            InputStream fin = request.getInputStream();
            OutputStream fout = new FileOutputStream(file);
            IOUtils.copy(fin, fout);
            if ((maxUploadSize != -1) && (file.length() > maxUploadSize)) {
                LOG.info("max upload size exeeded: " + file.length());
                MCRSWORDUtils.makeErrorDocument(ErrorCodes.MAX_UPLOAD_SIZE_EXCEEDED, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                        "The uploaded file exceeded the maximum file size this server will accept (the file is " + fLength
                                + " kB but the server will only accept files as large as " + maxUploadSize + " kB)", request, response);
                return;
            }

            // Check the MD5 hash
            String receivedMD5 = ChecksumUtils.generateMD5(file.getAbsolutePath());
            LOG.debug("Received filechecksum: " + receivedMD5);
            d.setMd5(receivedMD5);
            String md5 = request.getHeader("Content-MD5");
            LOG.debug("Received file checksum header: " + md5);
            if ((md5 != null) && (!md5.equals(receivedMD5))) {
                // Return an error document
                MCRSWORDUtils.makeErrorDocument(ErrorCodes.ERROR_CHECKSUM_MISMATCH, HttpServletResponse.SC_PRECONDITION_FAILED,
                        "The received MD5 checksum for the deposited file did not match the checksum sent by the deposit client", request, response);
                LOG.debug("Bad MD5 for file. Aborting with appropriate error message");
                return;
            } else {
                // Set the file
                fis = new FileInputStream(file);
                d.setFile(file);

                // Set the X-On-Behalf-Of header
                String onBehalfOf = request.getHeader(HttpHeaders.X_ON_BEHALF_OF);
                if ((onBehalfOf != null) && (onBehalfOf.equals("reject"))) {
                    // user name is "reject", so throw a not know error to allow
                    // the client to be tested
                    throw new SWORDErrorException(ErrorCodes.TARGET_OWNER_UKNOWN, "unknown user \"reject\"");
                } else {
                    d.setOnBehalfOf(onBehalfOf);
                }

                // Set the X-Packaging header
                d.setPackaging(request.getHeader(HttpHeaders.X_PACKAGING));

                // Set the X-No-Op header
                String noop = request.getHeader(HttpHeaders.X_NO_OP);
                LOG.debug("X_NO_OP value is " + noop);
                if (Boolean.toString(true).equals(noop)) {
                    d.setNoOp(true);
                } else if (Boolean.toString(false).equals(noop)) {
                    d.setNoOp(false);
                } else if (noop == null) {
                    d.setNoOp(false);
                } else {
                    throw new SWORDErrorException(ErrorCodes.ERROR_BAD_REQUEST, "Bad no-op");
                }

                // Set the X-Verbose header
                String verbose = request.getHeader(HttpHeaders.X_VERBOSE);
                if ((verbose != null) && (verbose.equals("true"))) {
                    d.setVerbose(true);
                } else if ((verbose != null) && (verbose.equals("false"))) {
                    d.setVerbose(false);
                } else if (verbose == null) {
                    d.setVerbose(false);
                } else {
                    throw new SWORDErrorException(ErrorCodes.ERROR_BAD_REQUEST, "Bad verbose");
                }

                // Set the slug
                String slug = request.getHeader(HttpHeaders.SLUG);
                if (slug != null) {
                    d.setSlug(slug);
                }

                // Set the content disposition
                d.setContentDisposition(request.getHeader(HttpHeaders.CONTENT_DISPOSITION));

                // Set the IP address
                d.setIPAddress(request.getRemoteAddr());

                // Set the content type
                d.setContentType(request.getContentType());

                // Set the content length
                String cl = request.getHeader(HttpHeaders.CONTENT_LENGTH);
                if ((cl != null) && (!cl.equals(""))) {
                    d.setContentLength(Integer.parseInt(cl));
                }

                // Get the DepositResponse
                DepositResponse dr = myRepository.doDeposit(d, job);
                
                Generator g = dr.getEntry().getGenerator();
                if (g == null) {
                    dr.getEntry().setGenerator(new Generator());
                }
                g.setUri(MCRFrontendUtil.getBaseURL() + MCRConfiguration.instance().getString("MCR.SWORD.generator.uri", ""));
                g.setVersion(MCRConfiguration.instance().getString("MCR.SWORD.generator.version", ""));

                // Echo back the user agent
                if (request.getHeader(HttpHeaders.USER_AGENT) != null) {
                    dr.getEntry().setUserAgent(request.getHeader(HttpHeaders.USER_AGENT));
                }

                // Echo back the packaging format
                if (request.getHeader(HttpHeaders.X_PACKAGING) != null) {
                    dr.getEntry().setPackaging(request.getHeader(HttpHeaders.X_PACKAGING));
                }

                // Print out the Deposit Response
                response.setStatus(dr.getHttpResponse());
                String location = dr.getEntry().getLocation();
                if ((location != null) && (!location.isEmpty())) {
                    response.setHeader("Location", location);
                }
                response.setContentType("application/atom+xml; charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.write(dr.marshall());
                out.flush();
            }
        } catch (SWORDAuthenticationException sae) {
            // Ask for credentials again
            if ("Basic".equals(MCRSWORDUtils.getAuthN())) {
                String s = "BASIC realm=\"SWORD\"";
                response.setHeader("WWW-Authenticate", s);
                response.setStatus(401);
            }
        } catch (SWORDErrorException see) {
            // Get the details and send the right SWORD error document
            LOG.error(see.getMessage());
            MCRSWORDUtils.makeErrorDocument(see.getErrorURI(), see.getStatus(), see.getDescription(), request, response);
            return;
        } catch (SWORDException se) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            LOG.error(se.toString());
        } catch (NoSuchAlgorithmException nsae) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            LOG.error(nsae.toString());
        } catch (Exception e) {
            LOG.error(e.toString(), e);
            throw e;
        } finally {
            // Close the input stream if it still open
            if (fis != null) {
                fis.close();
            }

            // Try deleting the temp file
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
