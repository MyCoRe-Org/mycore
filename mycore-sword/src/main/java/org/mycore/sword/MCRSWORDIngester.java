package org.mycore.sword;

import java.io.IOException;

import org.mycore.frontend.servlets.MCRServletJob;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;

/**
 * A instance of this class should be able to perform a persist of a given sword
 * deposit.
 * 
 * @author Nils Verheyen
 * 
 */
public interface MCRSWORDIngester {

    /**
     * Performs a {@link Deposit} done by a {@link MCRSWORDDepositServlet}.
     * 
     * @param deposit
     *            contains the {@link Deposit} object which should be deposited
     * @param response
     *            contains the SWORDResponse to use for adding deposit
     *            information into
     * @param verboseInfo
     *            verbose info to fill if the client asked for it.
     * @param job
     *            contains the {@link MCRServletJob} delivered from the
     *            servlets
     * @see MCRSWORDServer
     * @throws IOException
     *             thrown if any {@link IOException} occurred
     * @throws SWORDException
     *             thrown if any internal error occurred during ingest
     * @throws SWORDErrorException
     *             thrown if any information given inside deposit or package
     *             descriptor is invalid
     */
    void ingest(Deposit deposit, SWORDEntry response, StringBuffer verboseInfo, MCRServletJob job) throws IOException, SWORDException, SWORDErrorException;

}
