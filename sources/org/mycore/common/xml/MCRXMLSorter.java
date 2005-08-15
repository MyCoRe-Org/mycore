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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.transform.JDOMResult;

import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

/**
 * This Class uses XPath expressions for sorting the MCRResult list
 * 
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
 */
public class MCRXMLSorter implements MCRXMLSortInterface {

	private boolean sorted = false;

	private boolean reverseSorted = false;

	private ArrayList objectPool, orderList, sortKeys;

	private StringBuffer stylesheetBegin, stylesheetEnd, stylesheetSorter,
			stylesheet;

	private MCRXMLContainer finalCont;

	private Object[] fContCont;

	private static Logger LOGGER = Logger.getLogger(MCRXMLSorter.class);

	private static ServletContext CONTEXT;

	private static Hashtable STYLE_POOL;

	private static TransformerFactory FACTORY;

	private static String STYLE_POOL_STR = MCRXMLSorter.class + ".StylePool";

	/**
	 * initialize the Sorter with some Objects to sort
	 * 
	 * @param XMLCont
	 *            XMLContainer to be sort
	 */
	public MCRXMLSorter(MCRXMLContainer XMLCont) {
		init();
		for (int i = 0; i < XMLCont.size(); i++) {
			add(XMLCont.exportElementToContainer(i));
		}
	}

	public MCRXMLSorter() {
		init();
	}

