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
                    LOGGER.info("URN " + urn + " is already registered, performing update of url");
                    if (!urn.isRegistered()) {
                        // setting attribute in database
                        urn.setRegistered(true);
                    }
                    elp = epicurLiteProvider.getEpicurLite(urn);

                    if (elp.getUrl() == null) {
                        LOGGER.warn("The url for " + urn + " is " + null + ". Canceling request");
                        return;
                    }

                    // performing update
                    int postStatus = server.post(elp);
                    switch (postStatus) {
                        case HttpStatus.SC_NO_CONTENT:
                            LOGGER.info("URN " + urn + " updated to " + elp.getUrl());
                            if (urn.toString().contains(urnProvider.getNISS() + "-dfg")) {
                                modifyToOriginalURN(urn);
                                urn.setDfg(true);
                            }
                            // update record in database
                            MCRURNManager.update(urn);
                            break;
                        default:
                            LOGGER.warn("URN " + urn + " could not be updated. Status " + postStatus);
                    }

                    break;
                // urn not registered
                case HttpStatus.SC_NOT_FOUND:
                    LOGGER.info("URN " + urn + " is NOT registered");
                    elp = epicurLiteProvider.getEpicurLite(urn);
                    int putStatus = server.put(elp);
                    switch (putStatus) {
                        case HttpStatus.SC_CREATED:
                            LOGGER.info("URN " + urn + " registered to " + elp.getUrl());
                            if (!urn.toString().contains(urnProvider.getNISS() + "-dfg")) {
                                urn.setRegistered(true);
                            } else {
                                modifyToOriginalURN(urn);
                                urn.setDfg(true);
                            }
                            MCRURNManager.update(urn);
                            break;
                        case HttpStatus.SC_SEE_OTHER:
                            LOGGER.warn("URN " + urn + " could NOT registered to " + elp.getUrl() + "\n"
                                + "At least one of the given URLs is already registered under another URN");
                            break;
                        case HttpStatus.SC_CONFLICT:
                            LOGGER.warn("URN " + urn + " could NOT registered to " + elp.getUrl() + "\n"
                                + "Conflict: URN-Record already exists and can not be created again");
                            break;
                        default:
                            LOGGER.warn("Could not handle urn request: status=" + putStatus + ", urn=" + urn + ", url="
                                + elp.getUrl()
                                + "\nEpicur Lite:\n\n"
                                + new XMLOutputter(Format.getPrettyFormat()).outputString(elp.getEpicurLite()));
                    }
                    break;
                default:
                    LOGGER.warn("Could not handle request for urn " + urn + " Status code " + headStatus);
            }
        } catch (Exception ex) {
            LOGGER.error("Error while registering urn" + ex);
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
