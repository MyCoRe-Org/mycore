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

import java.io.*;
import java.util.*;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSortable;
import org.mycore.common.MCRUtils;
import org.apache.log4j.Logger;

/**
 * This class is the cache of one result list included all XML files as result
 * of one query. They holds informations about host, rank and the XML byte
 * stream. You can get the complete result or elements of them as XML JDOM
 * Document for transforming with XSLT.
 *
 * @author Jens Kupferschmidt
 * @author Mathias Zarick
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
 */
public class MCRXMLContainer implements MCRSortable {

    // data
    private ArrayList host;

    private ArrayList mcrId;

    private ArrayList rank;

    private ArrayList xml;

    private ArrayList status;

    private String defaultEncoding;

    private static final Logger LOGGER = Logger
            .getLogger(MCRXMLContainer.class);

    protected static final SAXBuilder SAX_BUILDER = new org.jdom.input.SAXBuilder();

    /** The tag for the result collection * */
    public static final String TAG_RESULTS = "mcr_results";

    /** The tag for one result * */
    public static final String TAG_RESULT = "mcr_result";

    /** The tag for the object * */
    public static final String TAG_OBJECT = "mycoreobject";

    /** The attribute of the host name * */
    public static final String ATTR_HOST = "host";

    /** The attribute of the MCRObjectId * */
    public static final String ATTR_ID = "id";

    /** The attribute of the rank * */
    public static final String ATTR_RANK = "rank";

    /** The attribute of the neighbour status * */
    public static final String ATTR_PRED = "hasPred";

    /** The attribute of the neighbour status * */
    public static final String ATTR_SUCC = "hasSucc";

    /** The value for status when Doc is single */
    public static final int SINGLE_DOC = 0;

    /** The value for status when Doc is first */
    public static final int FIRST_DOC = 1;

    /** The value for status when Doc is last */
    public static final int LAST_DOC = 2;

    /** The value for status when Doc is in between */
    public static final int MIDDLE_DOC = 3;

    private static String ERROR_TEXT = "The stream for the MCRXMLContainer import is false.";

    /**
     * This constructor create the MCRXMLContainer class with an empty query
     * result list.
     */
    public MCRXMLContainer() {
        init();
    }

    private void init() {
        MCRConfiguration config = MCRConfiguration.instance();
        defaultEncoding = config.getString("MCR.metadata_default_encoding",
                "UTF-8");
        host = new ArrayList();
        mcrId = new ArrayList();
        rank = new ArrayList();
        xml = new ArrayList();
        status = new ArrayList();
    }

    /**
     * This constructor create the MCRXMLContainer class with a given query
     * result list.
     *
     * @param in
     *            a MCRXMLContainer as input
     */
    public MCRXMLContainer(MCRXMLContainer in) {
        init();
        for (int i = 0; i < in.size(); i++) {
            host.add(in.getHost(i));
            mcrId.add(in.getId(i));
            rank.add(new Integer(in.getRank(i)));
            xml.add(in.getXML(i));
        }
        resetStatus();
    }

    /**
     * This methode return the size of the result list.
     *
     * @return the size of the result list
     */
    public final int size() {
        return host.size();
    }

    /**
     * This methode return the host of an element index.
     *
     * @param index
     *            the index in the list
     * @return an empty string if the index is outside the border, else return
     *         the host name
     */
    public final String getHost(int index) {
        if ((index < 0) || (index >= host.size())) {
            return "";
        }
        return (String) host.get(index);
    }

    /**
     * This method sets a host element at a specified position.
     *
     * @param index
     *            the index in the list
     * @param newhost
     *            the new value in the list
     */
    public final void setHost(int index, String newhost) {
        host.set(index, newhost);
    }

    /**
     * This methode return the MCRObjectId of an element index as string.
     *
     * @param index
     *            the index in the list
     * @return an empty string if the index is outside the border, else return
     *         the MCRObjectId as string.
     */
    public final String getId(int index) {
        if ((index < 0) || (index >= mcrId.size())) {
            return "";
        }
        return (String) mcrId.get(index);
    }