	private void init() {
		this.objectPool = new ArrayList();
		this.orderList = new ArrayList();
		this.sortKeys = new ArrayList();
		prepareStylesheets();
		if (FACTORY == null) {
			FACTORY = TransformerFactory.newInstance();
			FACTORY.setURIResolver(MCRURIResolver.instance());
		}
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#addSortKey(java.lang.Object)
	 */
	public MCRXMLSortInterface addSortKey(Object sortKey) throws MCRException {
		return addSortKey(sortKey, MCRXMLSortInterface.INORDER);
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#addSortKey(java.lang.Object,
	 *      boolean)
	 */
	public MCRXMLSortInterface addSortKey(Object sortKey, boolean order)
			throws MCRException {
		doUnsort();
		/* How can I check XPATH expression for correctness? */
		String StrSortKey = null;
		try {
			StrSortKey = (String) sortKey;
		} catch (ClassCastException cce) {
			throw new MCRException(
					"Error while converting XPATH expression to String", cce);
		} finally {
			if (StrSortKey == null)
				return null;
		}
		LOGGER.info("MCRXMLSorter: adding sort key: " + StrSortKey
				+ (order ? " ascending" : " descending"));
		sortKeys.add(StrSortKey);
		orderList.add(new Boolean(order));
		return this;
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#add(java.lang.Object)
	 */
	public MCRXMLSortInterface add(Object[] sortObjects) throws MCRException {
		for (int i = 0; i < sortObjects.length; i++) {
			try {
				add(sortObjects[i]);
			} catch (MCRException me) {
				throw new MCRException("Error occured at position " + (i + 1)
						+ " of " + sortObjects.length + "!", me);
			}
		}
		return this;
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#add(java.lang.Object)
	 */
	public MCRXMLSortInterface add(Object sortObject) throws MCRException {
		doUnsort();
		if (sortObject instanceof MCRXMLContainer) {
			/* We have a new document to sort */
			/* seperate single results */
			MCRXMLContainer result = null;
			MCRXMLContainer toSort = (MCRXMLContainer) sortObject;
			for (int i = 0; i < toSort.size(); i++) {
				result = new MCRXMLContainer();
				try {
					result.importElements(toSort.exportElementToDocument(i));
					objectPool.add(result);
				} catch (JDOMException e) {
					throw new MCRException(
							"There was an error while importing a JDOM Document:\n"
									+ e.getMessage(), e);
				}
			}
		} else
			throw new MCRException(
					"Object must be an instance of MCRXMLContainer!");
		return this;
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#sort()
	 */
	public Object[] sort() throws MCRException {
		return sort(false);
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#sort(boolean)
	 */
	public Object[] sort(boolean reversed) throws MCRException {
		if (objectPool.size() == 0) {
			//throw new MCRException("ObjectPool is empty!\n What should I
			// sort?");
			LOGGER
					.warn("MCRXMLSorter: ObjectPool is empty!\n What should I sort?");
			return objectPool.toArray();
		}
		if ((orderList.size() == 0) || (sortKeys.size() == 0)) {
			//throw new MCRException("List of sorting keys is empty!\n How
			// should I sort?");
			//maybe the list should returned unsorted here?
			LOGGER
					.warn("MCRXMLSorter: List of sorting keys is empty!\n How should I sort?");
			return objectPool.toArray();
		}
		if (orderList.size() != sortKeys.size())
			throw new MCRException("List of sorting keys is incorrect!\n"
					+ "Sizes of orderList(" + orderList.size()
					+ ") and sortKeys(" + sortKeys.size()
					+ ") differ!\nHow should I sort?");
		if (!sorted && reverseSorted == reversed) {
			ArrayList newOrderList = (ArrayList) orderList.clone();
			if (reversed) {
				boolean test;
				for (int i = 0; i < newOrderList.size(); i++) {
					test = ((Boolean) newOrderList.get(i)).booleanValue();
					newOrderList.set(i, new Boolean(test ? false : true));
				}
			}
			finalCont = new MCRXMLContainer();
			MCRXMLContainer tempCont = new MCRXMLContainer();
			for (int i = 0; i < objectPool.size(); i++) {
				tempCont.importElements((MCRXMLContainer) objectPool.get(i));
			}
			buildSortingStylesheet();
			try {
				LOGGER.info("MCRXMLSorter: sorting jdom...");
				Document jdom = transform(tempCont.exportAllToDocument());
				LOGGER.info("\t done!");
				LOGGER.info("MCRXMLSorter: building MCRXMLContainer...");
				finalCont.importElements(jdom);
				LOGGER.info("\t done!");
				sorted = true;
				reverseSorted = reversed;
				// finalCont.debug(); //checked
			} catch (IOException ioe) {
				throw new MCRException(
						"IOException while transforming document!", ioe);
			} catch (TransformerException te) {
				te.printStackTrace();
				throw new MCRException(
						"TransformerException while transforming document!", te);
			} catch (JDOMException je) {
				throw new MCRException(
						"JDOMException while transforming document!", je);
			} finally {
				//now we split the resulting XMLContainer into single ones
				if (finalCont.size() == 0)
					fContCont = null;
				else {
					fContCont = new Object[finalCont.size()];
					for (int i = 0; i < finalCont.size(); i++) {
						fContCont[i] = finalCont.exportElementToContainer(i);
					}
				}
			}
		}
		return fContCont;
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#clearObjects()
	 */
	public void clearObjects() {
		objectPool.clear();
		sorted = false;
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#clearSortKeys()
	 */
	public void clearSortKeys() {
		sortKeys.clear();
		orderList.clear();
		sorted = false;
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#setServletContext(ServletContext)
	 */
	public void setServletContext(ServletContext context) {
		CONTEXT = context;
		if (CONTEXT.getAttribute(STYLE_POOL_STR) != null)
			STYLE_POOL = (Hashtable) CONTEXT.getAttribute(STYLE_POOL_STR);
		else {
			if (STYLE_POOL == null)
				STYLE_POOL = new Hashtable();
			CONTEXT.setAttribute(STYLE_POOL_STR, STYLE_POOL);
		}
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#getServletContext()
	 */
	public ServletContext getServletContext() {
		return CONTEXT;
	}

	private void prepareStylesheets() {
		stylesheetBegin = new StringBuffer(
				"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n\n")
				.append(
						"<!-- This file is machine generated and can be safely removed -->\n\n")
				.append("<xsl:stylesheet version=\"1.0\"\n")
				.append(
						"     xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n")
				.append("     xmlns:xlink=\"").append(MCRDefaults.XLINK_URL)
				.append("\">\n\n").append("<xsl:output\n method=\"xml\"/>\n\n")
				.append("<xsl:template match=\"/mcr_results\">\n\n").append(
						"<xsl:element name=\"mcr_results\">\n\n").append(
						"<xsl:for-each select=\"mcr_result\">\n");

		stylesheetEnd = new StringBuffer("  <xsl:copy-of select=\".\"/>\n")
				.append("</xsl:for-each>\n\n").append("</xsl:element>\n\n")
				.append("</xsl:template>\n\n").append("</xsl:stylesheet>\n");

	}

	/**
	 * <em>buildSortingStylesheet</em> creates an XSL stylesheet which can
	 * sort a list of documents according to a given attribute in ascending
	 * order.
	 *  
	 */
	private void buildSortingStylesheet() {
		if (!sorted) {
			generateXSLSort();
			stylesheet = new StringBuffer(stylesheetBegin.toString()).append(
					stylesheetSorter.toString()).append(
					stylesheetEnd.toString());
			//TODO: removed this after get it working
			LOGGER.debug(stylesheet);
		}
	}

	private void generateXSLSort() {
		if (!sorted) {
			stylesheetSorter = new StringBuffer();
			for (int i = 0; i < sortKeys.size(); i++) {
				stylesheetSorter
						.append("<xsl:sort order=\"")
						.append(
								(((((Boolean) orderList.get(i)).booleanValue()) == MCRXMLSortInterface.INORDER) ? "ascending"
										: "descending")).append("\" select=\"")
						.append((String) sortKeys.get(i)).append("\"/>\n");
			}
		}
	}

	private final void doUnsort() {
		sorted = false;
		reverseSorted = false;
	}

	/**
	 * 
	 * @param sourceDoc
	 *            XMLDocument to be sorted with a stylesheet
	 * @return Document transformed XMLDocument
	 * @throws IOException
	 *             for an i/o eception
	 * @throws TransformerException
	 *             if stylesheet fails
	 * @throws JDOMException
	 *             if Document is somehow not well formed
	 */
	public Document transform(Document sourceDoc) throws IOException,
			TransformerException, JDOMException {
		JDOMResult toXML = new JDOMResult();
		Templates stylesheet = getCompiledStylesheet();
		TransformerHandler handler = ((SAXTransformerFactory) FACTORY)
				.newTransformerHandler(stylesheet);
		handler.setResult(toXML);
		new org.jdom.output.SAXOutputter(handler).output(sourceDoc);
		Document result = toXML.getDocument();
		if (LOGGER.isDebugEnabled()) {
			File tmpdir = new File(System.getProperty("java.io.tmpdir"));
			LOGGER.debug("Saving XML files for debugging in: "
					+ tmpdir.getAbsolutePath());
			MCRUtils.saveJDOM(sourceDoc, new File(tmpdir, "unsorted.xml"));
			MCRUtils.saveJDOM(result, new File(tmpdir, "sorted.xml"));
		}
		return result;
	}

	private Templates getCompiledStylesheet() {
		if (STYLE_POOL != null) {
			Templates stylesheet;
			Hashtable orderList = (Hashtable) STYLE_POOL.get(this.orderList);
			if (orderList == null) {
				orderList = new Hashtable();
				STYLE_POOL.put(this.orderList, orderList);
			}
			stylesheet = (Templates) orderList.get(this.sortKeys);
			if (stylesheet == null) {
				stylesheet = compileStylesheet();
				orderList.put(this.sortKeys, stylesheet);
				STYLE_POOL.put(this.orderList, orderList);
			}
			return stylesheet;
		}
		return compileStylesheet();
	}

	private Templates compileStylesheet() {
		buildSortingStylesheet();
		Templates stylesheet;
		try {
			stylesheet = FACTORY.newTemplates(new StreamSource(
					new ByteArrayInputStream(this.stylesheet.toString()
							.getBytes())));
		} catch (TransformerConfigurationException e) {
			String msg = "Error while compiling XSL stylesheet ";
			throw new MCRException(msg, e);
		}
		return stylesheet;
	}
}