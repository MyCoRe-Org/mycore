package org.mycore.common.xml;

import org.mycore.common.MCRException;
import javax.servlet.ServletContext;
import java.util.Hashtable;
// JDOM imports
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.jdom.transform.JDOMResult;
// Xalan-J 2 imports
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * This Class uses XPath expressions for sorting the MCRResult list
 *
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
 */
public class MCRXMLSorter implements MCRXMLSortInterface {
	
	private boolean sorted			= false;
	private boolean reverse_sorted	= false;
	private ArrayList ObjectPool,orderList,sortKeys;
	private StringBuffer stylesheetBegin, 
	                     stylesheetEnd,
	                     stylesheetSorter,
	                     stylesheet;
	private MCRXMLContainer finalCont;
	private Object[] fContCont;
	private static Logger logger=Logger.getLogger(MCRXMLSorter.class);
	private static ServletContext CONTEXT;
	private static Hashtable StylePool;
	private static TransformerFactory factory;
	private static String StylePoolStr=MCRXMLSorter.class+".StylePool";
	/**
	 * initialize the Sorter with some Objects to sort
	 * @param XMLCont XMLContainer to be sort
	 */
	public MCRXMLSorter (MCRXMLContainer XMLCont){
		init();
		for (int i=0; i<XMLCont.size(); i++){
			add(XMLCont.exportElementToContainer(i));
		}
	}
	
	public MCRXMLSorter (){
		init();
	}
	
