/*
 * $Revision$ 
 * $Date$
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

package org.mycore.oai;

import static org.mycore.oai.MCROAIConstants.ERROR_BAD_RESUMPTION_TOKEN;
import static org.mycore.oai.MCROAIConstants.ERROR_NO_RECORDS_MATCH;
import static org.mycore.oai.MCROAIConstants.NS_OAI;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRMetaISO8601Date;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;

/**
 * Represents complete results to a ListRecords or ListIdentifiers request. 
 * 
 * @author Frank L\u00fctzenkirchen
 */
public class MCROAIResults {
    private String token;

    private List<String> ids;

    private MCRMetadataFormat format;

    private Date expirationDate;

    private static Map<String, MCROAIResults> tokenMap = new HashMap<String, MCROAIResults>();

    private static int partitionSize;

    private static int maxAge;

    protected final static Logger LOGGER = Logger.getLogger(MCROAIResults.class);

    static {
        String prefix = "MCR.OAIDataProvider.ResumptionTokens.";
        partitionSize = MCRConfiguration.instance().getInt(prefix + "PartitionSize", 50);
        maxAge = MCRConfiguration.instance().getInt(prefix + "MaxAge", 30) * 60 * 1000;

        TimerTask tt = new TimerTask() {
            @SuppressWarnings("rawtypes")
            public void run() {
                for (Iterator it = tokenMap.keySet().iterator(); it.hasNext();) {
                    String token = (String) (it.next());
                    MCROAIResults results = tokenMap.get(token);
                    if ((results != null) && results.isExpired()) {
                        LOGGER.info("Removing expired resumption token " + token);
                        tokenMap.remove(token);
                    }
                }
            }
        };
        new Timer().schedule(tt, new Date(System.currentTimeMillis() + maxAge), maxAge);
    }

    MCROAIResults(MCRResults results, MCRMetadataFormat format, MCROAIDataProvider provider) {
        this.format = format;
        this.token = results.getID();

        ids = new ArrayList<String>(results.getNumHits());
        String prefix = "oai:" + provider.getRepositoryIdentifier() + ":";

        for (Iterator<MCRHit> hits = results.iterator(); hits.hasNext();) {
            MCRHit hit = hits.next();
            String identifier = prefix + hit.getID();

            List<MCRMetadataFormat> formats = provider.getAdapter().listMetadataFormats(identifier, provider.getMetadataFormats());
            if (formats.contains(format))
                ids.add(hit.getID());
        }

        if (ids.size() > partitionSize) {
            tokenMap.put(token, this);
            expirationDate = new Date(System.currentTimeMillis() + maxAge);
        }
    }

    static MCROAIResults getResults(String token) {
        if (!token.contains("@"))
            return null;
        return tokenMap.get(token.split("@")[0]);
    }

    boolean isExpired() {
        return (new Date().compareTo(expirationDate) > 0);
    }

    void addHits(MCRListDataHandler handler) {
        if (ids.size() == 0)
            handler.addError(ERROR_NO_RECORDS_MATCH, null);
        else
            addHits(handler, 0);
    }

    void addHits(MCRListDataHandler handler, String token) {
        int offset = -1;

        try {
            offset = Integer.parseInt(token.split("@")[1]);
        } catch (Exception ex) {
        }

        if (offset < 0) {
            handler.addError(ERROR_BAD_RESUMPTION_TOKEN, "Bad resumption token: " + token);
            return;
        }

        addHits(handler, offset);
    }

    private void addHits(MCRListDataHandler handler, int offset) {
        int max = Math.min(ids.size(), offset + partitionSize);

        for (int i = offset; i < max; i++)
            handler.addHit(ids.get(i), format);

        if (ids.size() > partitionSize) {
            Element resTokenElem = new Element("resumptionToken", NS_OAI);
            resTokenElem.setAttribute("completeListSize", String.valueOf(ids.size()));
            resTokenElem.setAttribute("cursor", String.valueOf(offset));
            handler.output.addContent(resTokenElem);

            if (max < ids.size()) {
                resTokenElem.setText(token + "@" + max);

                expirationDate = new Date(System.currentTimeMillis() + maxAge);
                String ed = MCRVerbHandler.buildUTCDate(expirationDate, MCRMetaISO8601Date.IsoFormat.COMPLETE_HH_MM_SS);
                resTokenElem.setAttribute("expirationDate", ed);
            }
        }
    }
}
