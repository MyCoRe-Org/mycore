/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.common.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.transform.JDOMSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.services.query.MCRQueryCache;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;

/**
 * Reads XML documents from various URI types. This resolver is used to read
 * DTDs, XML Schema files, XSL document() usages, xsl:include usages and MyCoRe
 * Editor include declarations. DTDs and Schema files are read from the
 * CLASSPATH of the application when XML is parsed. XML document() calls and
 * xsl:include calls within XSL stylesheets can be read from URIs of type
 * resource, webapp, file, session, query or object. MyCoRe editor include 
 * declarations can read XML files from resource, webapp, file, session, 
 * http or https, query, or object URIs.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRURIResolver implements javax.xml.transform.URIResolver,
		EntityResolver {
	private static final Logger LOGGER = Logger.getLogger(MCRURIResolver.class);

	private static final String HOST_PARAM = "host";

	private static final String TYPE_PARAM = "type";

	private static final String QUERY_PARAM = "query";

	private static final String HOST_DEFAULT = "local";

	private static final String URL_ENCODING = MCRConfiguration.instance()
			.getString("MCR.request_charencoding", "UTF-8");;

	/**
	 * Initializes the MCRURIResolver for servlet applications.
	 * 
	 * @param ctx
	 *            the servlet context of this web application
	 * @param webAppBase
	 *            the base URL of this web application
	 */
	public static synchronized void init(ServletContext ctx, String webAppBase) {
		context = ctx;
		base = webAppBase;
	}

	private static MCRURIResolver singleton = null;

	/**
	 * Returns the MCRURIResolver singleton
	 */
	public static synchronized MCRURIResolver instance() {
		if (singleton == null)
			singleton = new MCRURIResolver();
		return singleton;
	}

	private static ServletContext context;

	private static String base;

	/** A cache of parsed XML files * */
	private MCRCache fileCache;

	private MCRCache bytesCache;

	/**
	 * Creates a new MCRURIResolver
	 */
	private MCRURIResolver() {
		MCRConfiguration config = MCRConfiguration.instance();
		String prefix = "MCR.URIResolver.";
		int cacheSize = config.getInt(prefix + "StaticFiles.CacheSize", 100);
		fileCache = new MCRCache(cacheSize);
		bytesCache = new MCRCache(cacheSize);
	}

	/**
	 * Returns the filename part of a path.
	 * 
	 * @param path
	 *            the path of a file
	 * @return the part after the last / or \\
	 */
	private String getFileName(String path) {
		int posA = path.lastIndexOf("/");
		int posB = path.lastIndexOf("\\");
		int pos = (posA == -1 ? posB : posA);
		return (pos == -1 ? path : path.substring(pos + 1));
	}

	/**
	 * URI Resolver that resolves XSL document() or xsl:include calls.
	 * 
	 * @see javax.xml.transform.URIResolver
	 */
	public Source resolve(String href, String base) throws TransformerException {
		if (base != null)
			LOGGER.debug("Including " + href + " from " + getFileName(base));
		else
			LOGGER.debug("Including " + href);

		if (href.indexOf(":") == -1)
			return null;

		String scheme = getScheme(href);
		if ("resource webapp file session query object".indexOf(scheme) != -1)
			return new JDOMSource(resolve(href));
		else
			return null;
	}

	/**
	 * Implements the SAX EntityResolver interface. This resolver type is used
	 * to read DTDs and XML Schema files when parsing XML documents. This
	 * resolver searches such files in the CLASSPATH of the current application.
	 * 
	 * @see org.xml.sax.EntityResolver
	 */
	public InputSource resolveEntity(String publicId, String systemId)
			throws org.xml.sax.SAXException, java.io.IOException {
		LOGGER.debug("Resolving " + publicId + " :: " + systemId);

		if (systemId == null)
			return null; // Use default resolver

		InputStream is = getCachedResource("resource:" + getFileName(systemId));
		if (is == null)
			return null; // Use default resolver

		LOGGER.debug("Reading " + getFileName(systemId));
		return new InputSource(is);
	}

	/**
	 * Returns the protocol or scheme for the given URI.
	 * 
	 * @param uri
	 *            the URI to parse
	 * @return the protocol/scheme part before the ":"
	 */
	public String getScheme(String uri) {
		return new StringTokenizer(uri, ":").nextToken();
	}

	/**
	 * Reads XML from URIs of various type.
	 * 
	 * @param uri
	 *            the URI where to read the XML from
	 * @return the root element of the XML document
	 */
	public Element resolve(String uri) {
		LOGGER.info("Reading xml from uri " + uri);

		String scheme = getScheme(uri);

		if ("resource".equals(scheme))
			return readFromResource(uri);
		else if ("webapp".equals(scheme))
			return readFromWebapp(uri);
		else if ("file".equals(scheme))
			return readFromFile(uri);
		else if ("query".equals(scheme))
			return readFromQuery(uri);
		else if ("object".equals(scheme))
			return readFromObject(uri);
		else if ("http".equals(scheme) || "https".equals(scheme))
			return readFromHTTP(uri);
		else if ("request".equals(scheme))
			return readFromRequest(uri);
		else if ("session".equals(scheme))
			return readFromSession(uri);
		else {
			String msg = "Unsupported URI type: " + uri;
			throw new MCRUsageException(msg);
		}
	}

	/**
	 * Reads XML from the CLASSPATH of the application.
	 * 
	 * @param uri
	 *            the location of the file in the format resource:path/to/file
	 * @return the root element of the XML document
	 */
	private Element readFromResource(String uri) {
		Element parsed = parseStream(getResourceStream(uri));
		return parsed;
	}

	private InputStream getResourceStream(String uri) {
		String path = uri.substring(uri.indexOf(":") + 1);
		LOGGER.debug("Reading xml from classpath resource " + path);
		return this.getClass().getResourceAsStream("/" + path);
	}

	private InputStream getCachedResource(String uri) throws IOException {
		byte[] bytes = (byte[]) (bytesCache.get(uri));
		if (bytes == null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			InputStream in = getResourceStream(uri);
			if (in == null)
				return null;
			MCRUtils.copyStream(in, baos);
			baos.close();
			in.close();
			bytes = baos.toByteArray();
			bytesCache.put(uri, bytes);
		}
		return new ByteArrayInputStream(bytes);
	}

	/**
	 * Reads XML from a static file in the current web application.
	 * 
	 * @param uri
	 *            the path to the file in the format webapp:path/to/file
	 * @return the root element of the XML document
	 */
	private Element readFromWebapp(String uri) {
		String path = uri.substring(uri.indexOf(":") + 1);
		LOGGER.debug("Reading xml from webapp " + path);
		uri = "file://" + context.getRealPath(path);
		return readFromFile(uri);
	}

	/**
	 * Reads XML from a file URL.
	 * 
	 * @param uri
	 *            the URL of the file in the format file://path/to/file
	 * @return the root element of the xml document
	 */
	private Element readFromFile(String uri) {
		String path = uri.substring("file://".length());
		LOGGER.debug("Reading xml from file " + path);
		File file = new File(path);
		Element fromCache = (Element) fileCache.getIfUpToDate(path, file
				.lastModified());
		if (fromCache != null)
			return (Element) (fromCache.clone());

		try {
			Element parsed = parseStream(new FileInputStream(file));
			fileCache.put(path, parsed);
			return (Element) (parsed.clone());
		} catch (FileNotFoundException ex) {
			String msg = "Could not find file for URI " + uri;
			throw new MCRUsageException(msg, ex);
		}
	}

	/**
	 * Reads XML from a http or https URL.
	 * 
	 * @param url
	 *            the URL of the xml document
	 * @return the root element of the xml document
	 */
	private Element readFromHTTP(String url) {
		LOGGER.debug("Reading xml from url " + url);

		try {
			return parseStream(new URL(url).openStream());
		} catch (java.net.MalformedURLException ex) {
			String msg = "Malformed http url: " + url;
			throw new MCRUsageException(msg, ex);
		} catch (IOException ex) {
			String msg = "Unable to open input stream at " + url;
			throw new MCRUsageException(msg, ex);
		}
	}

	/**
	 * Reads XML from a HTTP request to this web application.
	 * 
	 * @param uri
	 *            the URI in the format request:path/to/servlet
	 * @return the root element of the xml document
	 */
	private Element readFromRequest(String uri) {
		String path = uri.substring(uri.indexOf(":") + 1);
		LOGGER.debug("Reading xml from request " + path);

		StringBuffer url = new StringBuffer(MCRServlet.getBaseURL());
		url.append(path);

		if (path.indexOf("?") != -1)
			url.append("&");
		else
			url.append("?");

		url.append("MCRSessionID=");
		url.append(MCRSessionMgr.getCurrentSession().getID());

		return readFromHTTP(url.toString());
	}

	/**
	 * Reads XML from URIs of type session:key. The method MCRSession.get( key )
	 * is called and must return a JDOM element.
	 * 
	 * @see org.mycore.common.MCRSession#get( java.lang.String )
	 * 
	 * @param uri
	 *            the URI in the format session:key
	 * @return the root element of the xml document
	 */
	private Element readFromSession(String uri) {
		String key = uri.substring(uri.indexOf(":") + 1);

		LOGGER.debug("Reading xml from session using key " + key);
		Object value = MCRSessionMgr.getCurrentSession().get(key);
		return (Element) (((Element) value).clone());
	}
	
	/**
	 * Reads local MCRObject with a given ID from the store
	 * and returns its XML representation within MCRXMLContainer.
	 * 
	 * @param uri
	 * @return
	 **/
	private Element readFromObject(String uri) {
	  String id = uri.substring(uri.indexOf(":") + 1);
	  LOGGER.debug("Reading MCRObject with ID " + id );

	  try
	  {
	    MCRXMLContainer result = new MCRXMLContainer();
	    MCRObjectID mcrid = new MCRObjectID(id);
	    byte[] xml = MCRXMLTableManager.instance().retrieve(mcrid);
	    result.add("local",id,0,xml);
	    return result.exportAllToDocument().getRootElement();
	  }
	  catch( Exception ex )
	  {
	    LOGGER.debug( "Exception while reading MCRObject as XML", ex );
	    return null;
	  }
	}

	/**
	 * Returns query results as XML  
	 **/
	private Element readFromQuery(String uri) {
		String key = uri.substring(uri.indexOf(":") + 1);
		LOGGER.debug("Reading xml from query result using key :" + key);
		String[] param;
		String host, type, query;
		int pos;
		StringTokenizer tok = new StringTokenizer(key, "&");
		Hashtable params = new Hashtable();
		while (tok.hasMoreTokens()) {
			param = tok.nextToken().split("=");
			params.put(param[0], param[1]);
		}
		if (params.get(HOST_PARAM) == null)
			host = HOST_DEFAULT;
		else
			host = (String) params.get(HOST_PARAM);
		type = (String) params.get(TYPE_PARAM);
		query = (String) params.get(QUERY_PARAM);
		if (type == null)
			return null;
		StringTokenizer hosts = new StringTokenizer(host, ",");
		MCRXMLContainer results = new MCRXMLContainer();
		while (hosts.hasMoreTokens()) {
			try {
				results.importElements(query(hosts.nextToken(), type, query));
			} catch (NumberFormatException e) {
				LOGGER.error("Error while processing query: " + key, e);
			} catch (UnsupportedEncodingException e) {
				LOGGER.error("Error while processing query: " + key, e);
			}
		}
		return results.exportAllToDocument().getRootElement();
	}

	private MCRXMLContainer query(String host, String type, String query)
			throws NumberFormatException, UnsupportedEncodingException {
		if (query == null)
			query = "";
		return MCRQueryCache
				.getResultList(URLDecoder.decode(host, URL_ENCODING),
						URLDecoder.decode(query, URL_ENCODING), URLDecoder
								.decode(type, URL_ENCODING), MCRConfiguration
								.instance().getInt("MCR.query_max_results", 10));
	}

	/**
	 * Reads xml from an InputStream and returns the parsed root element.
	 * 
	 * @param in
	 *            the InputStream that contains the XML document
	 * @return the root element of the parsed input stream
	 */
	public Element parseStream(InputStream in) {
		SAXBuilder builder = new SAXBuilder();
		builder.setValidation(false);
		builder.setEntityResolver(this);

		try {
			return builder.build(in).getRootElement();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			String msg = "Exception while reading and parsing XML InputStream: "
					+ e.getMessage();
			throw new MCRUsageException(msg, e);
		} catch (Exception ex) {
			String msg = "Exception while reading and parsing XML InputStream: "
					+ ex.getMessage();
			throw new MCRUsageException(msg, ex);
		}
	}
}
