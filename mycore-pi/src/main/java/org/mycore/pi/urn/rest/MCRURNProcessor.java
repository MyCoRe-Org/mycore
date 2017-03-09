package org.mycore.pi.urn.rest;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRURNUtils;

/**
 * Created by chi on 25.01.17.
 * @author shermann
 * @author Huu Chi Vu
 */
public class MCRURNProcessor {
//    private static final Logger LOGGER = LogManager.getLogger();
//
//    protected MCRURNServer server;
//
//    protected MCREpicurLiteProvider epicurLiteProvider;
//
//    protected MCRIURNProvider urnProvider;
//
//    @SuppressWarnings("unchecked")
//    public MCRURNProcessor(MCRURNServer server, MCREpicurLiteProvider epicurLiteProvider) throws ClassNotFoundException, InstantiationException,
//            IllegalAccessException {
//        this.server = server;
//        this.epicurLiteProvider = epicurLiteProvider;
//
//        String className = MCRConfiguration.instance().getInstanceOf("MCR.URN.Provider.Class", MCRIURNProvider.class);
//        Class<MCRIURNProvider> c = (Class<MCRIURNProvider>) Class.forName(className);
//        urnProvider = c.newInstance();
//    }
//
//    protected void process(MCRPIRegistrationInfo urnInfo) {
//        MCRSession session = MCRSessionMgr.getCurrentSession();
//        session.beginTransaction();
//        try {
//            int headStatus = server.head(urnInfo);
//            MCREpicurLite elp = null;
//            switch (headStatus) {
//            // urnInfo already registered
//            case HttpStatus.SC_NO_CONTENT:
//                LOGGER.info("URN " + urnInfo.getIdentifier() + " is already registered, performing update of url");
//                if (!urnInfo.isRegistered()) {
//                    // setting attribute in database
//                    urnInfo.setRegistered(true);
//                }
//                elp = epicurLiteProvider.toXML(urnInfo);
//
//                if (elp.getUrl() == null) {
//                    LOGGER.warn("The url for " + urnInfo + " is " + null + ". Canceling request");
//                    return;
//                }
//
//                // performing update
//                int postStatus = server.post(elp);
//                switch (postStatus) {
//                case HttpStatus.SC_NO_CONTENT:
//                    LOGGER.info("URN " + urnInfo + " updated to " + elp.getUrl());
//                    if (urnInfo.toString().contains("-dfg")) {
//                        modifyToOriginalURN(urnInfo);
//                        urnInfo.setDfg(true);
//                    }
//                    // update record in database
//                    MCRURNManager.update(urnInfo);
//                    break;
//                default:
//                    LOGGER.warn("URN " + urnInfo + " could not be updated. Status " + postStatus);
//                }
//
//                break;
//            // urnInfo not registered
//            case HttpStatus.SC_NOT_FOUND:
//                LOGGER.info("URN " + urnInfo + " is NOT registered");
//                elp = epicurLiteProvider.toXML(urnInfo);
//                int putStatus = server.put(elp);
//                switch (putStatus) {
//                case HttpStatus.SC_CREATED:
//                    LOGGER.info("URN " + urnInfo + " registered to " + elp.getUrl());
//                    if (!urnInfo.toString().contains(urnProvider.getNISS() + "-dfg")) {
//                        urnInfo.setRegistered(true);
//                    } else {
//                        modifyToOriginalURN(urnInfo);
//                        urnInfo.setDfg(true);
//                    }
//                    MCRURNManager.update(urnInfo);
//                    break;
//                case HttpStatus.SC_SEE_OTHER:
//                    LOGGER.warn("URN " + urnInfo + " could NOT registered to " + elp.getUrl() + "\n"
//                                        + "At least one of the given URLs is already registered under another URN");
//                    break;
//                case HttpStatus.SC_CONFLICT:
//                    LOGGER.warn("URN " + urnInfo + " could NOT registered to " + elp.getUrl() + "\n"
//                                        + "Conflict: URN-Record already exists and can not be created again");
//                    break;
//                default:
//                    LOGGER.warn("Could not handle urnInfo request: status=" + putStatus + ", urnInfo=" + urnInfo + ", url=" + elp.getUrl()
//                                        + "\nEpicur Lite:\n\n" + new XMLOutputter(Format.getPrettyFormat()).outputString(elp.toXML()));
//                }
//                break;
//            default:
//                LOGGER.warn("Could not handle request for urnInfo " + urnInfo + " Status code " + headStatus);
//            }
//        } catch (Exception ex) {
//            LOGGER.error("Error while registering urnInfo" + ex);
//            session.rollbackTransaction();
//        } finally {
//            if (session.isTransactionActive()) {
//                session.commitTransaction();
//            }
//            session.close();
//        }
//    }
//
//    /**
//     * @param dfgURN
//     */
//    private void modifyToOriginalURN(MCRURN dfgURN) {
//        // set dfg state
//        String spec = dfgURN.getKey().getMcrurn().replace(urnProvider.getNISS() + "-dfg", urnProvider.getNISS());
//        //cut off checksum
//        spec = spec.substring(0, spec.length() - 1);
//        dfgURN.getKey().setMcrurn(spec);
//    }
}
