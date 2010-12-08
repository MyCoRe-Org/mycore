/*
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.oai;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Implements an OAI-PMH 2.0 Data Provider as a servlet.
 * 
 * @author Frank L\u00fctzenkirchen
 */
public class MCROAIDataProvider extends MCRServlet {
    protected final static Logger LOGGER = Logger.getLogger(MCROAIDataProvider.class);

    protected void logRequest(HttpServletRequest req) {
        StringBuffer log = new StringBuffer(this.getServletName());
        for (Iterator it = req.getParameterMap().keySet().iterator(); it.hasNext();) {
            String name = (String) it.next();
            for (String value : req.getParameterValues(name))
                log.append(" ").append(name).append("=").append(value);
        }
        LOGGER.info(log.toString());
    }

    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        if (myBaseURL == null)
            myBaseURL = getBaseURL() + request.getServletPath().substring(1);

        logRequest(request);

        String[] verb = request.getParameterValues("verb");
        MCRVerbHandler handler = null;

        if ((verb == null) || (verb.length == 0))
            handler = new MCRBadVerbHandler(this, "Missing required argument 'verb'");
        else if (verb.length > 1)
            handler = new MCRBadVerbHandler(this, "Multiple 'verb' arguments in request");
        else if (verb[0].trim().length() == 0)
            handler = new MCRBadVerbHandler(this, "Required argument 'verb' is empty");
        else if (MCRIdentifyHandler.VERB.equals(verb[0]))
            handler = new MCRIdentifyHandler(this);
        else if (MCRGetRecordHandler.VERB.equals(verb[0]))
            handler = new MCRGetRecordHandler(this);
        else if (MCRListMetadataFormatsHandler.VERB.equals(verb[0]))
            handler = new MCRListMetadataFormatsHandler(this);
        else if (MCRListSetsHandler.VERB.equals(verb[0]))
            handler = new MCRListSetsHandler(this);
        else if (MCRListRecordsHandler.VERB.equals(verb[0]))
            handler = new MCRListRecordsHandler(this);
        else if (MCRListIdentifiersHandler.VERB.equals(verb[0]))
            handler = new MCRListIdentifiersHandler(this);
        else
            handler = new MCRBadVerbHandler(this, "Bad verb: " + verb[0]);

        Document response = handler.handle(request.getParameterMap());

        job.getResponse().setContentType("text/xml");
        XMLOutputter xout = new XMLOutputter();
        xout.setFormat(Format.getPrettyFormat().setEncoding("UTF-8"));
        xout.output(response, job.getResponse().getOutputStream());
    }

    private MCROAIAdapter adapter;

    private String repositoryName;

    private String repositoryIdentifier;

    private String adminEmail;

    /** The earliest datestamp supported by this data provider instance. */
    private static String EARLIEST_Datestamp;
    static {
        try {
            Date compareDate = new Date();
            long start = System.currentTimeMillis();
            List<String> idList = MCRXMLMetadataManager.instance().listIDs();
            for (String id : idList) {
                Date dateCreated = MCRMetadataManager.retrieve(MCRObjectID.getInstance(id)).getService()
                        .getDate(MCRObjectService.DATE_TYPE_CREATEDATE);
                if (dateCreated.before(compareDate)) {
                    compareDate = dateCreated;
                }
            }
            LOGGER.info("Checked " + idList.size() + " objects in " + ((System.currentTimeMillis() - start) / 1000) + " ms.");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(TimeZone.getDefault());
            EARLIEST_Datestamp = sdf.format(compareDate);
        } catch (Exception ex) {
            LOGGER.error("Error occured while examining create date of first created object", ex);
            EARLIEST_Datestamp = "1970-01-01";
        }
    }

    private String recordSampleID;

    private String deletedRecord;

    private String myBaseURL;

    /**
     * List of metadata formats supported by this data provider instance.
     */
    private List<MCRMetadataFormat> metadataFormats = new ArrayList<MCRMetadataFormat>();

    private String prefix;

    String getPrefix() {
        return prefix;
    }

    public void init() throws ServletException {
        super.init();

        MCRConfiguration config = MCRConfiguration.instance();
        prefix = "MCR.OAIDataProvider." + getServletName() + ".";

        repositoryName = config.getString(prefix + "RepositoryName");
        repositoryIdentifier = config.getString(prefix + "RepositoryIdentifier");
        adminEmail = config.getString(prefix + "AdminEmail");

        recordSampleID = config.getString(prefix + "RecordSampleID");
        deletedRecord = config.getString(prefix + "DeletedRecord");

        adapter = (MCROAIAdapter) (config.getInstanceOf(prefix + "Adapter"));
        adapter.init(prefix + "Adapter.");

        String formats = config.getString(prefix + "MetadataFormats");
        StringTokenizer st = new StringTokenizer(formats, ", ");
        while (st.hasMoreTokens())
            metadataFormats.add(MCRMetadataFormat.getFormat(st.nextToken()));
    }

    MCROAIAdapter getAdapter() {
        return adapter;
    }

    String getRepositoryName() {
        return repositoryName;
    }

    String getRepositoryIdentifier() {
        return repositoryIdentifier;
    }

    /**
     * Returns the base URL of this data provider instance
     */
    String getOAIBaseURL() {
        return myBaseURL;
    }

    /**
     * Returns the earliest datestamp supported by this data provider instance.
     * That is the guaranteed lower limit of all datestamps recording changes,
     * modifications, or deletions in the repository. A repository must not use
     * datestamps lower than the one specified by the content of the
     * earliestDatestamp element. Configuration is done using a property, for
     * example MCR.OAIDataProvider.OAI.EarliestDatestamp=1970-01-01
     */
    String getEarliestDatestamp() {
        return EARLIEST_Datestamp;
    }

    String getRecordSampleID() {
        return recordSampleID;
    }

    String getDeletedRecord() {
        return deletedRecord;
    }

    String getAdminEmail() {
        return adminEmail;
    }

    /**
     * Returns the metadata formats supported by this data provider instance.
     * For each instance, a configuration property lists the prefixes of all
     * supported formats, for example
     * MCR.OAIDataProvider.OAI.MetadataFormats=oai_dc Each metadata format must
     * be globally configured with its prefix, schema and namespace, for example
     * MCR.OAIDataProvider.MetadataFormat.oai_dc.Schema=http://www.openarchives.
     * org/OAI/2.0/oai_dc.xsd
     * MCR.OAIDataProvider.MetadataFormat.oai_dc.Namespace
     * =http://www.openarchives.org/OAI/2.0/oai_dc/
     * 
     * @see MCRMetadataFormat
     */
    List<MCRMetadataFormat> getMetadataFormats() {
        return metadataFormats;
    }
}