	private void init(){
		this.ObjectPool=new ArrayList();
		this.orderList=new ArrayList();
		this.sortKeys=new ArrayList();
		prepareStylesheets();
		if (factory==null) factory=TransformerFactory.newInstance();
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#addSortKey(java.lang.Object)
	 */
	public MCRXMLSortInterface addSortKey(Object sortKey) throws MCRException {
		return addSortKey(sortKey, MCRXMLSortInterface.INORDER);
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#addSortKey(java.lang.Object, boolean)
	 */
	public MCRXMLSortInterface addSortKey(Object sortKey, boolean order)
		throws MCRException {
		doUnsort();
		/* How can I check XPATH expression for correctness? */
		String StrSortKey=null;
		try{StrSortKey= (String)sortKey;
		}
		catch (ClassCastException cce){
		  throw new MCRException("Error while converting XPATH expression to String",cce);
		}
		finally {
			if (StrSortKey==null) return null;
		}
		logger.info("MCRXMLSorter: adding sort key: "+StrSortKey+ (order? " ascending":" descending"));
		sortKeys.add(StrSortKey);
		orderList.add(new Boolean(order));
		return this;
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#add(java.lang.Object)
	 */
	public MCRXMLSortInterface add(Object[] sortObjects) throws MCRException {
		for (int i=0; i<sortObjects.length; i++){
			try{
				add(sortObjects[i]);
			}
			catch (MCRException me){
				throw new MCRException("Error occured at position "+(i+1)+" of "+
				                       sortObjects.length+"!",me);
			}
		}
		return this;
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#add(java.lang.Object)
	 */
	public MCRXMLSortInterface add(Object sortObject) throws MCRException {
		doUnsort();
		if (sortObject instanceof MCRXMLContainer){
			/*We have a new document to sort*/
			/*seperate single results*/
			MCRXMLContainer result=null;
			MCRXMLContainer toSort=(MCRXMLContainer)sortObject;
			for (int i=0; i<toSort.size(); i++){
				result=new MCRXMLContainer();
				try {
					result.importElements(toSort.exportElementToDocument(i));
					ObjectPool.add(result);
				} catch (JDOMException e) {
					throw new MCRException(
					  "There was an error while importing a JDOM Document:\n"+
					  e.getMessage(),e);
				}
			}
		}
		else
			throw new MCRException("Object must be an instance of MCRXMLContainer!"); 
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
		if (ObjectPool.size()==0){
			//throw new MCRException("ObjectPool is empty!\n What should I sort?");
			logger.warn("MCRXMLSorter: ObjectPool is empty!\n What should I sort?");
			return ObjectPool.toArray();
		}
		if ((orderList.size()==0) || (sortKeys.size()==0)){
			//throw new MCRException("List of sorting keys is empty!\n How should I sort?");
			//maybe the list should returned unsorted here?
			logger.warn("MCRXMLSorter: List of sorting keys is empty!\n How should I sort?");
			return ObjectPool.toArray();
		}
		if (orderList.size()!=sortKeys.size())
			throw new MCRException("List of sorting keys is incorrect!\n"+
			          "Sizes of orderList("+orderList.size()+
			          ") and sortKeys("+sortKeys.size()+") differ!\nHow should I sort?");
		if (!sorted && reverse_sorted==reversed){
			ArrayList newOrderList=(ArrayList)orderList.clone();
			if (reversed){
				boolean test;
				for (int i=0;i<newOrderList.size();i++){
					test=((Boolean)newOrderList.get(i)).booleanValue();
					newOrderList.set(i,new Boolean(test?false:true));
				}
			}
			finalCont=new MCRXMLContainer();
			MCRXMLContainer tempCont=new MCRXMLContainer();
			for (int i=0;i<ObjectPool.size();i++){
				tempCont.importElements((MCRXMLContainer)ObjectPool.get(i));
			}
			buildSortingStylesheet();
			try{
				logger.info("MCRXMLSorter: sorting jdom...");
				Document jdom = transform(tempCont.exportAllToDocument());
				logger.info("\t done!");
				logger.info("MCRXMLSorter: building MCRXMLContainer...");
				finalCont.importElements(jdom);
				logger.info("\t done!");
				sorted=true;
				reverse_sorted=reversed;
				// finalCont.debug(); //checked
			}
			catch (IOException ioe){
				throw new MCRException("IOException while transforming document!",ioe);
			}
			catch (TransformerException te){
				te.printStackTrace();
				throw new MCRException("TransformerException while transforming document!",te);
			}
			catch (JDOMException je){
				throw new MCRException("JDOMException while transforming document!",je);
			}
			finally{
				//now we split the resulting XMLContainer into single ones
				if (finalCont.size()==0) fContCont=null;
				else{
					fContCont=new Object[finalCont.size()];
					for (int i=0;i<finalCont.size();i++){
						fContCont[i]=finalCont.exportElementToContainer(i);
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
		ObjectPool.clear();
		sorted=false;
	}

	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#clearSortKeys()
	 */
	public void clearSortKeys() {
		sortKeys.clear();
		orderList.clear();
		sorted=false;
	}
	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#setServletContext(ServletContext)
	 */
	public void setServletContext(ServletContext context){
		CONTEXT=context;
		if (CONTEXT.getAttribute(StylePoolStr)!=null)
		  StylePool=(Hashtable) CONTEXT.getAttribute(StylePoolStr);
		else {
			if (StylePool==null)
				StylePool=new Hashtable();
			CONTEXT.setAttribute(StylePoolStr,StylePool);
		}
	}
	/**
	 * @see org.mycore.common.xml.MCRXMLSortInterface#getServletContext()
	 */
	public ServletContext getServletContext(){
		return CONTEXT;
	}
	
	private void prepareStylesheets(){
		stylesheetBegin= 
		  new StringBuffer("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n\n")
		      .append("<!-- This file is machine generated and can be safely removed -->\n\n")
			  .append("<xsl:stylesheet version=\"1.0\"\n")
			  .append("     xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n\n")
			  .append("<xsl:output\n method=\"xml\"/>\n\n")
			  .append("<xsl:template match=\"/mcr_results\">\n\n")
			  .append("<xsl:element name=\"mcr_results\">\n\n")
			  .append("<xsl:for-each select=\"mcr_result\">\n");
		  
		stylesheetEnd=
		  new StringBuffer("  <xsl:copy-of select=\".\"/>\n")
		      .append("</xsl:for-each>\n\n")
		      .append("</xsl:element>\n\n")
		      .append("</xsl:template>\n\n")
		      .append("</xsl:stylesheet>\n");
		
	}

	/**
	 * <em>buildSortingStylesheet</em> creates an XSL stylesheet which can sort a list
	 * of documents according to a given attribute in ascending order.
	 *
	 */
	private void buildSortingStylesheet (){
		if (!sorted){
			generateXSLSort();
			stylesheet=new StringBuffer(stylesheetBegin.toString())
		               .append(stylesheetSorter.toString())
		               .append(stylesheetEnd.toString());
		}
	}
	
	private void generateXSLSort(){
		if (!sorted){
			stylesheetSorter=new StringBuffer();
			for (int i=0; i<sortKeys.size();i++){
				stylesheetSorter.append("<xsl:sort order=\"")
   .append( (((((Boolean)orderList.get(i)).booleanValue())==MCRXMLSortInterface.INORDER)? "ascending" : "descending") )
   		       	.append("\" select=\"")
       		    .append((String)sortKeys.get(i))
      		    .append("\"/>\n");
			}
		}
	}

	private final void doUnsort(){
		sorted=false;
		reverse_sorted=false;
	}
	/**
	 * 
	 * @param sourceDoc XMLDocument to be sorted with a stylesheet
	 * @return Document transformed XMLDocument
	 * @throws IOException for an i/o eception
	 * @throws TransformerException if stylesheet fails
	 * @throws JDOMException if Document is somehow not well formed
	 */
	public Document transform(Document sourceDoc)
	 throws IOException, TransformerException, JDOMException {
		JDOMResult toXML=new JDOMResult();
		Templates stylesheet=getCompiledStylesheet();
		TransformerHandler handler = ((SAXTransformerFactory)factory).newTransformerHandler( stylesheet );
		handler.setResult(toXML);
		new org.jdom.output.SAXOutputter( handler ).output( sourceDoc );
		return toXML.getDocument();
	}
	private Templates getCompiledStylesheet(){
		if (StylePool!=null){
			Templates stylesheet;
			Hashtable orderList=(Hashtable)StylePool.get(this.orderList);
			if (orderList==null){
				orderList=new Hashtable();
				StylePool.put(this.orderList, orderList);
			}
			stylesheet=(Templates)orderList.get(this.sortKeys);
			if (stylesheet==null){
				stylesheet=compileStylesheet();
				orderList.put(this.sortKeys, stylesheet);
				StylePool.put(this.orderList, orderList);
			}
			return stylesheet;
		}
		else return compileStylesheet();
	}
	private Templates compileStylesheet(){
		buildSortingStylesheet();
        Templates stylesheet;
		try {
			stylesheet =
				factory.newTemplates(
					new StreamSource(
						new ByteArrayInputStream(
							this.stylesheet.toString().getBytes())));
		} catch (TransformerConfigurationException e) {
        	String msg = "Error while compiling XSL stylesheet ";
        	throw new MCRException( msg, e );
		}
        return stylesheet;
	}
}