    /**
     * This methode return the rank of an element index.
     *
     * @param index
     *            the index in the list
     * @return -1 if the index is outside the border, else return the rank
     */
    public final int getRank(int index) {
        if ((index < 0) || (index >= rank.size())) {
            return -1;
        }
        return ((Integer) rank.get(index)).intValue();
    }

    /**
     * This method returns the neighbour status of an element index.
     *
     * @param index
     *            the index in the list
     * @return -1 if the index is outside the border, else return the rank
     */
    public final int getStatus(int index) {
        if ((index < 0) || (index >= status.size())) {
            return -1;
        }
        return ((Integer) status.get(index)).intValue();
    }

    /**
     * This method sets the neighbour status of an element index.
     *
     * @param index
     *            the index in the list
     * @param instatus
     *            the new value in the list
     */
    public final void setStatus(int index, int instatus) {
        status.set(index, new Integer(instatus));
    }

    /**
     * This methode return the mycoreobject as JDOM Element of an element index.
     *
     * @param index
     *            the index in the list
     * @return an null if the index is outside the border, else return the
     *         mycoreobject as JDOM Element
     */
    public final org.jdom.Element getXML(int index) {
        if ((index < 0) || (index >= host.size())) {
            return null;
        }
        return (org.jdom.Element) xml.get(index);
    }

    /**
     * This methode add one element to the result list.
     *
     * @param in_host
     *            the host input as a string
     * @param in_id
     *            the MCRObjectId input as a string
     * @param in_rank
     *            the rank input as an integer
     * @param in_xml
     *            the JDOM Element of a mycoreobject
     */
    public final void add(String in_host, String in_id, int in_rank,
            org.jdom.Element in_xml) {
        int index;
        synchronized (host) {
            index = host.size();
        }
        host.add(index, in_host);
        mcrId.add(index, in_id);
        rank.add(index, new Integer(in_rank));
        xml.add(index, in_xml);
        int in_status = 0;
        if (index > 0) {
            status.set((index - 1), new Integer(((Integer) status
                    .get(index - 1)).intValue() + 1));
            in_status = 2;
        }
        status.add(new Integer(in_status));
    }

    /**
     * This methode add one element to the result list.
     *
     * @param in_host
     *            the host input as a string
     * @param in_id
     *            the MCRObjectId input as a string
     * @param in_rank
     *            the rank input as an integer
     * @param in_xml
     *            the well formed XML stream as a byte array
     * @exception org.jdom.JDOMException
     *                if a JDOm error was occured
     */
    public final void add(String in_host, String in_id, int in_rank,
            byte[] in_xml) throws JDOMException, IOException {
        BufferedInputStream bin = new BufferedInputStream(
                new ByteArrayInputStream(in_xml));
        org.jdom.Document jdom = SAX_BUILDER.build(bin);
        bin.close();
        org.jdom.Element root = jdom.getRootElement();
        add(in_host, in_id, in_rank, root);
    }

