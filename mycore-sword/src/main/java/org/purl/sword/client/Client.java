/**
 * Copyright (c) 2008, Aberystwyth University
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 */
package org.purl.sword.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.purl.sword.base.ChecksumUtils;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.SwordValidationInfo;
import org.purl.sword.base.UnmarshallException;

/**
 * This is an example Client implementation to demonstrate how to connect to a
 * SWORD server. The client supports BASIC HTTP Authentication. This can be
 * initialised by setting a username and password.
 * 
 * @author Neil Taylor
 */
public class Client implements SWORDClient {
	/**
	 * The status field for the response code from the recent network access.
	 */
	private Status status;

	/**
	 * The name of the server to contact.
	 */
	private String server;

	/**
	 * The port number for the server.
	 */
	private int port;

	/**
	 * Specifies if the network access should use HTTP authentication.
	 */
	private boolean doAuthentication;

	/**
	 * The username to use for Basic Authentication.
	 */
	private String username;

	/**
	 * User password that is to be used.
	 */
	private String password;

	/**
	 * The userAgent to identify this application.
	 */
	private String userAgent;

	/**
	 * The client that is used to send data to the specified server.
	 */
	private DefaultHttpClient client;

	/**
	 * The default connection timeout. This can be modified by using the
	 * setSocketTimeout method.
	 */
	public static final int DEFAULT_TIMEOUT = 20000;

	/**
	 * Logger.
	 */
	private static Logger log = Logger.getLogger(Client.class);

