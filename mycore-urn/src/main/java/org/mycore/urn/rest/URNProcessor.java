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

package org.mycore.urn.rest;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.urn.epicurlite.EpicurLite;
import org.mycore.urn.epicurlite.IEpicurLiteProvider;
import org.mycore.urn.hibernate.MCRURN;
import org.mycore.urn.services.MCRIURNProvider;
import org.mycore.urn.services.MCRURNManager;

@Deprecated
public class URNProcessor {

    private static final Logger LOGGER = LogManager.getLogger(URNProcessor.class);

    protected URNServer server;

    protected IEpicurLiteProvider epicurLiteProvider;

    protected MCRIURNProvider urnProvider;

    @SuppressWarnings("unchecked")
    public URNProcessor(URNServer server, IEpicurLiteProvider epicurLiteProvider)
        throws ClassNotFoundException, InstantiationException,
        IllegalAccessException {
        this.server = server;
        this.epicurLiteProvider = epicurLiteProvider;

        String className = MCRConfiguration.instance().getString("MCR.URN.Provider.Class");
        Class<MCRIURNProvider> c = (Class<MCRIURNProvider>) Class.forName(className);
        urnProvider = c.newInstance();
    }

    protected void process(MCRURN urn) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.beginTransaction();
        try {
            int headStatus = server.head(urn);
            EpicurLite elp = null;
            switch (headStatus) {
                // urn already registered
                case HttpStatus.SC_NO_CONTENT:
                    LOGGER.info("URN {} is already registered, performing update of url", urn);
                    if (!urn.isRegistered()) {
                        // setting attribute in database
                        urn.setRegistered(true);
                    }
                    elp = epicurLiteProvider.getEpicurLite(urn);

                    if (elp.getUrl() == null) {
                        LOGGER.warn("The url for {} is " + null + ". Canceling request", urn);
                        return;
                    }

                    // performing update
                    int postStatus = server.post(elp);
                    switch (postStatus) {
                        case HttpStatus.SC_NO_CONTENT:
                            LOGGER.info("URN {} updated to {}", urn, elp.getUrl());
                            if (urn.toString().contains(urnProvider.getNISS() + "-dfg")) {
                                modifyToOriginalURN(urn);
                                urn.setDfg(true);
                            }
                            // update record in database
                            MCRURNManager.update(urn);
                            break;
                        default:
                            LOGGER.warn("URN {} could not be updated. Status {}", urn, postStatus);
                    }

                    break;
                // urn not registered
                case HttpStatus.SC_NOT_FOUND:
                    LOGGER.info("URN {} is NOT registered", urn);
                    elp = epicurLiteProvider.getEpicurLite(urn);
                    int putStatus = server.put(elp);
                    switch (putStatus) {
                        case HttpStatus.SC_CREATED:
                            LOGGER.info("URN {} registered to {}", urn, elp.getUrl());
                            if (!urn.toString().contains(urnProvider.getNISS() + "-dfg")) {
                                urn.setRegistered(true);
                            } else {
                                modifyToOriginalURN(urn);
                                urn.setDfg(true);
                            }
                            MCRURNManager.update(urn);
                            break;
                        case HttpStatus.SC_SEE_OTHER:
                            LOGGER.warn(
                                "URN {} could NOT registered to {}\nAt least one of the given URLs is already registered under another URN",
                                urn, elp.getUrl());
                            break;
                        case HttpStatus.SC_CONFLICT:
                            LOGGER.warn(
                                "URN {} could NOT registered to {}\nConflict: URN-Record already exists and can not be created again",
                                urn, elp.getUrl());
                            break;
                        default:
                            LOGGER.warn("Could not handle urn request: status={}, urn={}, url={}\nEpicur Lite:\n\n{}",
                                putStatus, urn, elp.getUrl(),
                                new XMLOutputter(Format.getPrettyFormat()).outputString(elp.getEpicurLite()));
                    }
                    break;
                default:
                    LOGGER.warn("Could not handle request for urn {} Status code {}", urn, headStatus);
            }
        } catch (Exception ex) {
            LOGGER.error("Error while registering urn {}", urn, ex);
            session.rollbackTransaction();
        } finally {
            if (session.isTransactionActive()) {
                session.commitTransaction();
            }
            session.close();
        }
    }

    /**
     * @param dfgURN
     */
    private void modifyToOriginalURN(MCRURN dfgURN) {
        // set dfg state 
        String spec = dfgURN.getKey().getMcrurn().replace(urnProvider.getNISS() + "-dfg", urnProvider.getNISS());
        //cut off checksum
        spec = spec.substring(0, spec.length() - 1);
        dfgURN.getKey().setMcrurn(spec);
    }

}