    /**
     * This methode return a well formed XML stream of the result collection as
     * a JDOM document. <br>
     * &lt;?xml version="1.0" encoding="..."?&gt; <br>
     * &lt;mcr_results&gt; <br>
     * &lt;mcr_result host=" <em>host</em> id=" <em>MCRObjectId</em>"
     * rank=" <em>rank</em>" &gt; <br>
     * &lt;mycore...&gt; <br>
     * ... <br>
     * &lt;/mycore...&gt; <br>
     * &lt;/mcr_result&gt; <br>
     * &lt;/mcr_results&gt; <br>
     *
     * @return the result collection as a JDOM document.
     */
    public final org.jdom.Document exportAllToDocument() {
        org.jdom.Element root = new org.jdom.Element(TAG_RESULTS);
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink",
                MCRDefaults.XLINK_URL));
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",
                MCRDefaults.XSI_URL));
        org.jdom.Document doc = new org.jdom.Document(root);
        for (int i = 0; i < rank.size(); i++) {
            org.jdom.Element res = new org.jdom.Element(TAG_RESULT);
            res.setAttribute(ATTR_HOST, ((String) host.get(i)).trim());
            res.setAttribute(ATTR_ID, ((String) mcrId.get(i)).trim());
            res.setAttribute(ATTR_RANK, rank.get(i).toString());
            res.setAttribute(ATTR_PRED,
                    (((((getStatus(i) >> 1)) % 2) == 1) ? "true" : "false"));
            res.setAttribute(ATTR_SUCC, ((((getStatus(i)) % 2) == 1) ? "true"
                    : "false"));
            org.jdom.Element tmp = (Element) ((Element) xml.get(i)).clone();
            res.addContent(tmp);
            root.addContent(res);
        }
        return doc;
    }

    /**
     * This methode return a well formed XML stream of the result collection as
     * a JDOM document. <br>
     * &lt;?xml version="1.0" encoding="..."?&gt; <br>
     * &lt;mcr_results&gt; <br>
     * &lt;mcr_result host=" <em>host</em> id=" <em>MCRObjectId</em>"
     * rank=" <em>rank</em>" &gt; <br>
     * &lt;mycore...&gt; <br>
     * ... <br>
     * &lt;/mycore...&gt; <br>
     * &lt;/mcr_result&gt; <br>
     * &lt;/mcr_results&gt; <br>
     *
     * @return the result collection as a JDOM document.
     * @exception IOException
     *                if an error in the XMLOutputter was occured
     */
    public final byte[] exportAllToByteArray() throws IOException {
        org.jdom.Document doc = exportAllToDocument();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(os);
        XMLOutputter op = new XMLOutputter(Format.getRawFormat().setEncoding(
                defaultEncoding));
        op.output(doc, bos);
        bos.close();
        return os.toByteArray();
    }

    /**
     * This methode return a well formed XML stream as a JDOM document. <br>
     * &lt;?xml version="1.0" encoding="..."?&gt; <br>
     * &lt;mcr_results&gt; <br>
     * &lt;mcr_result host=" <em>host</em> id=" <em>MCRObjectId</em>"
     * rank=" <em>rank</em>" &gt; <br>
     * &lt;mycore...&gt; <br>
     * ... <br>
     * &lt;/mycore...&gt; <br>
     * &lt;/mcr_result&gt; <br>
     * &lt;/mcr_results&gt; <br>
     *
     * @param index
     *            the index number of the element
     * @return one result as a JDOM document. If index is out of border an empty
     *         body was returned.
     */
    public final org.jdom.Document exportElementToDocument(int index) {
        org.jdom.Element root = new org.jdom.Element(TAG_RESULTS);
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink",
                MCRDefaults.XLINK_URL));
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",
                MCRDefaults.XSI_URL));
        org.jdom.Document doc = new org.jdom.Document(root);
        if ((index >= 0) && (index <= rank.size())) {
            org.jdom.Element res = new org.jdom.Element(TAG_RESULT);
            res.setAttribute(ATTR_HOST, (String) host.get(index));
            res.setAttribute(ATTR_ID, (String) mcrId.get(index));
            res.setAttribute(ATTR_RANK, rank.get(index).toString());
            res
                    .setAttribute(ATTR_PRED,
                            (((((getStatus(index) >> 1)) % 2) == 1) ? "true"
                                    : "false"));
            res.setAttribute(ATTR_SUCC,
                    ((((getStatus(index)) % 2) == 1) ? "true" : "false"));
            org.jdom.Element tmp = (Element) ((Element) xml.get(index)).clone();
            res.addContent(tmp);
            root.addContent(res);
        }
        return doc;
    }

    /**
     * This methode return a well formed XML stream as a byte array. <br>
     * &lt;?xml version="1.0" encoding="..."?&gt; <br>
     * &lt;mcr_results&gt; <br>
     * &lt;mcr_result host=" <em>host</em> id=" <em>MCRObjectId</em>"
     * rank=" <em>rank</em>" &gt; <br>
     * &lt;mycore...&gt; <br>
     * ... <br>
     * &lt;/mycore...&gt; <br>
     * &lt;/mcr_result&gt; <br>
     * &lt;/mcr_results&gt; <br>
     *
     * @param index
     *            the index number of the element
     * @exception IOException
     *                if an error in the XMLOutputter was occured
     * @return one result as a JDOM document. If index is out of border an empty
     *         body was returned.
     */
    public final byte[] exportElementToByteArray(int index) throws IOException {
        org.jdom.Document doc = exportElementToDocument(index);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(os);
        XMLOutputter op = new XMLOutputter(Format.getCompactFormat()
                .setEncoding(defaultEncoding));
        op.output(doc, bos);
        bos.close();
        return os.toByteArray();
    }

    /**
     * This methode return a MCRXMLContainer. <br>
     * &lt;?xml version="1.0"
     *
     * @param index
     *            the index number of the element
     * @return one result as MCRXMLContainer. If index is out of border an
     *         Container was returned.
     */
    public final MCRXMLContainer exportElementToContainer(int index) {
        MCRXMLContainer returns = new MCRXMLContainer();
        returns.host.add(host.get(index));
        returns.mcrId.add(mcrId.get(index));
        returns.rank.add(rank.get(index));
        returns.xml.add(xml.get(index));
        return returns;
    }

    /**
     * This methode import a well formed XML stream of results as byte array and
     * add it to an existing list. in form of <br>
     * &lt;?xml version="1.0" encoding="..."?&gt; <br>
     * &lt;mcr_results&gt; <br>
     * &lt;mcr_result host=" <em>host</em> id=" <em>MCRObjectId</em>"
     * rank=" <em>rank</em>" &gt; <br>
     * &lt;mycore...&gt; <br>
     * ... <br>
     * &lt;/mycore...&gt; <br>
     * &lt;/mcr_result&gt; <br>
     * &lt;/mcr_results&gt; <br>
     *
     * @param in
     *            XML input stream as InputStream
     * @exception MCRException
     *                a MyCoRe error is occured
     * @exception org.jdom.JDOMException
     *                cant read the byte array as XML
     */
    public final void importElements(InputStream in) throws MCRException,
            JDOMException, IOException {
        org.jdom.Document jdom = SAX_BUILDER.build(in);
        importElements(jdom);
    }

    /**
     * This methode import a well formed XML stream of results as byte array and
     * add it to an existing list. in form of <br>
     * &lt;?xml version="1.0" encoding="..."?&gt; <br>
     * &lt;mcr_results&gt; <br>
     * &lt;mcr_result host=" <em>host</em> id=" <em>MCRObjectId</em>"
     * rank=" <em>rank</em>" &gt; <br>
     * &lt;mycore...&gt; <br>
     * ... <br>
     * &lt;/mycore...&gt; <br>
     * &lt;/mcr_result&gt; <br>
     * &lt;/mcr_results&gt; <br>
     *
     * @param jdom
     *            the XML input as a JDom Object
     * @exception MCRException
     *                a MyCoRe error is occured
     * @exception org.jdom.JDOMException
     *                cant read the byte array as XML
     */
    public final void importElements(org.jdom.Document jdom)
            throws MCRException, org.jdom.JDOMException {
        org.jdom.Element root = jdom.getRootElement();
        if (!root.getName().equals(TAG_RESULTS)) {
            throw new MCRException("The input is not an MCRXMLContainer.");
        }
        List list = root.getChildren(TAG_RESULT);
        int irank = 0;
        Element curElem;
        for (int i = 0; i < list.size(); i++) {
            curElem = (org.jdom.Element) list.get(i);
            try {
                irank = Integer.parseInt(curElem.getAttributeValue(ATTR_RANK));
            } catch (NumberFormatException e) {
                throw new MCRException(ERROR_TEXT, e);
            }
            add(curElem.getAttributeValue(ATTR_HOST), curElem
                    .getAttributeValue(ATTR_ID), irank, (Element) curElem
                    .getChildren().get(0));
        }
    }

    /**
     * This method imports another MCRXMLContainer and add it to the existing
     * list.
     *
     * @param in
     *            other list as input
     */
    public final void importElements(MCRXMLContainer in) {
        for (int i = 0; i < in.size(); i++) {
            add(in.getHost(i), in.getId(i), in.getRank(i), in.getXML(i));
        }
    }

    /**
     * This methode print the content of this MCRXMLContainer as an XML String.
     *
     * @exception IOException
     *                if an error in the XMLOutputter was occured
     */
    public final void debug() throws IOException {
        LOGGER.debug("Debug of MCRXMLContainer");
        LOGGER.debug("============================");
        LOGGER.debug("Size = " + size());
        LOGGER.debug(new String(exportAllToByteArray()));
    }

    private final void resetStatus() {
        if (size() != status.size()) {
            int in_status = 0;
            status.clear();
            for (int i = 0; i < size(); i++) {
                if (status.size() > 0) {
                    status.set((status.size() - 1), new Integer(
                            ((Integer) status.get(status.size() - 1))
                                    .intValue() + 1));
                    in_status = 2;
                }
                status.add(new Integer(in_status));
                LOGGER
                        .info("MCRXMLContainer: refreshing status : "
                                + in_status);
            }
        }
    }

    /**
     * sorts the entries in the XMLContainer with ascending order.
     *
     * @see org.mycore.common.MCRSortable#sort(MCRXMLSortInterface)
     * @throws MCRException
     *             if sorting fails
     */
    public void sort(MCRXMLSortInterface sorter) throws MCRException {
        /* do some sorting here */
        sort(sorter, false);
    }

    /**
     * sorts the entries in the XMLContainer with given order.
     *
     * @param reversed
     *            true if descending order, fals otherwise
     * @see org.mycore.common.MCRSortable#sort(MCRXMLSortInterface, boolean)
     * @throws MCRException
     *             if sorting fails
     */
    public synchronized void sort(MCRXMLSortInterface sorter, boolean reversed)
            throws MCRException {
        /* do some sorting here */

        //pack myself into the sorter
        sorter.add(this);
        //make myself clear
        this.host.clear();
        this.mcrId.clear();
        this.rank.clear();
        this.status.clear();
        this.xml.clear();
        //get the sorted results
        Object[] result = sorter.sort(reversed);
        //import every single MCRXMLContainer in the new order
        for (int i = 0; i < result.length; i++) {
            importElements((MCRXMLContainer) result[i]);
        }
    }

    /**
     * permutates the entries in the XMLContainer
     *
     * @param order The new order of the elements. Each entry of the array references the object (by index) of
     *              the element that should be at this position in the new object.
     */
    public synchronized void permutate(int order[]) throws MCRException
    {
        /* sanity check */
        if(order.length != this.host.size() ||
           order.length != this.mcrId.size() ||
           order.length != this.rank.size() ||
           order.length != this.xml.size() ||
           order.length != this.status.size()) {
            throw new MCRException("internal error: sizes don't match: "+ order.length + "!=" + this.host.size());
        }
        ArrayList host = new ArrayList(order.length);
        ArrayList mcrId = new ArrayList(order.length);
        ArrayList rank = new ArrayList(order.length);
        ArrayList xml = new ArrayList(order.length);
        ArrayList status = new ArrayList(order.length);
        int t;
        for(t=0;t<order.length;t++) {
            host.add(t, this.host.get(order[t]));
            mcrId.add(t, this.mcrId.get(order[t]));
            rank.add(t, this.rank.get(order[t]));
            xml.add(t, this.xml.get(order[t]));
            status.add(t, this.status.get(order[t]));
        }
        this.host = host;
        this.mcrId = mcrId;
        this.rank = rank;
        this.xml = xml;
        this.status = status;
    }

    /**
     * makes a copy of MCRXMLContainer
     *
     * @see java.lang.Object#clone()
     */
    public synchronized Object clone() {
        MCRXMLContainer clone = new MCRXMLContainer();
        clone.defaultEncoding = this.defaultEncoding.intern();
        clone.host = (ArrayList) this.host.clone();
        clone.mcrId = (ArrayList) this.mcrId.clone();
        clone.rank = (ArrayList) this.rank.clone();
        clone.status = (ArrayList) this.status.clone();
        clone.xml = (ArrayList) this.xml.clone();
        return clone;
    }

    /**
     * Removes everything after the n-th Element if existing
     *
     * @param newsize
     *            the new size of the MCRXMLContainer
     */
    public synchronized void cutDownTo(int newsize) {
        if (newsize < size()) {
            host = new ArrayList(host.subList(0, newsize));
            mcrId = new ArrayList(mcrId.subList(0, newsize));
            rank = new ArrayList(rank.subList(0, newsize));
            status = new ArrayList(status.subList(0, newsize));
            xml = new ArrayList(xml.subList(0, newsize));
            resetStatus();
        }
    }
}