	/**
	 * Create a new Client. The client will not use authentication by default.
	 */
	public Client() {
		client = new DefaultHttpClient();
		client.getParams().setParameter("http.socket.timeout",
                DEFAULT_TIMEOUT);
		HttpHost proxy = (HttpHost) client.getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY);
		if (proxy != null) {
		    
		    log.debug("proxy host: " + proxy.getHostName());
		    log.debug("proxy port: " + proxy.getPort());
		}
        doAuthentication = false;
	}

	/**
	 * Initialise the server that will be used to send the network access.
	 */
	public void setServer(String server, int port) {
		this.server = server;
		this.port = port;
	}

	/**
	 * Set the user credentials that will be used when making the access to the
	 * server.
	 * 
	 * @param username
	 *            The username.
	 * @param password
	 *            The password.
	 */
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
		doAuthentication = true;
	}

	/**
	 * Set the basic credentials. You must have previously set the server and
	 * port using setServer.
	 * 
	 * @param username
	 * @param password
	 */
	private void setBasicCredentials(String username, String password) {
		log.debug("server: " + server + " port: " + port + " u: '" + username
				+ "' p '" + password + "'");
		client.getCredentialsProvider().setCredentials(new AuthScope(server, port),
                new UsernamePasswordCredentials(username, password));
	}

	/**
	 * Set a proxy that should be used by the client when trying to access the
	 * server. If this is not set, the client will attempt to make a direct
	 * direct connection to the server. The port is set to 80.
	 * 
	 * @param host
	 *            The hostname.
	 */
	public void setProxy(String host) {
		setProxy(host, 80);
	}

	/**
	 * Set a proxy that should be used by the client when trying to access the
	 * server. If this is not set, the client will attempt to make a direct
	 * direct connection to the server.
	 * 
	 * @param host
	 *            The name of the host.
	 * @param port
	 *            The port.
	 */
	public void setProxy(String host, int port) {
	    client.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, new HttpHost(host, port));
	}

	/**
	 * Clear the proxy setting.
	 */
	public void clearProxy() {
	    client.getParams().removeParameter(ConnRouteParams.DEFAULT_PROXY);
	}

	/**
	 * Clear any user credentials that have been set for this client.
	 */
	public void clearCredentials() {
		client.setCredentialsProvider(new BasicCredentialsProvider());
		doAuthentication = false;
	}

    public void setUserAgent(String userAgent){
        this.userAgent = userAgent;
    }
	/**
	 * Set the connection timeout for the socket.
	 * 
	 * @param milliseconds
	 *            The time, expressed as a number of milliseconds.
	 */
	public void setSocketTimeout(int milliseconds) {
		client.getParams().setParameter("http.socket.timeout",
                milliseconds);
	}

	/**
	 * Retrieve the service document. The service document is located at the
	 * specified URL. This calls getServiceDocument(url,onBehalfOf).
	 * 
	 * @param url
	 *            The location of the service document.
	 * @return The ServiceDocument, or <code>null</code> if there was a
	 *         problem accessing the document. e.g. invalid access.
	 * 
	 * @throws SWORDClientException
	 *             If there is an error accessing the resource.
	 */
	public ServiceDocument getServiceDocument(String url)
			throws SWORDClientException {
		return getServiceDocument(url, null);
	}

	/**
	 * Retrieve the service document. The service document is located at the
	 * specified URL. This calls getServiceDocument(url,onBehalfOf).
	 * 
	 * @param url
	 *            The location of the service document.
	 * @return The ServiceDocument, or <code>null</code> if there was a
	 *         problem accessing the document. e.g. invalid access.
	 * 
	 * @throws SWORDClientException
	 *             If there is an error accessing the resource.
	 */
	public ServiceDocument getServiceDocument(String url, String onBehalfOf)
			throws SWORDClientException {
		URL serviceDocURL = null;
		try {
			serviceDocURL = new URL(url);
		} catch (MalformedURLException e) {
			// Try relative URL
			URL baseURL = null;
			try {
				baseURL = new URL("http", server, port, "/");
				serviceDocURL = new URL(baseURL, (url == null) ? "" : url);
			} catch (MalformedURLException e1) {
				// No dice, can't even form base URL...
				throw new SWORDClientException(url + " is not a valid URL ("
						+ e1.getMessage()
						+ "), and could not form a relative one from: "
						+ baseURL + " / " + url, e1);
			}
		}
		
		HttpGet httpget = new HttpGet(serviceDocURL.toExternalForm());
		if (doAuthentication) {
			// this does not perform any check on the username password. It
			// relies on the server to determine if the values are correct.
		    
			setBasicCredentials(username, password);
			// Create AuthCache instance
            AuthCache authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local auth cache
            HttpHost targetHost = URIUtils.extractHost(httpget.getURI());
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(targetHost, basicAuth);

            // Add AuthCache to the execution context
            BasicHttpContext localcontext = new BasicHttpContext();
            localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
		}

        Properties properties = new Properties();

		if (containsValue(onBehalfOf)) {
			log.debug("Setting on-behalf-of: " + onBehalfOf);
			httpget.addHeader(HttpHeaders.X_ON_BEHALF_OF, onBehalfOf);
            properties.put(HttpHeaders.X_ON_BEHALF_OF, onBehalfOf);
		}

		if (containsValue(userAgent)) {
			log.debug("Setting userAgent: " + userAgent);
			httpget.addHeader(HttpHeaders.USER_AGENT,
					userAgent);
            properties.put(HttpHeaders.USER_AGENT, userAgent);
		}

		ServiceDocument doc = null;

		try {
			HttpResponse httpResponse = client.execute(httpget);
			// store the status code
			StatusLine statusLine = httpResponse.getStatusLine();
            status = new Status(statusLine.getStatusCode(), statusLine.getReasonPhrase());

			if (status.getCode() == HttpStatus.SC_OK) {
				String message = EntityUtils.toString(httpResponse.getEntity());
				log.debug("returned message is: " + message);
				doc = new ServiceDocument();
				lastUnmarshallInfo = doc.unmarshall(message, properties);
			} else {
				throw new SWORDClientException(
						"Received error from service document request: "
								+ status);
			}
		} catch (IOException ioex) {
			throw new SWORDClientException(ioex.getMessage(), ioex);
		} catch (UnmarshallException uex) {
			throw new SWORDClientException(uex.getMessage(), uex);
		}

		return doc;
	}

    private SwordValidationInfo lastUnmarshallInfo;

    public SwordValidationInfo getLastUnmarshallInfo()
    {
        return lastUnmarshallInfo;
    }

	/**
	 * Post a file to the server. The different elements of the post are encoded
	 * in the specified message.
	 * 
	 * @param message
	 *            The message that contains the post information.
	 * 
	 * @throws SWORDClientException
	 *             if there is an error during the post operation.
	 */
	public DepositResponse postFile(PostMessage message)
			throws SWORDClientException {
		if (message == null) {
			throw new SWORDClientException("Message cannot be null.");
		}

		HttpPost httppost = new HttpPost(message.getDestination());
		BasicHttpContext localcontext = null;
		
		if (doAuthentication) {
			setBasicCredentials(username, password);
			
			// Create AuthCache instance
            AuthCache authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local auth cache
            HttpHost targetHost = URIUtils.extractHost(httppost.getURI());
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(targetHost, basicAuth);

            // Add AuthCache to the execution context
            localcontext = new BasicHttpContext();
            localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
		}

		DepositResponse response = null;
		FileInputStream stream = null;

		String messageBody = "";
		
		try {
			if (message.isUseMD5()) {
				String md5 = ChecksumUtils.generateMD5(message.getFilepath());
				if (message.getChecksumError()) {
					md5 = "1234567890";
				}
				log.debug("checksum error is: " + md5);
				if (md5 != null) {
					httppost.addHeader(HttpHeaders.CONTENT_MD5, md5);
				}
			}

			String filename = message.getFilename();
			if (! "".equals(filename)) {
				httppost.addHeader(HttpHeaders.CONTENT_DISPOSITION, " filename="
								+ filename);
			}

			if (containsValue(message.getSlug())) {
				httppost.addHeader(HttpHeaders.SLUG, message
						.getSlug());
			}

            if(message.getCorruptRequest())
            {
                // insert a header with an invalid boolean value
                httppost.addHeader(HttpHeaders.X_NO_OP, "Wibble");
            }else{
                httppost.addHeader(HttpHeaders.X_NO_OP, Boolean
					.toString(message.isNoOp()));
            }
			httppost.addHeader(HttpHeaders.X_VERBOSE, Boolean
					.toString(message.isVerbose()));

			String packaging = message.getPackaging();
			if (packaging != null && packaging.length() > 0) {
				httppost.addHeader(HttpHeaders.X_PACKAGING, packaging);
			}

			String onBehalfOf = message.getOnBehalfOf();
			if (containsValue(onBehalfOf)) {
				httppost.addHeader(HttpHeaders.X_ON_BEHALF_OF, onBehalfOf);
			}
			
			String userAgent = message.getUserAgent();
			if (containsValue(userAgent)) {
				httppost.addHeader(HttpHeaders.USER_AGENT, userAgent);
			}

			File file = new File(message.getFilepath());
			stream = new FileInputStream(file);

			InputStreamEntity requestEntity = new InputStreamEntity(stream, file.length());
			httppost.setEntity(requestEntity);

			HttpResponse httpresponse = localcontext != null ? client.execute(httppost, localcontext) : client.execute(httppost);
			
			StatusLine statusLine = httpresponse.getStatusLine();
            status = new Status(statusLine.getStatusCode(), statusLine.getReasonPhrase());

			log.info("Checking the status code: " + status.getCode());

			if (status.getCode() == HttpStatus.SC_ACCEPTED
					|| status.getCode() == HttpStatus.SC_CREATED) {
				messageBody = EntityUtils.toString(httpresponse.getEntity());
				response = new DepositResponse(status.getCode());
				response.getEntry().setLocation(httpresponse.getLastHeader("Location").getValue());
				// added call for the status code.
				lastUnmarshallInfo = response.unmarshall(messageBody, new Properties());
            }
			else {
				messageBody = EntityUtils.toString(httpresponse.getEntity());
				response = new DepositResponse(status.getCode());
				response.unmarshallErrorDocument(messageBody);
			}
			return response;

		} catch (NoSuchAlgorithmException nex) {
			throw new SWORDClientException("Unable to use MD5. "
					+ nex.getMessage(), nex);
		} catch (IOException ioex) {
			throw new SWORDClientException(ioex.getMessage(), ioex);
		} catch (UnmarshallException uex) {
			throw new SWORDClientException(uex.getMessage() + "(<pre>" + messageBody + "</pre>)", uex);
		} finally {

			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException ioe) {
				log.error("Error closing a stream");
				throw new SWORDClientException(ioe.getMessage(), ioe);
			}
		}

	}

	/**
	 * Return the status information that was returned from the most recent
	 * request sent to the server.
	 * 
	 * @return The status code returned from the most recent access.
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Check to see if the specified item contains a non-empty string.
	 * 
	 * @param item
	 *            The string to check.
	 * @return True if the string is not null and has a length greater than 0
	 *         after any whitespace is trimmed from the start and end.
	 *         Otherwise, false.
	 */
	private boolean containsValue(String item) {
		return ((item != null) && (item.trim().length() > 0));
	}

}